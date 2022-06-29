package local.pbaranowski.chat.server;

class DiskFileStorageUtils {

    private static final String FILE_LIST_FORMAT_STRING = "%4s %-16s : %s";

    public static String fileToKeyString(String user, String channel, String filename) {
        return String.format("%s:%s:%s", user, channel, filename);
    }

    public static String fileRecordToString(String key, FileStorageRecord file) {
        return String.format(FILE_LIST_FORMAT_STRING, key, file.getSender(), file.getFilename());
    }

    private DiskFileStorageUtils() {
    }
}
