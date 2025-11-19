package appnet.hstp.labs.util.db.sqlite;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.labs.util.db.ComparisonOperator;
import appnet.hstp.labs.util.db.TableResultSet;
import appnet.hstp.labs.util.db.Update;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

class SQLiteUpdate<T> extends SQLiteWhereClause<T> implements Update<T> {
    private final Map<String, Object> updates = new LinkedHashMap<>();

    public SQLiteUpdate(Connection connection, String tableName) {
        super(connection,tableName);
    }

    @Override
    public Update<T> set(String column, Object value) throws Exception {
        set(updates,column, value);
        return this;
    }

    public static void set(Map<String, Object> values, String column, Object value) throws Exception {
        column = column.trim().toLowerCase();
        if(!(value instanceof Number)&&!(value instanceof String)
                &&!(value instanceof Boolean)&&!(value instanceof byte[])){
            value = JSON.toJson(value);
        }
        values.put(column, value);
    }

    public static <E> TableResultSet<E> createUpdateTableResultSet(Object result) {
        return new TableResultSet<E>() {
            @Override
            public int updatedRows() {
                return (int)result;
            }
        };
    }

    @Override
    public TableResultSet<T> createTableResultSet(Object result) {
        return createUpdateTableResultSet(result);
    }

    @Override
    public TableResultSet<T> execute() throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(tableName)
                .append(" SET ");

        // Add SET clauses
        List<String> setClauses = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (entry.getKey().startsWith("$.")) {
                // Handle JSON path updates
//                String[] parts = entry.getKey().split("\\.", 2);
//                String column = parts[0].substring(1);
//                String jsonPath = "$." + parts[1];
                String column = entry.getKey().substring(2);
                int i = column.lastIndexOf('.');
//            String jsonPath = "$." + column.substring(i + 1);
                column = column.substring(0, i);
                setClauses.add(column + " = json_set(" + column + ", ?, ?)");
            } else {
                setClauses.add(entry.getKey() + " = ?");
            }
        }
        sql.append(String.join(", ", setClauses));

        // Add WHERE clauses
        buildWhereClause(sql);
//        if (!conditions.isEmpty()) {
//            sql.append(" WHERE ");
//            sql.append(buildCondition(conditions.get(0)));
//
//            for (int i = 1; i < conditions.size(); i++) {
//                String op = (i-1 < logicalOperators.size()) ? logicalOperators.get(i-1) : "AND";
//                sql.append(" ").append(op).append(" ");
//                sql.append(buildCondition(conditions.get(i)));
//            }
//        }

        System.out.println(sql);
        int paramIndex = 1;
        PreparedStatement stmt = null;
        if(connection!=null) {
            stmt = connection.prepareStatement(sql.toString());
        }
        if(stmt!=null) {
            // Set update values
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                if (entry.getKey().startsWith("$.")) {
                    String column = entry.getKey().substring(2);
                    int i = column.lastIndexOf('.');
                    String jsonPath = "$." + column.substring(i + 1);
//                column = column.substring(0, i);
                    stmt.setString(paramIndex++, jsonPath);
                }
                stmt.setObject(paramIndex++, entry.getValue());
            }

            // Set condition values
//            for (ComparisonOperator condition : conditions) {
//                if (condition.column.startsWith("$.")) {
//                    String column = condition.column.substring(2);
//                    int i = column.lastIndexOf('.');
//                    String jsonPath = "$." + column.substring(i + 1);
//                    stmt.setString(paramIndex++, jsonPath);
//                }
//                stmt.setObject(paramIndex++, condition.value);
//            }
//
//            stmt.executeUpdate();
            int rows = executeUpdate(conditions,stmt,paramIndex);
            return createTableResultSet(rows);
        }else for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (entry.getKey().startsWith("$.")) {
                String column = entry.getKey().substring(2);
                int i = column.lastIndexOf('.');
                String jsonPath = "$." + column.substring(i + 1);
//                column = column.substring(0, i);
                System.out.println((paramIndex++) + " : " + jsonPath);
            }
            System.out.println((paramIndex++) + " : " + entry.getValue());
        }

        return createTableResultSet(-1);
    }
    public static int executeUpdate(List<ComparisonOperator> conditions,PreparedStatement stmt, int paramIndex) throws SQLException {
        for (ComparisonOperator condition : conditions) {
            if (condition.column.startsWith("$.")) {
//                    String[] parts = condition.column.split("\\.", 2);
//                    String jsonPath = "$." + parts[1];
                String column = condition.column.substring(2);
                int i = column.lastIndexOf('.');
                String jsonPath = "$." + column.substring(i + 1);
//                    column = column.substring(0, i);
                stmt.setString(paramIndex++, jsonPath);
            }
            stmt.setObject(paramIndex++, condition.value);
        }
        return stmt.executeUpdate();
    }
//    private String buildCondition(ComparisonOperator operator) {
//        if (operator.column.startsWith("$.")) {
//            String[] parts = operator.column.split("\\.", 2);
//            String column = parts[0].substring(1);
//            String jsonPath = "$." + parts[1];
//            return String.format("json_extract(%s, ?) %s ?", column, operator.operator);
//        }
//        return String.format("%s %s ?", operator.column, operator.operator);
//    }

//    @Override
//    public <E> List<E> execute(Class<E> type) throws Exception {
//        return List.of();
//    }
}