package appnet.hstp.labs.util.db.sqlite;
import appnet.hstp.labs.util.db.*;

import java.sql.*;
import java.util.HashSet;

public class SQLiteHelper {
    private final Connection connection;
    public SQLiteHelper(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
    public <T> Table<T> table(String table,Class<T> type)  throws Exception {
        return new SQLiteTable<>(connection,table,type);
    }
    public boolean tableExists(String table) throws Exception {
        String sql = "SELECT COUNT(*)  FROM sqlite_master WHERE type = 'table' AND name = '"+table+"'";
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        return resultSet.next();
    }
    public boolean tableColumnExists(String table, String column) throws Exception {
        String sql = "PRAGMA table_info('"+table+"')";
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        while (resultSet.next()){
            String name = resultSet.getString("name");
            if(name.equals(column)){
                return true;
            }
        }
        return false;
    }
    public HashSet<String> tableColumns(String table) throws Exception {
        HashSet<String> set = new HashSet<>();
        ResultSet resultSet = connection.createStatement().executeQuery("PRAGMA table_info('"+table.trim().toLowerCase()+"')");
        while (resultSet.next()){
            set.add(resultSet.getString("name"));
        }
        return set;
    }
}




