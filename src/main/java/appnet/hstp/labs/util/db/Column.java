package appnet.hstp.labs.util.db;

public class Column {
    public final String name;
    public final String type;
    public final boolean primaryKey;
    public Object defaultValue;
    public Column(String name, String type, boolean primaryKey) {
        this.name = name.trim().toLowerCase();
        this.type = type;
        this.primaryKey = primaryKey;
    }
    public Column(String name, String type, boolean primaryKey,Object defaultValue) {
        this.name = name.trim().toLowerCase();
        this.type = type;
        this.primaryKey = primaryKey;
        this.defaultValue = defaultValue;
    }

}