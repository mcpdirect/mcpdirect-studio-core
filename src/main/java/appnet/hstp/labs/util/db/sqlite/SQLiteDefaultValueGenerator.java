//package appnet.hstp.labs.util.db.sqlite;
//
//import java.sql.Timestamp;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//
//public class SQLiteDefaultValueGenerator {
//    public static Object value(String declaredType) {
//        return value(declaredType, null);
//    }
//    public static Object value(String declaredType,Object defaultValue) {
//        if(defaultValue!=null){
//            return defaultValue;
//        }
//        String lowerType = declaredType.toLowerCase();
//
//        if (lowerType.contains("int")||lowerType.contains("numeric") || lowerType.contains("decimal")
//                ||lowerType.contains("real") || lowerType.contains("double") || lowerType.contains("float")) {
//            return 0;
//        } else if (lowerType.contains("char") || lowerType.contains("varchar") || lowerType.contains("text") || lowerType.contains("clob")) {
//            return "";
//        } else if (lowerType.contains("blob")) {
//            return new byte[0];
//        } else if (lowerType.contains("boolean")) {
//            return false;
//        } else if (lowerType.contains("date")) {
//            return LocalDate.now().toString(); // Example: random date within the last year
//        } else if (lowerType.contains("time")) {
//            return LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME); // Example: random time within the last day
//        } else if (lowerType.contains("datetime")) {
//            return Timestamp.valueOf(LocalDateTime.now());
//        } else {
//            throw new IllegalArgumentException("unknown type "+declaredType); // Default for unknown or unhandled types
//        }
//    }
//
//    public static void main(String[] args) {
//        System.out.println("INT Default: " + value("INT"));
//        System.out.println("VARCHAR(255) Default: " + value("VARCHAR(255)"));
//        System.out.println("TEXT Default: " + value("TEXT"));
//        System.out.println("BLOB Default: " + value("BLOB"));
//        System.out.println("REAL Default: " + value("REAL"));
//        System.out.println("DECIMAL(10, 2) Default: " + value("DECIMAL(10, 2)"));
//        System.out.println("BOOLEAN Default: " + value("BOOLEAN"));
//        System.out.println("DATE Default: " + value("DATE"));
//        System.out.println("TIME Default: " + value("TIME"));
//        System.out.println("DATETIME Default: " + value("DATETIME"));
//        System.out.println("UNKNOWN Default: " + value("UNKNOWN_TYPE"));
//    }
//}