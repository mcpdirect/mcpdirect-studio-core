package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortToolPermission{
    public long userId;
    public long accessKeyId;
    public long toolId;
    public long lastUpdated;
    public int status;
    public Long agentId;
    public Long makerId;
    public String name;

    public  AIPortToolPermission(){}


    public AIPortToolPermission copy (){
        AIPortToolPermission p = new AIPortToolPermission();
        p.userId=userId;
        p.accessKeyId=accessKeyId;
        p.toolId=toolId;
        p.lastUpdated=lastUpdated;
        p.status=status;
        p.agentId=agentId;
        p.makerId=makerId;
        p.name=name;
        return p;
    }
}
