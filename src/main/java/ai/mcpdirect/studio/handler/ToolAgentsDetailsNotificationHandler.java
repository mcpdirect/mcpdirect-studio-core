package ai.mcpdirect.studio.handler;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolPermission;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolAgent;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;

import java.util.List;

public interface ToolAgentsDetailsNotificationHandler {
    void onToolAgentsNotification(List<AIPortToolAgent> agents, List<AIPortToolMaker> makers,
                                   List<AIPortTool> tools, List<AIPortToolPermission> permissions,
                                  AIPortToolAgent local);
}

