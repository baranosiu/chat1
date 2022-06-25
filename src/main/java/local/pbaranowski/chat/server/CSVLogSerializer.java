package local.pbaranowski.chat.server;

// Obecnie używane tylko do formatowania logów dla Slf4j

public class CSVLogSerializer implements LogSerializer {
    public static final String FIELD_SEPARATOR = ",";

    @Override
    public String fromMessageToString(Message message) {
        return message.getMessageType().name()+FIELD_SEPARATOR+message.getSender()+FIELD_SEPARATOR+message.getReceiver()+FIELD_SEPARATOR+message.getPayload();
    }
}
