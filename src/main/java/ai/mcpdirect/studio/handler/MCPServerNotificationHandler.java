package ai.mcpdirect.studio.handler;

import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;

import java.util.List;
import java.util.Map;

public interface MCPServerNotificationHandler {
    void onMCPServersNotification(List<MCPServer> servers);
    void onLocalMCPServersNotification(List<MCPServer> servers);
}
