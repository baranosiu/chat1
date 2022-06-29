package local.pbaranowski.chat.transportlayer;

import local.pbaranowski.chat.server.MessageType;
import lombok.Data;

@Data
public class MessageInternetFrame {
    MessageType messageType;
    String sourceName;
    String destinationName;
    byte[] data;
}
