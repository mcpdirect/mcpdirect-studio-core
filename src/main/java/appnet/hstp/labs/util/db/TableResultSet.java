package appnet.hstp.labs.util.db;

import java.util.List;

public interface TableResultSet<T> {
    default int updatedRows() {return -1;}
    default T get(String column) throws Exception {return null;}
    default Long getLong(String column) throws Exception {return null;}
    default Integer getInt(String column) throws Exception {return null;}
    default String getString(String column) throws Exception {return null;}
    default T get(String... columns) throws Exception {return null;}
    default List<T> getList(String column) throws Exception {return List.of();}
    default List<T> getList(String... columns) throws Exception {return List.of();}
    default <E> E get(Class<E> type,String column) throws Exception {return null;}
    default <E> E get(Class<E> type,String... columns) throws Exception {return null;}
    default <E> List<E> getList(Class<E> type,String column) throws Exception {return List.of();}

    default <E> List<E> getList(Class<E> type,String... columns) throws Exception {return List.of();}

    default void close() throws Exception {};
}
