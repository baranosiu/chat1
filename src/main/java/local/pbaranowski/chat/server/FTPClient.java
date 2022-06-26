package local.pbaranowski.chat.server;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static local.pbaranowski.chat.server.MessageType.*;


@Slf4j
@RequiredArgsConstructor
public class FTPClient implements Client, Runnable {
    private final MessageRouter messageRouter;
    private final FTPStorage ftpStorage;

    @Override
    public String getName() {
        return "@ftp";
    }

    @Override
    public void write(Message message) {
        LogSerializer serializer = new CSVLogSerializer();
        log.info("{}", serializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_APPEND_FILE:
                ftpStorage.appendFile(message);
                break;
            case MESSAGE_DOWNLOAD_FILE:
                getFile(message);
                break;
            case MESSAGE_ERASE_FILE:
                ftpStorage.removeFile(message);
                break;
            case MESSAGE_LIST_FILES:
                listFiles(message);
                break;
        }
    }

    @SneakyThrows
    private void getFile(Message message) {
        try (InputStream inputStream = ftpStorage.getFile(message)) {
            if (inputStream == null) {
                messageRouter.sendMessage(new Message(MESSAGE_TEXT, "@ftp", message.getSender(), "No file with id="+message.getPayload().split("[ ]+")[1]));
            } else {
                while (inputStream.available() > 0) {
                    String base64Text = java.util.Base64.getEncoder().encodeToString(inputStream.readNBytes(256));
                    messageRouter.sendMessage(new Message(MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), base64Text + " " + message.getPayload().split("[ ]+")[1]));
                }
                messageRouter.sendMessage(new Message(MESSAGE_SEND_CHUNK_TO_CLIENT, getName(), message.getSender(), "C" + " " + message.getPayload().split("[ ]+")[1]));
            }
        }
    }

    private void listFiles(Message message) {
        log.info("listFiles: {}", message.getReceiver());
        Map<String, FTPFileRecord> files = ftpStorage.getFilesOnChannel(message.getReceiver());
        for (String fileKey : files.keySet()) {
            messageRouter.sendMessage(new Message(MESSAGE_TEXT, message.getReceiver(), message.getSender(), FTPClientUtils.fileRecordToString(fileKey, files.get(fileKey))));
        }
    }


    // Na razie bez kolejkowania, więc tylko się subskrybuje aby dostawać wiadomości
    @Override
    public void run() {
        try {
            messageRouter.subscribe(this);
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }
    }
}
