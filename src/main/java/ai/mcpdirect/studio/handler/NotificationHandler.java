package ai.mcpdirect.studio.handler;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolAgent;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.dao.entity.OpenAPIServer;

import java.util.List;

public interface NotificationHandler {
    default void onToolAgentNotification(AIPortToolAgent agent){}
    default void onToolMakerNotification(AIPortToolMaker server){};
//    default void onMCPServerNotification(MCPServer server){};
//    default void onOpenAPIServerNotification(OpenAPIServer server){};
}
