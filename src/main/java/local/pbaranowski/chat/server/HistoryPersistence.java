package local.pbaranowski.chat.server;

import java.util.Iterator;

public interface HistoryPersistence {
    void save(String user, String text);

    Iterator<String> retrieve(String user);
}
