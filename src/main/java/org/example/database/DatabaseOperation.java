package org.example.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Classe per operazioni di selezione dati dal database.
 * Fornisce un metodo per eseguire query su una tabella e restituire risultati in base a condizioni specifiche.
 */
public class DatabaseOperation {
    private Connection conn;

    /**
     * Costruttore che inizializza la connessione al database.
     *
     * @param conn la connessione al database.
     */
    public DatabaseOperation(Connection conn) {
        this.conn = conn;
    }

    /**
     * Restituisce la connessione corrente.
     *
     * @return la connessione al database.
     */
    public Connection getConn() {
        return conn;
    }

    /**
     * Imposta una nuova connessione al database.
     *
     * @param conn la nuova connessione da impostare.
     */
    public void setConn(Connection conn) {
        this.conn = conn;
    }

    /**
     * Esegue una query di selezione su una tabella e restituisce i risultati in base alle condizioni specificate.
     * Se non ci sono condizioni (`where` è null o vuoto), restituisce solo la colonna "denominazione".
     * Altrimenti, restituisce l'intero record.
     *
     * @param table il nome della tabella su cui eseguire la query.
     * @param where una tabella hash che contiene le condizioni per la clausola WHERE (può essere null).
     * @return una lista di oggetti contenenti i risultati della query.
     */
    public ArrayList<Object> selectFromTable(String table, Hashtable<String, String> where) {
        ArrayList<Object> list = new ArrayList<>();
        Statement stmt = null;

        try {
            stmt = this.conn.createStatement();

            // Costruisce la query di selezione
            StringBuilder query;
            if (where == null || where.isEmpty()) {
                // Se non ci sono condizioni, seleziona solo la colonna "denominazione"
                query = new StringBuilder("SELECT denominazione FROM " + table);
            } else {
                // Se ci sono condizioni, seleziona tutto
                query = new StringBuilder("SELECT * FROM " + table);

                // Aggiunge la clausola WHERE con le condizioni
                query.append(" WHERE ");

                // Aggiunge tutte le condizioni della clausola WHERE
                ArrayList<String> conditions = new ArrayList<>();
                for (String key : where.keySet()) {
                    String value = where.get(key);
                    conditions.add(key + " = '" + value + "'");
                }

                // Unisce le condizioni con " AND "
                query.append(String.join(" AND ", conditions));
            }

            // Esegue la query
            ResultSet rs = stmt.executeQuery(query.toString());

            // Elabora i risultati
            if (where == null || where.isEmpty()) {
                // Se non ci sono condizioni, ritorna solo la colonna "denominazione"
                while (rs.next()) {
                    String denominazione = rs.getString("denominazione");
                    list.add(denominazione);
                }
            } else {
                // Se ci sono condizioni, ritorna l'intero record
                while (rs.next()) {
                    // Crea un array con tutte le colonne della tabella
                    Object[] row = new Object[]{
                            rs.getInt("id"),
                            rs.getString("codice"),
                            rs.getInt("d"),
                            rs.getString("denominazione"),
                            rs.getString("indirizzo"),
                            rs.getString("cap"),
                            rs.getString("comune"),
                            rs.getString("prova"),
                            rs.getString("pivacf"),
                            rs.getInt("Ambito"),
                            rs.getString("tel"),
                            rs.getString("email"),
                            rs.getString("Email_2")
                    };
                    list.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Chiude le risorse aperte
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

}
