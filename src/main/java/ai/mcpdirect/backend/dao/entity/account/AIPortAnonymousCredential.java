package ai.mcpdirect.backend.dao.entity.account;

public class AIPortAnonymousCredential extends AIPortAnonymous{
    public String secretKey;
    public String password;
    public AIPortAnonymousCredential(){}

    public AIPortAnonymousCredential(long id, long created, String deviceId,String secretKey,  String password) {
        super(id,created,deviceId);
        this.secretKey = secretKey;
        this.password = password;
    }
}
