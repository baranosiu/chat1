package local.pbaranowski.chat.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class Server {
    private final int port;
    private static final int MAX_EXECUTORS = 1024;
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_EXECUTORS);
    @Getter
    private final MessageRouter messageRouter = new MessageRouter(new HashMapClients<>(), new CSVLogSerializer(), this);

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        HistoryClient historyClient = new HistoryClient(messageRouter);
        FTPClient ftpClient = new FTPClient(messageRouter, new FTPDiskStorage());
        execute(historyClient);
        execute(ftpClient);
        while (true) {
            Socket socket = serverSocket.accept();
            if (socket.isClosed()) {
                continue;
            }
            System.out.println("Connection establish " + socket.toString());
            SocketClient socketClient = new SocketClient(messageRouter, socket);
            execute(socketClient);
        }
    }

    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

}
