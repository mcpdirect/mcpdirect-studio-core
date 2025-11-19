package ai.mcpdirect.studio.tool.openapi;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.util.OpenAPISchemaConverter;
import appnet.hstp.ServiceDescription;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.labs.util.http.HttpClient;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

public class OpenAPITool extends AIPortTool implements AITool {
    @JsonIgnore
    private OpenAPIToolProvider provider;
    @JsonIgnore
    private final String path;
    @JsonIgnore
    private final String method;
    @JsonIgnore
    private final Map<String, Parameter> parameterMap = new HashMap<>();

    public OpenAPITool (String name,String method,String path){
        this.name = name;
        this.path = path;
        this.method = method;
        if(method==null||path==null){
            status = -1;
        }
        lastUpdated = 1;
    }

    public static String name(String method,String path){
        StringBuilder name = new StringBuilder();
        for (String s : path.split("/")) {
            if(!s.isEmpty()){
                if(s.startsWith("{")&&s.endsWith("}")){
                    s = s.substring(1,s.length()-1);
                }
                name.append("_").append(s);
            }
        }
        return method+name;
    }

    public void setOpenAPIToolProvider(OpenAPIToolProvider provider,Operation operation){
        this.provider = provider;
        this.makerId = provider.id;
        parameterMap.clear();
        List<Parameter> parameters = operation.getParameters();
        if(parameters!=null) parameterMap.putAll(parameters.stream().collect(
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
            hash = metaData.hashCode();
        }catch (Exception ignore){}

    }
    public void merge(AIPortTool tool){
        id=tool.id;
        makerId=tool.makerId;
        status=tool.status;
        name=tool.name;
        hash=tool.hash;
        tags=tool.tags;
        agentId=tool.agentId;
        lastUpdated = -1;
        int hash = metaData.hashCode();
        if(id>0) {
            if (hash == this.hash) {
                lastUpdated = 0;
            } else {
                lastUpdated = System.currentTimeMillis();
            }
        }else{
            this.status = 1;
        }
        this.hash = hash;
    }
    @Override
    public String name() {
        return name;
    }

    @Override
    public String call(Map<String, Object> parameters) {
        boolean isError = false;
        String result;
        if(status>0) {
            String path = this.path;
            Map<String, String> header = new HashMap<>();
            List<String> queries = new ArrayList<>();
            try {
                String requestBody = (String) parameters.get("requestBody");
                parameters = (Map<String, Object>) parameters.get("parameters");
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    Parameter parameter = parameterMap.get(entry.getKey());
                    if (parameter != null) {
                        String name = parameter.getName();
                        String value = (String) entry.getValue();
                        String in = parameter.getIn().toLowerCase();
                        switch (in) {
                            case "header" -> header.put(name, value);
                            case "path" -> path = path.replace("{" + name + "}", value);
                            case "query" -> queries.add(name + "=" + value);
                        }
                    }
                }
                String url = provider.url + path + "?" + String.join("&", queries);
                result = HttpClient.doRequest(method, url, header, requestBody);
            } catch (Exception e) {
                result = "Error " + INTERNAL_ERROR + ": " + JSON.quote(e.getMessage());
                isError = true;
            }
        }else if(status==0){
            result = "Error " + INTERNAL_ERROR + ": tool is disabled";
        }else{
            result = "Error " + INTERNAL_ERROR + ": tool is deprecated";
        }
        return "{\"content\":[\""+result+"\"],\"isError\":"+isError+"}";
    }
}
