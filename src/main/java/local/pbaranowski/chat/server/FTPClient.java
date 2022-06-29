package local.pbaranowski.chat.server;

import local.pbaranowski.chat.transportlayer.MessageInternetFrame;
import local.pbaranowski.chat.transportlayer.Transcoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;

import static local.pbaranowski.chat.constants.Constants.*;
import static local.pbaranowski.chat.server.MessageType.*;


@Slf4j
@RequiredArgsConstructor
public class FTPClient implements Client, Runnable {
    private final MessageRouter messageRouter;
    private final Transcoder<MessageInternetFrame> transcoder;
    private final FTPStorage ftpStorage;

    @Override
    public String getName() {
        return FTP_ENDPOINT_NAME;
    }

    @Override
    public void write(Message message) {
        LogSerializer serializer = new CSVLogSerializer();
        log.info("{}", serializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_APPEND_FILE:
                try {
                    ftpStorage.appendFile(message);
                } catch (MaxFilesExceededException e) {
                    messageRouter.sendMessage(MESSAGE_TEXT, message.getReceiver(), message.getSender(), "ERROR: " + e.getClass().getSimpleName());
                }
                break;
            case MESSAGE_DOWNLOAD_FILE:
                getFile(message);
                break;
            case MESSAGE_DELETE_FILE:
                ftpStorage.deleteFile(message);
                break;
            case MESSAGE_DELETE_ALL_FILES_ON_CHANNEL:
                ftpStorage.deleteAllFilesOnChannel(message);
                break;
            case MESSAGE_LIST_FILES:
                listFiles(message);
                break;
            default:
                break;
        }
    }

    // MESSAGE_SEND_CHUNK_TO_CLIENT dla ramek z danymi i MESSAGE_PUBLISH_FILE dla końca transferu pliku
    // format payload dla MESSAGE_DOWNLOAD_FILE: idPliku [spacja] nazwaPlikuPodJakąZapisujeUżytkownikUSiebie
    @SneakyThrows
    private void getFile(Message message) {
        try (InputStream inputStream = ftpStorage.getFile(message)) {
            if (inputStream == null) {
                messageRouter.sendMessage(MESSAGE_TEXT, FTP_ENDPOINT_NAME, message.getSender(), "ERROR: No file with id = " + message.getPayload().split("[ ]+")[0]);
            } else {
                MessageInternetFrame frame = new MessageInternetFrame();
                frame.setDestinationName(message.getPayload().split("[ ]+")[1]); // nazwa pliku pod jaką chce zapisać user
                frame.setSourceName(message.getPayload().split("[ ]+")[0]); // id pliku o jaki requestuje user
                frame.setMessageType(MESSAGE_SEND_CHUNK_TO_CLIENT);
                while (inputStream.available() > 0) {
                    frame.setData(inputStream.readNBytes(256));
                    synchronized (transcoder) {
                        messageRouter.sendMessage(MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), transcoder.encodeObject(frame, MessageInternetFrame.class));
                    }
                }
                frame.setMessageType(MESSAGE_PUBLISH_FILE);
                frame.setData(null);
                synchronized (transcoder) {
                    messageRouter.sendMessage(MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), transcoder.encodeObject(frame, MessageInternetFrame.class));
                }
            }
        }
    }

    private void listFiles(Message message) {
        log.info("listFiles: {}", message.getReceiver());
        Map<String, FTPFileRecord> files = ftpStorage.getFilesOnChannel(message.getReceiver());
        files.keySet()
                .forEach(fileKey -> messageRouter.sendMessage(MESSAGE_TEXT,
                        message.getReceiver(),
                        message.getSender(),
                        FTPClientUtils.fileRecordToString(fileKey, files.get(fileKey)))
                );
    }


    // Na razie bez kolejkowania, więc tylko się subskrybuje aby dostawać wiadomości
    @Override
    public void run() {
        messageRouter.subscribe(this);
    }
}
