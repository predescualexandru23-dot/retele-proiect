import java.net.*;
import java.io.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    public String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String linie;
            while ((linie = in.readLine()) != null) {
                System.out.println("[" + username + "] " + linie);
                proceseazaMesaj(linie);
            }

        } catch (IOException e) {
            System.out.println("Client deconectat brusc: " + username);
        } finally {
            inchide();
        }
    }

    private void proceseazaMesaj(String mesaj) {
        String[] parts = mesaj.split("\\|");
        String comanda = parts[0];

        switch (comanda) {
            case "AUTH":
                handleAuth(parts);
                break;
            default:
                trimite("EROARE|Comanda necunoscuta");
        }
    }

    private void handleAuth(String[] parts) {
        // Verificam ca mesajul are forma corecta: AUTH|username|parola
        if (parts.length < 3) {
            trimite("AUTH_FAIL|Format invalid");
            return;
        }

        String user = parts[1];
        String parola = parts[2];

        try {
            // Verificam daca userul exista deja
            if (UserManager.existaUser(user)) {
                // Exista — verificam parola
                if (UserManager.verifica(user, parola)) {
                    this.username = user;
                    trimite("AUTH_OK");
                    System.out.println("Autentificat: " + username);
                } else {
                    trimite("AUTH_FAIL|Parola gresita");
                }
            } else {
                // Nu exista — il inregistram automat
                UserManager.inregistreaza(user, parola);
                this.username = user;
                trimite("AUTH_NEW_OK");
                System.out.println("Cont nou creat: " + username);
            }
        } catch (Exception e) {
            trimite("AUTH_FAIL|Eroare server");
            System.out.println("Eroare la autentificare: " + e.getMessage());
        }
    }

    public void trimite(String mesaj) {
        out.println(mesaj);
    }

    private void inchide() {
        Server.clients.remove(this);
        try { socket.close(); } catch (IOException e) {}
        System.out.println("Conexiune inchisa: " + username);
    }
}