package ai.mcpdirect.studio.tool.openapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class OpenAPIServerConfig {
    @JsonProperty
    public int status = 1;
    @JsonProperty
    public String url;
    @JsonProperty
    public String docPath;
    @JsonProperty
    public String doc;

    @JsonProperty
    public Map<String, String> header ;
}
