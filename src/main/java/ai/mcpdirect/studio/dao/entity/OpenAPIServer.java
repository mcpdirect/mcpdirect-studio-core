package ai.mcpdirect.studio.dao.entity;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class OpenAPIServer extends AIPortToolMaker {
    @JsonProperty
    public String url;
    @JsonProperty
    public Map<String, String> securities;
}
