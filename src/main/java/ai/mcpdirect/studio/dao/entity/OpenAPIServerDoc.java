package ai.mcpdirect.studio.dao.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAPIServerDoc {
        public static class Security{
            public String description;
            public String key;
        }
        public static class Server{
            public String description;
            public String url;
        }
        public static class Path{
            public String method;
            public String path;
        }
        public String doc;
        public List<Server> servers;
        public Map<String,Security> securities;
        public Map<String,Path> paths;
        public void addServer(String description,String url){
            if(servers==null){
                servers = new ArrayList<>();
            }
            Server server = new Server();
            server.description = description;
            server.url = url;
            servers.add(server);
        }
        public void addSecurity(String description,String keyName){
            if(securities==null){
                securities = new HashMap<>();
            }
            Security security = new Security();
            security.description = description;
            securities.put(keyName,security);
        }
        public void addPath(String name,String method,String path){
            if(paths==null){
                paths = new HashMap<>();
            }
            Path p = new Path();
            p.method = method;
            p.path = path;
            paths.put(name,p);
        }
    }