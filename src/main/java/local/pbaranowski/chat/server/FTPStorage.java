package local.pbaranowski.chat.server;

import java.io.InputStream;
import java.util.List;

public interface FTPStorage {
    void appendFile(Message message);

    void removeFile(Message message);

    List<FTPFileRecord> getFilesOnChannel(String channel);

    InputStream getFile(Message message);
}
