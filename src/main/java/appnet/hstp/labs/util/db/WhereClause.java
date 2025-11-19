package appnet.hstp.labs.util.db;


public interface WhereClause<T>{
    WhereClause<T> and(ComparisonOperator operator);
    WhereClause<T> or(ComparisonOperator operator);

    WhereClause<T> and(WhereConditions other);

    WhereClause<T> or(WhereConditions other);
    TableResultSet<T> createTableResultSet(Object result) throws Exception;
    TableResultSet<T> execute() throws Exception;
}