package appnet.hstp.labs.util.db.sqlite;

import appnet.hstp.labs.util.db.Insert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class SQLiteInsert implements Insert {
        private final Connection connection;
        private final String tableName;
        private final Map<String, Object> values = new LinkedHashMap<>();

        public SQLiteInsert(Connection connection, String tableName) {
            this.connection = connection;
            this.tableName = tableName;
        }

        @Override
        public Insert value(String column, Object value) throws Exception{
//            column = column.trim().toLowerCase();
//            if(!(value instanceof Number)&&!(value instanceof String)
//                    &&!(value instanceof Boolean)&&!(value instanceof byte[])){
//                value = JSON.toJson(value);
//            }
//            values.put(column, value);
            SQLiteUpdate.set(values,column,value);
            return this;
        }

    private boolean replaceIfExists;
    @Override
    public Insert replaceIfExists() throws Exception {
        this.replaceIfExists = true;
        return this;
    }

    @Override
        public boolean execute() throws Exception {
            String columns = String.join(", ", values.keySet());
            String placeholders = String.join(", ",
                    Collections.nCopies(values.size(), "?"));

            String sql = String.format("INSERT "+(replaceIfExists?"OR REPLACE":"")+" INTO %s (%s) VALUES (%s)",
                    tableName, columns, placeholders);

            if(connection!=null)try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int i = 1;
                for (Object value : values.values()) {
                    stmt.setObject(i++, value);
                }
                boolean result =  stmt.executeUpdate()>0;
                return result;
            }else{
                System.out.println(sql);
                int i = 1;
                for (Object value : values.values()) {
                    System.out.println((i++)+" : "+value);
                }
                return false;
            }
        }
    }