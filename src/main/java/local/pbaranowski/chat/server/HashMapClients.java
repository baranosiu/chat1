package local.pbaranowski.chat.server;

import java.util.*;

import static java.util.Collections.synchronizedMap;

class HashMapClients<T extends Client> implements ClientsCollection<T> {
    private final Map<String,T> clients = synchronizedMap(new HashMap<>());

    @Override
    public void add(T client) {
        clients.put(client.getName(),client);
    }

    @Override
    public void remove(T client) {
        clients.remove(client.getName());
    }

    @Override
    public Map<String,T> getClients() {
        return clients;
    }

    @Override
    public T getClient(String name) {
        return clients.get(name);
    }

}
