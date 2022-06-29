package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.MessageType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class Message {
    private final MessageType messageType;
    private final String sender;
    private final String receiver;
    private final String payload;
}
