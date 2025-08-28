package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortToolMaker {
    public long id;
    public long created;
    public int status;
    public long lastUpdated;
    public long hash;
    public String tools;

    public static final int TYPE_MCP = 1000;
    /**
     * 1000 is MCP
     */
    public int type;
    public String name;
    public String tags;
    public long agentId;

    public AIPortToolMaker() {}

    public AIPortToolMaker(long id, long created, int status, long lastUpdated, long hash, String tools, int type, String name, String tags, long agentId) {
        this.id = id;
        this.created = created;
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.hash = hash;
        this.tools = tools;
        this.type = type;
        this.name = name;
        this.tags = tags;
        this.agentId = agentId;
    }
}
