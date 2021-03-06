package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.Constants;
import local.pbaranowski.chat.commons.transportlayer.Base64Transcoder;
import local.pbaranowski.chat.commons.transportlayer.MessageInternetFrame;
import local.pbaranowski.chat.commons.transportlayer.Transcoder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
class Server {
    private final int port;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Constants.MAX_EXECUTORS);
    @Getter
    private final MessageRouter messageRouter = new MessageRouter(new HashMapClients<>(), new CSVLogSerializer(), this);

    void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ChannelClient global = new ChannelClient(Constants.GLOBAL_ENDPOINT_NAME, messageRouter, new HashMapClients<>());
            execute(global);
            HistoryClient historyClient = new HistoryClient(messageRouter, new HistoryFilePersistence(new HistoryLogSerializer()));
            execute(historyClient);
            Transcoder<MessageInternetFrame> transcoder = new Base64Transcoder<>();
            FTPClient ftpClient = new FTPClient(messageRouter, transcoder, new DiskFileStorage(transcoder));
            execute(ftpClient);
            while (true) {
                Socket socket = serverSocket.accept();
                if (socket.isClosed()) {
                    continue;
                }
                log.info("Connection established {}", socket);
                SocketClient socketClient = new SocketClient(messageRouter, socket);
                execute(socketClient);
            }
        }
    }

    void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

}
