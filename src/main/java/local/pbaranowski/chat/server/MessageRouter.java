package local.pbaranowski.chat.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MessageRouter {
    @Getter
    private final ClientsCollection<Client> clients;
    private final LogSerializer logSerializer;
    private final Server server;

    public void subscribe(Client client) {
        clients.add(client);
    }

    public void unsubscribe(Client client) {
        clients.getClients().forEach((key, value) -> {
            if (value instanceof ChannelClient) {
                ((ChannelClient) value).removeClient(client);
            }
        });
        clients.remove(client);
    }

    public void sendMessage(Message message) {
        log.info(logSerializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_TO_ALL:
                clients.forEach(client -> client.write(message));
                break;
            case MESSAGE_TEXT: {
                Client client;
                if ((client = clients.getClient(message.getReceiver())) != null) {
                    client.write(message);
                }
            }
            break;
            case MESSAGE_LIST_USERS_ON_CHANNEL: {
                Client client;
                if ((client = clients.getClient(message.getReceiver())) != null) {
                    client.write(message);
                }
            }
            break;
            case MESSAGE_JOIN_CHANNEL:
                if (!clients.contains(message.getReceiver())) {
                    ChannelClient channelClient = new ChannelClient(message.getReceiver(), this, new HashMapClients<Client>());
                    channelClient.addClient(clients.getClient(message.getSender()));
                    subscribe(channelClient);
                    server.execute(channelClient);
                }
                clients.getClient(message.getReceiver()).write(message);
                break;
            case MESSAGE_LEAVE_CHANNEL: {
                Client client;
                if (clients.contains(message.getReceiver())) {
                    client = clients.getClient(message.getReceiver());
                    client.write(message);
                    removeChannelClient(client);
                }
            }
            break;
            case MESSAGE_HISTORY_STORE:
                clients.getClient("@history").write(message);
                break;
            case MESSAGE_HISTORY_RETRIEVE: {
                Client client;
                if ((client = clients.getClient(message.getReceiver())) != null) {
                    client.write(message);
                }
            }
            break;
            case MESSAGE_APPEND_FILE:
                if (clients.getClient(message.getReceiver()) != null) {
                    clients.getClient("@ftp").write(message);
                }
                break;
            case MESSAGE_DOWNLOAD_FILE:
            case MESSAGE_LIST_FILES:
            case MESSAGE_DELETE_FILE:
                clients.getClient("@ftp").write(message);
                break;
            case MESSAGE_SEND_CHUNK_TO_CLIENT:
                clients.getClient(message.getReceiver()).write(message);
                break;
            case MESSAGE_USER_DISCONNECTED: {
                Client socketClient = clients.getClient(message.getSender());
                for (Client client : clients.getClients().values()) {
                    if (client instanceof ChannelClient) {
                        client.write(
                                new Message(MessageType.MESSAGE_USER_DISCONNECTED, socketClient.getName(), client.getName(), null)
                        );
                        removeChannelClient(client);
                    }
                }
            }
        }
    }

    private void removeChannelClient(Client client) {
        if (client.isEmpty() && !client.getName().equals("@global")) {
            clients.getClient("@ftp").write(
                    new Message(MessageType.MESSAGE_DELETE_ALL_FILES_ON_CHANNEL, "@server", client.getName(), null)
            );
            clients.remove(client);
        }
    }

}
