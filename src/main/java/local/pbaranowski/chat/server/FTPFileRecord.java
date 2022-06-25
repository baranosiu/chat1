package local.pbaranowski.chat.server;

import lombok.Value;

@Value
public class FTPFileRecord {
    private String sender;
    private String channel;
    private String filename; // Nazwa nadana przez użytkownika wysyłającego
    private String diskFilename; // Nazwa pod jaką plik jest przechowywany w storage
}
