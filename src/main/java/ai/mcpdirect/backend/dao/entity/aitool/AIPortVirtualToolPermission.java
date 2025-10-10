package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortVirtualToolPermission extends AIPortToolPermission{
    public long originalToolId;

    public AIPortVirtualToolPermission() {}

    public AIPortVirtualToolPermission(long userId, long accessKeyId,long originalToolId, long toolId, long lastUpdated, short status) {
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.originalToolId = originalToolId;
        this.toolId = toolId;
        this.lastUpdated = lastUpdated;
        this.status = status;
    }
    public AIPortVirtualToolPermission copy (){
        AIPortVirtualToolPermission p = new AIPortVirtualToolPermission();
        p.userId=userId;
        p.accessKeyId=accessKeyId;
        p.toolId=toolId;
        p.lastUpdated=lastUpdated;
        p.status=status;
        p.agentId=agentId;
        p.makerId=makerId;
        p.name=name;
        p.originalToolId = originalToolId;
        return p;
    }
}
