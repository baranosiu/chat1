package local.pbaranowski.chat.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

import static local.pbaranowski.chat.constants.Constants.HISTORY_ENDPOINT_NAME;

@Slf4j
@RequiredArgsConstructor
public class HistoryClient implements Client, Runnable {
    private final MessageRouter messageRouter;
    private final HistoryPersistence historyPersistence = new HistoryFilePersistence();
    private final LogSerializer logSerializer = new CSVLogSerializer();

    @Override
    public String getName() {
        return HISTORY_ENDPOINT_NAME;
    }

    @Override
    public void write(Message message) {
        log.info(logSerializer.fromMessageToString(message));
        switch (message.getMessageType()) {
            case MESSAGE_HISTORY_STORE:
                save(message.getSender(), message.getPayload());
                break;
            case MESSAGE_HISTORY_RETRIEVE:
                log.info("History of {}", message.getSender());
                retrieveHistory(message);
        }
    }

    private void retrieveHistory(Message message) {
        Iterator<String> history = retrieve(message.getSender());
        while (history.hasNext()) {
            String historyRecord = history.next();
            if (historyRecord.startsWith(message.getSender() + ":")) {
                messageRouter.sendMessage(new Message(MessageType.MESSAGE_TEXT, getName(), message.getSender(), historyRecord));
            }
        }
    }

    @Override
    public void run() {
        messageRouter.subscribe(this);
    }

    public void save(String user, String text) {
        historyPersistence.save(user,text);
    }

    public Iterator<String> retrieve(String user) {
        return historyPersistence.retrieve(user);
    }
}
