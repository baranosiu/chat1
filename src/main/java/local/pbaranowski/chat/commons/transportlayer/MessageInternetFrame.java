package local.pbaranowski.chat.commons.transportlayer;

import local.pbaranowski.chat.commons.MessageType;
import lombok.Data;

@Data
public class MessageInternetFrame {
    MessageType messageType;
    String sourceName;
    String destinationName;
    byte[] data;
}
