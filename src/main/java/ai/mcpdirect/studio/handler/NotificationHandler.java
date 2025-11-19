package ai.mcpdirect.studio.handler;

import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.dao.entity.OpenAPIServer;

import java.util.List;

public interface NotificationHandler {
    default void onMCPServerNotification(MCPServer server){};
    default void onOpenAPIServerNotification(OpenAPIServer server){};
}
