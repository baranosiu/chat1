package local.pbaranowski.chat.server;

public class FTPClientUtils {

    public static String fileToKeyString(String user, String channel, String filename) {
        return String.format("%s:%s:%s", user,channel,filename);
    }

    public static String fileRecordToString(String key,FTPFileRecord file) {
        return key+ " " +file.getSender()+" : "+ file.getFilename();
    }
}
