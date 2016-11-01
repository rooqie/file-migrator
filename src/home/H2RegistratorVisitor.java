package home;

import home.db.DataBaseHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by general on 2016/10/28.
 */
public class H2RegistratorVisitor extends SimpleFileVisitor<Path> {
    //TODO: shoud keep as static?
    private static DataBaseHelper dbh;

    public H2RegistratorVisitor(){
        dbh = new DataBaseHelper();
    }

    public H2RegistratorVisitor(String outDbName){
        dbh = new DataBaseHelper(outDbName);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs){
        try(PreparedStatement prep = dbh.getConn().prepareStatement(
                "insert into FILES (NAME, PATH, SIZE, CHECKSUM) values(?, ?, ?, ?)")){

            prep.setString(1, sanitizeName(file.getFileName().toString()));
            prep.setString(2, sanitizeName(file.toAbsolutePath().toString()));
            prep.setLong(3, attrs.size());
            prep.setBytes(4, calculateChecksum(file));

            prep.execute();

        } catch (SQLException se) {
            se.printStackTrace();
            return FileVisitResult.CONTINUE.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    private byte[] calculateChecksum(Path p) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        try(InputStream is = Files.newInputStream(p); DigestInputStream dis = new DigestInputStream(is, md); BufferedInputStream bis = new BufferedInputStream(dis)){
            while (true){
                if (bis.read() == -1) break;
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return md.digest();
    }

    private String sanitizeName(String originalName){
        if (originalName != null && (originalName.indexOf('\'') >= 0)){
            return originalName.replaceAll("'", "''");
        }
        return originalName;
    }

    public static DataBaseHelper getDbh() {
        return dbh;
    }
}
