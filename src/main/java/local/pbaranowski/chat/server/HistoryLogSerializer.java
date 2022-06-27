package local.pbaranowski.chat.server;

public class HistoryLogSerializer implements LogSerializer{
    @Override
    public String fromMessageToString(Message message) {
        return String.format("%s:%s%s",message.getSender() ,message.getPayload(),System.lineSeparator());
    }
}
