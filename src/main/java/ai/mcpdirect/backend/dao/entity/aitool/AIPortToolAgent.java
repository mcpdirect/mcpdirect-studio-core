package ai.mcpdirect.backend.dao.entity.aitool;

//import appnet.hstp.labs.util.ID;

public class AIPortToolAgent {
    public long id;
    public long userId;
    public String engineId;
    public long created;
    public long deviceId;
    public String device;
    public String name;
    public String tags;
    public int status;
    public long lastKeepalive;
    public AIPortToolAgent() {}
    public AIPortToolAgent(String name) {
        this.name = name;
    }
}