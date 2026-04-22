import javax.swing.*;
import java.awt.*;

public class AppWindow extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private LoginPanel loginPanel;
    private MainPanel mainPanel;
    private Client client;

    public AppWindow() {
        setTitle("FileShare");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null); // centrat pe ecran
        setResizable(false);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        client = new Client(this);

        loginPanel = new LoginPanel(this);
        mainPanel = new MainPanel(this);

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(mainPanel, "MAIN");

        add(mainContainer);

        // Pornim pe ecranul de login
        cardLayout.show(mainContainer, "LOGIN");

        setVisible(true);
    }

    // Trece la ecranul principal dupa login reusit
    public void arataPrincipal(String username) {
        mainPanel.setUsername(username);
        cardLayout.show(mainContainer, "MAIN");
        setTitle("FileShare - " + username);
    }

    // Trece inapoi la login
    public void arataLogin() {
        cardLayout.show(mainContainer, "LOGIN");
        setTitle("FileShare");
    }

    public Client getClient() {
        return client;
    }

    public MainPanel getMainPanel() {
        return mainPanel;
    }

    public static void main(String[] args) {
        // Rulam GUI pe thread-ul de evenimente al Swing
        SwingUtilities.invokeLater(AppWindow::new);
    }
}