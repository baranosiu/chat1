package local.pbaranowski.chat.server;

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
                    messageRouter.sendMessage(MESSAGE_TEXT,message.getReceiver(),message.getSender(),"ERROR: "+e.getClass().getSimpleName());
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

    // format payload dla MESSAGE_SEND_CHUNK_TO_CLIENT: blokBase64 [spacja] nazwaPlikuPodJakąZapisujeUżytkownikUSiebie
    // format payload dla MESSAGE_DOWNLOAD_FILE: blokBase64 [spacja] idPliku
    @SneakyThrows
    private void getFile(Message message) {
        try (InputStream inputStream = ftpStorage.getFile(message)) {
            if (inputStream == null) {
                messageRouter.sendMessage(MESSAGE_TEXT, FTP_ENDPOINT_NAME, message.getSender(), "No file with id=" + message.getPayload().split("[ ]+")[1]);
            } else {
                while (inputStream.available() > 0) {
                    String base64Text = java.util.Base64.getEncoder().encodeToString(inputStream.readNBytes(256));
                    messageRouter.sendMessage(MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), base64Text + " " + message.getPayload().split("[ ]+")[1]);
                }
                messageRouter.sendMessage(MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), FILE_TRANSFER_COMPLETED + " " + message.getPayload().split("[ ]+")[1]);
            }
        }
    }

    private void listFiles(Message message) {
        log.info("listFiles: {}", message.getReceiver());
        Map<String, FTPFileRecord> files = ftpStorage.getFilesOnChannel(message.getReceiver());
        for (String fileKey : files.keySet()) {
            messageRouter.sendMessage(MESSAGE_TEXT, message.getReceiver(), message.getSender(), FTPClientUtils.fileRecordToString(fileKey, files.get(fileKey)));
        }
    }


    // Na razie bez kolejkowania, więc tylko się subskrybuje aby dostawać wiadomości
    @Override
    public void run() {
        messageRouter.subscribe(this);
    }
}
