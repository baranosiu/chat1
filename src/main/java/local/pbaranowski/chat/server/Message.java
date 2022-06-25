package local.pbaranowski.chat.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Message {
    private final MessageType messageType;
    private final String sender;
    private final String receiver;
    private final String payload;
}
