package appnet.hstp.labs.util.db;

import java.util.Collection;

public interface Table<T> {
    String name();
    Collection<Column> columns();
    Table<T> column(String name, String type, boolean primaryKey);
    Table<T> column(String name, String type, boolean primaryKey,Object defaultValue);
    Collection<Index> indexes();
    Table<T> index(boolean unique, String... columns);
    Table<T> create() throws Exception;

//    WhereClause<T> select(ComparisonOperator operator);
    WhereClause<T> select(String... columns);

    Insert insert(String column,Object value) throws Exception;
    Update<T> update(String column,Object value) throws Exception;
    Delete<T> delete(ComparisonOperator operator) throws Exception;
}