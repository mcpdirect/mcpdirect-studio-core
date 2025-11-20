package ai.mcpdirect.studio.tool.openapi;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.util.OpenAPISchemaConverter;
import appnet.hstp.ServiceDescription;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.labs.util.http.HttpClient;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

public class OpenAPITool extends AIPortTool implements AITool {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAPITool.class);
    @JsonIgnore
    private OpenAPIToolProvider provider;
    @JsonIgnore
    private final String path;
    @JsonIgnore
    private final String method;
    @JsonIgnore
    private final Map<String, Parameter> parameterMap = new HashMap<>();
    @JsonIgnore
    private String _metaData;
    public OpenAPITool (String name,String method,String path){
        this.name = name;
        this.path = path;
        this.method = method;
        if(method==null||path==null){
            status = -1;
            lastUpdated = -1;
        }else {
            status = 1;
            lastUpdated = 1;
        }
    }

    public static String name(String prefix,String method,String path){
        StringBuilder name = new StringBuilder();
        for (String s : path.split("/")) {
            if(!s.isEmpty()){
                if(s.startsWith("{")&&s.endsWith("}")){
                    s = s.substring(1,s.length()-1);
                }
                name.append("_").append(s);
            }
        }
        return prefix+"_"+method+name;
    }

    public void setOpenAPIToolProvider(
            OpenAPIToolProvider provider,
            Operation operation){
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
            _metaData = JSON.toJson(new ServiceDescription("aitools",
                    "call/openapi/" + Long.toString(provider.id, Character.MAX_RADIX) + "/" + name(),
                    description, inputSchema, "{}"));
            hash = _metaData.hashCode();
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
        int hash = _metaData.hashCode();
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
    public String metaData(){
        return _metaData;
    }
    @Override
    public String name() {
        return name;
    }

    @Override
    public String call(Map<String, Object> parameters) {
        boolean isError = true;
        String result;
        if(status>0) {
            String path = this.path;
            Map<String, String> header = new HashMap<>();
            List<String> queries = new ArrayList<>();
            for (OpenAPISecurity security : provider.openAPISecurities) {
                SecurityScheme scheme = security.scheme();
                SecurityScheme.Type type = scheme.getType();
                if(type== SecurityScheme.Type.HTTP){
                    switch (scheme.getScheme()){
                        case "bearer" -> {
                            header.put("Authorization","Bearer "+security.security());
                        }
                        case "basic" -> header.put("Authorization","Basic "
                                +Base64.getEncoder().encodeToString(security.security().getBytes()));
                    }
                } else if(type== SecurityScheme.Type.APIKEY){
                    SecurityScheme.In in = scheme.getIn();
                    if(in== SecurityScheme.In.HEADER){

                    }
                }
            }
            try {
                Map<String,Object> responseContentType = (Map<String,Object>) parameters.get("responseContentType");
                String mediaType;
                if(responseContentType!=null&&(mediaType = (String)responseContentType.get("mediaType"))!=null){
                    header.put("Accept",mediaType);
                }
                Map<String,Object> content = (Map<String,Object>) parameters.get("content");
                Object data = null;
                if(content!=null&&(mediaType = (String)content.get("mediaType"))!=null){
                    header.put("Content-Type",mediaType);
                    data = content.get("data");
                }
                parameters = (Map<String, Object>) parameters.get("parameters");
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    Parameter parameter = parameterMap.get(entry.getKey());
                    if (parameter != null) {
                        String name = parameter.getName();
                        String value = (String) entry.getValue();
                        String in = parameter.getIn().toLowerCase();
                        switch (in) {
                            case "header" -> header.put(name, value);
                            case "path" ->
                                    path = path.replace("{" + name + "}",
                                            URLEncoder.encode(value, StandardCharsets.UTF_8));
                            case "query" -> queries.add(name + "=" + URLEncoder.encode(
                                    value,StandardCharsets.UTF_8));
                        }
                    }
                }
                String url = provider.url + path;
                if(!queries.isEmpty()){
                    url += "?" + String.join("&", queries);
                }
                LOG.info("url:"+url);
                result = HttpClient.doRequest(method, url, header, data);
                isError = false;
            } catch (Exception e) {
                result = "Error " + INTERNAL_ERROR + ": " + e.getMessage();

            }
        }else if(status==0){
            result = "Error " + INTERNAL_ERROR + ": tool is disabled";
        }else{
            result = "Error " + INTERNAL_ERROR + ": tool is deprecated";
        }
        try {
            McpSchema.CallToolResult callToolResult = McpSchema.CallToolResult.builder()
                    .addTextContent(result).isError(isError).build();
            return JSON.toJson(callToolResult);
        }catch (Exception e){
            return "{}";
        }
    }
}
