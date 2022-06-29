package local.pbaranowski.chat.server;

interface Client {
    String getName();

    void write(Message message);

    // Używane na potrzeby kanałów czy mają jakichś użytkowników
    // Dla zwykłych użytkowników zwraca zawsze false aby "garbageCollector" clientów nie usuwał zwykłych użytkowników
    default boolean isEmpty() {
        return false;
    }
}
