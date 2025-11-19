package appnet.hstp.labs.util.db.sqlite;
import appnet.hstp.labs.util.db.*;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class SQLiteWhereClause<T> implements WhereClause<T> {
    private static final Logger LOG = LoggerFactory.getLogger(WhereClause.class);
    protected final Connection connection;
    protected final String tableName;
    protected final String[] columns;

    protected final List<ComparisonOperator> conditions = new ArrayList<>();
    protected final List<String> logicalOperators = new ArrayList<>();

    public SQLiteWhereClause(Connection connection, String tableName,String... columns) {
        this.connection = connection;
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public WhereClause<T> and(ComparisonOperator operator) {
        conditions.add(operator);
        logicalOperators.add("AND");
        return this;
    }

    @Override
    public WhereClause<T> or(ComparisonOperator operator) {
        conditions.add(operator);
        logicalOperators.add("OR");
        return this;
    }

    @Override
    public WhereClause<T> and(WhereConditions other) {
        conditions.addAll(other.getConditions());
        logicalOperators.add("AND");
        logicalOperators.addAll(other.getLogicalOperators());
        return this;
    }

    @Override
    public WhereClause<T> or(WhereConditions other) {
        conditions.addAll(other.getConditions());
        logicalOperators.add("OR");
        logicalOperators.addAll(other.getLogicalOperators());
        return this;
    }



    @Override
    public TableResultSet<T> execute() throws Exception {
        StringBuilder columns = new StringBuilder();
        if(this.columns != null) for (String column : this.columns)
        if(!(column=column.trim()).isEmpty()){
            if(columns.isEmpty()){
                columns = new StringBuilder(column);
            }else{
                columns.append(",").append(column);
            }
        }
        if(columns.isEmpty()) columns.append("*");
        StringBuilder sql = new StringBuilder("SELECT "+columns+" FROM ").append(tableName);
        buildWhereClause(sql);
        System.out.println(sql);
        setParameters(null);
        if(connection!=null) try(PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            setParameters(stmt);
            ResultSet set=  stmt.executeQuery();
            return createTableResultSet(set);

        }
        // else{
        //     System.out.println(sql);
        //     setParameters(null);
        // }
        return createTableResultSet(null);
    }

//    @Override
//    public <E> List<E> execute(Class<E> type) throws Exception {
//        try (ResultSet rs = execute()) {
//            return mapResultSetToList(rs, type);
//        }
//    }

    protected void buildWhereClause(StringBuilder sql) {
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(buildCondition(conditions.get(0)));

            for (int i = 1; i < conditions.size(); i++) {
                sql.append(" ").append(logicalOperators.get(i-1)).append(" ");
                sql.append(buildCondition(conditions.get(i)));
            }
        }
    }

    protected String buildCondition(ComparisonOperator operator) {
        if (operator.column.startsWith("$.")) {
//            String[] parts = operator.column.split("\\.", 2);
            String column = operator.column.substring(2);
            int i = column.lastIndexOf('.');
//            String jsonPath = "$." + column.substring(i + 1);
            column = column.substring(0, i);
            return String.format("json_extract(%s, ?) %s ?", column, operator.operator);
        }
        return String.format("%s %s ?", operator.column, operator.operator);
    }

    private void setParameters(PreparedStatement stmt) throws SQLException {
        int paramIndex = 1;
        for (ComparisonOperator condition : conditions) {
            if (condition.column.startsWith("$.")) {
//                String[] parts = condition.column.split("\\.", 2);
//                String jsonPath = "$." + parts[1];
                String column = condition.column.substring(2);
                int i = column.lastIndexOf('.');
                String jsonPath = "$." + column.substring(i + 1);
//                column = column.substring(0, i);
                if(stmt!=null) stmt.setString(paramIndex++, jsonPath);
                else System.out.println((paramIndex++)+" : "+jsonPath);
            }
            if(stmt!=null)stmt.setObject(paramIndex++, condition.value);
            else System.out.println((paramIndex++)+" : "+condition.value);
        }
    }

    public static void printConditionParameters(List<ComparisonOperator> conditions) {
        // Set condition values
        int paramIndex = 1;
        for (ComparisonOperator condition : conditions) {
            if (condition.column.startsWith("$.")) {
                String column = condition.column.substring(2);
                int i = column.lastIndexOf('.');
                String jsonPath = "$." + column.substring(i + 1);
                System.out.println((paramIndex++) + " : " + jsonPath);
            }
            System.out.println((paramIndex++) + " : " + condition.value);
        }
    }

//    private <E> List<E> mapResultSetToList(ResultSet rs, Class<E> type) throws SQLException {
//        List<E> results = new ArrayList<>();
//        if(rs!=null) {
//            ResultSetMetaData metaData = rs.getMetaData();
//            int columnCount = metaData.getColumnCount();
//
//            while (rs.next()) {
//
//            }
//        }
//        return results;
//    }

}