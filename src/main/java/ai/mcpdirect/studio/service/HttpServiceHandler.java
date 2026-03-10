package ai.mcpdirect.studio.service;

import appnet.hstp.annotation.ServiceName;
import appnet.hstp.annotation.ServiceRequestMapping;
import appnet.hstp.annotation.ServiceRequestMessage;
import appnet.hstp.annotation.ServiceResponseMessage;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

@ServiceName("studio.http")
@ServiceRequestMapping("/")
public class HttpServiceHandler {
    private HttpServer server;
    public static class RequestOfHttp{
        public int action;
        public int port;
    }
    @ServiceRequestMapping("start")
    public void start(
            @ServiceRequestMessage RequestOfHttp req,
            @ServiceResponseMessage AIPortServiceResponse<String> resp
    ) throws IOException {
        if(server==null) {
            server = HttpServer.create(new InetSocketAddress(req.port), 0);
            // Define a context that serves files from the current directory
            server.createContext("/", exchange -> {
                // Handle requests here (simple file serving)
            });

            // Start the server
            server.start();
        }
    }
    @ServiceRequestMapping("stop")
    public void stop(
            @ServiceResponseMessage AIPortServiceResponse<String> resp
    ){
        if(server!=null){
            server.stop(30);
        }
    }
}
