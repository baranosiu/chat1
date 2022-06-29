package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.Constants;

import java.io.IOException;

public class Application {

    public static void main(String[] args) throws IOException {
        int port = Constants.DEFAULT_PORT;
        if(args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new Server(port).start();
    }
}
