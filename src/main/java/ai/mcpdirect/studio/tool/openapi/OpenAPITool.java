package ai.mcpdirect.studio.tool.openapi;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.util.OpenAPISchemaConverter;
import appnet.hstp.ServiceDescription;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.labs.util.http.HttpClient;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

public class OpenAPITool extends AIPortTool implements AITool {
    @JsonIgnore
    private final OpenAPIServerConfig config;
    @JsonIgnore
    private final String path;
    @JsonIgnore
    private final String method;
//    @JsonIgnore
//    private final Operation operation;
    @JsonIgnore
    private final Map<String, Parameter> parameterMap = new HashMap<>();
    private MediaType defaultMediaType;
    private String metaData;

    public OpenAPITool (OpenAPIServerConfig config,String path, String method){
        this.config = config;
        this.path = path;
        StringBuilder name = new StringBuilder();
        for (String s : path.split("/")) {
            if(!s.isEmpty()){
                if(s.startsWith("{")&&s.endsWith("}")){
                    s = s.substring(1,s.length()-1);
                }
                name.append("_").append(s);
            }
        }
        this.name = method+name;
        this.method = method;
//        parameterMap = o.getParameters().stream().collect(
//                Collectors.toMap(Parameter::getName, v -> v));
//        this.operation = o;

    }
    public void setOpenAPIToolProvider(OpenAPIToolProvider provider,Operation operation){
        parameterMap.clear();
        parameterMap.putAll(operation.getParameters().stream().collect(
                Collectors.toMap(Parameter::getName, v -> v)));
        String description = operation.getDescription();
        if(description==null){
            description = operation.getSummary();
        }
        String inputSchema;
        try {
            inputSchema = OpenAPISchemaConverter.toJsonSchema(operation).toString();
        }catch (Exception e){
            inputSchema = "{}";
        }
        try {
            metaData = JSON.toJson(new ServiceDescription("aitools",
                    "call/" + Long.toString(provider.id, Character.MAX_RADIX) + "/" + name(),
                    description, inputSchema, "{}"));
        }catch (Exception ignore){}
    }
    @Override
    public String name() {
        return name;
    }

    @Override
    public String call(Map<String, Object> parameters) {
        String path = this.path;
        Map<String,String> header = new HashMap<>();
        List<String> queries = new ArrayList<>();
        boolean isError = false;
        String result;
        try {
            String requestBody = (String)parameters.get("requestBody");
            parameters = (Map<String, Object>)parameters.get("parameters");
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                Parameter parameter = parameterMap.get(entry.getKey());
                if(parameter!=null){
                    String name = parameter.getName();
                    String value = (String)entry.getValue();
                    String in = parameter.getIn().toLowerCase();
                    switch (in){
                        case "header" -> header.put(name,value);
                        case "path" -> path=path.replace("{"+name+"}",value);
                        case "query" -> queries.add(name+"="+value);
                    }
                }
            }
            String url = config.url+path+"?"+String.join("&", queries);
            result = HttpClient.doRequest(method, url, header, requestBody);
        }catch (Exception e){
            result = "Error "+INTERNAL_ERROR+": "+JSON.quote(e.getMessage());
            isError = true;
        }
        return "{\"content\":[\""+result+"\"],\"isError\":"+isError+"}";
    }
}
