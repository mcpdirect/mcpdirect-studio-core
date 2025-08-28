package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortToolPermission {
    public long userId;
    public long accessKeyId;
    public long toolId;
    public long lastUpdated;
    public int status;
    public  AIPortToolPermission(){}

    public AIPortToolPermission(long userId, long accessKeyId, long toolId, long lastUpdated, int status) {
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.toolId = toolId;
        this.lastUpdated = lastUpdated;
        this.status = status;
    }
}
