package local.pbaranowski.chat.server;

import java.util.Map;
import java.util.function.Consumer;

public interface ClientsCollection<T> {
    void add(T t);

    void remove(T t);

    Map<String,T> getClients();

    default void forEach(Consumer<T> function) {
        getClients().forEach((name,client) -> function.accept(client));
    }

    default boolean isEmpty() {
        return getClients().isEmpty();
    }

    default boolean contains(String name) {
        return getClients().containsKey(name);
    }

    T getClient(String name);
}
