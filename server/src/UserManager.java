import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public class UserManager {
    // Fisierul unde stocam username:hash_parola
    private static final String USERS_FILE = "data/users.txt";

    // Hashuieste o parola cu SHA-256
    public static String hashParola(String parola) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(parola.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Inregistreaza un user nou — returneaza false daca exista deja
    public static synchronized boolean inregistreaza(String username, String parola) throws Exception {
        if (existaUser(username)) return false;

        // Cream folderul data daca nu exista
        new java.io.File("data").mkdirs();

        String hash = hashParola(parola);
        try (FileWriter fw = new FileWriter(USERS_FILE, true)) {
            fw.write(username + ":" + hash + "\n");
        }
        return true;
    }

    // Verifica credentialele — returneaza true daca sunt corecte
    public static synchronized boolean verifica(String username, String parola) throws Exception {
        String hash = hashParola(parola);
        File f = new File(USERS_FILE);
        if (!f.exists()) return false;

        for (String linie : Files.readAllLines(f.toPath())) {
            String[] parts = linie.split(":");
            if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(hash)) {
                return true;
            }
        }
        return false;
    }

    // Verifica daca un username exista deja
    public static synchronized boolean existaUser(String username) throws Exception {
        File f = new File(USERS_FILE);
        if (!f.exists()) return false;

        for (String linie : Files.readAllLines(f.toPath())) {
            String[] parts = linie.split(":");
            if (parts.length >= 1 && parts[0].equals(username)) return true;
        }
        return false;
    }
}