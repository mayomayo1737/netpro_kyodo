// ChatServer.java
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        System.out.println("Chat Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket sock = serverSocket.accept();
                ClientHandler handler = new ClientHandler(sock);
                clients.add(handler);
                new Thread(handler).start();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket sock;
        private BufferedReader in;
        private PrintWriter out;
        ClientHandler(Socket s) throws IOException {
            sock = s;
            in  = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new PrintWriter(s.getOutputStream(), true);
        }
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    synchronized (clients) {
                        for (ClientHandler c : clients) {
                            c.out.println(line);
                        }
                    }
                }
            } catch (IOException ignored) { }
            finally {
                try { sock.close(); } catch (IOException ignored) {}
                clients.remove(this);
            }
        }
    }
}
