package appnet.hstp.labs.util.db.sqlite;

import appnet.hstp.labs.util.db.Column;
import appnet.hstp.labs.util.db.Index;
import appnet.hstp.labs.util.db.Table;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

// Provided classes (assuming they are in the same package or imported)

//class Column {
//    public final String name;
//    public final String type;
//    public final boolean primaryKey; // Note: Ignored when adding a column via ALTER TABLE
//    // Future extension could include:
//    // public final boolean notNull;
//    // public final String defaultValue;
//
//    public Column(String name, String type, boolean primaryKey) {
//        this.name = name;
//        this.type = type;
//        this.primaryKey = primaryKey;
//    }
//}
//
//class Index {
//    public final String name;
//    public final String[] columns;
//    public final boolean unique;
//    public Index(String name, boolean unique, String... columns) {
//        this.name = name;
//        this.columns = columns;
//        this.unique = unique;
//    }
//}
//
//class Table {
//    public String name;
//    public Column[] columns;
//    public Index[] indexes;
//    public Table(String name, Column[] columns, Index[] indexes) {
//        this.name = name;
//        this.columns = columns;
//        this.indexes = indexes;
//    }
//}

//INT, INTEGER, TINYINT, SMALLINT, etc.	INTEGER
//CHARACTER(n), VARCHAR(n), TEXT, CLOB	TEXT
//BLOB	BLOB
//REAL, DOUBLE, FLOAT	REAL
//NUMERIC, DECIMAL(p,s), BOOLEAN, DATE, DATETIME	NUMERIC
/**
 * Generates SQLite CREATE TABLE, CREATE INDEX, and ALTER TABLE ADD COLUMN SQL statements.
 */
public class SQLiteGenerator {
    public static Object defaultValue(String declaredType) {
        return defaultValue(declaredType, null);
    }
    public static Object defaultValue(String declaredType, Object defaultValue) {
        if(defaultValue!=null){
            return defaultValue;
        }
        String lowerType = declaredType.toLowerCase();

        if (lowerType.contains("int")||lowerType.contains("numeric") || lowerType.contains("decimal")
                ||lowerType.contains("real") || lowerType.contains("double") || lowerType.contains("float")) {
            return 0;
        } else if (lowerType.contains("char") || lowerType.contains("varchar") || lowerType.contains("text") || lowerType.contains("clob")) {
            return "";
        } else if (lowerType.contains("blob")) {
            return new byte[0];
        } else if (lowerType.contains("boolean")) {
            return false;
        } else if (lowerType.contains("date")) {
            return LocalDate.now().toString(); // Example: random date within the last year
        } else if (lowerType.contains("time")) {
            return LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME); // Example: random time within the last day
        } else if (lowerType.contains("datetime")) {
            return Timestamp.valueOf(LocalDateTime.now());
        } else {
            throw new IllegalArgumentException("unknown type "+declaredType); // Default for unknown or unhandled types
        }
    }
    /**
     * Generates the necessary SQL statements to create a table and its indexes
     * based on the provided Table object.
     *
     * @param table The Table object describing the table structure.
     * @return A List of Strings, where the first element is the CREATE TABLE
     * statement, and subsequent elements are CREATE INDEX statements.
     * Returns an empty list if the table or columns are null/empty.
     */
    public static <T> List<String> createSql(Table<T> table) {
        List<String> sqlStatements = new ArrayList<>();

        if (table == null || table.name() == null || table.name().trim().isEmpty() ||
            table.columns() == null || table.columns().isEmpty()) {
            System.err.println("Invalid table definition provided for CREATE.");
            return sqlStatements; // Return empty list for invalid input
        }

        // --- Generate CREATE TABLE statement ---
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ").append(quoteIdentifier(table.name())).append(" (\n");

        StringJoiner columnDefinitions = new StringJoiner(",\n");
        List<String> primaryKeyColumns = new ArrayList<>();

        for (Column column : table.columns()) {
            if (column == null || column.name == null || column.name.trim().isEmpty() ||
                column.type == null || column.type.trim().isEmpty()) {
                System.err.println("Skipping invalid column definition in table " + table.name());
                continue; // Skip invalid columns
            }
            StringBuilder colDef = new StringBuilder();
            colDef.append("  ").append(quoteIdentifier(column.name)).append(" ").append(column.type.toUpperCase());
            colDef.append("  DEFAULT ").append(defaultValue(column.type,column.defaultValue));
            // Handle single-column primary key inline
            if (column.primaryKey) {
                primaryKeyColumns.add(column.name);
                // Only add inline constraint if it's the only PK column identified so far
                if (primaryKeyColumns.size() == 1 && table.columns().stream().filter(c -> c.primaryKey).count() == 1) {
                   colDef.append(" PRIMARY KEY");
                }
            }
            columnDefinitions.add(colDef.toString());
        }

        createTableSql.append(columnDefinitions.toString());

        // Handle composite primary key if more than one column was marked
        long pkCount = table.columns().stream().filter(c -> c != null && c.primaryKey).count();
        if (pkCount > 1) {
            createTableSql.append(",\n");
            createTableSql.append("  PRIMARY KEY (");
            createTableSql.append(table.columns().stream()
                                     .filter(c -> c != null && c.primaryKey)
                                     .map(c -> quoteIdentifier(c.name))
                                     .collect(Collectors.joining(", ")));
            createTableSql.append(")");
        } else if (pkCount == 0) {
             // Optional: Warn if no primary key is defined
             // System.out.println("Warning: No primary key defined for table " + table.name);
        }


        createTableSql.append("\n);");
        sqlStatements.add(createTableSql.toString());

        // --- Generate CREATE INDEX statements ---
        if (table.indexes() != null) {
            for (Index index : table.indexes()) {
                if (index == null || index.name == null || index.name.trim().isEmpty() ||
                    index.columns == null || index.columns.length == 0) {
                     System.err.println("Skipping invalid index definition for table " + table.name());
                    continue; // Skip invalid indexes
                }

                StringBuilder createIndexSql = new StringBuilder();
                createIndexSql.append("CREATE ");
                if (index.unique) {
                    createIndexSql.append("UNIQUE ");
                }
                createIndexSql.append("INDEX ").append(quoteIdentifier(index.name))
                              .append(" ON ").append(quoteIdentifier(table.name())).append(" (");

                StringJoiner indexColumns = new StringJoiner(", ");
                for (String indexColName : index.columns) {
                     if (indexColName != null && !indexColName.trim().isEmpty()) {
                        indexColumns.add(quoteIdentifier(indexColName.trim()));
                     } else {
                         System.err.println("Skipping invalid column name in index " + index.name);
                     }
                }

                if (indexColumns.length() > 0) {
                    createIndexSql.append(indexColumns.toString());
                    createIndexSql.append(");");
                    sqlStatements.add(createIndexSql.toString());
                } else {
                     System.err.println("Skipping index " + index.name + " because it has no valid columns.");
                }
            }
        }

        return sqlStatements;
    }

    /**
     * Generates the SQL statement to add a new column to an existing table.
     * Note: SQLite has limitations on ALTER TABLE; constraints like PRIMARY KEY
     * or UNIQUE cannot be added reliably this way. The primaryKey flag in the
     * Column object is ignored by this method.
     *
     * @param tableName The name of the table to alter.
     * @param columnToAdd The Column object representing the column to add.
     * @return The `ALTER TABLE ... ADD COLUMN ...` SQL statement string, or null if input is invalid.
     */
    public static String alterColumnSql(String tableName, Column columnToAdd) {
        if (tableName == null || tableName.trim().isEmpty() ||
            columnToAdd == null || columnToAdd.name == null || columnToAdd.name.trim().isEmpty() ||
            columnToAdd.type == null || columnToAdd.type.trim().isEmpty()) {
            System.err.println("Invalid input provided for ADD COLUMN.");
            return null; // Return null for invalid input
        }

        // Add support for DEFAULT and NOT NULL here if Column class is extended
        // Example:
        // if (columnToAdd.notNull) {
        //     addColumnSql.append(" NOT NULL");
        //     // Requires a DEFAULT value if table has rows, handle with care
        //     if (columnToAdd.defaultValue != null) {
        //         addColumnSql.append(" DEFAULT ").append(quoteLiteral(columnToAdd.defaultValue)); // Need quoteLiteral helper
        //     } else {
        //          System.err.println("Warning: Adding NOT NULL column [" + columnToAdd.name + "] without DEFAULT to table [" + tableName + "] might fail if table is not empty.");
        //     }
        // } else if (columnToAdd.defaultValue != null) {
        //      addColumnSql.append(" DEFAULT ").append(quoteLiteral(columnToAdd.defaultValue)); // Need quoteLiteral helper
        // }

        return "ALTER TABLE " +
                quoteIdentifier(tableName) +
                " ADD COLUMN " +
                quoteIdentifier(columnToAdd.name) +
                " " +
                columnToAdd.type.toUpperCase() +

                // Add support for DEFAULT and NOT NULL here if Column class is extended
                // Example:
                // if (columnToAdd.notNull) {
                //     addColumnSql.append(" NOT NULL");
                //     // Requires a DEFAULT value if table has rows, handle with care
                //     if (columnToAdd.defaultValue != null) {
                //         addColumnSql.append(" DEFAULT ").append(quoteLiteral(columnToAdd.defaultValue)); // Need quoteLiteral helper
                //     } else {
                //          System.err.println("Warning: Adding NOT NULL column [" + columnToAdd.name + "] without DEFAULT to table [" + tableName + "] might fail if table is not empty.");
                //     }
                // } else if (columnToAdd.defaultValue != null) {
                //      addColumnSql.append(" DEFAULT ").append(quoteLiteral(columnToAdd.defaultValue)); // Need quoteLiteral helper
                // }

                ";";
    }

    /**
     * Helper method to quote identifiers (table/column names).
     * @param identifier The identifier to quote.
     * @return The quoted identifier.
     */
    private static String quoteIdentifier(String identifier) {
        if (identifier == null) return null;
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

     /**
      * Helper method to quote literal values (e.g., for DEFAULT).
      * Basic implementation - might need refinement based on data types.
      * @param literal The literal value string.
      * @return The quoted literal.
      */
     private static String quoteLiteral(String literal) {
        if (literal == null) return "NULL";
        // Simple quoting for strings, numbers might not need quotes
        // This assumes text type default values for simplicity
        String escaped = literal.replace("'", "''"); // SQL standard escape for single quotes
        return "'" + escaped + "'";
     }


    // --- Example Usage ---
//    public static void main(String[] args) {
//        // --- Create Table Example ---
//        Column colId = new Column("user_id", "INTEGER", true);
//        Column colName = new Column("username", "TEXT", false);
//        Column colEmail = new Column("email", "TEXT", false);
//        Table userTable = new Table(
//            "users",
//            new Column[]{colId, colName, colEmail},
//            new Index[]{new Index("idx_user_email", true, "email")}
//        );
//
//        SQLiteGenerator generator = new SQLiteGenerator();
//
//        System.out.println("--- SQL for creating users table ---");
//        List<String> userCreateSql = generator.createSql(userTable);
//        userCreateSql.forEach(System.out::println);
//
//        // --- Add Column Example ---
//        System.out.println("\n--- SQL for adding a new column ---");
//        // Define the new column to add
//        Column colStatus = new Column("status", "TEXT", false); // The 'primaryKey=false' is ignored here
//
//        // Generate the ALTER TABLE statement
//        String addColumnSql = generator.alterColumnSql(userTable.name, colStatus);
//        if (addColumnSql != null) {
//            System.out.println(addColumnSql);
//        }
//
//        // Example of adding a column that might represent a foreign key (but constraint isn't added here)
//        Column colAccountId = new Column("account_id", "INTEGER", false);
//         String addFkColSql = generator.alterColumnSql(userTable.name, colAccountId);
//         if (addFkColSql != null) {
//            System.out.println(addFkColSql);
//         }
//
//         // --- Example of Composite PK Create Table ---
//         System.out.println("\n--- SQL for order_details table (Composite PK) ---");
//         Column orderId = new Column("order_id", "INTEGER", true);
//         Column productId = new Column("product_id", "INTEGER", true); // Mark both as PK
//         Column quantity = new Column("quantity", "INTEGER", false);
//         Table orderDetailsTable = new Table(
//            "order_details",
//            new Column[]{orderId, productId, quantity},
//            null
//         );
//         List<String> orderDetailsSql = generator.createSql(orderDetailsTable);
//         orderDetailsSql.forEach(System.out::println);
//
//         // --- Example of adding column to composite PK table ---
//         System.out.println("\n--- SQL for adding column to order_details table ---");
//         Column notes = new Column("notes", "TEXT", false);
//         String addNotesSql = generator.alterColumnSql(orderDetailsTable.name, notes);
//         if (addNotesSql != null) {
//            System.out.println(addNotesSql);
//         }
//
//    }
}