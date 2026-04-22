import java.util.*;
import java.util.concurrent.*;

public class FileRegistry {
    private static ConcurrentHashMap<String, List<String>> registry = new ConcurrentHashMap<>();

    public static void publicaFisiere(String username, List<String> fisiere) {
        registry.put(username, new ArrayList<>(fisiere));
    }

    public static void adaugaFisier(String username, String numeFisier) {
        registry.computeIfAbsent(username, k -> new ArrayList<>()).add(numeFisier);
    }

    public static void stergeFisier(String username, String numeFisier) {
        List<String> fisiere = registry.get(username);
        if (fisiere != null) fisiere.remove(numeFisier);
    }

    public static void stergeUser(String username) {
        registry.remove(username);
    }

    public static String getListaAltora(String username) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : registry.entrySet()) {
            if (entry.getKey().equals(username)) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(String.join(",", entry.getValue()));
        }
        return sb.toString();
    }

    public static List<String> getFisiere(String username) {
        return registry.getOrDefault(username, new ArrayList<>());
    }

    public static boolean areUser(String username) {
        return registry.containsKey(username);
    }
}