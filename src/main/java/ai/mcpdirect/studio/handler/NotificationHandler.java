package ai.mcpdirect.studio.handler;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortAppVersion;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolAgent;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.dao.entity.OpenAPIServer;

import java.util.List;

public interface NotificationHandler {
    default void onToolAgentNotification(AIPortToolAgent agent){}
    default void onToolMakerNotification(AIPortToolMaker server){};
    default void onToolMakerNotification(List<AIPortToolMaker> makers){};
    default void onToolNotification(List<AIPortTool> tools){};
    default void onAppVersionNotification(List<AIPortAppVersion> versions){};
//    default void onMCPServerNotification(MCPServer server){};
//    default void onOpenAPIServerNotification(OpenAPIServer server){};
}
