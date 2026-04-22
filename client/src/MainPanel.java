import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class MainPanel extends JPanel {
    private AppWindow window;
    private String username;

    private DefaultListModel<String> myFilesModel;
    private DefaultListModel<String> othersModel;
    private JList<String> myFilesList;
    private JList<String> othersList;
    private JLabel statusLabel;

    public MainPanel(AppWindow window) {
        this.window = window;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        JLabel userLabel = new JLabel("Conectat ca: ");
        userLabel.setName("userLabel");
        JButton exitBtn = new JButton("Deconectare");
        exitBtn.addActionListener(e -> window.getClient().deconecteaza());
        header.add(userLabel, BorderLayout.WEST);
        header.add(exitBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Panoul central cu cele doua liste ──
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));

        // Lista fisierelor proprii
        myFilesModel = new DefaultListModel<>();
        myFilesList = new JList<>(myFilesModel);
        JScrollPane myScroll = new JScrollPane(myFilesList);

        JPanel myPanel = new JPanel(new BorderLayout(0, 5));
        myPanel.setBorder(new TitledBorder("Fișierele tale (trage aici)"));
        myPanel.add(myScroll, BorderLayout.CENTER);

        // Zona de drag and drop
        JLabel dropLabel = new JLabel("📁 Trage fișiere aici", SwingConstants.CENTER);
        dropLabel.setPreferredSize(new Dimension(0, 40));
        dropLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
        myPanel.add(dropLabel, BorderLayout.SOUTH);

        // Activam drag and drop pe intregul panel
        activateDragAndDrop(myPanel);
        activateDragAndDrop(dropLabel);
        activateDragAndDrop(myScroll);

        // Lista fisierelor altora
        othersModel = new DefaultListModel<>();
        othersList = new JList<>(othersModel);
        JScrollPane othersScroll = new JScrollPane(othersList);

        JPanel othersPanel = new JPanel(new BorderLayout(0, 5));
        othersPanel.setBorder(new TitledBorder("Fișiere disponibile"));
        othersPanel.add(othersScroll, BorderLayout.CENTER);

        JButton downloadBtn = new JButton("⬇ Descarcă fișier selectat");
        downloadBtn.addActionListener(e -> descarcaFisierSelectat());
        othersPanel.add(downloadBtn, BorderLayout.SOUTH);

        center.add(myPanel);
        center.add(othersPanel);
        add(center, BorderLayout.CENTER);

        // ── Status bar ──
        statusLabel = new JLabel("Gata.");
        statusLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void activateDragAndDrop(Component comp) {
        new DropTarget(comp, new DropTargetListener() {
            public void dragEnter(DropTargetDragEvent e) { e.acceptDrag(DnDConstants.ACTION_COPY); }
            public void dragOver(DropTargetDragEvent e) {}
            public void dropActionChanged(DropTargetDragEvent e) {}
            public void dragExit(DropTargetEvent e) {}

            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) e.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) {
                        window.getClient().adaugaFisier(f);
                    }
                } catch (Exception ex) {
                    setStatus("Eroare la adaugarea fisierului: " + ex.getMessage());
                }
            }
        });
    }

    private void descarcaFisierSelectat() {
        String selectat = othersList.getSelectedValue();
        if (selectat == null) {
            setStatus("Selecteaza un fisier de descarcat!");
            return;
        }
        // Format in lista: "iustin: muzica.mp3"
        String[] parts = selectat.split(": ", 2);
        if (parts.length < 2) return;

        String ownerUser = parts[0].trim();
        String numeFisier = parts[1].trim();
        setStatus("Se descarca " + numeFisier + " de la " + ownerUser + "...");
        window.getClient().descarcaFisier(ownerUser, numeFisier);
    }

    // ── Metode apelate din Client pentru a actualiza UI ──

    public void setUsername(String username) {
        this.username = username;
        // Actualizam labelul din header
        Component[] comps = ((JPanel)((BorderLayout)getLayout())
                .getLayoutComponent(BorderLayout.NORTH)).getComponents();
        for (Component c : comps) {
            if (c instanceof JLabel) {
                ((JLabel) c).setText("Conectat ca: " + username);
            }
        }
    }

    public void adaugaFisierPropriu(String numeFisier) {
        SwingUtilities.invokeLater(() -> {
            if (!myFilesModel.contains(numeFisier)) {
                myFilesModel.addElement(numeFisier);
            }
            setStatus("Fisier adaugat: " + numeFisier);
        });
    }

    public void stergeFilePropriu(String numeFisier) {
        SwingUtilities.invokeLater(() -> myFilesModel.removeElement(numeFisier));
    }

    public void adaugaClientNou(String user, String[] fisiere) {
        SwingUtilities.invokeLater(() -> {
            for (String f : fisiere) {
                if (!f.isEmpty()) {
                    othersModel.addElement(user + ": " + f);
                }
            }
            setStatus("[+] " + user + " s-a conectat");
        });
    }

    public void stergeClientDinLista(String user) {
        SwingUtilities.invokeLater(() -> {
            for (int i = othersModel.size() - 1; i >= 0; i--) {
                if (othersModel.get(i).startsWith(user + ": ")) {
                    othersModel.remove(i);
                }
            }
            setStatus("[-] " + user + " s-a deconectat");
        });
    }

    public void adaugaFisierAlt(String user, String fisier) {
        SwingUtilities.invokeLater(() -> {
            String entry = user + ": " + fisier;
            if (!othersModel.contains(entry)) {
                othersModel.addElement(entry);
            }
            setStatus("[+] " + user + " a adaugat: " + fisier);
        });
    }

    public void stergeFileAlt(String user, String fisier) {
        SwingUtilities.invokeLater(() -> {
            othersModel.removeElement(user + ": " + fisier);
            setStatus("[-] " + user + " a sters: " + fisier);
        });
    }

    public void setStatus(String mesaj) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(mesaj));
    }

    public void reseteaza() {
        SwingUtilities.invokeLater(() -> {
            myFilesModel.clear();
            othersModel.clear();
            setStatus("Gata.");
        });
    }
}