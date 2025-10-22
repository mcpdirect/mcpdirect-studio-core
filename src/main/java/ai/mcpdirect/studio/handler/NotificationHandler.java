package ai.mcpdirect.studio.handler;

import ai.mcpdirect.studio.dao.entity.MCPServer;

import java.util.List;

public interface NotificationHandler {
    default void onMCPServerNotification(MCPServer server){};
}
