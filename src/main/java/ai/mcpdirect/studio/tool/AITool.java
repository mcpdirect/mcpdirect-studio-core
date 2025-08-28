package ai.mcpdirect.studio.tool;

import java.util.Map;

public interface AITool {
    String name();
    String description();
    String inputSchema();
    String call(Map<String,Object> parameters);
}
