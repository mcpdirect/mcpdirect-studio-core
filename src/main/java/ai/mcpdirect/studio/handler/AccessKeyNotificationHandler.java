package ai.mcpdirect.studio.handler;

import ai.mcpdirect.backend.dao.entity.account.AIPortAccessKeyCredential;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolPermission;

import java.util.List;

public interface AccessKeyNotificationHandler {
    void onAccessKeysNotification(List<AIPortAccessKeyCredential> keys);
    void onAccessKeyPermissionsNotification(List<AIPortToolPermission> permissions);
}
