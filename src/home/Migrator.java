package home;

import home.cli.CliHelper;
import home.db.DataBaseHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import static java.lang.System.out;

public class Migrator {
    public static void main(String[] args) throws IOException, SQLException {
        Migrator m = new Migrator();
        CliHelper.initFromCommandLine(args);
        H2RegistratorVisitor visitor;
        Path dir;
        String dbOut = null;
        String dbIn;

        switch (CliHelper.getMode()){
            case READ_TO_FILE:
                dbOut = CliHelper.getOutputFile();
            case READ_DEFAULT:
                dir = Paths.get(CliHelper.getTargetPath());
                visitor = (dbOut != null) ? new H2RegistratorVisitor(dbOut) : new H2RegistratorVisitor();
                m.walk(dir, visitor);
                break;
            case COMPARE_FILE_FOLDER:
                dir = Paths.get(CliHelper.getTargetPath());
                visitor =  new H2RegistratorVisitor();
                m.walk(dir, visitor); // we have files.mv.db ready to compare
                dbIn = CliHelper.getInputFile();
                m.compare(Paths.get("files.mv.db"), Paths.get(dbIn));
                break;
            case COMPARE_FILE_FILE:
                dbOut = CliHelper.getOutputFile();
                dbIn = CliHelper.getInputFile();
                m.compare(Paths.get(dbOut), Paths.get(dbIn));
        }
    }

    private void compare(Path db1, Path db2){
        DataBaseHelper dbh1 = new DataBaseHelper(db1.toString());
        DataBaseHelper dbh2 = new DataBaseHelper(db2.toString());

        try (Statement statement = dbh1.nextStatement();
             PreparedStatement preparedStatement = dbh2.getConn().prepareStatement("SELECT name, path FROM files where checksum=? and size=?")){
            ResultSet rs = statement.executeQuery("SELECT name, path, size, checksum FROM files");
            while (rs.next()){
                String name = rs.getString(1);
                String path = rs.getString(2);
                int size = rs.getInt(3);
                byte[] md5 = rs.getBytes(4);

                preparedStatement.setBytes(1, md5);
                preparedStatement.setInt(2, size);
                ResultSet matchRs = preparedStatement.executeQuery();
                if (matchRs.next()){
                    if (!name.equals(matchRs.getString(1))){
                        out.printf("Name mismatch: \"%s\" != \"%s\"%n", name, matchRs.getString(1));
                        out.println("First -> " + path);
                        out.println("Second -> " + matchRs.getString(2));
                    }
                } else {
                    out.printf("Not found file \"%s\"%n", path);
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private void walk(Path dir, H2RegistratorVisitor visitor) throws IOException {
        assert dir != null : "Target dir is null";
        if (Files.notExists(dir)){
            out.println(String.format("Unable to find %s", dir.getFileName()));
        } else {
            assert visitor != null : "Visitor instance is null";
            Files.walkFileTree(dir, visitor);
            visitor.getDbh().cleanUp();
        }
    }
}
