package ai.mcpdirect.studio.tool.util;

import ai.mcpdirect.studio.MCPDirectStudio;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.METHOD_NOT_FOUND;

public class MCPToolProvider extends MCPServer implements AIToolProvider{
    private static final Logger LOG = LoggerFactory.getLogger(MCPToolProvider.class);
    public static final McpJsonMapper JSON_MAPPER = McpJsonDefaults.getMapper();
    @JsonIgnore
    public String baseUrl;
    @JsonIgnore
    public String sseEndpoint;
    @JsonIgnore
    private final McpSchema.Implementation clientInfo;
    @JsonIgnore
    private McpAsyncClient client;

//    public MCPToolProvider(String clientName, String clientVersion,
//                           int type,String baseUrl, String sseEndpoint, String command,
//                           List<String> args, Map<String, String> env, String serverName) {
//        super(type,baseUrl==null?sseEndpoint:(baseUrl+(sseEndpoint==null?"":sseEndpoint)),
//                command, args, env, serverName);
//    public MCPToolProvider(String clientName, String clientVersion,
//                           String baseUrl, String sseEndpoint,MCPServerConfig config) {
//        super(config);
//        this.url = baseUrl==null?sseEndpoint:(baseUrl+(sseEndpoint==null?"":sseEndpoint));
//        this.baseUrl = baseUrl;
//        this.sseEndpoint = sseEndpoint;
//        clientInfo = new McpSchema.Implementation(clientName,clientVersion);
////        createMcpSyncClient();
//    }
    public MCPToolProvider(String clientName, String clientVersion, MCPServerConfig config) {
        super(config);
        clientInfo = new McpSchema.Implementation(clientName,clientVersion);
    //        createMcpSyncClient();
    }

    public void createMcpSyncClient(){
        if(client!=null){
            client.close();
        }
        McpClientTransport transport;
        try {
            if(command!=null&&!command.isEmpty()) {
                switch (command) {
                    case "npx" -> command = MCPDirectStudio.getNpxPath();
                    case "npm" -> command = MCPDirectStudio.getNpmPath();
                    case "node" -> command = MCPDirectStudio.getNodePath();
                }
                ServerParameters parameters = ServerParameters.builder(command).args(args).env(env).build();
                transport = new MCPDirectStdioClientTransport(parameters,JSON_MAPPER){
                    @Override
                    public void onException(Throwable throwable) {
                        errorMessage +=(throwable.getMessage()+"\n");
                    }
                };
            }else{
                HttpRequest.Builder builder = HttpRequest.newBuilder();
                builder.header("Content-Type", "application/json");
                if(env!=null) for (Map.Entry<String, String> entry : env.entrySet()) {
                    builder.header(entry.getKey(),entry.getValue());
                }
                if(this.transport ==2)
                    transport = HttpClientStreamableHttpTransport
                            .builder(baseUrl).endpoint(sseEndpoint)
                            .requestBuilder(builder)
                            .build();
                else transport = HttpClientSseClientTransport
                        .builder(baseUrl).sseEndpoint(sseEndpoint)
                        .requestBuilder(builder)
                        .build();
            }
            McpClient.AsyncSpec builder = McpClient.async(transport)
                    .clientInfo(clientInfo)
                    .requestTimeout(Duration.ofSeconds(15))
                    .initializationTimeout(Duration.ofSeconds(15))
                    .capabilities(McpSchema.ClientCapabilities.builder()
                            .roots(true)
//                            .sampling()
                            .build());
            client = builder.build();
            errorCode = 0;
            errorMessage = "";
            client.initialize();
            status = STATUS_ON;
        } catch (Throwable e) {
//            status = STATUS_ERROR;
            errorCode = ERROR;
            errorMessage += e.getMessage();
        }
    }
    public void close(){
        client.close();
//        status = STATUS_ABANDONED;
    }
//    public void refreshTools(){
//        try(McpSyncClient mcpClient = createMcpSyncClient()) {
//            mcpClient.initialize();
//            status = STATUS_ON;
////            statusMessage = "successful";
//            McpSchema.ListToolsResult tools = mcpClient.listTools();
//            for (McpSchema.Tool tool : tools.tools()) {
//                LOG.info("refreshTools({},{})",tool.name(),tool.description());
//                MCPTool mcpTool = this.tools.get(tool.name());
//                if(mcpTool==null){
//                    mcpTool = new MCPTool();
//                    this.tools.put(tool.name(),mcpTool);
//                }
//                mcpTool.setMCPToolProvider(this,tool);
////                this.tools.put(tool.name(),new MCPTool(this,tool));
//            }
//        }catch (Throwable e){
//            status = STATUS_ERROR;
//        }
//    }

    @Override
    public void refreshTools(){
        try{
            status = STATUS_ON;
            McpSchema.ListToolsResult tools = client.listTools().block();
            if(tools!=null)for (McpSchema.Tool tool : tools.tools()) {
                LOG.info("refreshTools({},{})",tool.name(),tool.description());
                MCPTool mcpTool = this.tools.get(tool.name());
                if(mcpTool==null){
                    mcpTool = new MCPTool();
                    mcpTool.id = (id+tool.name()).hashCode();
                    this.tools.put(tool.name(),mcpTool);
                }
                mcpTool.setMCPToolProvider(this,tool);
            }
            errorCode = 0;
            errorMessage = "";
        }catch (Throwable e){
            errorCode = ERROR;
            errorMessage = e.getMessage();
        }
    }

//    @Override
//    public String callTool(String name,Map<String,Object> parameters){
//        AITool tool = getTool(name);
//        if(tool!=null) try(McpSyncClient mcpClient = createMcpSyncClient()) {
//            mcpClient.initialize();
//            McpSchema.CallToolResult result = mcpClient.callTool(
//                    new McpSchema.CallToolRequest(tool.name(), parameters)
//            );
//            try {
//                return JSON.toJson(result);
//            }catch (Exception e){
//                return "{}";
//            }
//        }
//        return "The tool of '"+name+"' not available";
//    }
    @Override
    public String callTool(String name,Map<String,Object> parameters){
        String error = "Error "+METHOD_NOT_FOUND;
        AITool tool = getTool(name);
        if(tool!=null) try{
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest(tool.name(), parameters)
            ).block();
            return JSON.toJson(result);
        } catch (Throwable e) {
            errorCode = ERROR;
            error = "Error "+INTERNAL_ERROR+": " + errorMessage;
        }
        return MCPTool.buildCallResult(error,true);
    }
}
