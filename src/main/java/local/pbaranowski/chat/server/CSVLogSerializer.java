package local.pbaranowski.chat.server;

// Obecnie używane tylko do formatowania logów dla Slf4j

public class CSVLogSerializer implements LogSerializer {
    public static final String FIELD_SEPARATOR = ",";

    @Override
    public String fromMessageToString(Message message) {
        return String.join(FIELD_SEPARATOR,
                message.getMessageType().name(),
                message.getSender(),
                message.getReceiver(),
                message.getPayload()
                );
    }
}
