package home.db;

import org.h2.tools.RunScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by general on 2016/10/28.
 */
public class DataBaseHelper {
    private Connection conn = null;
    private static String JDBCDriver = null;
    private final static String DEFAULT_DB_NAME = "files.mv.db";
    private List<Statement> statements = new ArrayList();

    public Connection getConn() {
        if (conn != null){
            return conn;
        } else {
            try {
                return DriverManager.getConnection(JDBCDriver);
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        // should reach this point
        assert false : "Reached an unreachable part of code.";
        return null;
    }

    public DataBaseHelper() {
        this(DEFAULT_DB_NAME);
    }

    public DataBaseHelper(String outDbName){
        try {
            init(outDbName);
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void init(String outDbName) throws SQLException {
        Path dbfile = FileSystems.getDefault().getPath(outDbName);
        String dbname = dbfile.toAbsolutePath().toString();
        if (outDbName.endsWith(".mv.db")){
            JDBCDriver = "jdbc:h2:" + dbname.substring(0, dbname.length() - 6); // + ";MV_STORE=FALSE;MVCC=FALSE";
        } else {
            JDBCDriver = "jdbc:h2:" + dbname; // + ";MV_STORE=FALSE ;MVCC=FALSE";
            dbfile = FileSystems.getDefault().getPath(dbname+=".mv.db");
        }

        boolean isInitNeeded = false;
        InputStream is;

        if (Files.notExists(dbfile)){
            isInitNeeded = true;
        }

        conn = DriverManager.getConnection(JDBCDriver);
        if (isInitNeeded){
            is = DataBaseHelper.class.getClassLoader().getResourceAsStream("init.sql");
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr);
                 ){
                RunScript.execute(conn, br);
            } catch (IOException | SQLException e12){
                e12.printStackTrace();
            } finally {
                if (is != null){
                    try {
                        is.close();
                    } catch (IOException e){
                        assert false : "IOException while closing input stream. Shouldn't ever get here.";
                        throw new IllegalStateException();
                    }
                }
            }
        }
        }

    public Connection nextConnection() throws SQLException{
        if (this.conn != null) {
            this.conn.close();
            this.conn = null;
        }

        this.conn = DriverManager.getConnection(JDBCDriver);
        return this.conn;
    }

    public Statement nextStatement() throws SQLException {
        Statement s;
        if (this.conn != null) {
            s = this.conn.createStatement();
        } else {
            s = this.nextConnection().createStatement();
        }
        statements.add(s);
        return s;
    }

    public void cleanUp() {

        statements.stream().forEach(statement -> {
            if (statement != null){
                try {
                    statement.close();
                } catch (SQLException sqle){
                    assert false : "Tried to close an already closed statement.";
                    sqle.printStackTrace();
                }
                statement = null;
            }
        });
    }
}
