package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortToolMaker {
    public long id;
    public long created;
    public int status;
    public long lastUpdated;
    public long hash;
    public String tools;

    public static final int TYPE_VIRTUAL = 0;
    public static final int TYPE_MCP = 1000;
    /**
     * 1000 is MCP
     */
    public int type;
    public String name;
    public String tags;
    public long agentId;
    public long userId;
}
