package ai.mcpdirect.studio;


import ai.mcpdirect.backend.dao.entity.aitool.*;
import ai.mcpdirect.backend.util.AIPortAccessKeyValidator;
import ai.mcpdirect.studio.handler.*;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import appnet.hstp.*;
import appnet.hstp.annotation.ServiceScan;
import appnet.hstp.engine.HstpServiceEngine;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.exception.ServiceException;
import appnet.hstp.exception.ServiceNotFoundException;
import ai.mcpdirect.backend.dao.entity.account.AIPortAccessKeyCredential;
import ai.mcpdirect.backend.dao.entity.account.AIPortOtp;
import ai.mcpdirect.backend.dao.entity.account.AIPortUser;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.service.AIToolServiceHandler;
import ai.mcpdirect.studio.tool.AITool;
import appnet.hstp.labs.util.http.HstpHttpClient;
import appnet.util.crypto.SHA256;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker.TYPE_MCP;

@ServiceScan
public class MCPDirectStudio {
    private static final Logger LOG = LoggerFactory.getLogger(MCPDirectStudio.class);

    private static final String hstpWebport;
    private static final String adminProvider;
    private static final String authenticationServiceAddress;
    private static final long machineId;
    private static String machineName;
    private static USL aitoolsManagementUSL;
    private static USL accountServiceUSL;
    private static ServiceEngine serviceEngine;
    private static ServiceHeaders authHeaders;
    private static ToolAgentDetails toolAgentDetails;
    private static AccountDetails accountDetails;
    private static ToolLogHandler toolLogHandler;
    private static final Map<String,AIPortAccessKeyCredential> accessKeyCredentials = new ConcurrentHashMap<>();


    static{
        String env =System.getenv("AI_MCPDIRECT_HSTP_WEBPORT");
        hstpWebport = env==null?"https://hstp.mcpdirect.ai/hstp/":env;
        env = System.getenv("AI_MCPDIRECT_ADMIN_PROVIDER");
        adminProvider = env == null?"admin.mcpdirect.ai":env;
        authenticationServiceAddress="authentication@"+ adminProvider;
        String mid = null;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            // Windows系统获取机器GUID
            if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec(
                        "reg query HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography /v MachineGuid");
                process.waitFor();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("MachineGuid")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            mid = parts[3];
                            break;
//                                System.out.println("Windows机器GUID: " + parts[3]);
                        }
                    }
                }
                if(mid==null) {
                    process = Runtime.getRuntime().exec("wmic computersystem get model");
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    boolean firstLine = true;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty() && !firstLine) {
                            machineName = line.trim();
                        }
                        firstLine = false;
                    }
                }
            }
            // Linux系统可以读取/etc/machine-id或/var/lib/dbus/machine-id
            else if(os.contains("linux")){
                Process process = Runtime.getRuntime().exec("cat /etc/machine-id");
                process.waitFor();
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                mid = reader.readLine();
            } else if(os.contains("mac os")||os.contains("macos")) {
                //macOS
                Process process = Runtime.getRuntime().exec(
                        "system_profiler SPHardwareDataType | grep UUID");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line = reader.readLine();
                if (line != null) {
                    mid = line.split(":")[1].trim();
                }
                process = Runtime.getRuntime().exec("sysctl -n hw.model");
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String model = reader.readLine();

                if (model.startsWith("Macmini")) {
                    machineName = "Mac mini";
                } else if (model.startsWith("MacBookPro")) {
                    machineName = "MacBook Pro";
                } else if (model.startsWith("MacBookAir")) {
                    machineName = "MacBook Air";
                } else if (model.startsWith("iMac")) {
                    machineName = "iMac";
                } else if (model.startsWith("MacPro")) {
                    machineName = "Mac Pro";
                }
            }
        } catch (Exception ignore) {}

//        if(mid==null)try {
//            //MAC
//            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//            while (networkInterfaces.hasMoreElements()) {
//                NetworkInterface networkInterface = networkInterfaces.nextElement();
//                if (networkInterface.getHardwareAddress() != null) {
//                    byte[] mac = networkInterface.getHardwareAddress();
//
//                    // 使用MAC地址和时间戳生成UUID
//                    long mostSigBits = 0;
//                    for (int i = 0; i < 8; i++) {
//                        mostSigBits = (mostSigBits << 8) | (mac[i % mac.length] & 0xff);
//                    }
//
//                    mid = Long.toString(mostSigBits,16);
//                    break;
//                }
//            }
//        }catch (Exception ignore){}

        if(mid==null) try {
            String home = System.getProperty("user.home");
            BasicFileAttributes attrs = Files.readAttributes(
                    Path.of(home), BasicFileAttributes.class);
            
            FileTime creationTime = attrs.creationTime();
            mid = System.getProperty("user.name")+","+home+","+ creationTime.toMillis();
        } catch (IOException e) {
            mid = System.getProperty("user.name")+","+System.getProperty("user.home")+",0";
        }

        machineId = AIPortAccessKeyValidator.hashCode(mid);
//        ServiceEngineFactory.setServiceEngineIdSeed("ai.mcpdirect.studio."+machineId);
        if(machineName==null){
            machineName = System.getProperty("os.name");
        }

    }
    private static void start(String keySeed) throws Exception {
//        serviceEngine = ServiceEngineFactory.getServiceEngine();
//        hstpWebport= serviceEngine.getProperty("appnet.hstp.labs.aiport.mcpwings/hstp_webport").toString();
//        String adminProvider = serviceEngine.getProperty("appnet.hstp.labs.aiport.mcpwings/admin_provider").toString();
//        authenticationServiceAddress="authentication@"+ adminProvider;
        serviceEngine = new HstpServiceEngine(null,null,
                "ai.mcpdirect.studio."+machineId+"."+keySeed);
        accountServiceUSL = new USL("account.management", adminProvider);
        aitoolsManagementUSL = new USL("aitools.management", adminProvider);
        LOG.info("ServiceEngine {} started", serviceEngine);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logout();
            } catch (Exception ignore) {
            }
        }));
    }

    public static void setToolLogHandler(ToolLogHandler toolLogHandler) {
        MCPDirectStudio.toolLogHandler = toolLogHandler;
        if(toolLogHandler!=null) for (ToolLogHandler.ToolLog log : getToolLogIndex()) {
            toolLogHandler.log(log);
        }
    }
    public static void logTool(AIPortAccessKeyCredential key, String agent, String maker, String tool, Map<String,Object> input, String output){
        if(toolLogHandler !=null){
            ToolLogHandler.ToolLog toolLog = new ToolLogHandler.ToolLog(key.id, key.name, agent, maker, tool);
            saveToolLog(toolLog,input,output);
            toolLogHandler.log(toolLog);
        }
    }
    private static File toolLogIndexFile;
    private final static List<ToolLogHandler.ToolLog> toolLogIndex = new ArrayList<>();
    private static boolean checkToolLogIndexFile(){
        if(toolLogIndexFile==null) {
            File dir = new File(System.getProperty("user.home"), ".mcpdirect/studio/" + Long.toString(accountDetails.userInfo.id, 36) + "/logs/");
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            toolLogIndexFile = new File(dir,"tool_logs.index");
            if(toolLogIndexFile.exists()) try{
                FileInputStream in = new FileInputStream(toolLogIndexFile);
                toolLogIndex.addAll(JSON.fromJson(in,new TypeReference<>(){}));
            }catch (Exception e){
                return false;
            }
        }
        return true;
    }
    private synchronized static void saveToolLog(ToolLogHandler.ToolLog log, Map<String,Object> input, String output){
        if(checkToolLogIndexFile()) try(FileOutputStream out = new FileOutputStream(toolLogIndexFile)) {
            toolLogIndex.add(log);
            out.write(JSON.toJsonBytes(toolLogIndex));
            FileOutputStream details = new FileOutputStream(new File(toolLogIndexFile.getParentFile(),log.id));
            details.write(JSON.toJsonBytes(Map.of("input",JSON.toPrettyJson(input),"output",output)));
            details.close();
        }catch (Exception ignore){}
    }
    public static ToolLogHandler.ToolLogDetails getToolLogDetails(String id){
        try(FileInputStream in = new FileInputStream(new File(toolLogIndexFile.getParentFile(),id))){
            return JSON.fromJson(in.readAllBytes(), new TypeReference<>() {});
        }catch (Exception e){
            return new ToolLogHandler.ToolLogDetails();
        }
    }

    public static List<ToolLogHandler.ToolLog> getToolLogIndex(){
        if(checkToolLogIndexFile()){
            return toolLogIndex;
        }
        return Collections.emptyList();
    }

    private static final Map<String,Long> invalidKeys = new ConcurrentHashMap<>();
    public static AIPortAccessKeyCredential getAccessKeyCredential(String keyId) throws Exception {
        AIPortAccessKeyCredential key = accessKeyCredentials.get(keyId);
        if(key==null&&!invalidKeys.containsKey(keyId)){
            key = queryAccessKey(Long.parseLong(keyId));
        }
        return key;
    }


    private static long otpId;
    public static int register(String account,Locale locale) throws Exception {
        if(locale==null){
            // Get the default locale of the Java Virtual Machine (JVM)
            // This is typically initialized based on the operating system's settings.
            locale = Locale.getDefault();
        }
        // Get the language code (e.g., "en", "es", "fr")
        String languageCode = locale.getLanguage();
//            System.out.println("System Language Code (ISO 639-1): " + languageCode);

        // Get the country code (e.g., "US", "GB", "ES", "FR")
        String countryCode = locale.getCountry();
//            System.out.println("System Country Code (ISO 3166-1 alpha-2): " + countryCode);
        String language = languageCode+"-"+countryCode;
        SimpleServiceResponseMessage<AIPortOtp> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/register",null,
                Map.of("account",account,"userInfo",Map.of("language",language)),
                new TypeReference<>(){});
        if(httpResp.code== Service.SERVICE_SUCCESSFUL){
            if((otpId = httpResp.data.id)>0){
                return Service.SERVICE_SUCCESSFUL;
            }
        }
        return httpResp.code;
    }
    public static boolean register(String account,String name,Locale locale,String otp,String password) throws Exception {
        if(locale==null){
            // Get the default locale of the Java Virtual Machine (JVM)
            // This is typically initialized based on the operating system's settings.
            locale = Locale.getDefault();
        }
        // Get the language code (e.g., "en", "es", "fr")
        String languageCode = locale.getLanguage();
//            System.out.println("System Language Code (ISO 639-1): " + languageCode);

        // Get the country code (e.g., "US", "GB", "ES", "FR")
        String countryCode = locale.getCountry();
//            System.out.println("System Country Code (ISO 3166-1 alpha-2): " + countryCode);
        String language = languageCode+"-"+countryCode;
        if(name==null){
            name = account;
        }
        SimpleServiceResponseMessage<AIPortOtp> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/register",null,
                Map.of("account",account,"otpId",otpId,"otp",otp,
                        "userInfo",Map.of("language",language,"name",name)),
                new TypeReference<>(){});
        if(httpResp.code== Service.SERVICE_SUCCESSFUL){
            if(otpId == httpResp.data.id){
//                httpResp = HstpHttpClient.hstpRequest(
//                        hstpWebport,authenticationServiceAddress+"/forgot_password",null,
//                        Map.of("account",account,"otpId",otpId,"otp",otp,"password", SHA256.digest(password)),
//                        new TypeReference<>(){});
//                return httpResp.code == 0 && otpId == httpResp.data.id;
                return forgotPassword(account,otp,password);
            }
        }
        return false;
    }
    public static int forgotPassword(String account,Locale locale) throws Exception {
        if(locale==null){
            // Get the default locale of the Java Virtual Machine (JVM)
            // This is typically initialized based on the operating system's settings.
            locale = Locale.getDefault();
        }
        // Get the language code (e.g., "en", "es", "fr")
        String languageCode = locale.getLanguage();
//            System.out.println("System Language Code (ISO 639-1): " + languageCode);

        // Get the country code (e.g., "US", "GB", "ES", "FR")
        String countryCode = locale.getCountry();
//            System.out.println("System Country Code (ISO 3166-1 alpha-2): " + countryCode);
        String language = languageCode+"-"+countryCode;
        SimpleServiceResponseMessage<AIPortOtp> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/forgot_password",null,
                Map.of("account",account,"userInfo",Map.of("language",language)),
                new TypeReference<>(){});
        if(httpResp.code== Service.SERVICE_SUCCESSFUL){
            if((otpId = httpResp.data.id)>0){
                return Service.SERVICE_SUCCESSFUL;
            }
        }
        return httpResp.code;
    }
    public static boolean forgotPassword(String account,String otp,String password) throws Exception {
        SimpleServiceResponseMessage<AIPortOtp> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/forgot_password",null,
                Map.of("account",account,"otpId",otpId,"otp",otp,"password", SHA256.digest(password)),
                new TypeReference<>(){});
        return httpResp.code == 0 && otpId == httpResp.data.id;
    }
    private static class AccountDetails {
        public String account;
        public String accountKeySeed;
        public String accessToken;
        public int accessTokenType;
        public boolean newAccount;
        public AIPortUser userInfo;
    }
    public static String getAccount(){
        return accountDetails!=null?accountDetails.account:null;
    }
    public static AIPortUser getUserInfo(){
        return accountDetails!=null?accountDetails.userInfo:null;
    }

    private static UserInfoNotificationHandler userInfoNotificationHandler;
    public static void setUserInfoNotificationHandler(UserInfoNotificationHandler handler){
        userInfoNotificationHandler = handler;
    }
    private static void notifyUserInfo(){
        if(userInfoNotificationHandler!=null){
            userInfoNotificationHandler.onUserInfoNotification(getUserInfo());
        }
    }
    private static void onLoginHttpResponse(SimpleServiceResponseMessage<AccountDetails> httpResp,String userDevice) throws Exception {
        if(httpResp.code== Service.SERVICE_SUCCESSFUL){
            accountDetails = httpResp.data;
            if(accountDetails.userInfo.name==null){
                accountDetails.userInfo.name = accountDetails.account;
            }
            notifyUserInfo();
//            System.setProperty(ServiceEngineConfiguration.ENGINE_ID_SEED_PROPERTY,
//                    "ai.mcpdirect.studio."+machineId+"."+accountDetails.userInfo.id);
//            System.setProperty(ServiceEngineConfiguration.ENGINE_ID_SEED_PROPERTY,
//                    "ai.mcpdirect.studio."+machineId);
            start(accountDetails.accountKeySeed);
            authHeaders = new ServiceHeaders()
                    .addHeader("hstp-auth", accountDetails.accessToken)
                    .addHeader("mcpdirect-device",userDevice);
            initToolAgent();
        }
    }
    public static boolean login(String account, String password) throws Exception {
        if(serviceEngine!=null){
            logout();
        }
        if(serviceEngine!=null){
            System.exit(0);
        }
        long milliseconds = System.currentTimeMillis();
        String hashedPassword = SHA256.digest(password);
//        String userDevice = ServiceEngineFactory.getEngineId();
        String userDevice = serviceEngine.getEngineId();
        SimpleServiceResponseMessage<AccountDetails> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/login",userDevice,
                Map.of("account",account,
                        "secretKey",SHA256.digest(hashedPassword+milliseconds),
                        "timestamp",milliseconds
//                        , "userDevice",serviceEngine.getEngineId().hashCode()
                ), new TypeReference<>(){});
        onLoginHttpResponse(httpResp,userDevice);
        return accountDetails !=null;
    }

    private static void saveAnonymousKey(String key){
        File dir = new File(System.getProperty("user.home"),".mcpdirect/studio/");
        if(!dir.exists()&&!dir.mkdirs()){
            return;
        }
        File file = new File(dir,"anonymous");
        try(FileOutputStream out = new FileOutputStream(file)){
            out.write(key.getBytes());
        }catch (Exception ignore){
        }
    }
    public static String getAnonymousKey(){
        File dir = new File(System.getProperty("user.home"),".mcpdirect/studio/");
        if(!dir.exists()&&!dir.mkdirs()){
            return null;
        }
        File file = new File(dir,"anonymous");
        if(file.exists()) try(FileInputStream in = new FileInputStream(file)){
            return new String(in.readAllBytes());
        }catch (Exception ignore){
        }
        return null;
    }
    public static String anonymousRegister(Locale locale) throws Exception {
        if(locale==null){
            // Get the default locale of the Java Virtual Machine (JVM)
            // This is typically initialized based on the operating system's settings.
            locale = Locale.getDefault();
        }
        // Get the language code (e.g., "en", "es", "fr")
        String languageCode = locale.getLanguage();
//            System.out.println("System Language Code (ISO 639-1): " + languageCode);

        // Get the country code (e.g., "US", "GB", "ES", "FR")
        String countryCode = locale.getCountry();
//            System.out.println("System Country Code (ISO 3166-1 alpha-2): " + countryCode);
        String language = languageCode+"-"+countryCode;
        SimpleServiceResponseMessage<AIPortAccessKeyCredential> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/register/anonymous",null,
                Map.of("deviceId",Long.toString(machineId),"userInfo",Map.of("language",language)),
                new TypeReference<>(){});
        if(httpResp.code== Service.SERVICE_SUCCESSFUL){
            if((otpId = httpResp.data.id)>0){
                saveAnonymousKey(httpResp.data.secretKey);
                return httpResp.data.secretKey;
            }
        }
        return null;
    }
    public static boolean anonymousLogin(String password) throws Exception {
        if(serviceEngine!=null){
            logout();
        }
        if(serviceEngine!=null){
            System.exit(0);
        }
        long milliseconds = System.currentTimeMillis();
        String hashedPassword = SHA256.digest(password);
//        String userDevice = ServiceEngineFactory.getEngineId();
        String userDevice = Long.toString(machineId);
        SimpleServiceResponseMessage<AccountDetails> httpResp = HstpHttpClient.hstpRequest(
                hstpWebport,authenticationServiceAddress+"/login/anonymous",userDevice,
                Map.of("id", AIPortAccessKeyValidator.hashCode(password),
                        "secretKey",SHA256.digest(hashedPassword+milliseconds),
                        "timestamp",milliseconds
//                        , "userDevice",serviceEngine.getEngineId().hashCode()
                ), new TypeReference<>(){});
        onLoginHttpResponse(httpResp,userDevice);
        return accountDetails !=null;
    }
    public static void logout() throws Exception {
        if(serviceEngine==null){
            return;
        }
        Service service = accountServiceUSL.appendPath("logout")
                .createServiceClient()
                .headers(authHeaders)
                .content("{}")
                .request(serviceEngine);
        if(service.getErrorCode()==0){
            serviceEngine.stop();
            serviceEngine = null;
            accountDetails = null;
            authHeaders = null;
            toolAgentDetails = null;
            mcpServerConfigs.clear();
        }else{
            throw new ServiceException("Service request failed. Error="+service.getErrorCode());
        }
    }

    public static int transferAnonymous(String anonymousKey,String password) throws Exception {
        if(password==null){
            password="";
        }
        long milliseconds = System.currentTimeMillis();
        String hashedPassword = SHA256.digest(password);
        Service service = accountServiceUSL.appendPath("anonymous/transfer")
                .createServiceClient()
                .headers(authHeaders)
                .content(Map.of(
                        "id",AIPortAccessKeyValidator.hashCode(anonymousKey),
                        "secretKey",SHA256.digest(hashedPassword+milliseconds),
                        "timestamp",milliseconds,
                        "password",SHA256.digest(password)
                ))
                .request(serviceEngine);

        if(service.getErrorCode()==0){
            SimpleServiceResponseMessage<Boolean> resp = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            return resp.code;
        }
        return Service.SERVICE_FAILED;
    }

    public static int changePassword(String currentPassword,String password) throws Exception {
        long milliseconds = System.currentTimeMillis();
        String hashedPassword = SHA256.digest(currentPassword);
        Service service = accountServiceUSL.appendPath("password/change")
                .createServiceClient()
                .headers(authHeaders)
                .content(Map.of(
                        "secretKey",SHA256.digest(hashedPassword+milliseconds),
                        "timestamp",milliseconds,
                        "password",SHA256.digest(password)
                ))
                .request(serviceEngine);

        if(service.getErrorCode()==0){
            SimpleServiceResponseMessage<Boolean> resp = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            return resp.code;
        }
        return Service.SERVICE_FAILED;
    }


    private static MCPServerNotificationHandler mcpServerHandler;
    public static void setMcpServerNotificationHandler(MCPServerNotificationHandler handler){
        mcpServerHandler = handler;
    }
    private static void notifyMCPServer(List<MCPServer> servers){
        if(mcpServerHandler!=null){
            mcpServerHandler.onMCPServersNotification(servers);
        }
    }
    private static void notifyLocalMCPServer(List<MCPServer> servers){
        if(mcpServerHandler!=null){
            mcpServerHandler.onLocalMCPServersNotification(servers);
        }
    }
    public static List<MCPServer> addMCPServer(String json) throws Exception {
        List<MCPServer> mcpServers = AIToolServiceHandler.addMCPServer(json);
        for (MCPServer mcpServer : mcpServers) {
            String name = mcpServer.name;
            if(name==null||(name=name.trim()).isEmpty()||name.length()>32){
                throw new Exception("The name must not be empty and the max length is 32");
            }
        }
        for (MCPServer mcpServer : mcpServers) {
            mcpServerConfigs.put(mcpServer.name,new MCPServerConfig(mcpServer.url,mcpServer.command,mcpServer.args,mcpServer.env));
        }
        notifyMCPServer(mcpServers);
        writeMCPServerConfigs();
        return mcpServers;
    }
    public static MCPServer addMCPServer(String serverName, String url,String command,
                                    List<String> args,Map<String,String> env) throws Exception {
        if(serverName==null||(serverName=serverName.trim()).isEmpty()||serverName.length()>32){
            throw new Exception("The name must not be empty and the max length is 32");
        }
        MCPServer mcpServer = AIToolServiceHandler.addMCPServer(serverName, url, command, args, env);
        mcpServerConfigs.put(serverName,new MCPServerConfig(mcpServer.url,mcpServer.command,mcpServer.args,mcpServer.env));
        notifyMCPServer(List.of(mcpServer));
        writeMCPServerConfigs();
        return mcpServer;
    }
    public static Collection<? extends MCPServer> getMCPServers(){
        return AIToolServiceHandler.getMCPServers();
    }
    public static MCPServer getMCPServer(String serverName){
        return AIToolServiceHandler.getMCPServer(serverName);
    }
    public static class ToolAgentDetails {
        public AIPortToolAgent toolAgent;
        public List<AIPortToolMaker> makers;
        public List<AIPortMCPServerConfig> mcpServerConfigs;
        public List<AIPortTool> tools;
    }

    private static Service hstpRequest(USL baseUsl,String path,Map<String,Object> parameters) throws Exception {
        return baseUsl.appendPath(path).createServiceClient()
                .headers(authHeaders)
                .content(parameters)
                .request(serviceEngine);
    }
    private static final ConcurrentHashMap<String, MCPServerConfig> mcpServerConfigs = new ConcurrentHashMap<>();
    private static File mcpServerConfigFile;
    private static void writeMCPServerConfigs(){
        try(FileOutputStream out = new FileOutputStream(mcpServerConfigFile)) {
            out.write(JSON.toJsonBytes(mcpServerConfigs));
        }catch (Exception ignore){}
    }
    public static void getLocalMCPServers(){
        String userHome = System.getProperty("user.home");
        File file = new File(userHome, ".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36));
        if(!file.exists()){
            file.mkdirs();
        }
        if(file.exists()) try{
            mcpServerConfigFile = new File(file,"mcpservers");
            if(file.exists()) try(FileInputStream in = new FileInputStream(mcpServerConfigFile)) {
                List<MCPServer> mcpServers = new ArrayList<>();
                Map<String, MCPServerConfig> map = JSON.fromJson(in.readAllBytes(), new TypeReference<>() {});
                map.forEach((n,c)->{
                    if(!mcpServerConfigs.containsKey(n)) try {
                        MCPServer mcpServer
                                = AIToolServiceHandler.addMCPServer(n, c.url, c.command, c.args, c.env);
                        if (mcpServer.id == 0) mcpServers.add(mcpServer);
                        mcpServerConfigs.put(n,c);
                    } catch (Exception ignore) {}
                });
                writeMCPServerConfigs();
                notifyLocalMCPServer(mcpServers);
            }
        }catch (Exception ignore){}
    }
    public static void removeLocalMCPServer(MCPServer server){
        mcpServerConfigs.remove(server.name);
        writeMCPServerConfigs();
        AIToolServiceHandler.remoteMCPServer(server.name);
        List<MCPServer> mcpServers = new ArrayList<>();
        mcpServerConfigs.forEach((n,c)->{
            try {
                MCPServer mcpServer
                        = AIToolServiceHandler.addMCPServer(n, c.url, c.command, c.args, c.env);
                if (mcpServer.id == 0) mcpServers.add(mcpServer);
            } catch (Exception ignore) {}
        });
        notifyLocalMCPServer(mcpServers);
    }

    public static void initToolAgent(){
        new Thread(()->{
            try {
                for (int i = 0; i < 15; i++) {
                    Service service = null;
                    int code;
                    SimpleServiceResponseMessage<ToolAgentDetails> resp;
                    try {
                        Thread.sleep(1000);
                        service = aitoolsManagementUSL.appendPath("tool_agent/init")
                                .createServiceClient()
                                .headers(authHeaders)
                                .content(Map.of("deviceId",machineId,
                                        "device", machineName))
                                .request(serviceEngine);

                        code = service.getErrorCode();
                    } catch (ServiceNotFoundException e) {
                        code = Service.SERVICE_NOT_FOUND;
                    }
                    if (code == Service.SERVICE_NOT_FOUND) {
                        continue;
                    }
                    if (code == 0 && (resp = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {})).code == 0) {
                        toolAgentDetails = resp.data;
                        if (toolAgentDetails.makers != null) {
                            if(toolAgentDetails.tools==null) toolAgentDetails.tools = new ArrayList<>();
                            Map<Long, AIPortToolMaker> collect = resp.data.makers.stream().collect(
                                    Collectors.toMap(v -> v.id, v -> v));
                            if (toolAgentDetails.mcpServerConfigs != null) {
                                List<MCPServer> mcpServers = new ArrayList<>();
                                for (AIPortMCPServerConfig c : toolAgentDetails.mcpServerConfigs) {
                                    AIPortToolMaker maker = collect.get(c.id);
                                    MCPServer mcpServer = AIToolServiceHandler.addMCPServer(
                                            maker.name, c.url, c.command,
                                            JSON.fromJson(c.args, new TypeReference<>() {}),
                                            JSON.fromJson(c.env, new TypeReference<>() {}));
                                    mcpServer.id = c.id;
//                                    if(mcpServer.status()==STATUS_ON){
//                                        Map<String, AIPortTool> toolCollect = getAIPortTools(mcpServer).stream()
//                                                .collect(Collectors.toMap(t -> t.name, t -> {
//                                                    t.lastUpdated = -1;
//                                                    return t;
//                                                }));
//                                        for (AITool tool : mcpServer.getTools()) try {
//                                            String metaData = JSON.toJson(new ServiceDescription("aitools",
//                                                    "call/" + mcpServer.name + "/" + tool.name(),
//                                                    tool.description(), tool.inputSchema(), "{}"));
//                                            int hash = metaData.hashCode();
//                                            AIPortTool aiPortTool = toolCollect.get(tool.name());
//                                            if(aiPortTool==null) {
//                                                aiPortTool = new AIPortTool(
//                                                        0, mcpServer.id, 0, 0, tool.name(), hash, metaData, ""
//                                                );
//                                                toolAgentDetails.tools.add(aiPortTool);
//                                            }else if(aiPortTool.hash==hash){
//                                                aiPortTool.lastUpdated = 0;
//                                            }else{
//                                                aiPortTool.lastUpdated = System.currentTimeMillis();
//                                            }
//
//                                        }catch (Exception ignore){}
//                                    }
                                    mcpServerConfigs.put(maker.name, new MCPServerConfig(mcpServer.url, mcpServer.command, mcpServer.args, mcpServer.env));
                                    mcpServers.add(mcpServer);                                }
                                notifyMCPServer(mcpServers);
                                writeMCPServerConfigs();
                            }
                        }
                    }
                    getLocalMCPServers();
                    break;
                }
                queryAccessKeys();
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public static List<AIPortTool> getAIPortTools(MCPServer mcpServer){
        Map<String, AIPortTool> collect;
        if(mcpServer.id>0&&toolAgentDetails.tools!=null) {
            collect = toolAgentDetails.tools.stream()
                    .filter(t -> t.makerId == mcpServer.id)
                    .collect(Collectors.toMap(t->t.name,t-> {
                        t = t.duplicate();
                        t.lastUpdated = -1;
                        return t;
                    }));
        }else{
            collect = new HashMap<>();
        }
        for (AITool tool : mcpServer.getTools()) try {
            String metaData = JSON.toJson(new ServiceDescription("aitools",
                    "call/" + mcpServer.name + "/" + tool.name(),
                    tool.description(), tool.inputSchema(), "{}"));
            int hash = metaData.hashCode();
            AIPortTool aiPortTool = collect.get(tool.name());
            if(aiPortTool==null) {
                aiPortTool = new AIPortTool(
                        0, mcpServer.id, 1, 1, tool.name(), hash, tool.description(), ""
                );
//                toolAgentDetails.tools.add(aiPortTool);
                collect.put(aiPortTool.name,aiPortTool);
            }else if(aiPortTool.hash==hash){
                aiPortTool.lastUpdated = 0;
                aiPortTool.metaData = tool.description();
            }else{
                aiPortTool.lastUpdated = System.currentTimeMillis();
                aiPortTool.metaData = tool.description();
            }

        }catch (Exception ignore){}
        return collect.values().stream().toList();
    }
    public static ToolAgentDetails getToolAgentDetails() throws Exception {
        Service service = aitoolsManagementUSL.appendPath("tool_agent/details/get")
                .createServiceClient()
                .headers(authHeaders)
                .content("{}")
                .request(serviceEngine);
        SimpleServiceResponseMessage<ToolAgentDetails> resp;
        if(service.getErrorCode()==0&&
                (resp=JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {})).code==0) {
            toolAgentDetails = resp.data;
        }
        return toolAgentDetails;
    }

    private static String getToolMetadata(MCPServer mcpServer) throws Exception {
        List<ServiceDescription> list = new ArrayList<>();
        for (AITool tool : mcpServer.getTools()) {
            list.add(new ServiceDescription("aitools","call/"+mcpServer.name+"/"+tool.name(),
                    tool.description(),tool.inputSchema(),"{}"));
        }
        return JSON.toJson(list);
    }

    private static List<AIPortTool> createPublishingTools(MCPServer mcpServer){
        Map<String, AIPortTool> collect;
        if(mcpServer.id>0&&toolAgentDetails.tools!=null){
            collect = toolAgentDetails.tools.stream()
                    .filter(t->t.makerId==mcpServer.id)
                    .collect(Collectors.toMap(t -> t.name, t -> {
                        t = t.duplicate();
                        t.status = -1;
                        return t;
                    }));
        }else{
            collect = new HashMap<>();
        }
        for (AITool tool : mcpServer.getTools()) try {
            String metaData = JSON.toJson(new ServiceDescription("aitools",
                    "call/" + mcpServer.name + "/" + tool.name(),
                    tool.description(), tool.inputSchema(), "{}"));
            int hash = metaData.hashCode();
            AIPortTool aiPortTool = collect.get(tool.name());
            if(aiPortTool==null) {
                aiPortTool = new AIPortTool(
                        0, mcpServer.id, 1, 0, tool.name(), hash, metaData, ""
                );
                collect.put(aiPortTool.name,aiPortTool);
            }else if(aiPortTool.hash==hash){
                collect.remove(tool.name());
            }else{
                aiPortTool.lastUpdated = System.currentTimeMillis();
            }

        }catch (Exception ignore){}
        return collect.values().stream().toList();
    }

    public static class RequestOfPublishTools{
        public AIPortToolMaker maker = new AIPortToolMaker();
        public AIPortMCPServerConfig mcpServerConfig = new AIPortMCPServerConfig();
        public List<AIPortTool> tools;
    }
    public static MCPServer unpublishTools(MCPServer server) throws Exception {
        if(server.id==0){
            throw new Exception("The server was not published");
        }
        Service service = aitoolsManagementUSL
                .appendPath("tool_agent/tools/unpublish")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of("maker",Map.of("id",server.id))))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<Long> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                return server;
            }
        }
        return null;
    }
    public static MCPServer publishTools(MCPServer mcpServer) throws Exception {
        String name = mcpServer.name;
        if(name==null||(name=name.trim()).isEmpty()||name.length()>32){
            throw new Exception("The name must not be empty and the max length is 32");
        }
        RequestOfPublishTools req = new RequestOfPublishTools();
        req.maker.id = mcpServer.id;
        req.maker.name = name;
        req.maker.type = TYPE_MCP;
        req.maker.agentId = toolAgentDetails.toolAgent.id;
//        req.maker.tools = getToolsString(mcpServer);
        req.maker.tags="";
        req.mcpServerConfig.url = mcpServer.url;
        req.mcpServerConfig.command = mcpServer.command;
        req.mcpServerConfig.args = mcpServer.args!=null?JSON.toJson(mcpServer.args):"[]";
        req.mcpServerConfig.env = mcpServer.args!=null?JSON.toJson(mcpServer.env):"{}";
        req.tools = createPublishingTools(mcpServer);
        Service service = aitoolsManagementUSL
                .appendPath("tool_agent/tools/publish")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(req))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<Long> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                mcpServer.id = resp.data;
            }
        }
        return mcpServer;
    }

    public static AIPortAccessKeyCredential generateAccessKey(String name) throws Exception {
        Service service = hstpRequest(accountServiceUSL,"access_key/create",Map.of("name", name));
        AIPortAccessKeyCredential key = null;
        SimpleServiceResponseMessage<AIPortAccessKeyCredential> resp;
        if(service.getErrorCode()==0&&(resp=JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {
        }))!=null&&resp.code==0){
            key = resp.data;
        }
//        saveAccessKey(key);
        return key;
    }

    public static String createMCPConfigFromKey(AIPortAccessKeyCredential credential){
        return "{\"mcpServers\":{\""+credential.name+"\":{\"url\":\"https://connect.mcpdirect.ai/"+credential.secretKey.substring(4)+"/sse\"}}}";
    }

    public static AIPortAccessKeyCredential modifyAccessKey(long id,String name,Integer status) throws Exception {
        if((name==null||(name=name.trim()).isEmpty())&& status==null){
            return null;
        }

        Map<String,Object> parameters = new HashMap<>();
        parameters.put("id",id);
        if(name!=null){
            parameters.put("name",name);
        }
        if(status!=null){
            parameters.put("status",status);
        }
        Service service = hstpRequest(accountServiceUSL,"access_key/modify",parameters);
        AIPortAccessKeyCredential key = null;
        SimpleServiceResponseMessage<AIPortAccessKeyCredential> resp;
        if(service.getErrorCode()==0&&(resp=JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {
        }))!=null&&resp.code==0){
            key = resp.data;
            String s = Long.toString(key.id,36);
            key.secretKey = "aik-"+s.substring(0,4)+"........";
        }
        return key;
    }
    private static AccessKeyNotificationHandler accessKeyHandler;
    public static void setAccessKeyNotificationHandler(AccessKeyNotificationHandler handler){
        accessKeyHandler = handler;
    }
    private static void notifyAccessKeys(List<AIPortAccessKeyCredential> keys){
        if(accessKeyHandler!=null){
            accessKeyHandler.onAccessKeysNotification(keys);
        }
    }
    private static void notifyAccessKeyPermissions(List<AIPortToolPermission> permissions){
        if(accessKeyHandler!=null){
            accessKeyHandler.onAccessKeyPermissionsNotification(permissions);
        }
    }

    public static void queryAccessKeys() throws Exception {
        Service service = hstpRequest(accountServiceUSL,"access_key/query",Map.of());
        List<AIPortAccessKeyCredential> keys = Collections.emptyList();
        SimpleServiceResponseMessage<List<AIPortAccessKeyCredential>> resp;
        if(service.getErrorCode()==0&&(resp=JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {
        }))!=null&&resp.code==0){
            keys = resp.data;
            for (AIPortAccessKeyCredential key : keys) {
                String s = Long.toString(key.id,36);
                key.secretKey = "aik-"+s.substring(0,4)+"........";
            }
        }
        accessKeyCredentials.clear();
        keys.forEach(key->{
            accessKeyCredentials.put(Long.toString(key.id),key);
        });
        notifyAccessKeys(keys);
    }
    public static AIPortAccessKeyCredential queryAccessKey(long keyId) throws Exception {
        Service service = hstpRequest(accountServiceUSL,"access_key/query",Map.of("keyId",keyId));
        List<AIPortAccessKeyCredential> keys = Collections.emptyList();
        SimpleServiceResponseMessage<List<AIPortAccessKeyCredential>> resp;
        if(service.getErrorCode()==0&&(resp=JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {
        }))!=null&&resp.code==0){
            keys = resp.data;
            for (AIPortAccessKeyCredential key : keys) {
                String s = Long.toString(key.id,36);
                key.secretKey = "aik-"+s.substring(0,4)+"........";
            }
        }
        for (AIPortAccessKeyCredential key : keys) {
            String s = Long.toString(key.id);
            accessKeyCredentials.put(s,key);
            invalidKeys.remove(s);
            return key;
        }
        invalidKeys.put(Long.toString(keyId),keyId);
        return null;
    }

    public static boolean saveAccessKey(AIPortAccessKeyCredential key){
        File dir = new File(System.getProperty("user.home"),".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36)+"/credentials/");
        if(!dir.exists()&&!dir.mkdirs()){
            return false;
        }
        File file = new File(dir,Integer.toString(accountDetails.account.hashCode(),36));
        Map<Long,String> keys = null;
        if(file.exists()) try {
            keys = JSON.fromJson(file, new TypeReference<>() {});
        }catch (Exception ignore){}
        if(keys==null) keys = new HashMap<>();
        keys.put(key.id,key.secretKey);
        try(FileOutputStream out = new FileOutputStream(file)){
            out.write(JSON.toJsonBytes(keys));
            return true;
        }catch (Exception ignore){
            return false;
        }
    }
    public static String getAccessKey(long id){
        File dir = new File(System.getProperty("user.home"),".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36)+"/credentials/");
        File file = new File(dir,Integer.toString(accountDetails.account.hashCode(),36));
        if(file.exists()) try {
            Map<Long,String> keys = JSON.fromJson(file, new TypeReference<>() {});
            return keys.get(id);
        }catch (Exception ignore){}
        return null;
    }

    private static ToolAgentsDetailsNotificationHandler toolAgentHandler;
    public static void setToolAgentsDetailsNotificationHandler(ToolAgentsDetailsNotificationHandler handler){
        toolAgentHandler = handler;
    }
    private static void notifyToolAgents(List<AIPortToolAgent> agents, List<AIPortToolMaker> makers,
                                         List<AIPortTool> tools, List<AIPortToolPermission> permissions){
        if(toolAgentHandler !=null){
            toolAgentHandler.onToolAgentsNotification(agents,makers,tools,permissions,toolAgentDetails.toolAgent);
        }
    }
    public static class AllToolAgentsDetails {
        public List<AIPortToolAgent> agents;
        public List<AIPortToolMaker> makers;

        public List<AIPortTool> tools;
        public List<AIPortToolPermission> permissions;
    }

    public static void getAllToolAgentsDetails() throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool_agent/all/details/query")
                .createServiceClient()
                .headers(authHeaders)
                .content("{}")
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<AllToolAgentsDetails> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                notifyToolAgents(resp.data.agents,resp.data.makers,resp.data.tools,resp.data.permissions);
            }
        }
    }
    public static void grantToolPermission(List<AIPortToolPermission> permissions) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool/permission/grant")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of("permissions",permissions)))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortToolPermission>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                notifyAccessKeyPermissions(resp.data);
            }
        }
    }
}
