import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginPanel extends JPanel {
    private AppWindow window;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JLabel statusLabel;

    public LoginPanel(AppWindow window) {
        this.window = window;
        setLayout(new GridBagLayout()); // centreaza continutul
        buildUI();
    }

    private void buildUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Titlu
        JLabel title = new JLabel("FileShare", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(title, gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Username:"), gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        add(usernameField, gbc);

        // Parola
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Parola:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 2;
        add(passwordField, gbc);

        // Buton conectare
        connectButton = new JButton("Conectare");
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(connectButton, gbc);

        // Status (erori, mesaje)
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 4;
        add(statusLabel, gbc);

        // Actiune buton
        connectButton.addActionListener(e -> incercaConectare());

        // Enter pe campul de parola = click pe buton
        passwordField.addActionListener(e -> incercaConectare());
    }

    private void incercaConectare() {
        String username = usernameField.getText().trim();
        String parola = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || parola.isEmpty()) {
            statusLabel.setText("Completeaza toate campurile!");
            return;
        }

        // Dezactivam butonul cat timp se conecteaza
        connectButton.setEnabled(false);
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setText("Se conecteaza...");

        // Conectarea se face pe un thread separat ca sa nu blocheze GUI
        new Thread(() -> {
            try {
                window.getClient().conecteaza("localhost", 5000);
                String rezultat = window.getClient().autentifica(username, parola);

                SwingUtilities.invokeLater(() -> {
                    if (rezultat.equals("OK")) {
                        window.arataPrincipal(username);
                    } else {
                        statusLabel.setForeground(Color.RED);
                        statusLabel.setText(rezultat);
                        connectButton.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Nu ma pot conecta la server!");
                    connectButton.setEnabled(true);
                });
            }
        }).start();
    }

    public void reseteaza() {
        usernameField.setText("");
        passwordField.setText("");
        statusLabel.setText("");
        connectButton.setEnabled(true);
    }
}