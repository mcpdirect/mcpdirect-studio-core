package ai.mcpdirect.studio.tool.util;

import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
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
    private final String baseUrl;
    private final String sseEndpoint;
    private final McpSchema.Implementation clientInfo;

    public MCPToolProvider(String clientName, String clientVersion, String baseUrl, String sseEndpoint, String command, List<String> args, Map<String, String> env, String serverName) {
        super(baseUrl+sseEndpoint, command, args, env, serverName);
        this.baseUrl = baseUrl;
        this.sseEndpoint = sseEndpoint;
        clientInfo = new McpSchema.Implementation(clientName,clientVersion);
    }

    private McpSyncClient createMcpSyncClient(){
        McpClientTransport transport;
        if(command!=null&&!command.isEmpty()) {
            ServerParameters parameters = ServerParameters.builder(command).args(args).env(env).build();
            transport = new StdioClientTransport(parameters);
        }else{
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.header("Content-Type", "application/json");
            if(env!=null) for (Map.Entry<String, String> entry : env.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }

            transport = HttpClientSseClientTransport
                    .builder(baseUrl).sseEndpoint(sseEndpoint)
                    .requestBuilder(builder)
                    .build();
        }
        McpClient.SyncSpec builder = McpClient.sync(transport)
                .clientInfo(clientInfo)
                .requestTimeout(Duration.ofSeconds(14))
                .initializationTimeout(Duration.ofSeconds(2))
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
            statusMessage = "successful";
            McpSchema.ListToolsResult tools = mcpClient.listTools();
            for (McpSchema.Tool tool : tools.tools()) {
                LOG.info("refreshTools({},{},{})",tool.name(),tool.description(),tool.inputSchema());
                this.tools.put(tool.name(),new MCPTool(this,tool));
            }
        }catch (Exception e){
            status = STATUS_ERROR;
            statusMessage = e.getMessage();
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
            return result.toString();
        }
        return "The tool of '"+name+"' not available";
    }
}
