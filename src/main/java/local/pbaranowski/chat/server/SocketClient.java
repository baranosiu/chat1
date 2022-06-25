package local.pbaranowski.chat.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SocketClient implements Runnable, Client {
    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    private MessageRouter messageRouter;
    private String lastDestination;

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
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_TO_ALL, "SERVER", "*", "New user " + getName() + " connected"));
            help();
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_JOIN_CHANNEL, getName(), "@global", null));
            lastDestination = "@global";
            while ((nickname = bufferedReader.readLine()) != null) {
                parseInput(nickname.strip());
            }
        } catch (IOException e) {
            log.error("{} ", e.getMessage(), e);
        } finally {
            messageRouter.clientDisconnected(this);
        }
    }

    @SneakyThrows
    private void parseInput(String text) {
        if (text.startsWith("/?")) {
            help();
            return;
        }
        if (text.startsWith("/h")) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_HISTORY_RETRIEVE, getName(), "@history", null));
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
        Message message = new Message(MessageType.MESSAGE_TEXT, getName(), lastDestination, text);
        messageRouter.sendMessage(message);
        if (Validators.isChannelNameValid(lastDestination) || lastDestination.equals("@global"))
            storeInHistory(message);
    }

    private void eraseFile(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_ERASE_FILE, getName(), null, fields[1]));
        }
    }

    private void downloadFile(String text) {
        String[] fields = text.split("[ ]+", 3);
        if (fields.length == 3) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_DOWNLOAD_FILE, getName(), null, fields[1] + " " + fields[2]));
        }
    }

    private void listFiles(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_LIST_FILES, getName(), fields[1], null));
        }
    }

    private void uploadFile(String text) {
        String[] fields = text.split("[ ]+", 4);
        if (fields.length == 4) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_APPEND_FILE, getName(), fields[1], fields[2] + " " + fields[3]));
        }
    }

    private void leaveChannel(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_LEAVE_CHANNEL, getName(), fields[1], null));
            lastDestination = "@global";
        }
    }

    private void listUsers(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_LIST_USERS_ON_CHANNEL, getName(), fields[1], null));
        }
    }

    private void commandJoin(String text) {
        String[] fields = text.split("[ ]+", 2);
        if (fields.length == 2) {
            if (Validators.isChannelNameValid(fields[1])) {
                messageRouter.sendMessage(new Message(MessageType.MESSAGE_JOIN_CHANNEL, getName(), fields[1], null));
                lastDestination = fields[1];
            }
        }
    }

    private void commandMessage(String text) {
        String[] fields = text.split("[ ]+", 3);
        if (fields.length == 3 && (Validators.isNameOrChannelValid(fields[1]) || Validators.isChannelSpecial(fields[1]))) {
            Message message = new Message(MessageType.MESSAGE_TEXT, getName(), fields[1], fields[2]);
            messageRouter.sendMessage(message);
            if (Validators.isChannelNameValid(fields[1]) || fields[1].equals("@global"))
                storeInHistory(message);
            lastDestination = fields[1];
        }
    }

    @SneakyThrows
    private void help() {
        List<String> lines = Files.lines(Paths.get("src/main/resources/help.txt")).collect(Collectors.toList());
        for (String line : lines) {
            writeln(line, null);
        }
    }

    @SneakyThrows
    private void write(String text, String prefix, boolean appendNewLine) {
        if (prefix == null) {
            prefix = "m:";
        }
        synchronized (bufferedWriter) {
            bufferedWriter.write(prefix + text);
            if(appendNewLine) {
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        }
    }

    // Kompatybilność z poprzednią metodą write bez parametru appendNewLine
    private void write(String text, String prefix) {
        write(text,prefix,false);
    }

    private void writeln(String text, String prefix) {
            write(text, prefix,true);
    }

    @SneakyThrows
    @Override
    public void write(Message message) {
        if (message.getMessageType() == MessageType.MESSAGE_SEND_CHUNK_TO_CLIENT) {
            writeln(message.getPayload(), "f:");
            return;
        }
        writeln(formatMessage(message), null);
        // TODO: Decyzja czy zapisujemy w historii
//            storeInHistory(message);
    }

    private void storeInHistory(Message message) {
            messageRouter.sendMessage(new Message(MessageType.MESSAGE_HISTORY_STORE, getName(), "@history", formatMessage(message)));
    }

    private String formatMessage(Message message) {
        return message.getSender() + "->" + message.getReceiver() + " " + message.getPayload();
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
