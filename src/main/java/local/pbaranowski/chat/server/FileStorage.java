package local.pbaranowski.chat.server;

import java.io.InputStream;
import java.util.List;

public interface FileStorage {
    String requestNewKey(String userName, String channel, String fileName) throws MaxFilesExceededException;
    void append(String key, byte[] data);
    void publish(String key) throws MaxFilesExceededException;
    void delete(String key);
    List<String> getFilesOnChannel(String channel);
    void deleteAllFilesOnChannel(String channel);
    InputStream getFile(String key);
    String getSender(String key);
    String getChannel(String key);
    String getOriginalFileName(String key);
}
