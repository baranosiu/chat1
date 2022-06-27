package local.pbaranowski.chat.server;

import java.io.InputStream;
import java.util.Map;

public interface FTPStorage {
    void appendFile(Message message) throws MaxFilesExceededException;

    void uploadDone(Message message) throws MaxFilesExceededException;

    void deleteFile(Message message);

    Map<String,FTPFileRecord> getFilesOnChannel(String channel);

    InputStream getFile(Message message);

    void deleteAllFilesOnChannel(Message message);
}
