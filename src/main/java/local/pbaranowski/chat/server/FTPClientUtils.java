package local.pbaranowski.chat.server;

public class FTPClientUtils {

    public static String fileToKeyString(String user, String channel, String filename) {
        return String.format("%s:%s:%s", user,channel,filename);
    }

    public static String fileRecordToString(FTPFileRecord file) {
        return file.getDiskFilename()+ " " +file.getSender()+" : "+ file.getFilename();
    }
}
