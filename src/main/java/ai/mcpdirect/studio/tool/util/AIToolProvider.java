package ai.mcpdirect.studio.tool.util;

import ai.mcpdirect.studio.tool.AITool;

import java.util.Collection;
import java.util.Map;

public interface AIToolProvider {
    Collection<? extends AITool> getTools();

    public AITool getTool(String name);
    String callTool(String name, Map<String,Object> parameters);
}
