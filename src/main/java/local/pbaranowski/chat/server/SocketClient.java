package local.pbaranowski.chat.server;

import local.pbaranowski.chat.constants.Constants;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static local.pbaranowski.chat.constants.Constants.HELP_FILE;

@Slf4j
public class SocketClient implements Runnable, Client {
    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    private final MessageRouter messageRouter;
    private String lastDestination;
    private static final String MESSAGE_FORMAT_STRING = "%s->%s %s";
    private final Object synchronizationObject = new Object();

    public SocketClient(MessageRouter messageRouter, Socket socket) {
        this.socket = socket;
        this.messageRouter = messageRouter;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            log.error("{} ", e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        socketClient();
    }

    @SneakyThrows
    private void socketClient() {
        String nickname;
        try {
            while (true) {
                writeln("Enter name \\w{3,16}", null);
                nickname = bufferedReader.readLine();
                if (!Validators.isNameValid(nickname)) continue;
                if (messageRouter.getClients().contains(nickname)) {
                    writeln("Nick " + nickname + " already in use", null);
                    continue;
                }
                break;
            }
            setName(nickname);
            messageRouter.subscribe(this);
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_JOIN_CHANNEL, getName(), Constants.GLOBAL_ENDPOINT_NAME, null));
            lastDestination = Constants.GLOBAL_ENDPOINT_NAME;
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_TEXT, Constants.SERVER_ENDPOINT_NAME, getName(), "/? - pomoc"));
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                parseInput(inputLine.stripTrailing());
            }
        } catch (IOException e) {
            log.error("{} ", e.getMessage(), e);
        } finally {
            socket.close();
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_USER_DISCONNECTED, getName(), null, null));
        }
    }

    @SneakyThrows
    private void parseInput(String text) {
        if (text.startsWith("/?")) {
            help();
            return;
        }
        if (text.startsWith("/h")) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_HISTORY_RETRIEVE, getName(), Constants.HISTORY_ENDPOINT_NAME, null));
            return;
        }
        if (text.startsWith("/m ")) {
            commandMessage(text);
            return;
        }
        if (text.startsWith("/j ")) {
            commandJoin(text);
            return;
        }
        if (text.startsWith("/lu ")) {
            listUsers(text);
            return;
        }
        if (text.startsWith("/l ")) {
            leaveChannel(text);
            return;
        }
        if (text.startsWith("/uf ")) {
            uploadFile(text);
            return;
        }

        if (text.startsWith("/df ")) {
            downloadFile(text);
            return;
        }

        if (text.startsWith("/lf ")) {
            listFiles(text);
            return;
        }

        if (text.startsWith("/ef ")) {
            eraseFile(text);
            return;
        }
        commandMessage("/m " + lastDestination + " " + text);
    }

    private Message sendMessage(MessageType messageType, String source, String destination, String payload) {
        return messageRouter.sendMessage(messageType, source, destination, payload);
    }

    private void eraseFile(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            sendMessage(MessageType.MESSAGE_DELETE_FILE, getName(), null, fields[1]);
        }
    }

    private void downloadFile(String text) {
        String[] fields = text.split("[ ]+", 3);
        if (fields.length == 3) {
            sendMessage(MessageType.MESSAGE_DOWNLOAD_FILE, getName(), null, fields[1] + " " + fields[2]);
        }
    }

    private void listFiles(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            sendMessage(MessageType.MESSAGE_LIST_FILES, getName(), fields[1], null);
        }
    }

    // Aplikacja klienta wysyła ramki w formacie: /uf base64data
    // koniec pliku gdy wyśle MessageType == MESSAGE_PUBLISH_FILE w payload (fields[1])
    private void uploadFile(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            sendMessage(MessageType.MESSAGE_APPEND_FILE,getName(), Constants.FTP_ENDPOINT_NAME,fields[1]);
        }
    }

    private void leaveChannel(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            sendMessage(MessageType.MESSAGE_LEAVE_CHANNEL, getName(), fields[1], null);
            lastDestination = Constants.GLOBAL_ENDPOINT_NAME;
        }
    }

    private void listUsers(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            sendMessage(MessageType.MESSAGE_LIST_USERS_ON_CHANNEL, getName(), fields[1], null);
        }
    }

    private void commandJoin(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            if (Validators.isChannelName(fields[1])) {
                sendMessage(MessageType.MESSAGE_JOIN_CHANNEL, getName(), fields[1], null);
                lastDestination = fields[1];
            }
        }
    }

    private void commandMessage(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            String[] arguments = fields[1].split(" ", 2);
            if (arguments.length == 2 && (Validators.isNameOrChannelValid(arguments[0]) || Validators.isChannelSpecial(arguments[0]))) {
                Message message = sendMessage(MessageType.MESSAGE_TEXT, getName(), arguments[0], arguments[1]);
                // Zapis prywatnych (nie na kanał) wiadomości w historii, bo nie robimy echa lokalnego dla takich wiadomości
                if (!message.getReceiver().matches("[@#]\\w{2,16}")) {
                    storeInHistory(message);
                }
                lastDestination = arguments[0];
            }
        }
    }

    @SneakyThrows
    private void help() {
        try(BufferedInputStream reader = new BufferedInputStream(this.getClass().getClassLoader().getResourceAsStream(HELP_FILE))) {
            while (reader.available() > 0) {
                String text = new String(reader.readAllBytes(), StandardCharsets.UTF_8);
                text.lines().forEach(line -> writeln(line,null));
            }
        }
    }

    @SneakyThrows
    private void write(String text, String prefix, boolean appendNewLine) {
        if (socket.isOutputShutdown())
            return;
        synchronized (synchronizationObject) {
            bufferedWriter.write((prefix == null ? Constants.MESSAGE_TEXT_PREFIX : prefix) + text);
            if (appendNewLine) {
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        }
    }

    private void writeln(String text, String prefix) {
        write(text, prefix, true);
    }

    @SneakyThrows
    @Override
    public void write(Message message) {
        if (message.getMessageType() == MessageType.MESSAGE_SEND_CHUNK_TO_CLIENT) {
            writeln(message.getPayload(), Constants.MESSAGE_FILE_PREFIX);
            return;
        }
        writeln(formatMessage(message), null);
        // TODO: Przenieść walidację do funkcji
        if (!List.of(Constants.FTP_ENDPOINT_NAME, Constants.HISTORY_ENDPOINT_NAME).contains(message.getSender())) {
            storeInHistory(message);
        }
    }

    private void storeInHistory(Message message) {
        sendMessage(MessageType.MESSAGE_HISTORY_STORE, getName(), Constants.HISTORY_ENDPOINT_NAME, formatMessage(message));
    }

    private String formatMessage(Message message) {
        return String.format(MESSAGE_FORMAT_STRING, message.getSender(), message.getReceiver(), message.getPayload());
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
