package appnet.hstp.labs.util.db.sqlite;

import appnet.hstp.engine.util.JSON;
import appnet.hstp.labs.util.db.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

class SQLiteTable<T> implements Table<T>{

    private final Connection connection;
    private final String tableName;
    private final HashSet<Column> columns = new HashSet<>();
    private final HashSet<Index> indexes = new HashSet<>();
    private final Class<T> entityType;

    public SQLiteTable(Connection connection, String tableName,Class<T> entityType) {
        this.connection = connection;
        this.tableName = tableName;
        this.entityType = entityType;
    }

    @Override
    public String name() {
        return tableName;
    }

    @Override
    public Collection<Column> columns() {
        return Collections.unmodifiableSet(columns);
    }

    @Override
    public Table<T> column(String name, String type, boolean primaryKey) {
        columns.add(new Column(name, type, primaryKey));
        return this;
    }
    @Override
    public Table<T> column(String name, String type, boolean primaryKey,Object defaultValue) {
        columns.add(new Column(name, type, primaryKey,defaultValue));
        return this;
    }

    @Override
    public Collection<Index> indexes() {
        return Collections.unmodifiableSet(indexes);
    }

    @Override
    public Table<T> index( boolean unique, String... columns) {
        indexes.add(new Index(tableName, unique, columns));
        return this;
    }

    @Override
    public Table<T> create() throws Exception {
        // First check if table exists
        boolean tableExists;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            tableExists = rs.next();
        }

        if (!tableExists) {
            // Create new table with all columns
            createNewTable();
        } else {
            // Table exists - check for missing columns
            addMissingColumns();
        }

        // Create indexes
        createIndexes();

        return this;
    }

    private void createNewTable() throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE ")
                .append(tableName)
                .append(" (");

        String columnsSql = columns.stream()
                .map(this::buildColumnDefinition)
                .collect(Collectors.joining(", "));

        sql.append(columnsSql).append(")");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    private void addMissingColumns() throws SQLException {
        // Get existing columns
        Set<String> existingColumns = new HashSet<>();
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }

        // Find and add missing columns
        for (Column column : columns) {
            if (!existingColumns.contains(column.name.toLowerCase())) {
                addColumn(column);
            }
        }
    }

    private void addColumn(Column column) throws SQLException {
        String sql = String.format("ALTER TABLE %s ADD COLUMN %s",
                tableName,
                buildColumnDefinition(column));

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private String buildColumnDefinition(Column column) {
        StringBuilder colDef = new StringBuilder(column.name)
                .append(" ")
                .append(column.type);

        if (column.primaryKey) {
            colDef.append(" PRIMARY KEY");
        }
        if (column.defaultValue != null) {
            colDef.append(" DEFAULT ");
            if (column.defaultValue instanceof String) {
                colDef.append("'").append(column.defaultValue).append("'");
            } else {
                colDef.append(column.defaultValue);
            }
        }
        return colDef.toString();
    }

    private void createIndexes() throws SQLException {
        // Get existing indexes
        Set<String> existingIndexes = new HashSet<>();
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                existingIndexes.add(rs.getString("INDEX_NAME").toLowerCase());
            }
        }

        // Create missing indexes
        for (Index index : indexes) {
            if (!existingIndexes.contains(index.name.toLowerCase())) {
                createIndex(index);
            }
        }
    }

    private void createIndex(Index index) throws SQLException {
        String sql = String.format("CREATE %sINDEX IF NOT EXISTS %s ON %s (%s)",
                index.unique ? "UNIQUE " : "",
                index.name,
                tableName,
                String.join(", ", index.columns));

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

//    @Override
//    public List<T> select(String field, Object value) throws Exception {
//        return select(field, value, entityType);
//    }
//
//    @Override
//    public T selectOne(String field, Object value) throws Exception {
//        List<T> select = select(field, value, entityType);
//        if (!select.isEmpty()) {
//            return select.get(0);
//        }
//        return null;
//    }
//
//    @Override
//    public <E> List<E> select(String field, Object value, Class<E> type) throws Exception {
//        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, field);
//        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setObject(1, value);
//            ResultSet rs = stmt.executeQuery();
//            return mapResultSetToList(rs, type);
//        }
//    }
//
//    @Override
//    public <E> E selectOne(String field, Object value, Class<E> type) throws Exception {
//        List<E> select = select(field, value, type);
//        if (!select.isEmpty()) {
//            return select.get(0);
//        }
//        return null;
//    }

    public TableResultSet<T> createTableResultSet(ResultSet resultSet) throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (resultSet.next()) {
            Map<String, Object> rowMap = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                // Use column label to handle potential aliases ('SELECT user_id AS id FROM...')
                String columnLabel = metaData.getColumnLabel(i);
                Object columnValue = resultSet.getObject(i); // Get value as Object

                 // Handle potential nulls or specific type conversions if needed before mapping
                 // For example, sometimes specific date/time types might need pre-processing

                // Put the column label (or name) and value into the map
                // Jackson will match map keys to POJO field names (considering naming strategy)
                rowMap.put(columnLabel, columnValue);
            }

            // Use Jackson to convert the Map representing the row into the target POJO
            resultList.add(rowMap);
        }
        return new TableResultSet<T>() {
            int index;
            public <E> E _get(Class<E> type, String column) throws Exception {
                if(!resultList.isEmpty()){
                    Object object = resultList.get(index).get(column);
                    if (object instanceof String) {
                        return JSON.fromJson(object.toString(), type);
                    } else if (object instanceof byte[]) {
                        return JSON.fromJson((byte[]) object, type);
                    }
                }
                return null;
            }
            @Override
            public T get(String column) throws Exception {
                return get(entityType, column);
            }

            @Override
            public Integer getInt(String column) throws Exception {
                index = 0;
                Long value = null;
                if(!resultList.isEmpty()){
                    value = (Long) resultList.get(index).get(column);
                }
                return value!=null?value.intValue():null;
            }

            @Override
            public Long getLong(String column) throws Exception {
                index = 0;
                if(!resultList.isEmpty()){
                    return (Long) resultList.get(index).get(column);
                }
                return null;
            }

            @Override
            public String getString(String column) throws Exception {
                index = 0;
                if(!resultList.isEmpty()){
                    return (String) resultList.get(index).get(column);
                }
                return null;
            }

            @Override
            public <E> E get(Class<E> type, String column) throws Exception {
                index = 0;
                return _get(type, column);
            }

            @Override
            public List<T> getList(String column) throws Exception {
                return getList(entityType, column);
            }

            @Override
            public T get(String[] columns) throws Exception {
                return get(entityType,columns);
            }

            @Override
            public List<T> getList(String[] columns) throws Exception {
                return getList(entityType,columns);
            }

            @Override
            public <E> List<E> getList(Class<E> type, String column) throws Exception {
                List<E> list = new ArrayList<>();
                E e;
                for (index=0;index<resultList.size();index++) if((e = _get(type,column))!=null) list.add(e);
                return list;
            }
            public <E> E _get(Class<E> type, String[] columns) throws Exception {
                if(!resultList.isEmpty()){
                    Map<String,Object> map = new HashMap<>();
                    Map<String,Object> resultMap = resultList.get(index);
                    if(columns==null||columns.length==0) map = resultMap;
                    else for (String column : columns) if(column != null) {
                        map.put(column, resultMap.get(column));
                    }
                    return JSON.convertTo(map, type);
                }
                return null;
            }
            @Override
            public <E> E get(Class<E> type, String[] columns) throws Exception {
                index = 0;
                return _get(type, columns);
            }

            @Override
            public <E> List<E> getList(Class<E> type, String... columns) throws Exception {
                List<E> list = new ArrayList<>();
                E e;
                for (index=0;index<resultList.size();index++)  if((e = _get(type,columns))!=null) list.add(e);
                return list;
            }
        };
    }

//    @Override
//    public WhereClause<T> select(ComparisonOperator operator){
//        return select().and(operator);
//    }

    @Override
    public WhereClause<T> select(String... columns){
        return new SQLiteWhereClause<T>(connection, tableName,columns){
            @Override
            public  TableResultSet<T> createTableResultSet(Object result) throws Exception {
                return SQLiteTable.this.createTableResultSet((ResultSet) result);
            }
        };
    }

    //    @Override
//    public List<T> selectAll() throws Exception {
//        return selectAll(entityType);
//    }
//
//    @Override
//    public <E> List<E> selectAll(Class<E> type) throws Exception {
//        String sql = "SELECT * FROM " + tableName;
//        try (Statement stmt = connection.createStatement()) {
//            ResultSet rs = stmt.executeQuery(sql);
//            return mapResultSetToList(rs, type);
//        }
//    }

    @Override
    public Insert insert(String column, Object value) throws Exception {
        return new SQLiteInsert(connection, tableName).value(column, value);
    }

    @Override
    public Update<T> update(String column, Object value) throws Exception {
        return new SQLiteUpdate<T>(connection, tableName).set(column, value);
    }

    @Override
    public Delete<T> delete(ComparisonOperator operator) throws Exception {
        SQLiteDelete<T> d =  new SQLiteDelete<>(connection, tableName);
        d.and(operator);
        return d;
    }

//    private <E> List<E> mapResultSetToList(ResultSet rs, Class<E> type) throws SQLException {
//        List<E> results = new ArrayList<>();
//        ResultSetMetaData metaData = rs.getMetaData();
//        int columnCount = metaData.getColumnCount();
//
//        while (rs.next()) {
//            try {
//                E entity = type.getDeclaredConstructor().newInstance();
//                for (int i = 1; i <= columnCount; i++) {
//                    String columnName = metaData.getColumnName(i);
//                    Object value = rs.getObject(i);
//
//                    try {
//                        java.lang.reflect.Field field = type.getDeclaredField(columnName);
//                        field.setAccessible(true);
//                        field.set(entity, value);
//                    } catch (NoSuchFieldException e) {
//                        // Ignore fields that don't exist in the entity
//                    }
//                }
//                results.add(entity);
//            } catch (Exception e) {
//                throw new SQLException("Failed to map result set to entity", e);
//            }
//        }
//        return results;
//    }
}