package local.pbaranowski.chat.server;

public enum MessageType {
    MESSAGE_TEXT, MESSAGE_JOIN_CHANNEL, MESSAGE_LEAVE_CHANNEL,
    MESSAGE_LIST_USERS, MESSAGE_USER_DISCONNECTED, MESSAGE_LIST_CHANNELS,
    MESSAGE_LIST_USERS_ON_CHANNEL, MESSAGE_TO_ALL, MESSAGE_HISTORY_STORE,
    MESSAGE_HISTORY_RETRIEVE,
    MESSAGE_PUBLISH_FILE, MESSAGE_APPEND_FILE, MESSAGE_DOWNLOAD_FILE, MESSAGE_SEND_CHUNK_TO_CLIENT,
    MESSAGE_LIST_FILES, MESSAGE_ERASE_FILE
}
