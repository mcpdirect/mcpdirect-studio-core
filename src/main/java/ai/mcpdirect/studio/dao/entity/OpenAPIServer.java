package ai.mcpdirect.studio.dao.entity;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import ai.mcpdirect.studio.tool.openapi.OpenAPITool;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OpenAPIServer extends AIPortToolMaker {
    @JsonProperty
    public String statusMessage="";
    @JsonProperty
    public String url;
    @JsonProperty
    public Map<String, String> securities;

    public Collection<? extends OpenAPITool> getTools(){
        return List.of();
    }
}
