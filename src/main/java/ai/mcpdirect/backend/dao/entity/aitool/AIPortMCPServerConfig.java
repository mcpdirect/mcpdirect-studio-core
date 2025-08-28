package ai.mcpdirect.backend.dao.entity.aitool;

//import appnet.hstp.labs.util.ID;

public class AIPortMCPServerConfig {
    public long id;
    
    public long created;
    public String url;
    public String command;
    public String args;
    public String env;

    public AIPortMCPServerConfig() {}

    public AIPortMCPServerConfig(long created, String url, String command, String args, String env) {
//        this.id = ID.nextId();
        this.created = created;
        this.url = url;
        this.command = command;
        this.args = args;
        this.env = env;
    }
}