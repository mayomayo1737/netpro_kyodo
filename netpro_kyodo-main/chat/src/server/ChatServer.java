package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * シンプルなマルチクライアントチャットサーバー
 */
public class ChatServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Chat Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket sock = serverSocket.accept();
                ClientHandler handler = new ClientHandler(sock);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("サーバー起動エラー: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket sock;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket s) {
            this.sock = s;
            try {
                in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                out = new PrintWriter(s.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("クライアント接続エラー: " + e.getMessage());
            }
        }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    broadcast(line);
                }
            } catch (IOException e) {
                System.err.println("通信エラー: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }

        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (sock != null && !sock.isClosed()) sock.close();
            } catch (IOException ignored) {}
            clients.remove(this);
            System.out.println("クライアント切断: " + sock.getRemoteSocketAddress());
        }
    }
}
