package ai.mcpdirect.studio.tool.util;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.backend.util.MCPDirectStdioClientTransport;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class MCPToolProvider extends MCPServer{
    private static final Logger LOG = LoggerFactory.getLogger(MCPToolProvider.class);
    public static final McpJsonMapper JSON_MAPPER = McpJsonMapper.getDefault();
    @JsonIgnore
    private final String baseUrl;
    @JsonIgnore
    private final String sseEndpoint;
    @JsonIgnore
    private final McpSchema.Implementation clientInfo;

//    public MCPToolProvider(String clientName, String clientVersion,
//                           int type,String baseUrl, String sseEndpoint, String command,
//                           List<String> args, Map<String, String> env, String serverName) {
//        super(type,baseUrl==null?sseEndpoint:(baseUrl+(sseEndpoint==null?"":sseEndpoint)),
//                command, args, env, serverName);
    public MCPToolProvider(String clientName, String clientVersion,
                           String baseUrl, String sseEndpoint,MCPServerConfig config) {
        super(config);
        this.url = baseUrl==null?sseEndpoint:(baseUrl+(sseEndpoint==null?"":sseEndpoint));
        this.baseUrl = baseUrl;
        this.sseEndpoint = sseEndpoint;
        clientInfo = new McpSchema.Implementation(clientName,clientVersion);
    }

    private McpSyncClient createMcpSyncClient(){
        McpClientTransport transport;
        if(command!=null&&!command.isEmpty()) {
            ServerParameters parameters = ServerParameters.builder(command).args(args).env(env).build();
            transport = new MCPDirectStdioClientTransport(parameters,JSON_MAPPER){
                @Override
                public void onException(Throwable throwable) {
                    statusMessage+=(throwable.getMessage()+"\n");
                }
            };
        }else{
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.header("Content-Type", "application/json");
            if(env!=null) for (Map.Entry<String, String> entry : env.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }
            if(this.transport ==2) transport = HttpClientStreamableHttpTransport
                        .builder(baseUrl).endpoint(sseEndpoint)
                        .requestBuilder(builder)
                        .build();
            else transport = HttpClientSseClientTransport
                    .builder(baseUrl).sseEndpoint(sseEndpoint)
                    .requestBuilder(builder)
                    .build();
        }
        McpClient.SyncSpec builder = McpClient.sync(transport)
                .clientInfo(clientInfo)
                .requestTimeout(Duration.ofSeconds(15))
                .initializationTimeout(Duration.ofSeconds(15))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build());
        return builder.build();
    }
    public void refreshTools(){
        try(McpSyncClient mcpClient = createMcpSyncClient()) {
            mcpClient.initialize();
            status = STATUS_ON;
//            statusMessage = "successful";
            McpSchema.ListToolsResult tools = mcpClient.listTools();
            for (McpSchema.Tool tool : tools.tools()) {
                LOG.info("refreshTools({},{})",tool.name(),tool.description());
                MCPTool mcpTool = this.tools.get(tool.name());
                if(mcpTool==null){
                    mcpTool = new MCPTool();
                    this.tools.put(tool.name(),mcpTool);
                }
                mcpTool.setMCPToolProvider(this,tool);
//                this.tools.put(tool.name(),new MCPTool(this,tool));
            }
        }catch (Throwable e){
            status = STATUS_ERROR;
        }
    }

    @Override
    public String callTool(String name,Map<String,Object> parameters){
        AITool tool = getTool(name);
        if(tool!=null) try(McpSyncClient mcpClient = createMcpSyncClient()) {
            mcpClient.initialize();
            McpSchema.CallToolResult result = mcpClient.callTool(
                    new McpSchema.CallToolRequest(tool.name(), parameters)
            );
            try {
                return JSON.toJson(result);
            }catch (Exception e){
                return "{}";
            }
        }
        return "The tool of '"+name+"' not available";
    }
}
