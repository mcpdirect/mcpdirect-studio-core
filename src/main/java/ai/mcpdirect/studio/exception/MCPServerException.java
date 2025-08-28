package ai.mcpdirect.studio.exception;

public class MCPServerException extends Exception{
    public MCPServerException(String serverName){
        super(serverName);
    }
}
