package com.holiholic.database.database;
import com.holiholic.database.auth.DatabaseConnection;
import java.sql.Statement;
import java.util.logging.Level;

/* QueryVisitTask - This is used for executing the query
 *
 */
public class QueryVisitTask implements Runnable{
    private String query;

    public QueryVisitTask(String query) {
        this.query = query;
    }

    @Override
    public void run() {

        try {
            Statement stmt = DatabaseConnection.getConnection().createStatement();
            stmt.executeUpdate(this.query);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            DatabaseManager.logMessage(Level.SEVERE, query);
        }

    }
}
