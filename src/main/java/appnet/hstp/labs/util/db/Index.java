package appnet.hstp.labs.util.db;

public class Index {
    public final String name;
    public final String[] columns;
    public final boolean unique;
    public Index(String table, boolean unique, String... columns) {
        StringBuilder tableBuilder = new StringBuilder(table.trim().toLowerCase());
        for (String column : columns) {
            tableBuilder.append("_").append(column.trim().toLowerCase());
        }
        table = tableBuilder.toString();
        this.name = table+(unique?"_unique":"");
        this.columns = columns;
        this.unique = unique;
    }
}