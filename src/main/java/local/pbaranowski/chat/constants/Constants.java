package local.pbaranowski.chat.constants;

import java.io.File;

public class Constants {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 9000;

    public static final int MAX_EXECUTORS = 1024;

    public static final String HELP_FILE = "help.txt";
    public static final String FILE_STORAGE_DIR = "storage";
    public static final int MAX_NUMBER_OF_FILES_IN_STORAGE = 2048;
    public static final String HISTORY_FILE_NAME = FILE_STORAGE_DIR + File.separator + "history.txt";
    public static final String FILE_TRANSFER_COMPLETED = "C";
    public static final String MESSAGE_TEXT_PREFIX = "m:";
    public static final String MESSAGE_FILE_PREFIX = "f:";

    public static final String SERVER_ENDPOINT_NAME = "@server";
    public static final String FTP_ENDPOINT_NAME = "@ftp";
    public static final String HISTORY_ENDPOINT_NAME = "@history";
    public static final String GLOBAL_ENDPOINT_NAME = "@global";

    private Constants() {
    }
}
