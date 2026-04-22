import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Client {
    private AppWindow window;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private String shareDir;
    private DirectoryWatcher watcher;
    private boolean watcherPornit = false;
    private boolean conectat = false;

    private final Object writeLock = new Object();

    public Client(AppWindow window) {
        this.window = window;
    }

    public void conecteaza(String host, int port) throws Exception {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in  = new DataInputStream(socket.getInputStream());
        conectat = true;
    }

    public void trimite(String mesaj) throws IOException {
        synchronized (writeLock) {
            byte[] bytes = mesaj.getBytes("UTF-8");
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        }
    }

    public void trimiteBytes(byte[] date) throws IOException {
        synchronized (writeLock) {
            out.writeInt(date.length);
            out.write(date);
            out.flush();
        }
    }

    private String citesteMesaj() throws IOException {
        int lungime = in.readInt();
        byte[] buffer = new byte[lungime];
        in.readFully(buffer);
        return new String(buffer, "UTF-8");
    }

    private byte[] citesteBytes() throws IOException {
        int lungime = in.readInt();
        byte[] buffer = new byte[lungime];
        in.readFully(buffer);
        return buffer;
    }

    public String autentifica(String user, String parola) throws Exception {
        trimite("AUTH|" + user + "|" + parola);
        String raspuns = citesteMesaj();

        if (raspuns.equals("AUTH_OK") || raspuns.equals("AUTH_NEW_OK")) {
            this.username = user;
            this.shareDir = "shares/" + username;
            new File(shareDir).mkdirs();
            publicaFisiere();
            ascultaNotificari();
            pornesteWatcher();
            return "OK";
        } else {
            String motiv = raspuns.contains("|")
                    ? raspuns.split("\\|")[1] : "Eroare necunoscuta";
            return motiv;
        }
    }

    private void publicaFisiere() throws Exception {
        File dir = new File(shareDir);
        String[] fisiere = dir.list((d, name) -> new File(d, name).isFile());
        String listaFisiere = (fisiere != null && fisiere.length > 0)
                ? String.join(",", fisiere) : "";

        if (fisiere != null) {
            for (String f : fisiere) {
                window.getMainPanel().adaugaFisierPropriu(f);
            }
        }

        trimite("PUBLISH|" + listaFisiere);
        String raspuns = citesteMesaj();
        if (raspuns.startsWith("FILE_LIST|")) {
            proceseazaListaInitiala(raspuns.substring("FILE_LIST|".length()));
        }
    }

    private void proceseazaListaInitiala(String lista) {
        if (lista.isEmpty()) return;
        for (String entry : lista.split(";")) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && !parts[1].isEmpty()) {
                window.getMainPanel().adaugaClientNou(parts[0], parts[1].split(","));
            }
        }
    }

    private void pornesteWatcher() {
        if (watcherPornit) return;
        watcherPornit = true;
        watcher = new DirectoryWatcher(shareDir, this);
        new Thread(watcher).start();
    }

    public void notificaFisierAdaugat(String numeFisier) {
        try {
            window.getMainPanel().adaugaFisierPropriu(numeFisier);
            trimite("FILE_ADD|" + numeFisier);
        } catch (IOException e) {
            window.getMainPanel().setStatus("Eroare notificare: " + e.getMessage());
        }
    }

    public void notificaFisierSters(String numeFisier) {
        try {
            window.getMainPanel().stergeFilePropriu(numeFisier);
            trimite("FILE_DEL|" + numeFisier);
        } catch (IOException e) {
            window.getMainPanel().setStatus("Eroare notificare: " + e.getMessage());
        }
    }

    public void adaugaFisier(java.io.File sursa) {
        new Thread(() -> {
            try {
                window.getMainPanel().setStatus("Se copiaza " + sursa.getName() + "...");
                File dest = new File(shareDir + "/" + sursa.getName());
                Files.copy(sursa.toPath(), dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                window.getMainPanel().setStatus("Eroare la adaugare: " + e.getMessage());
            }
        }).start();
    }

    public void descarcaFisier(String ownerUser, String numeFisier) {
        new Thread(() -> {
            try {
                window.getMainPanel().setStatus(
                        "⏳ Se descarca " + numeFisier + " de la " + ownerUser + "...");
                trimite("DOWNLOAD|" + ownerUser + "|" + numeFisier);
            } catch (Exception e) {
                window.getMainPanel().setStatus("Eroare download: " + e.getMessage());
            }
        }).start();
    }

    private void ascultaNotificari() {
        new Thread(() -> {
            try {
                while (conectat) {
                    String mesaj = citesteMesaj();
                    proceseazaNotificare(mesaj);
                }
            } catch (IOException e) {
                if (conectat) {
                    window.getMainPanel().setStatus("Conexiune pierduta.");
                }
            }
        }).start();
    }

    private void proceseazaNotificare(String mesaj) {
        String[] parts = mesaj.split("\\|", -1);
        switch (parts[0]) {
            case "NEW_CLIENT":
                String[] fisiere = parts.length > 2 && !parts[2].isEmpty()
                        ? parts[2].split(",") : new String[]{};
                window.getMainPanel().adaugaClientNou(parts[1], fisiere);
                break;
            case "CLIENT_LEFT":
                window.getMainPanel().stergeClientDinLista(parts[1]);
                break;
            case "FILE_ADD":
                window.getMainPanel().adaugaFisierAlt(parts[1], parts[2]);
                break;
            case "FILE_DEL":
                window.getMainPanel().stergeFileAlt(parts[1], parts[2]);
                break;
            case "FILE_START":
                // Blocam thread-ul de notificari si citim bytes
                primesteFile(parts[1]);
                break;
            case "SEND_FILE":
                // Trimitem fisierul pe un thread nou
                // writeLock permite trimiterea fara sa blocheze thread-ul de notificari
                trimiteFisierLaServer(parts[1]);
                break;
            case "EROARE":
                window.getMainPanel().setStatus("❌ Eroare: " + parts[1]);
                break;
            default:
                System.out.println("[Server necunoscut] " + mesaj);
        }
    }

    private void primesteFile(String numeFisier) {
        try {
            window.getMainPanel().setStatus("⬇ Primesc " + numeFisier + "...");
            byte[] continut = citesteBytes();
            File dest = new File(shareDir + "/" + numeFisier);
            Files.write(dest.toPath(), continut);
            window.getMainPanel().setStatus("✓ Descarcat: " + numeFisier
                    + " (" + continut.length + " bytes)");
            System.out.println("Salvat: " + dest.getAbsolutePath());
        } catch (Exception e) {
            window.getMainPanel().setStatus("❌ Eroare la primire: " + e.getMessage());
            System.out.println("Eroare primesteFile: " + e.getMessage());
        }
    }

    private void trimiteFisierLaServer(String numeFisier) {
        new Thread(() -> {
            try {
                File fisier = new File(shareDir + "/" + numeFisier);
                if (!fisier.exists()) {
                    System.out.println("Fisierul nu exista: " + numeFisier);
                    return;
                }
                byte[] continut = Files.readAllBytes(fisier.toPath());
                System.out.println("Trimit " + continut.length + " bytes: " + numeFisier);
                trimiteBytes(continut);
                System.out.println("Trimis cu succes: " + numeFisier);
            } catch (Exception e) {
                System.out.println("Eroare trimitere: " + e.getMessage());
            }
        }).start();
    }

    public void deconecteaza() {
        conectat = false;
        try {
            if (watcher != null) watcher.opreste();
            trimite("DISCONNECT");
            socket.close();
        } catch (Exception e) {}
        window.getMainPanel().reseteaza();
        window.arataLogin();
    }

    public String getShareDir() { return shareDir; }
    public String getUsername() { return username; }
}