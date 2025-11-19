package appnet.hstp.labs.util.db;

public interface Update<T> extends WhereClause<T> {
    /**
     * update SQLite table
     * @param column column name
     * @param value column value
     * @return Insert handler for next set
     */
    Update<T> set(String column, Object value) throws Exception;
}