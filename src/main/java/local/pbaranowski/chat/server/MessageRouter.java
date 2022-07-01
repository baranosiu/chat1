package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.Constants;
import local.pbaranowski.chat.commons.MessageType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class MessageRouter {
    @Getter
    private final ClientsCollection<Client> clients;
    private final LogSerializer logSerializer;
    private final Server server;

    void subscribe(Client client) {
        clients.add(client);
    }

    Message sendMessage(Message message) {
        log.info(logSerializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_TO_ALL:
                clients.forEach(client -> client.write(message));
                break;
            case MESSAGE_TEXT:
            case MESSAGE_HISTORY_RETRIEVE:
            case MESSAGE_LIST_USERS_ON_CHANNEL: {
                Client client;
                if ((client = clients.getClient(message.getReceiver())) != null) {
                    client.write(message);
                }
            }
            break;
            case MESSAGE_JOIN_CHANNEL:
                if (!clients.contains(message.getReceiver())) {
                    ChannelClient channelClient = new ChannelClient(message.getReceiver(), this, new HashMapClients<>());
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
                clients.getClient(Constants.HISTORY_ENDPOINT_NAME).write(message);
                break;
            case MESSAGE_APPEND_FILE:
            case MESSAGE_PUBLISH_FILE:
            case MESSAGE_REGISTER_FILE_TO_UPLOAD:
                if (clients.getClient(message.getReceiver()) != null) {
                    clients.getClient(Constants.FTP_ENDPOINT_NAME).write(message);
                }
                break;
            case MESSAGE_DOWNLOAD_FILE:
            case MESSAGE_LIST_FILES:
            case MESSAGE_DELETE_FILE:
                clients.getClient(Constants.FTP_ENDPOINT_NAME).write(message);
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
        return message;
    }


    Message sendMessage(MessageType messageType, String source, String destination, String payload) {
        return sendMessage(new Message(messageType, source, destination, payload));
    }

    private void removeChannelClient(Client client) {
        if (client.isEmpty() && !client.getName().equals(Constants.GLOBAL_ENDPOINT_NAME)) {
            clients.getClient(Constants.FTP_ENDPOINT_NAME).write(
                    new Message(MessageType.MESSAGE_DELETE_ALL_FILES_ON_CHANNEL, Constants.SERVER_ENDPOINT_NAME, client.getName(), null)
            );
            clients.remove(client);
        }
    }

    ChannelClient getChannelClient(String name) {
        if (clients.getClient(name) instanceof ChannelClient) {
            return ChannelClient.class.cast(clients.getClient(name));
        } else {
            return null;
        }
    }

}
