package local.pbaranowski.chat.transportlayer;

import local.pbaranowski.chat.server.MessageType;
import lombok.Value;

@Value
public class MessageInternetFrame {
    MessageType messageType;
    String sourceName; // Nazwa lub id
    String destinationName;
    byte[] data;
}
