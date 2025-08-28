package ai.mcpdirect.studio.handler;

import ai.mcpdirect.backend.dao.entity.account.AIPortUser;

public interface UserInfoNotificationHandler {
    void onUserInfoNotification(AIPortUser userInfo);
}
