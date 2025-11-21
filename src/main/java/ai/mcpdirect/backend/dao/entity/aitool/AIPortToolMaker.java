package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortToolMaker {
    public long id;
    public long created;
    public static final int STATUS_ABANDONED = -1;
    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_ERROR = 256;
    public static final int STATUS_WAITING = Integer.MAX_VALUE;
    public int status;
    public long lastUpdated;
    public static final int TYPE_VIRTUAL = 0;
    public static final int TYPE_OPENAPI = 1;
    public static final int TYPE_MCP = 1000;
    /**
     * 1000 is MCP
     */
    public int type;
    public String name;
    public String tags;
    public long agentId;
    public int agentStatus;
    public String agentName;
    public long userId;
    public long teamId;
    public long templateId;

    public boolean openapi(){
        return type==TYPE_OPENAPI;
    }
}
