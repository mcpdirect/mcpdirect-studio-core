package ai.mcpdirect.backend.dao.entity.account;

public class AIPortAnonymous {
    public long id;
    public long created;
    public String deviceId;
    public AIPortAnonymous(){}

    public AIPortAnonymous(long id, long created, String deviceId) {
        this.id = id;
        this.created = created;
        this.deviceId = deviceId;
    }
}
