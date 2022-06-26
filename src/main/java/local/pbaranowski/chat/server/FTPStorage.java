package local.pbaranowski.chat.server;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FTPStorage {
    void appendFile(Message message);

    void uploadDone(Message message);

    void removeFile(Message message);

    Map<String,FTPFileRecord> getFilesOnChannel(String channel);

    InputStream getFile(Message message);
}
