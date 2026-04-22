import java.io.*;
import java.nio.file.*;

public class DirectoryWatcher implements Runnable {
    private String shareDir;
    private Client client;
    private boolean running = true;

    public DirectoryWatcher(String shareDir, Client client) {
        this.shareDir = shareDir;
        this.client = client;
    }

    public void opreste() {
        running = false;
    }

    @Override
    public void run() {
        try {
            Path cale = Paths.get(shareDir);
            WatchService watchService = FileSystems.getDefault().newWatchService();

            // Inregistram folderul pentru evenimente de creare si stergere
            cale.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);

            System.out.println("Monitorizez folderul: " + cale.toAbsolutePath());

            while (running) {
                // Asteptam un eveniment (max 1 secunda, apoi verificam daca mai rulam)
                WatchKey key = watchService.poll(
                        1, java.util.concurrent.TimeUnit.SECONDS);

                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    String numeFisier = event.context().toString();

                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        // Verificam ca e fisier, nu director
                        File f = new File(shareDir + "/" + numeFisier);
                        if (f.isFile()) {
                            System.out.println("Fisier nou detectat: " + numeFisier);
                            client.notificaFisierAdaugat(numeFisier);
                        }
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        System.out.println("Fisier sters detectat: " + numeFisier);
                        client.notificaFisierSters(numeFisier);
                    }
                }

                key.reset();
            }

            watchService.close();

        } catch (Exception e) {
            System.out.println("Eroare DirectoryWatcher: " + e.getMessage());
        }
    }
}