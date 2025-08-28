package ai.mcpdirect.backend.dao.entity.account;

import ai.mcpdirect.backend.util.AIPortAccessKeyValidator;

public class AIPortAccessKeyCredential extends AIPortAccessKey{
    public String secretKey;

    public AIPortAccessKeyCredential() {}

    public AIPortAccessKeyCredential(long userId, int userRoles,String name, String secretKey, int status, long effectiveDate, long expirationDate, long created) {
        super(AIPortAccessKeyValidator.hashCode(secretKey), userId, userRoles,name, secretKey, status, effectiveDate, expirationDate, created);
        this.secretKey = secretKey;
    }
}