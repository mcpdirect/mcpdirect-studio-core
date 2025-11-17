package ai.mcpdirect.studio.tool.openapi;

import ai.mcpdirect.studio.dao.entity.OpenAPIServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.util.AIToolProvider;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OpenAPIToolProvider extends OpenAPIServer implements AIToolProvider {
    public OpenAPIToolProvider(OpenAPIServerConfig config){

    }

    public void refreshTools() {
    }
    public void close(){

    }
    @Override
    public Collection<? extends AITool> getTools() {
        return List.of();
    }

    @Override
    public AITool getTool(String name) {
        return null;
    }

    @Override
    public String callTool(String name, Map<String, Object> parameters) {
        return "";
    }

}
