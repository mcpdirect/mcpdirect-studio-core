package ai.mcpdirect.studio.tool.openapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAPIServerConfig {
    @JsonProperty
    public int status = 1;
    @JsonProperty
    public String url;
    @JsonProperty
    public String docUri;
    @JsonProperty
    public String doc;
    @JsonProperty
    public Map<String,String> securities;
}
