package org.example.gui;

import org.example.database.DatabaseOperation;
import org.example.engine.WordReplacer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * GUI per popolare documenti con dati da un database.
 * Permette la selezione di un file template, un percorso di destinazione,
 * e la scelta di una scuola per popolare il documento.
 * Include anche un'interfaccia per la lettura dei dati da un file.
 */
public class GUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Componenti per "Popola fogli"
    private JTextField filePathField;
    private JTextField fileNameField;
    private JTextField destinationPathField;
    private JComboBox<String> scuolaDropdown;
    private DefaultComboBoxModel<String> dropdownModel;
    private JTextField searchField;
    private WordReplacer wordReplacer;
    private Connection conn;
    private DatabaseOperation sql;
    private ArrayList<String> scuole = new ArrayList<>();

    // Componenti per "Lettura dati"
    private JTextField uploadFilePathField;
    private JComboBox<String> scuolaDropdownRead;
    private JTextField searchFieldRead;

    /**
     * Costruttore per la classe GUI.
     * Inizializza la connessione al database e carica le scuole.
     *
     * @param conn Connessione al database
     */
    public GUI(Connection conn) {
        this.conn = conn;

        // Imposta look and feel moderno
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.exit(0);
        }

        setTitle("Popola documenti - EUservice");
        setSize(1100, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createPopolaFogliPanel(), "PopolaFogli");
        mainPanel.add(createLetturaDatiPanel(), "LetturaDati");

        // Pulsante per switchare le interfacce
        JButton switchToLetturaDatiButton = new JButton("Passa a Lettura Dati");
        switchToLetturaDatiButton.addActionListener(e -> cardLayout.show(mainPanel, "LetturaDati"));

        JButton switchToPopolaFogliButton = new JButton("Passa a Popola Fogli");
        switchToPopolaFogliButton.addActionListener(e -> cardLayout.show(mainPanel, "PopolaFogli"));

        // Aggiunta dei pulsanti di switch al pannello principale
        JPanel switchPanel = new JPanel();
        switchPanel.add(switchToPopolaFogliButton);
        switchPanel.add(switchToLetturaDatiButton);

        add(switchPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        wordReplacer = new WordReplacer(this.conn);
        sql = new DatabaseOperation(conn);
        ArrayList<Object> denominazioni = sql.selectFromTable("gst_clienti", null);
        for (Object obj : denominazioni) {
            scuole.add((String) obj); // Cast a String
        }

        // Carica i primi 10 elementi nel dropdown
        updateDropdown(scuole.subList(0, Math.min(10, scuole.size())));
    }

    private JPanel createPopolaFogliPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        filePathField = new JTextField(30);
        filePathField.setBorder(new RoundedBorder(15));

        fileNameField = new JTextField(30);
        fileNameField.setBorder(new RoundedBorder(15));

        destinationPathField = new JTextField(30);
        destinationPathField.setBorder(new RoundedBorder(15));

        JButton browseButton = new JButton("Sfoglia");
        JButton destinationButton = new JButton("Scegli destinazione");
        JButton replaceButton = new JButton("Popola documento");

        // Campo di ricerca e dropdown per le scuole
        searchField = new JTextField(20);
        searchField.setBorder(new RoundedBorder(15));
        dropdownModel = new DefaultComboBoxModel<>();
        scuolaDropdown = new JComboBox<>(dropdownModel);
        scuolaDropdown.setPreferredSize(new Dimension(200, 30));

        // Listener per il filtraggio delle scuole durante la digitazione
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterScuole();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterScuole();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterScuole();
            }
        });

        // Listener per la selezione del file da popolare
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
                fileNameField.setText(selectedFile.getName());
            }
        });

        // Listener per la selezione della cartella di destinazione
        destinationButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = folderChooser.getSelectedFile();
                destinationPathField.setText(selectedFolder.getAbsolutePath());
            }
        });

        // Listener per popolare il documento con i dati selezionati
        replaceButton.addActionListener(e -> {
            String filePath = filePathField.getText();
            String fileName = fileNameField.getText();
            String destinationPath = destinationPathField.getText();

            if (!filePath.isEmpty() && !destinationPath.isEmpty()) {
                wordReplacer.setFilePath(filePath);
                wordReplacer.setScuola(Objects.requireNonNull(scuolaDropdown.getSelectedItem()).toString());

                try {
                    wordReplacer.replaceTextInWordFile(destinationPath, fileName);
                    JOptionPane.showMessageDialog(null, "Documento popolato correttamente!");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Errore: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(null, "Completa tutti i campi prima di continuare.");
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Aggiunta dei componenti alla GUI
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Percorso file template:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(filePathField, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Nome file popolato:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(fileNameField, gbc);

        // Allinea il pulsante di destinazione con il campo di input
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Percorso destinazione:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(destinationPathField, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1; // Reset the gridwidth
        panel.add(destinationButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Cerca scuola:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(searchField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Seleziona scuola:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(scuolaDropdown, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(replaceButton, gbc);

        return panel;
    }


    private JPanel createLetturaDatiPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        uploadFilePathField = new JTextField(30);
        uploadFilePathField.setBorder(new RoundedBorder(15));

        JButton browseUploadButton = new JButton("Scegli file da cui leggere dati");
        JButton readButton = new JButton("Leggi file");

        // Filtro e dropdown per le scuole
        searchFieldRead = new JTextField(20);
        searchFieldRead.setBorder(new RoundedBorder(15));
        DefaultComboBoxModel<String> dropdownModelRead = new DefaultComboBoxModel<>();
        scuolaDropdownRead = new JComboBox<>(dropdownModelRead);
        scuolaDropdownRead.setPreferredSize(new Dimension(200, 30));

        // Listener per il filtraggio delle scuole durante la digitazione
        searchFieldRead.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterScuoleRead();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterScuoleRead();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterScuoleRead();
            }
        });

        // Listener per la selezione del file da leggere
        browseUploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    uploadFilePathField.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        // Listener per leggere i dati dal file
        readButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String uploadFilePath = uploadFilePathField.getText();
                String selectedSchool = Objects.requireNonNull(scuolaDropdownRead.getSelectedItem()).toString();

                if (!uploadFilePath.isEmpty()) {
                    wordReplacer.setFilePath(uploadFilePath);
                    wordReplacer.setScuola(Objects.requireNonNull(scuolaDropdown.getSelectedItem()).toString());

                    try {
                        wordReplacer.getValueBookmarks();
                        JOptionPane.showMessageDialog(null, "Dati letti correttamente!");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Errore: " + ex.getMessage());
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Completa tutti i campi prima di continuare.");
                }
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Aggiunta dei componenti alla GUI
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Percorso file da cui leggere dati:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(uploadFilePathField, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panel.add(browseUploadButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Cerca scuola:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(searchFieldRead, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Seleziona scuola:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(scuolaDropdownRead, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(readButton, gbc);

        return panel;
    }

    private void updateDropdown(List<String> filteredScuole) {
        dropdownModel.removeAllElements();
        for (String scuola : filteredScuole) {
            dropdownModel.addElement(scuola);
        }
    }

    private void filterScuole() {
        String searchTerm = searchField.getText().toLowerCase();
        List<String> filteredScuole = scuole.stream()
                .filter(s -> s.toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
        updateDropdown(filteredScuole);
    }

    private void filterScuoleRead() {
        String searchTerm = searchFieldRead.getText().toLowerCase();
        List<String> filteredScuole = scuole.stream()
                .filter(s -> s.toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
        updateDropdownRead(filteredScuole);
    }

    private void updateDropdownRead(List<String> filteredScuole) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) scuolaDropdownRead.getModel();
        model.removeAllElements();
        for (String scuola : filteredScuole) {
            model.addElement(scuola);
        }
    }
}
