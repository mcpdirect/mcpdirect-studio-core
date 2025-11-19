package appnet.hstp.labs.util.db.sqlite;
import appnet.hstp.labs.util.db.*;

import java.sql.*;

import static appnet.hstp.labs.util.db.sqlite.SQLiteUpdate.createUpdateTableResultSet;
import static appnet.hstp.labs.util.db.sqlite.SQLiteUpdate.executeUpdate;

class SQLiteDelete<T> extends SQLiteWhereClause<T> implements Delete<T> {
    public SQLiteDelete(Connection connection, String tableName) {
        super(connection,tableName);
    }

//    @Override
//    public WhereClause where(ComparisonOperator operator) {
//        conditions.add(operator);
//        return new SQLiteWhereClause(connection, tableName, operator, null) {
//            @Override
//            public ResultSet execute() throws SQLException {
//                SQLiteDelete.this.execute();
//                return null;
//            }
//        };
//    }


    @Override
    public TableResultSet<T> createTableResultSet(Object result) {
        return createUpdateTableResultSet(result);
    }

    @Override
    public TableResultSet<T> execute() throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName);

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
        buildWhereClause(sql);
        System.out.println(sql);
        int paramIndex = 1;
        if(connection!=null) try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
//            for (ComparisonOperator condition : conditions) {
//                if (condition.column.startsWith("$.")) {
////                    String[] parts = condition.column.split("\\.", 2);
////                    String jsonPath = "$." + parts[1];
//                    String column = condition.column.substring(2);
//                    int i = column.lastIndexOf('.');
//                    String jsonPath = "$." + column.substring(i + 1);
////                    column = column.substring(0, i);
//                    stmt.setString(paramIndex++, jsonPath);
//                }
//                stmt.setObject(paramIndex++, condition.value);
//            }
//            stmt.executeUpdate();
            int rows = executeUpdate(conditions,stmt,paramIndex);
            return createUpdateTableResultSet(rows);
        }else printConditionParameters(conditions);
        return createUpdateTableResultSet(-1);
    }

//    @Override
//    public <E> List<E> execute(Class<E> type) throws Exception {
//        return List.of();
//    }
    //    private String buildCondition(ComparisonOperator operator) {
//        if (operator.column.startsWith("$.")) {
//            String[] parts = operator.column.split("\\.", 2);
//            String column = parts[0].substring(1);
//            String jsonPath = "$." + parts[1];
//            return String.format("json_extract(%s, ?) %s ?", column, operator.operator);
//        }
//        return String.format("%s %s ?", operator.column, operator.operator);
//    }
//    static class User{}
//    public static void main(String[] args) throws Exception {
//// Initialize connection
////        Connection connection = DriverManager.getConnection("jdbc:sqlite:mydatabase.db");
//
//// Create a table
//        Table<User> userTable = new SQLiteTable<>(null, "users", User.class)
//                .column("id", "INTEGER", true)
//                .column("name", "TEXT", false)
//                .column("metadata", "TEXT", false)
//                .index( false, "name")
//                .create();
//
//// Insert a user
//        userTable.insert("name", "John Doe")
//                .set("metadata", "{\"age\":30,\"roles\":[\"admin\"]}")
//                .execute();
//
//// Query users
//        List<User> users = userTable.select(ComparisonOperator.like("name", "John%"))
//                .and(ComparisonOperator.gt("$.metadata.age", 25))
//                .execute(User.class);
//
//// Update users
//        userTable.update("name", "Johnathan Doe")
//                .and(ComparisonOperator.eq("name", "John Doe"))
//                .and(ComparisonOperator.lt("$.metadata.age", 65))
//                .execute();
//
//// Delete users
//        userTable.delete(ComparisonOperator.lt("$.metadata.age", 65))
//                .execute();
//    }
}