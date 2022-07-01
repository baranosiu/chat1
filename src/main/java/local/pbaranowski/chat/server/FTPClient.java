package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.MessageType;
import local.pbaranowski.chat.commons.transportlayer.MessageInternetFrame;
import local.pbaranowski.chat.commons.transportlayer.Transcoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.synchronizedMap;
import static local.pbaranowski.chat.commons.Constants.FTP_ENDPOINT_NAME;


@Slf4j
@RequiredArgsConstructor
class FTPClient implements Client, Runnable {
    private final MessageRouter messageRouter;
    private final Transcoder<MessageInternetFrame> transcoder;
    private final FileStorage fileStorage;
    private Map<String, String> filesInProgress = synchronizedMap(new HashMap<>());

    @Override
    public String getName() {
        return FTP_ENDPOINT_NAME;
    }

    @Override
    public void write(Message message) {
        LogSerializer serializer = new CSVLogSerializer();
        log.info("{}", serializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_REGISTER_FILE_TO_UPLOAD:
                registerFileToUpload(message);
                break;
            case MESSAGE_APPEND_FILE:
                append(message);
                break;
            case MESSAGE_PUBLISH_FILE:
                publish(message);
                break;
            case MESSAGE_DOWNLOAD_FILE:
                getFile(message);
                break;
            case MESSAGE_DELETE_FILE:
                delete(message);
                break;
            case MESSAGE_DELETE_ALL_FILES_ON_CHANNEL:
                fileStorage.deleteAllFilesOnChannel(message.getReceiver());
                break;
            case MESSAGE_LIST_FILES:
                listFiles(message);
                break;
            default:
                break;
        }
    }

    private void delete(Message message) {
        if (!fileStorage.hasFile(message.getPayload())) {
            messageRouter.sendMessage(MessageType.MESSAGE_TEXT, FTP_ENDPOINT_NAME, message.getSender(), "ERROR: No file (id = " + message.getPayload() + ")");
            return;
        }
        if (message.getSender().equals(fileStorage.getSender(message.getPayload()))) {
            fileStorage.delete(message.getPayload());
        } else {
            messageRouter.sendMessage(MessageType.MESSAGE_TEXT, FTP_ENDPOINT_NAME, message.getSender(), "ERROR: Not owner (id = " + message.getPayload() + ")");
        }
    }

    private void publish(Message message) {
        MessageInternetFrame frame;
        synchronized (transcoder) {
            frame = transcoder.decodeObject(message.getPayload(), MessageInternetFrame.class);
        }
        try {
            if (fileStorage.publish(filesInProgress.get(frame.getDestinationName()))) {
                messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "Upload done");
            } else {
                messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: Upload failed");
            }
        } catch (MaxFilesExceededException e) {
            messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: " + e.getClass().getSimpleName());
        }
        filesInProgress.remove(frame.getDestinationName());
    }

    private void append(Message message) {
        MessageInternetFrame frame;
        synchronized (transcoder) {
            frame = transcoder.decodeObject(message.getPayload(), MessageInternetFrame.class);
        }
        fileStorage.append(filesInProgress.get(frame.getDestinationName()), frame.getData());
    }

    private void registerFileToUpload(Message message) {
        MessageInternetFrame frame;
        synchronized (transcoder) {
            frame = transcoder.decodeObject(message.getPayload(), MessageInternetFrame.class);
        }
        try {
            ChannelClient destinationChannel = messageRouter.getChannelClient(frame.getDestinationName());
            if (destinationChannel == null) {
                messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: No channel " + frame.getDestinationName());
                return;
            }
            if (!destinationChannel.hasClient(message.getSender())) {
                messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: Not a member of channel " + frame.getDestinationName());
                return;
            }
            String fileStorageKey = fileStorage.requestNewKey(message.getSender(), frame.getDestinationName(), frame.getSourceName());
            String senderTransferKey = new String(frame.getData(), StandardCharsets.UTF_8);
            filesInProgress.put(senderTransferKey, fileStorageKey);
        } catch (MaxFilesExceededException e) {
            messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: " + e.getClass().getSimpleName());
        }
    }

    // Na razie bez kolejkowania, więc tylko się subskrybuje aby dostawać wiadomości
    @Override
    public void run() {
        messageRouter.subscribe(this);
    }

    // MESSAGE_SEND_CHUNK_TO_CLIENT dla ramek z danymi i MESSAGE_PUBLISH_FILE dla końca transferu pliku
    // format payload dla MESSAGE_DOWNLOAD_FILE: idPliku [spacja] nazwaPlikuPodJakąZapisujeUżytkownikUSiebie
    @SneakyThrows
    private void getFile(Message message) {
        if (!fileStorage.hasFile(message.getReceiver())) {
            messageRouter.sendMessage(MessageType.MESSAGE_TEXT, FTP_ENDPOINT_NAME, message.getSender(), "ERROR: No file (id = " + message.getReceiver() + ")");
            return;
        }
        ChannelClient destinationChannel = messageRouter.getChannelClient(fileStorage.getChannel(message.getReceiver()));
        if (destinationChannel == null || !destinationChannel.hasClient(message.getSender())) {
            messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: Not allowed");
            return;
        }

        try (InputStream inputStream = fileStorage.getFile(message.getReceiver())) {
            if (inputStream == null) {
                messageRouter.sendMessage(MessageType.MESSAGE_TEXT, FTP_ENDPOINT_NAME, message.getSender(), "ERROR: No file with id = " + message.getReceiver());
            } else {
                MessageInternetFrame frame = new MessageInternetFrame();
                frame.setDestinationName(message.getPayload()); // nazwa pliku pod jaką chce zapisać user
                frame.setSourceName(message.getReceiver()); // id pliku o jaki requestuje user
                frame.setMessageType(MessageType.MESSAGE_SEND_CHUNK_TO_CLIENT);
                while (inputStream.available() > 0) {
                    frame.setData(inputStream.readNBytes(256));
                    synchronized (transcoder) {
                        messageRouter.sendMessage(MessageType.MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), transcoder.encodeObject(frame, MessageInternetFrame.class));
                    }
                }
                frame.setMessageType(MessageType.MESSAGE_PUBLISH_FILE);
                frame.setData(null);
                synchronized (transcoder) {
                    messageRouter.sendMessage(MessageType.MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), transcoder.encodeObject(frame, MessageInternetFrame.class));
                }
            }
        }
    }

    private void listFiles(Message message) {
        log.info("listFiles: {}", message.getReceiver());
        List<String> files = fileStorage.getFilesOnChannel(message.getReceiver());
        files.stream()
                .forEach(fileKey -> messageRouter.sendMessage(MessageType.MESSAGE_TEXT,
                        message.getReceiver(),
                        message.getSender(),
                        DiskFileStorageUtils.fileRecordToString(fileKey, fileStorage.getSender(fileKey), fileStorage.getOriginalFileName(fileKey)))
                );
    }

}
