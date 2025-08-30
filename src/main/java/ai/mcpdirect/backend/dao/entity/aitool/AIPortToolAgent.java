package ai.mcpdirect.backend.dao.entity.aitool;

//import appnet.hstp.labs.util.ID;

public class AIPortToolAgent {
    public long id;
    public long userId;
    public long engineId;
    public long appId;
    public long created;
    public long deviceId;
    public String device;
    public String name;
    public String tags;

    public AIPortToolAgent() {}

    public AIPortToolAgent(long id, long userId, long engineId, long appId, long created,long deviceId, String device, String name, String tags) {
//        this.id = ID.nextId();
        this.id = id;
        this.userId = userId;
        this.engineId = engineId;
        this.appId = appId;
        this.created = created;
        this.deviceId = deviceId;
        this.device = device;
        this.name = name;
        this.tags = tags;
    }
}