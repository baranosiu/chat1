package local.pbaranowski.chat.server;

// Obecnie używane tylko do formatowania logów dla Slf4j

class CSVLogSerializer implements LogSerializer {
    private static final String FIELD_SEPARATOR = ",";

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
