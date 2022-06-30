package local.pbaranowski.chat.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static local.pbaranowski.chat.commons.Constants.SERVER_ENDPOINT_NAME;
import static local.pbaranowski.chat.commons.MessageType.*;

@Slf4j
@RequiredArgsConstructor
class ChannelClient implements Client, Runnable {
    private final String name;
    private final MessageRouter messageRouter;
    private final ClientsCollection<Client> clients;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void write(Message message) {
        LogSerializer serializer = new CSVLogSerializer();
        log.info(serializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_TO_ALL: // Kanał nie odpowiada na wiadomości do wszystkich użytkowników
                break;
            case MESSAGE_TEXT:
                if (clients.contains(message.getSender())) {
                    writeToAll(message);
                }
                break;
            case MESSAGE_JOIN_CHANNEL:
                log.info("Join channel {}->{}", message.getSender(), message.getReceiver());
                clients.add(messageRouter.getClients().getClient(message.getSender()));
                writeToAll(message.getSender() + " joined channel");
                break;
            case MESSAGE_LEAVE_CHANNEL:
                if (clients.contains(message.getSender())) {
                    writeToAll(new Message(MESSAGE_TEXT, SERVER_ENDPOINT_NAME, getName(), message.getSender() + " left channel"));
                }
                clients.remove(messageRouter.getClients().getClient(message.getSender()));
                break;
            case MESSAGE_LIST_USERS_ON_CHANNEL:
                messageRouter.sendMessage(new Message(MESSAGE_TEXT, getName(), message.getSender(), "Users: " + usersOnChannel()));
                break;
            case MESSAGE_USER_DISCONNECTED:
                clients.remove(messageRouter.getClients().getClient(message.getSender()));
                if (getName().equals("@global"))
                    writeToAll(new Message(MESSAGE_TEXT, SERVER_ENDPOINT_NAME, getName(), message.getSender() + " disconnected"));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isEmpty() {
        return clients.isEmpty();
    }

    @Override
    public void run() {
        // Bez kolejkowania i obsługi kolejki w osobnym wątku, więc w tej chwili puste
    }

    ClientsCollection<Client> getClients() {
        return clients;
    }

    void addClient(Client client) {
        clients.add(client);
    }

    void writeToAll(Message message) {
        clients.forEach(client -> client.write(message));
    }

    void writeToAll(String text) {
        writeToAll(new Message(MESSAGE_TEXT, SERVER_ENDPOINT_NAME, getName(), text));
    }

    private String usersOnChannel() {
        return clients.getClients().keySet().stream().reduce((a, b) -> a + " " + b).orElse("[empty]");
    }
}
