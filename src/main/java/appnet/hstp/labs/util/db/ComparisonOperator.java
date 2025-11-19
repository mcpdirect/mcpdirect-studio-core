package appnet.hstp.labs.util.db;

public class ComparisonOperator {
    public final String column;
    public final String operator;
    public final Object value;

    /**
     * @param column if start with '$.' means json path, need to involve json_extract before compare.
     *               example: $.value.userName , '$.' means json path, value is column name, userName is json object key
     * @param operator =, !=, >, <, >=, <=, LIKE.
     * @param value Object
     * */
    public ComparisonOperator(String column, String operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public static ComparisonOperator eq(String column, Object value) {
        return new ComparisonOperator(column, "=", value);
    }
    public static ComparisonOperator neq(String column, Object value) {
        return new ComparisonOperator(column, "!=", value);
    }
    public static ComparisonOperator gt(String column, Object value) {
        return new ComparisonOperator(column, ">", value);
    }
    public static ComparisonOperator gte(String column, Object value) {
        return new ComparisonOperator(column, ">=", value);
    }
    public static ComparisonOperator like(String column, Object value) {
        return new ComparisonOperator(column, "LIKE", value);
    }
    public static ComparisonOperator lt(String column, Object value) {
        return new ComparisonOperator(column, "<", value);
    }
    public static ComparisonOperator lte(String column, Object value) {
        return new ComparisonOperator(column, "<=", value);
    }
}