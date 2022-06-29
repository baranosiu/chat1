package local.pbaranowski.chat.server;

import java.util.Iterator;

interface HistoryPersistence {
    void save(Message message);

    Iterator<String> retrieve(String user);
}
