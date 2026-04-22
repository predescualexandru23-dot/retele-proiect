import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    public String username;

    // Coada pentru transfer — clientul pune bytes aici, serverul ii ridica
    private final SynchronousQueue<byte[]> transferQueue = new SynchronousQueue<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (true) {
                // Citim lungimea
                int lungime = in.readInt();
                byte[] buffer = new byte[lungime];
                in.readFully(buffer);

                // Incercam sa interpretam ca text UTF-8
                String posibilMesaj = new String(buffer, "UTF-8");

                // Verificam daca e o comanda cunoscuta
                if (esteComanda(posibilMesaj)) {
                    System.out.println("[" + username + "] " + posibilMesaj);
                    proceseazaMesaj(posibilMesaj);
                } else {
                    // Sunt bytes binari — ii punem in coada pentru transfer
                    System.out.println("[" + username + "] Primit "
                            + lungime + " bytes binari pentru transfer");
                    transferQueue.put(buffer);
                }
            }

        } catch (IOException e) {
            System.out.println("Client deconectat brusc: " + username);
        } catch (InterruptedException e) {
            System.out.println("Transfer intrerupt: " + username);
        } finally {
            inchide();
        }
    }

    private boolean esteComanda(String mesaj) {
        return mesaj.startsWith("AUTH|")
                || mesaj.startsWith("PUBLISH|")
                || mesaj.startsWith("FILE_ADD|")
                || mesaj.startsWith("FILE_DEL|")
                || mesaj.startsWith("DOWNLOAD|")
                || mesaj.equals("DISCONNECT");
    }

    public synchronized void trimite(String mesaj) {
        try {
            byte[] bytes = mesaj.getBytes("UTF-8");
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            System.out.println("Eroare trimitere mesaj catre "
                    + username + ": " + e.getMessage());
        }
    }

    public synchronized void trimiteBytes(byte[] date) {
        try {
            out.writeInt(date.length);
            out.write(date);
            out.flush();
        } catch (IOException e) {
            System.out.println("Eroare trimitere bytes catre "
                    + username + ": " + e.getMessage());
        }
    }

    // Asteapta pana clientul pune bytes in coada (timeout 15s)
    public byte[] iaBytesDinCoada() throws InterruptedException {
        System.out.println("Astept bytes din coada pentru " + username + "...");
        byte[] date = transferQueue.poll(15, TimeUnit.SECONDS);
        if (date == null) throw new InterruptedException("Timeout asteptare fisier");
        System.out.println("Bytes primiti din coada: " + date.length);
        return date;
    }

    private void proceseazaMesaj(String mesaj) {
        String[] parts = mesaj.split("\\|", -1);
        switch (parts[0]) {
            case "AUTH":       handleAuth(parts);     break;
            case "PUBLISH":    handlePublish(parts);  break;
            case "FILE_ADD":   handleFileAdd(parts);  break;
            case "FILE_DEL":   handleFileDel(parts);  break;
            case "DOWNLOAD":   handleDownload(parts); break;
            case "DISCONNECT": inchide();             break;
            default: trimite("EROARE|Comanda necunoscuta: " + parts[0]);
        }
    }

    private void handleAuth(String[] parts) {
        if (parts.length < 3) {
            trimite("AUTH_FAIL|Format invalid");
            return;
        }
        try {
            String user   = parts[1];
            String parola = parts[2];
            if (UserManager.existaUser(user)) {
                if (UserManager.verifica(user, parola)) {
                    this.username = user;
                    trimite("AUTH_OK");
                    System.out.println("Autentificat: " + username);
                } else {
                    trimite("AUTH_FAIL|Parola gresita");
                }
            } else {
                UserManager.inregistreaza(user, parola);
                this.username = user;
                trimite("AUTH_NEW_OK");
                System.out.println("Cont nou creat: " + username);
            }
        } catch (Exception e) {
            trimite("AUTH_FAIL|Eroare server");
        }
    }

    private void handlePublish(String[] parts) {
        if (username == null) {
            trimite("EROARE|Nu esti autentificat");
            return;
        }
        String listaBruta = (parts.length > 1) ? parts[1] : "";
        List<String> fisiere = new ArrayList<>();
        if (!listaBruta.isEmpty()) {
            fisiere = new ArrayList<>(Arrays.asList(listaBruta.split(",")));
        }
        FileRegistry.publicaFisiere(username, fisiere);
        System.out.println(username + " a publicat: " + fisiere);
        trimite("FILE_LIST|" + FileRegistry.getListaAltora(username));
        Server.broadcast("NEW_CLIENT|" + username + "|" + listaBruta, this);
    }

    private void handleFileAdd(String[] parts) {
        if (username == null || parts.length < 2) return;
        String numeFisier = parts[1];
        FileRegistry.adaugaFisier(username, numeFisier);
        Server.broadcast("FILE_ADD|" + username + "|" + numeFisier, this);
        System.out.println(username + " a adaugat: " + numeFisier);
    }

    private void handleFileDel(String[] parts) {
        if (username == null || parts.length < 2) return;
        String numeFisier = parts[1];
        FileRegistry.stergeFisier(username, numeFisier);
        Server.broadcast("FILE_DEL|" + username + "|" + numeFisier, this);
        System.out.println(username + " a sters: " + numeFisier);
    }

    private void handleDownload(String[] parts) {
        if (parts.length < 3) {
            trimite("EROARE|Format invalid");
            return;
        }
        String ownerUser  = parts[1];
        String numeFisier = parts[2];

        ClientHandler owner = Server.gasestClient(ownerUser);
        if (owner == null) {
            trimite("EROARE|Clientul " + ownerUser + " nu mai este conectat");
            return;
        }

        System.out.println(username + " cere " + numeFisier + " de la " + ownerUser);

        ClientHandler solicitant = this;
        new Thread(() -> {
            try {
                // Cerem owner-ului sa trimita fisierul
                System.out.println("Cer fisierul " + numeFisier + " de la " + ownerUser);
                owner.trimite("SEND_FILE|" + numeFisier);

                // Asteptam bytes in coada — owner-ul ii pune acolo din run()
                byte[] continut = owner.iaBytesDinCoada();

                System.out.println("Primit " + continut.length
                        + " bytes, trimit catre " + solicitant.username);

                // Trimitem catre solicitant
                solicitant.trimite("FILE_START|" + numeFisier);
                solicitant.trimiteBytes(continut);

                System.out.println("Transfer complet: " + numeFisier);

            } catch (InterruptedException e) {
                System.out.println("Timeout transfer: " + numeFisier);
                solicitant.trimite("EROARE|Timeout la transfer");
            } catch (Exception e) {
                System.out.println("Eroare transfer: " + e.getMessage());
                solicitant.trimite("EROARE|Transfer esuat");
            }
        }).start();
    }

    private void inchide() {
        if (username != null) {
            FileRegistry.stergeUser(username);
            Server.broadcast("CLIENT_LEFT|" + username, this);
            System.out.println("Client deconectat: " + username);
            username = null;
        }
        Server.clients.remove(this);
        try { socket.close(); } catch (IOException e) {}
    }
}