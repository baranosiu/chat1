package local.pbaranowski.chat.server;

public interface Client {
    String getName();

    void write(Message message);

    // Używane na potrzeby kanałów czy mają jakichś użytkowników
    default boolean isEmpty() {
        return false;
    }
}
