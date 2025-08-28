package ai.mcpdirect.studio.handler;

public interface ToolLogHandler {
    class ToolLog{
        private static long BASE_ID_TIMESTAMP;
        private static long BASE_ID;
        static {
            BASE_ID_TIMESTAMP = System.currentTimeMillis()/1000;
            BASE_ID = BASE_ID_TIMESTAMP*1000;
        }
        synchronized static String nextId(){
            long now = System.currentTimeMillis()/1000;
            if(now>BASE_ID_TIMESTAMP){
                BASE_ID_TIMESTAMP = now;
                BASE_ID=BASE_ID_TIMESTAMP*1000;
            }
            return Long.toString(BASE_ID++,36);
        }
        public String id;
        public long keyId;
        public String keyName;
        public String clientName;
        public String makerName;
        public String toolName;
        public long timestamp;
        public ToolLog(){}
        public ToolLog(long keyId, String keyName, String clientName, String makerName, String toolName) {
            id = nextId();
            this.keyId = keyId;
            this.keyName = keyName;
            this.clientName = clientName;
            this.makerName = makerName;
            this.toolName = toolName;
            this.timestamp = System.currentTimeMillis();
        }
    }
    class ToolLogDetails{
        public String input;
        public String output;
    }
    void log(ToolLog log);
}
