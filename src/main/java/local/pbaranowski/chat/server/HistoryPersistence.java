package local.pbaranowski.chat.server;

import java.util.Iterator;

public interface HistoryPersistence {
    void save(Message message);

    Iterator<String> retrieve(String user);
}
