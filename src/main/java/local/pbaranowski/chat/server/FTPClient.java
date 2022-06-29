package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.Constants;
import local.pbaranowski.chat.commons.MessageType;
import local.pbaranowski.chat.commons.transportlayer.MessageInternetFrame;
import local.pbaranowski.chat.commons.transportlayer.Transcoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;


@Slf4j
@RequiredArgsConstructor
class FTPClient implements Client, Runnable {
    private final MessageRouter messageRouter;
    private final Transcoder<MessageInternetFrame> transcoder;
    private final FileStorage fileStorage;

    @Override
    public String getName() {
        return Constants.FTP_ENDPOINT_NAME;
    }

    @Override
    public void write(Message message) {
        LogSerializer serializer = new CSVLogSerializer();
        log.info("{}", serializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_APPEND_FILE:
                try {
                    fileStorage.appendFile(message);
                } catch (MaxFilesExceededException e) {
                    messageRouter.sendMessage(MessageType.MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: " + e.getClass().getSimpleName());
                }
                break;
            case MESSAGE_DOWNLOAD_FILE:
                getFile(message);
                break;
            case MESSAGE_DELETE_FILE:
                fileStorage.deleteFile(message);
                break;
            case MESSAGE_DELETE_ALL_FILES_ON_CHANNEL:
                fileStorage.deleteAllFilesOnChannel(message);
                break;
            case MESSAGE_LIST_FILES:
                listFiles(message);
                break;
            default:
                break;
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
        try (InputStream inputStream = fileStorage.getFile(message)) {
            if (inputStream == null) {
                messageRouter.sendMessage(MessageType.MESSAGE_TEXT, Constants.FTP_ENDPOINT_NAME, message.getSender(), "ERROR: No file with id = " + message.getPayload().split("[ ]+")[0]);
            } else {
                MessageInternetFrame frame = new MessageInternetFrame();
                frame.setDestinationName(message.getPayload().split("[ ]+")[1]); // nazwa pliku pod jaką chce zapisać user
                frame.setSourceName(message.getPayload().split("[ ]+")[0]); // id pliku o jaki requestuje user
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
        Map<String, FileStorageRecord> files = fileStorage.getFilesOnChannel(message.getReceiver());
        files.keySet()
                .forEach(fileKey -> messageRouter.sendMessage(MessageType.MESSAGE_TEXT,
                        message.getReceiver(),
                        message.getSender(),
                        DiskFileStorageUtils.fileRecordToString(fileKey, files.get(fileKey)))
                );
    }

}
