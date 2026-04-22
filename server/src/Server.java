import java.net.*;
import java.util.concurrent.*;

public class Server {
    public static final int PORT = 5000;
    public static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Server pornit pe portul " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client nou conectat: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.out.println("Eroare server: " + e.getMessage());
        }
    }

    public static void broadcast(String mesaj, ClientHandler expeditor) {
        for (ClientHandler c : clients) {
            if (c != expeditor) {
                c.trimite(mesaj);
            }
        }
    }

    public static ClientHandler gasestClient(String username) {
        for (ClientHandler c : clients) {
            if (username.equals(c.username)) return c;
        }
        return null;
    }
}