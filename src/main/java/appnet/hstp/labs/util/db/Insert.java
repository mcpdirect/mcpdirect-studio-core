package appnet.hstp.labs.util.db;

public interface Insert {

    /**
     * insert into SQLite table
     * @param column column name
     * @param value column value
     * @return Insert handler for next set
     */
    Insert value(String column, Object value) throws Exception;

    Insert replaceIfExists() throws Exception;
    /**
     * execute sql
     */
    boolean execute() throws Exception;
}