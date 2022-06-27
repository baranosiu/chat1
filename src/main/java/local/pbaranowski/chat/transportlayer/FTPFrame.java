package local.pbaranowski.chat.transportlayer;

import lombok.Value;

@Value
public class FTPFrame {
    String fileDescriptor; // Nazwa lub id
    byte[] data;
    String destination;
}
