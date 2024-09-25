package org.example.engine;

import org.apache.poi.xwpf.usermodel.*;
import org.example.database.DatabaseOperation;
import org.example.utils.StringUtils;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;

/**
 * La classe WordReplacer permette di effettuare sostituzioni di testo all'interno di file Word utilizzando segnalibri
 * e parametri specifici presi da un database.
 */
public class WordReplacer extends JFrame {

    private String filePath;
    private final List<String> bookmarksArray;
    private Hashtable<String, String> replace;
    private Connection conn;
    private String scuola;

    /**
     * Costruttore della classe WordReplacer.
     * @param conn La connessione al database per le operazioni di query.
     */
    public WordReplacer(Connection conn) {
        bookmarksArray = new ArrayList<>();
        bookmarksArray.add("indirizzo");
        bookmarksArray.add("istituzione");
        bookmarksArray.add("dir_gen");
        replace = new Hashtable<>();
        this.conn = conn;
    }

    /**
     * Crea una tabella di sostituzione per il testo basata sulle informazioni della scuola.
     * @param scuola La denominazione della scuola usata per recuperare i dati dal database.
     * @return Un hashtable con i segnaposto e i valori da sostituire.
     */
    private Hashtable<String, String> createReplace(String scuola) {
        Hashtable<String, String> replace = new Hashtable<>();
        DatabaseOperation sql = new DatabaseOperation(this.conn);

        Hashtable<String, String> conditions = new Hashtable<>();
        conditions.put("denominazione", scuola);

        ArrayList<Object> results = sql.selectFromTable("gst_clienti", conditions);

        for (Object row : results) {
            Object[] data = (Object[]) row;

            // Accedi ai valori del database e li inserisci nella tabella di sostituzione.
            String indirizzo = (String) data[4];
            replace.put("{{denominazione}}", scuola);
            replace.put("{{indirizzo}}", StringUtils.capitalizeFirstLetter(indirizzo));
        }

        return replace;
    }

    /**
     * Restituisce il percorso del file Word.
     * @return Il percorso del file.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Imposta il percorso del file Word.
     * @param filePath Il percorso del file da modificare.
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Restituisce la denominazione della scuola.
     * @return Il nome della scuola.
     */
    public String getScuola() {
        return scuola;
    }

    /**
     * Imposta la denominazione della scuola.
     * @param scuola Il nome della scuola.
     */
    public void setScuola(String scuola) {
        this.scuola = scuola;
    }

    /**
     * Sostituisce il testo all'interno di un file Word basato sui segnaposto e salva il documento modificato.
     * @param destinationPath Il percorso di destinazione del file modificato.
     * @param fileName Il nome del file di destinazione.
     * @throws IOException Se si verifica un errore durante la lettura o la scrittura del file.
     */
    public void replaceTextInWordFile(String destinationPath, String fileName) throws IOException {
        this.replace = this.createReplace(this.getScuola());
        File destinationFile = new File(destinationPath, fileName);

        try (FileInputStream fis = new FileInputStream(this.filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Sostituzione del testo nei paragrafi e nelle tabelle
            replaceTextInParagraphs(document);
            replaceTextInTables(document);

            // Salvataggio del documento
            try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
                document.write(fos);
            }
        }

    }

    /**
     * Sostituisce il testo nei paragrafi del documento.
     * @param document Il documento Word nel quale effettuare la sostituzione.
     */
    private void replaceTextInParagraphs(XWPFDocument document) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceTextInRuns(paragraph);
        }
    }

    /**
     * Sostituisce il testo nelle tabelle del documento Word.
     * @param document Il documento Word nel quale effettuare la sostituzione nelle tabelle.
     */
    private void replaceTextInTables(XWPFDocument document) {
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceTextInRuns(paragraph);
                    }
                }
            }
        }
    }

    /**
     * Sostituisce il testo nei run all'interno di un paragrafo.
     * @param paragraph Il paragrafo nel quale sostituire il testo.
     */
    private void replaceTextInRuns(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null) {
                for (Map.Entry<String, String> entry : replace.entrySet()) {
                    if (text.contains(entry.getKey())) {
                        text = text.replace(entry.getKey(), entry.getValue());
                    }
                }
                run.setText(text, 0);
            }
        }
    }

    /**
     * Recupera e stampa il testo associato ai segnalibri nel file Word.
     * @throws IOException Se si verifica un errore durante la lettura del file.
     */
    public void getValueBookmarks() throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            Map<String, String> bookmarks = getBookmarksText(document, bookmarksArray);

            for (Map.Entry<String, String> entry : bookmarks.entrySet()) {
                System.out.println("Text in bookmark '" + entry.getKey() + "': " + entry.getValue());
            }
        }
    }

    /**
     * Recupera il testo associato ai segnalibri specificati all'interno del documento Word, incluse le tabelle.
     * @param document Il documento Word dal quale estrarre i segnalibri.
     * @param bookmarkNames La lista di nomi dei segnalibri da cercare.
     * @return Una mappa che associa i nomi dei segnalibri ai rispettivi testi.
     */
    private static Map<String, String> getBookmarksText(XWPFDocument document, List<String> bookmarkNames) {
        Map<String, String> bookmarks = new HashMap<>();

        // Inizializza la mappa con i nomi dei segnalibri
        for (String bookmarkName : bookmarkNames) {
            bookmarks.put(bookmarkName, null);
        }

        // Cerca i segnalibri nei paragrafi
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            checkParagraphForBookmarks(paragraph, bookmarks);
        }

        // Cerca i segnalibri nelle tabelle
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        checkParagraphForBookmarks(paragraph, bookmarks);
                    }
                }
            }
        }

        return bookmarks;
    }

    /**
     * Controlla un paragrafo per la presenza di segnalibri e aggiorna la mappa.
     * @param paragraph Il paragrafo da controllare.
     * @param bookmarks La mappa dei segnalibri e dei loro testi.
     */
    private static void checkParagraphForBookmarks(XWPFParagraph paragraph, Map<String, String> bookmarks) {
        for (XWPFRun run : paragraph.getRuns()) {
            CTP ctp = paragraph.getCTP();
            if (ctp.getBookmarkStartList() != null) {
                for (CTBookmark bookmark : ctp.getBookmarkStartList()) {
                    String name = bookmark.getName();
                    if (bookmarks.containsKey(name)) {
                        bookmarks.put(name, run.toString());
                    }
                }
            }
        }
    }

}
