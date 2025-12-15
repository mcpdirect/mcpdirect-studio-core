package ai.mcpdirect.studio;

import ai.mcpdirect.studio.tool.util.MCPDirectStdioClientTransport;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

public class MCPClientTest {
//    https://mcp.figma.com/.well-known/oauth-authorization-server
    public static void main(String[] args){
        McpSchema.Implementation clientInfo = new McpSchema.Implementation("MCPdirect Studio","1.0.0");
            HttpRequest.Builder httpBuilder = HttpRequest.newBuilder();
            httpBuilder.header("Content-Type", "application/json");
        McpClientTransport transport = HttpClientStreamableHttpTransport
                    .builder("https://mcp.figma.com").endpoint("/mcp")
                    .requestBuilder(httpBuilder)
                    .build();

            McpClient.SyncSpec builder = McpClient.sync(transport)
                    .clientInfo(clientInfo)
                    .requestTimeout(Duration.ofSeconds(15))
                    .initializationTimeout(Duration.ofSeconds(15))
                    .capabilities(McpSchema.ClientCapabilities.builder()
                            .roots(true)
                            .sampling()
                            .build());
            McpSyncClient client = builder.build();
            client.initialize();

    }
}
