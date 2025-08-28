package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortTool {
    public long id;
    public long makerId;
    public int status;
    public long lastUpdated;
    public String name;
    public int hash;
    public String metaData;
    public String tags;

    public AIPortTool() {}

    public AIPortTool(long id,long makerId, int status, long lastUpdated, String name, int hash, String metaData, String tags) {
//        this.id = ID.nextId();
        this.id = id;
        this.makerId = makerId;
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.name = name;
        this.hash = hash;
        this.metaData = metaData;
        this.tags = tags;
    }
    public AIPortTool  duplicate(){
        return new AIPortTool(id,makerId,status,lastUpdated,name,hash,metaData,tags);
    }
}