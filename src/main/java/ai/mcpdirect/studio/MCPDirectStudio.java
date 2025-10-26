package ai.mcpdirect.studio;


import ai.mcpdirect.backend.dao.entity.account.*;
import ai.mcpdirect.backend.dao.entity.aitool.*;
import ai.mcpdirect.backend.util.AIPortAccessKeyValidator;
import ai.mcpdirect.studio.handler.*;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import appnet.communicator.ssl.SSLContextGenerator;
import appnet.hstp.*;
import appnet.hstp.annotation.ServiceScan;
import appnet.hstp.engine.HstpServiceEngine;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.exception.ServiceException;
import appnet.hstp.exception.ServiceNotFoundException;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.service.AIToolServiceHandler;
import appnet.hstp.labs.util.http.HstpHttpClient;
import appnet.util.crypto.SHA256;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker.TYPE_MCP;
import static ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker.TYPE_VIRTUAL;

@ServiceScan
public class MCPDirectStudio {
    private static final Logger LOG = LoggerFactory.getLogger(MCPDirectStudio.class);

    private static final String hstpWebport;
    private static final String adminProvider;
    private static final String authenticationServiceAddress;
    private static final ServiceEngineConfiguration engineConfig;
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
        Properties props = new Properties();
        try(InputStream resourceAsStream = MCPDirectStudio.class.getResourceAsStream("/mcpdirect-studio.properties")){
            props.load(resourceAsStream);
        }catch (Exception ignore){}
        String webportURL = props.getProperty("ai.mcpdirect.hstp.webport");
        String serviceGateway = props.getProperty("ai.mcpdirect.hstp.service.gateway");
        if(webportURL==null||webportURL.isEmpty()){
            webportURL = System.getProperty("ai.mcpdirect.hstp.webport");
        }
        if(serviceGateway==null||serviceGateway.isEmpty()){
            serviceGateway = System.getProperty("ai.mcpdirect.hstp.service.gateway");
        }
        if(webportURL==null||webportURL.isEmpty()){
            webportURL = System.getenv("AI_MCPDIRECT_HSTP_WEBPORT");
        }
        if(serviceGateway==null||serviceGateway.isEmpty()){
            serviceGateway = System.getenv("AI_MCPDIRECT_HSTP_SERVICE_GATEWAY");
        }
        if(webportURL==null||(webportURL=webportURL.trim()).isEmpty()){
            throw new RuntimeException("Please set 'ai.mcpdirect.hstp.webport' properties in mcpdirect-studio.properties\n" +
                    "or set environment variable 'AI_MCPDIRECT_HSTP_WEBPORT'");
        }
        hstpWebport = webportURL;
        if(serviceGateway==null||(serviceGateway=serviceGateway.trim()).isEmpty()){
            serviceGateway = URI.create(webportURL).getHost()+":53100";
        }
        try {
            engineConfig = ServiceEngineConfiguration.load(Map.of(
                    "gateways", List.of(serviceGateway),
                    "serviceSelectionPolicy", "|peerResolve"
            ));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        ServiceEngineFactory.setSSLContextFactory(((config, isClient) -> {
            try {
                return SSLContextGenerator.createTrustAllClientSSLContext();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }));
        String env = System.getenv("AI_MCPDIRECT_ADMIN_PROVIDER");
        adminProvider = env == null?"admin.mcpdirect.ai":env;
        authenticationServiceAddress="authentication@"+ adminProvider;
        String mid = null;
        try {
            String os = System.getProperty("os.name");
            try {
                machineName = os+","+ InetAddress.getLocalHost().getHostName();
            } catch (Exception ignore) {
            }
            if(machineName==null){
                machineName = os+","+System.getProperty("user.name");
            }
            os = os.toLowerCase();
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
                        }
                    }
                }
                try {
                    process = Runtime.getRuntime().exec("wmic computersystem get name");
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    boolean firstLine = true;
                    String name = null;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty() && !firstLine) {
                            name = line.trim();
                        }
                        firstLine = false;
                    }

                    process = Runtime.getRuntime().exec("wmic computersystem get systemfamily");
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    firstLine = true;
                    String family = null;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty() && !firstLine) {
                            family = line.trim();
                        }
                        firstLine = false;
                    }
                    if(name!=null){
                        machineName = os+","+name;
                    }
                    if(family!=null){
                        machineName = family+","+machineName;
                    }
                }catch (Exception ignore){}
            }
            // Linux系统可以读取/etc/machine-id或/var/lib/dbus/machine-id
            else if(os.contains("linux")){
                Process process = Runtime.getRuntime().exec("cat /etc/machine-id");
                process.waitFor();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                mid = reader.readLine();
                try {
                    Path path = Paths.get("/sys/class/dmi/id/product_name");
                    if (Files.exists(path)) {
                        String model = Files.readString(path).trim();
                        if (!model.isEmpty() && !model.equals("To be filled by O.E.M.")) {
                            machineName=model+","+machineName;
                        }
                    }
                } catch (Exception ignored) {}
            } else if(os.contains("mac os")||os.contains("macos")) {
                //macOS
                Process process = Runtime.getRuntime().exec(
                        new String[]{"/bin/sh", "-c", "system_profiler SPHardwareDataType"});

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                String model = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if(line.startsWith("Model Name")){
                        model = line.split(":")[1].trim();
                    }else if(line.startsWith("Hardware UUID")){
                        mid = line.split(":")[1].trim();
                    }
                }
                if(model!=null) machineName=model+","+machineName;

            }
        } catch (Exception ignore) {}

        if(mid==null) try {
            String home = System.getProperty("user.home");
            BasicFileAttributes attrs = Files.readAttributes(
                    Path.of(home), BasicFileAttributes.class);
            
            FileTime creationTime = attrs.creationTime();
            mid = System.getProperty("user.name")+","+home+","+ creationTime.toMillis();
        } catch (IOException e) {
            mid = System.getProperty("user.name")+","+System.getProperty("user.home")+",0";
        }

        env = System.getenv("MCPDIRECT_STUDIO_TEST_ID");
        if(env!=null){
            mid+=env;
        }

        machineId = AIPortAccessKeyValidator.hashCode(mid);
        if(machineName==null){
            machineName = System.getProperty("os.name");
        }

    }

    private static void start(String keySeed) throws Exception {
        serviceEngine = new HstpServiceEngine(engineConfig,null,
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
        String userDevice = Long.toString(machineId);
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

    public static void login(String account, String password,Callback<AIPortUser> callback){
        int code = -1;
        String message;
        AIPortUser user = null;
        try {
            if (serviceEngine != null) {
                logout();
            }
            if (serviceEngine != null) {
                System.exit(0);
            }
            long milliseconds = System.currentTimeMillis();
            String hashedPassword = SHA256.digest(password);
//        String userDevice = ServiceEngineFactory.getEngineId();
            String userDevice = Long.toString(machineId);
            SimpleServiceResponseMessage<AccountDetails> httpResp = HstpHttpClient.hstpRequest(
                    hstpWebport, authenticationServiceAddress + "/login", userDevice,
                    Map.of("account", account,
                            "secretKey", SHA256.digest(hashedPassword + milliseconds),
                            "timestamp", milliseconds
//                        , "userDevice",serviceEngine.getEngineId().hashCode()
                    ), new TypeReference<>() {
                    });
            onLoginHttpResponse(httpResp, userDevice);
            code = httpResp.code;
            message = httpResp.message;
            if(accountDetails!=null){
                user = accountDetails.userInfo;
            }
        } catch (Exception e) {
            message = e.getMessage();
        }
        callback.onResult(code,message,user);
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
        SimpleServiceResponseMessage<AIPortAnonymousCredential> httpResp = HstpHttpClient.hstpRequest(
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
            eventListeners.clear();
            accessKeyCredentials.clear();
            mcpServerConfigs.clear();
        }else{
            throw new ServiceException("Service request failed. Error="+service.getErrorCode());
        }
    }
    public static void logout(Callback<Boolean> callback){
        int code = -1;
        String message;
        boolean data = false;
        if(serviceEngine==null){
            return;
        }
        try {
            Service service = accountServiceUSL.appendPath("logout")
                    .createServiceClient()
                    .headers(authHeaders)
                    .content("{}")
                    .request(serviceEngine);
            code = service.getErrorCode();
            message = service.getErrorMessage();
            if (code == 0) {
                serviceEngine.stop();
                serviceEngine = null;
                accountDetails = null;
                authHeaders = null;
                toolAgentDetails = null;
                mcpServerConfigs.clear();
                data = true;
            }
        }catch (Exception e){
            message = e.getMessage();
        }
        callback.onResult(code,message,data);
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
//    public static List<MCPServer> addMCPServer(String json) throws Exception {
//        List<MCPServer> mcpServers = AIToolServiceHandler.addMCPServer(json);
//        for (MCPServer mcpServer : mcpServers) {
//            String name = mcpServer.name;
//            if(name==null||(name=name.trim()).isEmpty()||name.length()>32){
//                throw new Exception("The name must not be empty and the max length is 32");
//            }
//        }
//        for (MCPServer mcpServer : mcpServers) {
//            mcpServerConfigs.put(mcpServer.name,new MCPServerConfig(mcpServer.url,mcpServer.command,mcpServer.args,mcpServer.env));
//        }
//        notifyMCPServer(mcpServers);
//        writeMCPServerConfigs();
//        return mcpServers;
//    }
//    public static MCPServer addMCPServer(String serverName, int serverType,String url,String command,
//                                    List<String> args,Map<String,String> env) throws Exception {
    public static MCPServer connectMCPServer(String serverName, MCPServerConfig conf) throws Exception {
        if(serverName==null||(serverName=serverName.trim()).isEmpty()||serverName.length()>32){
            throw new Exception("The name must not be empty and the max length is 32");
        }
        MCPServer mcpServer = AIToolServiceHandler.connectMCPServer(localMCPServerId(serverName),serverName,conf);
        mcpServer.id = localMCPServerId(serverName);
        mcpServer.name = serverName;
        mcpServerConfigs.put(serverName,conf);
//        notifyMCPServer(List.of(mcpServer));
        notificationHandler.onMCPServerNotification(mcpServer);
        writeMCPServerConfigs();
        return mcpServer;
    }
    public static Collection<? extends MCPServer> getMCPServers(){
        return AIToolServiceHandler.getMCPServers();
    }
    public static MCPServer getMCPServer(long serverId){
        return AIToolServiceHandler.getMCPServer(serverId);
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
    public static long localMCPServerId(String name){
        return name.hashCode()|Long.MIN_VALUE;
    }
//    public static void getLocalMCPServers(){
//        String userHome = System.getProperty("user.home");
//        File file = new File(userHome, ".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36));
//        if(!file.exists()){
//            file.mkdirs();
//        }
//        if(file.exists()) try{
//            mcpServerConfigFile = new File(file,"mcpservers");
//            if(file.exists()) try(FileInputStream in = new FileInputStream(mcpServerConfigFile)) {
////                List<MCPServer> mcpServers = new ArrayList<>();
//                Map<String, MCPServerConfig> map = JSON.fromJson(in.readAllBytes(), new TypeReference<>() {});
//                map.forEach((n,c)->{
//                    if(!mcpServerConfigs.containsKey(n)) try {
//                        MCPServer mcpServer
//                                = AIToolServiceHandler.connectMCPServer(localMCPServerId(n),n ,c);
////                        if (mcpServer.id < 0) mcpServers.add(mcpServer);
//                        if (mcpServer.id < 0) notificationHandler.onMCPServerNotification(mcpServer);
//                        mcpServerConfigs.put(n,c);
//                    } catch (Exception ignore) {}
//                });
//                writeMCPServerConfigs();
////                notifyLocalMCPServer(mcpServers);
//            }
//        }catch (Exception ignore){}
//    }
    public static void removeLocalMCPServer(MCPServer server){
        mcpServerConfigs.remove(server.name);
        writeMCPServerConfigs();
        AIToolServiceHandler.removeMCPServer(server.id);
//        List<MCPServer> mcpServers = new ArrayList<>();
        mcpServerConfigs.forEach((n,c)->{
            try {
                MCPServer mcpServer
                        = AIToolServiceHandler.connectMCPServer(localMCPServerId(n),n,c);
//                if (mcpServer.id == 0) mcpServers.add(mcpServer);
                if (mcpServer.id < 0) notificationHandler.onMCPServerNotification(mcpServer);
            } catch (Exception ignore) {}
        });
//        notifyLocalMCPServer(mcpServers);
    }

    public static void initToolAgent(){new Thread(()->{
        Map<String, MCPServerConfig> localMCPServerConfigs = loadLocalMCPServerConfigs();
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
                            for (AIPortMCPServerConfig c : toolAgentDetails.mcpServerConfigs) {
                                AIPortToolMaker maker = collect.get(c.id);
                                if(maker!=null) {
                                    MCPServerConfig mcpServerConfig = new MCPServerConfig(c);
                                    mcpServerConfigs.put(maker.name, mcpServerConfig);
                                    notificationHandler.onMCPServerNotification(new MCPServer(mcpServerConfig) {{
                                        id = c.id;
                                        name = maker.name;
                                        status = Integer.MIN_VALUE;
                                    }});
                                    new Thread(() -> {
                                        try {
                                            MCPServer mcpServer = AIToolServiceHandler.connectMCPServer(
                                                    maker.id, maker.name, mcpServerConfig);
                                            mcpServer.merge(maker, toolAgentDetails.tools);
                                            mcpServer.id = c.id;
                                            mcpServer.tags = maker.tags;
                                            notificationHandler.onMCPServerNotification(mcpServer);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).start();
                                }
                            }
                        }
                    }
                }
                break;
            }
            queryAccessKeys();
        }catch (Exception e){
            e.printStackTrace();
        }
        connectLocalMCPServers(localMCPServerConfigs);
        writeMCPServerConfigs();
    }).start();}

    private static Map<String, MCPServerConfig> loadLocalMCPServerConfigs(){
        Map<String, MCPServerConfig> map = new HashMap<>();
        String userHome = System.getProperty("user.home");
        File file = new File(userHome, ".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36));
        if(!file.exists()){
            file.mkdirs();
        }
        if(file.exists()) try{
            mcpServerConfigFile = new File(file,"mcpservers");
            if(file.exists()) try(FileInputStream in = new FileInputStream(mcpServerConfigFile)) {
                map.putAll(JSON.fromJson(in.readAllBytes(), new TypeReference<>() {}));
            }
        }catch (Exception ignore){}
        return map;
    }
    private static void connectLocalMCPServers(Map<String, MCPServerConfig> configs){
        configs.forEach((n,c)->{
            if(!mcpServerConfigs.containsKey(n)) {
                mcpServerConfigs.put(n,c);
                long mcpServerId = localMCPServerId(n);
                notificationHandler.onMCPServerNotification(new MCPServer(c){{
                    id = mcpServerId;
                    name = n;
                    status = Integer.MIN_VALUE;
                }});
                new Thread(()-> {try {
                    MCPServer mcpServer
                            = AIToolServiceHandler.connectMCPServer(mcpServerId, n, c);
                    notificationHandler.onMCPServerNotification(mcpServer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }}).start();
            }
        });
    }

    public static List<AIPortTool> getAIPortTools(MCPServer mcpServer){
        List<AIPortTool> tools = new ArrayList<>();
        for (MCPTool tool : mcpServer.getTools()) {
            tools.add(tool.duplicate());
        }
        return tools;
//        Map<String, AIPortTool> collect;
//        if(mcpServer.id>0&&toolAgentDetails.tools!=null) {
//            collect = toolAgentDetails.tools.stream()
//                    .filter(t -> t.makerId == mcpServer.id)
//                    .collect(Collectors.toMap(t->t.name,t-> {
//                        t = t.duplicate();
//                        t.lastUpdated = -1;
//                        return t;
//                    }));
//        }else{
//            collect = new HashMap<>();
//        }
//        for (AITool tool : mcpServer.getTools()) try {
//            String metaData = JSON.toJson(new ServiceDescription("aitools",
//                    "call/" + Long.toString(mcpServer.id,Character.MAX_RADIX) + "/" + tool.name(),
//                    tool.description(), tool.inputSchema(), "{}"));
//            int hash = metaData.hashCode();
//            AIPortTool aiPortTool = collect.get(tool.name());
//            if(aiPortTool==null) {
//                aiPortTool = new AIPortTool(
//                        0, mcpServer.id, 1, 1, tool.name(), hash, null, ""
//                );
////                toolAgentDetails.tools.add(aiPortTool);
//                collect.put(aiPortTool.name,aiPortTool);
//            }else if(aiPortTool.hash==hash){
//                aiPortTool.lastUpdated = 0;
////                aiPortTool.metaData = tool.description();
//            }else{
//                aiPortTool.hash = hash;
//                aiPortTool.lastUpdated = System.currentTimeMillis();
////                aiPortTool.metaData = tool.description();
//            }
//
//        }catch (Exception ignore){}
//        return collect.values().stream().toList();
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

//    private static String getToolMetadata(MCPServer mcpServer) throws Exception {
//        List<ServiceDescription> list = new ArrayList<>();
//        for (AITool tool : mcpServer.getTools()) {
//            list.add(new ServiceDescription("aitools","call/"+mcpServer.name+"/"+tool.name(),
//                    tool.description(),tool.inputSchema(),"{}"));
//        }
//        return JSON.toJson(list);
//    }

//    private static List<AIPortTool> createPublishingTools(MCPServer mcpServer){
//        List<AIPortTool> tools = new ArrayList<>();
//        for (MCPTool tool : mcpServer.getTools()) if(tool.lastUpdated!=0){
//            AIPortTool duplicate = tool.duplicate();
//            duplicate.metaData = tool.metaData();
//            duplicate.hash = duplicate.metaData.hashCode();
//            tools.add(duplicate);
//        }
//        return tools;
//        Map<String, AIPortTool> collect;
//        if(mcpServer.id>0&&toolAgentDetails.tools!=null){
//            collect = toolAgentDetails.tools.stream()
//                    .filter(t->t.makerId==mcpServer.id)
//                    .collect(Collectors.toMap(t -> t.name, t -> {
//                        t = t.duplicate();
//                        t.status = -1;
//                        return t;
//                    }));
//        }else{
//            collect = new HashMap<>();
//        }
//        for (AITool tool : mcpServer.getTools()) try {
//            String metaData = JSON.toJson(new ServiceDescription("aitools",
//                    "call/" + Long.toString(mcpServer.id,Character.MAX_RADIX)+ "/" + tool.name(),
//                    tool.description(), tool.inputSchema(), "{}"));
//            int hash = metaData.hashCode();
//            AIPortTool aiPortTool = collect.get(tool.name());
//            if(aiPortTool==null) {
//                aiPortTool = new AIPortTool(
//                        0, mcpServer.id, 1, 0, tool.name(), hash, metaData, ""
//                );
//                collect.put(aiPortTool.name,aiPortTool);
//            }else if(aiPortTool.hash==hash){
//                collect.remove(tool.name());
//            }else{
//                aiPortTool.hash = hash;
//                aiPortTool.metaData = metaData;
//                aiPortTool.lastUpdated = System.currentTimeMillis();
//            }
//
//        }catch (Exception ignore){}
//        return collect.values().stream().toList();
//    }

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
    public static void publishTools(MCPServer mcpServer,Callback<MCPServer> callback) throws Exception {
        long oldMCPServerId = mcpServer.id;
        String name = mcpServer.name;
        if(name==null||(name=name.trim()).isEmpty()||name.length()>32){
            throw new Exception("The name must not be empty and the max length is 32");
        }
        AtomicInteger code = new AtomicInteger();
        AtomicReference<String> message = new AtomicReference<>();
        if(mcpServer.id<0) createToolMaker(name,"",mcpServer,
                (c,m,d)->{
            code.set(c);
            message.set(m);
            if(c==0){
                mcpServer.merge(d,null);
                AIToolServiceHandler.remapMCPServer(oldMCPServerId);
            }
        });

        if(code.get()==0){
            List<AIPortTool> publicTools = new ArrayList<>();
            List<AIPortTool> tools = new ArrayList<>();
            for (MCPTool tool : mcpServer.getTools()) if(tool.lastUpdated!=0){
                AIPortTool duplicate = tool.duplicate();
                duplicate.metaData = tool.metaData();
                duplicate.hash = duplicate.metaData.hashCode();
                publicTools.add(duplicate);
                tools.add(tool);
            }
            if(publicTools.isEmpty()){
                callback.onResult(0,"no tools updated",mcpServer);
                return;
            }
            RequestOfPublishTools req = new RequestOfPublishTools();
            req.maker.id = mcpServer.id;
            req.maker.name = name;
            req.maker.type = TYPE_MCP;
            req.maker.agentId = toolAgentDetails.toolAgent.id;
//        req.maker.tools = getToolsString(mcpServer);
            req.maker.tags="";
            req.mcpServerConfig.transport = mcpServer.transport;
            req.mcpServerConfig.url = mcpServer.url;
            req.mcpServerConfig.command = mcpServer.command;
            req.mcpServerConfig.args = mcpServer.args!=null?JSON.toJson(mcpServer.args):"[]";
            req.mcpServerConfig.env = mcpServer.args!=null?JSON.toJson(mcpServer.env):"{}";
            req.tools = publicTools;
            Service service = aitoolsManagementUSL
                    .appendPath("tool_agent/tools/publish")
                    .createServiceClient()
                    .headers(authHeaders)
                    .content(JSON.toJson(req))
                    .request(serviceEngine);
            code.set(service.getErrorCode());
            message.set(service.getErrorMessage());
            if(code.get() ==0) {
                SimpleServiceResponseMessage<Long> resp
                        = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
                code.set(resp.code);
                message.set(resp.message);
                if(resp.code==0){
                    mcpServer.id = resp.data;
                    for (AIPortTool tool : tools) {
                        tool.makerId = mcpServer.id;
                        tool.lastUpdated = 0;
                    }
                }
            }
        }
        callback.onResult(code.get(),message.get(),mcpServer);
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
        String host = System.getenv("AI_MCPDIRECT_GATEWAY_HOST");
        if(host==null){
            host = "https://connect.mcpdirect.ai/";
        }
        return "{\"mcpServers\":{\""+credential.name
                +"\":{\"url\":\"https://connect.mcpdirect.ai/sse\",\"env\":{\"X-MCPdirect-Key\":"
                + credential.secretKey+"}}}}";
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
    public static void grantToolPermission(List<AIPortToolPermission> permissions,List<AIPortVirtualToolPermission> virtualPermissions) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool/permission/grant")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of(
                        "permissions",permissions,
                        "virtualPermissions",virtualPermissions
                        )))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortToolPermission>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                notifyAccessKeyPermissions(resp.data);
            }
        }
    }

    private static ToolMakerNotificationHandler toolMakerHandler;
    public static void setToolMakerNotificationHandler(ToolMakerNotificationHandler handler){
        toolMakerHandler = handler;
    }
    private static void notifyToolMakers(int code,String message,String name,Integer type,List<AIPortToolMaker> makers){
        if(toolMakerHandler!=null){
            toolMakerHandler.onToolMakersNotification(code,message,name,type,makers);
        }
    }
    private static void notifyToolMaker(int code,String message, AIPortToolMaker maker){
        if(toolMakerHandler!=null){
            toolMakerHandler.onToolMakerNotification(code,message,maker);
        }
    }
    public interface Callback<T>{
        void onResult(int code,String message,T data);
    }
    public interface Convertor<T>{
        SimpleServiceResponseMessage<T> convert(byte[] data) throws Exception;
    }

    public static <T> void hstpRequest( USL usl,String path,Map<String, Object> parameters,
                                        Callback<T> callback,Convertor<T> convertor){
        int code;
        String message;
        T data = null;
        try {
//            Service service = hstpRequest(usl, path, parameters);
            Service service = usl
                    .appendPath(path)
                    .createServiceClient()
                    .headers(authHeaders)
                    .content(JSON.toJson(parameters))
                    .request(serviceEngine);
            code = service.getErrorCode();
            message = service.getErrorMessage();
            if (code == Service.SERVICE_SUCCESSFUL) {
                SimpleServiceResponseMessage<T> resp = convertor.convert(service.getResponseMessage());
                code = resp.code;
                message = resp.message;
                data = resp.data;
            }
        }catch (Exception e){
            code = -1;
            message = e.getMessage();
        }
        if(code!=0) System.err.println("HSTP request error: code="+code+", message="+message);
        callback.onResult(code,message,data);
    }
    public interface HstpResponseHandler{
        void onResponse(String resp);
    }
    public static void hstpRequest( String usl,String parameters,
                                        HstpResponseHandler handler){
        int code=-1;
        String message;
        try {
            Service service = USL.createServiceClient(usl)
                    .headers(authHeaders)
                    .content(parameters)
                    .request(serviceEngine);
            code = service.getErrorCode();
            message = service.getErrorMessage();
            if (code == Service.SERVICE_SUCCESSFUL) {
                handler.onResponse(service.getResponseMessageString());
            }else{
                handler.onResponse(JSON.toJson(code,message));
            }
        }catch (Exception e){
            handler.onResponse(JSON.toJson(code,e.getMessage()));
        }

    }
    public static void httpRequest(String usl,String parameters,
                                   HstpResponseHandler handler){
        try {
            handler.onResponse(HstpHttpClient.doPost(hstpWebport, Map.of(
                    "hstp-usl", usl,
                    "hstp-auth", accountDetails!=null?accountDetails.accessToken:""
            ), null, parameters));
        } catch (Exception e) {
            handler.onResponse(JSON.toJson(-1,e.getMessage()));
        }
    }
    private static class RequestOfToolMaker{
        public Long id;
        public Long toolAgentId;
        public Integer type;
        public String name;
        public String tags;
        public Integer status;

        public RequestOfToolMaker(Long id,Long toolAgentId, Integer type, String name, String tags, Integer status) {
            this.id = id;
            this.toolAgentId = toolAgentId;
            this.type = type;
            this.name = name;
            this.tags = tags;
            this.status = status;
        }
    }
    public static void queryToolMakers(Integer type,String name, Long toolAgentId,Long teamId,Callback<List<AIPortToolMaker>> callback) throws Exception{
        Map<String,Object> parameters = new HashMap<>(){{
            put("type",type);
            put("name", name);
            put("toolAgentId", toolAgentId);
            put("teamId", teamId);
        }};
        hstpRequest(aitoolsManagementUSL,"tool_maker/query",parameters,callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/query")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(
//                        new RequestOfToolMaker(null,toolAgentId,type,name,null,null)
//                ))
//                .request(serviceEngine);
//        if(service.getErrorCode()==0) {
//            SimpleServiceResponseMessage<List<AIPortToolMaker>> resp
//                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
//            if(resp.code==0){
//                callback.onResult(resp.code,resp.message,resp.data);
//            }
//        }else{
//            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
//        }
    }
    public static void createVirtualToolMaker(String name, String tags,
                                              Callback<AIPortToolMaker> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool_maker/create")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of(
                        "name",name,
                        "type", TYPE_VIRTUAL,
                        "tags",tags
                )))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<AIPortToolMaker> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                callback.onResult(resp.code,resp.message,resp.data);
            }
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }
    public static void createToolMaker(String name, String tags,MCPServer server,
                                       Callback<AIPortToolMaker> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool_maker/create")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of(
                        "name",name,
                        "type", TYPE_MCP,
                        "tags",tags,
                        "mcpServerConfig", new AIPortMCPServerConfig(){{
                            transport = server.transport;
                            url = server.url;
                            command = server.command;
                            args = JSON.toJson(server.args);
                            env = JSON.toJson(server.env);
                        }}
                )))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<AIPortToolMaker> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0){
                callback.onResult(resp.code,resp.message,resp.data);
            }
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

//    private static void modifyToolMaker(String field,long id,String name, String tags,Integer status, Callback<AIPortToolMaker> callback) throws Exception{
//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/"+field+"/modify")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(
//                        new RequestOfToolMaker(id,null,null,name,tags,status)
//                ))
//                .request(serviceEngine);
//        if(service.getErrorCode()==0) {
//            SimpleServiceResponseMessage<AIPortToolMaker> resp
//                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
//            if(resp.code==0){
//                callback.onResult(resp.code,resp.message,resp.data);
//            }
//        }else{
//            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
//        }
//    }
//
//    public static void modifyToolMakerName(long id,String name,Callback<AIPortToolMaker> callback) throws Exception{
//        modifyToolMaker("name",id,name,null,null,callback);
//    }
//    public static void modifyToolMakerTags(long id,String tags,Callback<AIPortToolMaker> callback) throws Exception{
//        modifyToolMaker("tags",id,null,tags,null,callback);
//    }
//    public static void modifyToolMakerStatus(long id,Integer status, Callback<AIPortToolMaker> callback) throws Exception{
//        modifyToolMaker("status",id,null,null,status,callback);
//    }

    public static class RequestOfQueryTools{
        public Long userId;
        public String name;
        public Long agentId;
        public Long makerId;
        public Integer status;

        public RequestOfQueryTools(Long userId,String name, Long agentId, Long makerId, Integer status) {
            this.userId = userId;
            this.name = name;
            this.agentId = agentId;
            this.makerId = makerId;
            this.status = status;
        }
    }
    public static void queryTools(Long userId,Integer status,Long agentId,Long makerId,String name,Callback<List<AIPortTool>> callback) throws Exception{

        Service service = aitoolsManagementUSL
                .appendPath("tool/query")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(
                        new RequestOfQueryTools(userId,name,agentId,makerId,status)
                ))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortTool>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

    public static void getTool(long toolId,Callback<AIPortTool> callback) throws Exception{

        Service service = aitoolsManagementUSL
                .appendPath("tool/get")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(
                        Map.of("toolId",toolId)
                ))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<AIPortTool> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

    private static class RequestOfModifyVirtualTools{
        public long makerId;
        public List<AIPortVirtualTool> tools;

        public RequestOfModifyVirtualTools(long makerId, List<AIPortVirtualTool> tools) {
            this.makerId = makerId;
            this.tools = tools;
        }
    }
    public static void modifyVirtualTools(long makerId,List<AIPortVirtualTool> tools, Callback<List<AIPortVirtualTool>> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool/virtual/modify")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(
                        new RequestOfModifyVirtualTools(makerId,tools)
                ))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortVirtualTool>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

    public static void queryVirtualTools(long makerId, Callback<List<AIPortVirtualTool>> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool/virtual/query")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(
                        Map.of("makerId",makerId)
                ))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortVirtualTool>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }
    public static void queryToolPermissions(long accessKeyId, Callback<List<AIPortToolPermission>> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool/permission/query")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(
                        Map.of("accessKeyId",accessKeyId)
                ))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortToolPermission>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }
    public static void queryVirtualToolPermissions(long accessKeyId, Callback<List<AIPortVirtualToolPermission>> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool/virtual/permission/query")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(
                        Map.of("accessKeyId",accessKeyId)
                ))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortVirtualToolPermission>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

    public static void queryToolAgents(Callback<List<AIPortToolAgent>> callback) throws Exception {
        Service service = aitoolsManagementUSL.appendPath("tool_agent/query")
                .createServiceClient()
                .headers(authHeaders)
                .content("{}")
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            SimpleServiceResponseMessage<List<AIPortToolAgent>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

    public static void generateAccessKey(String name,Callback<AIPortAccessKeyCredential> callback) throws Exception {
        Service service = hstpRequest(accountServiceUSL,"access_key/create",Map.of("name", name));
        AIPortAccessKeyCredential key = null;
        int code = service.getErrorCode();
        String message = service.getErrorMessage();
        if(code==Service.SERVICE_SUCCESSFUL){
            SimpleServiceResponseMessage<AIPortAccessKeyCredential> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            code = resp.code;
            message = resp.message;
            key = resp.data;
            if(code==Service.SERVICE_SUCCESSFUL&&key!=null){
                saveAccessKey(key);
            }
        }
        callback.onResult(code,message,key);
    }

    public static void queryAccessKeys(Callback<List<AIPortAccessKeyCredential>> callback) throws Exception {
        Service service = hstpRequest(accountServiceUSL,"access_key/query",Map.of());
        List<AIPortAccessKeyCredential> keys = null;
        int code = service.getErrorCode();
        String message = service.getErrorMessage();
        if(code==Service.SERVICE_SUCCESSFUL){
            SimpleServiceResponseMessage<List<AIPortAccessKeyCredential>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            code = resp.code;
            message = resp.message;
            keys = resp.data;
            if(code==Service.SERVICE_SUCCESSFUL&&keys!=null){
                accessKeyCredentials.clear();
                keys.forEach(key->{
                    accessKeyCredentials.put(Long.toString(key.id),key);
                });
            }
        }
        callback.onResult(code,message,keys);
    }
    public static void queryToolPermissionMakerSummaries(Callback<List<AIPortToolPermissionMakerSummary>> callback) throws Exception {
        Service service = hstpRequest(aitoolsManagementUSL,"tool/permission/maker/summary/query",Map.of());
        List<AIPortToolPermissionMakerSummary> keys = null;
        int code = service.getErrorCode();
        String message = service.getErrorMessage();
        if(code==Service.SERVICE_SUCCESSFUL){
            SimpleServiceResponseMessage<List<AIPortToolPermissionMakerSummary>> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            code = resp.code;
            message = resp.message;
            keys = resp.data;
        }
        callback.onResult(code,message,keys);
    }
    public static void modifyAccessKey(long id,String name,Integer status,
                                       Callback<AIPortAccessKeyCredential> callback
    ) throws Exception {
        if((name==null||(name=name.trim()).isEmpty())&& status==null){
            callback.onResult(-1,"name and status are empty",null);
            return;
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
        int code = service.getErrorCode();
        String message = service.getErrorMessage();
        if(code==Service.SERVICE_SUCCESSFUL){
            SimpleServiceResponseMessage<AIPortAccessKeyCredential> resp =
                    JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            code = resp.code;
            message = resp.message;
            key = resp.data;
        }
        callback.onResult(code,message,key);
    }
    public static void createTeam(String name, Callback<AIPortTeam> callback){
        if(name==null){
            callback.onResult(-1,"name is empty",null);
            return;
        }
        hstpRequest(accountServiceUSL,"team/create",Map.of("name",name), callback,
                (data)-> JSON.fromJson(data,
                        new TypeReference<SimpleServiceResponseMessage<AIPortTeam>>() {}));
    }
    public static void queryTeams(Callback<List<AIPortTeam>> callback){
        hstpRequest(accountServiceUSL,"team/query",Map.of(),callback,
                (data)-> JSON.fromJson(data,
                        new TypeReference<>() {}));
    }
    public static void modifyTeam(long teamId,String name,Integer status,Callback<AIPortTeam> callback){
        if(teamId<1&&(name==null||(name=name.trim()).isEmpty())&& status==null){
            callback.onResult(-1,"team id <1 or name and status are empty",null);
            return;
        }

        String finalName = name;
        Map<String,Object> parameters = new HashMap<>(){{
            put("teamId",teamId);
            put("name", finalName);
            put("status",status);
        }};
        hstpRequest(accountServiceUSL,"team/modify",parameters,callback,
                (data)-> JSON.fromJson(data,
                        new TypeReference<SimpleServiceResponseMessage<AIPortTeam>>() {}));
    }

    public static void inviteTeamMember(long teamId,String account, Callback<AIPortTeamMember> callback){
        if(teamId<1||account==null){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }
        hstpRequest(accountServiceUSL,"team/member/invite",
                Map.of("teamId",teamId, "account",account), callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }
    public static void queryTeamMembers(long teamId,Callback<List<AIPortTeamMember>> callback){
        if(teamId<1){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }
        hstpRequest(accountServiceUSL,"team/member/query",Map.of("teamId",teamId),callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }
    public static void modifyTeamMember(long teamId,long memberId,Integer status,Long expirationDate,Callback<AIPortTeamMember> callback){
        if((teamId<1||memberId<1)&&status==null&&expirationDate==null){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }

        Map<String,Object> parameters = new HashMap<>(){{
            put("teamId",teamId);
            put("memberId", memberId);
            put("status",status);
            put("expirationDate",expirationDate);
        }};
        hstpRequest(accountServiceUSL,"team/member/modify",parameters,callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }
    public static void acceptTeamMember(long teamId,long memberId,Callback<AIPortTeamMember> callback){
        if(teamId<1||memberId<1){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }
        Map<String,Object> parameters = new HashMap<>(){{
            put("teamId",teamId);
            put("memberId", memberId);
        }};
        hstpRequest(accountServiceUSL,"team/member/accept",parameters,callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }
    public static ToolAgentDetails getLocalToolAgentDetails(){
        return toolAgentDetails;
    }
    public static void modifyTeamToolMakers(AIPortTeam team, List<AIPortTeamToolMaker> teamToolMakers, Callback<List<AIPortTeamToolMaker>> callback){
        if(team==null||team.id<1||team.ownerId<1||teamToolMakers==null||teamToolMakers.isEmpty()){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }

        Map<String,Object> parameters = new HashMap<>(){{
            put("teamId",team.id);
            put("teamOwnerId",team.ownerId);
            put("teamToolMakers", teamToolMakers);
        }};
        hstpRequest(aitoolsManagementUSL,"tool_maker/team/modify",parameters,callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }
    public static void queryTeamToolMakers(AIPortTeam team, Callback<List<AIPortTeamToolMaker>> callback){
        if(team==null||team.id<1||team.ownerId<1){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }

        Map<String,Object> parameters = new HashMap<>(){{
            put("teamId",team.id);
            put("teamOwnerId",team.ownerId);
        }};
        hstpRequest(aitoolsManagementUSL,"tool_maker/team/query",parameters,callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }
    public static void modifyToolAgent(long toolAgentId,String name,String tags,Integer status, Callback<List<AIPortToolAgent>> callback){
        if(toolAgentId<1){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }

        Map<String,Object> parameters = new HashMap<>(){{
            put("agentId",toolAgentId);
            put("name",name);
            put("tags",tags);
            put("status",status);
        }};
        hstpRequest(aitoolsManagementUSL,"tool_agent/modify",parameters,callback,
                (data)-> JSON.fromJson(data, new TypeReference<>() {}));
    }

    public static void modifyToolMaker(long toolMakerId,String name,String tags,Integer status,
                                       Callback<AIPortToolMaker> callback){
        if(toolMakerId<1){
            callback.onResult(-1,"invalid parameters",null);
            return;
        }
        Map<String, Object> parameters = new HashMap<>() {{
            put("makerId", toolMakerId);
            put("name", name);
            put("tags", tags);
            put("status", status);
        }};
        hstpRequest(aitoolsManagementUSL, "tool_maker/modify", parameters, callback,
                (data) -> JSON.fromJson(data, new TypeReference<>() {
                }));
    }

    public static void configMCPServerConfig(long serverId, MCPServerConfig conf,
                                             Callback<MCPServer> callback){
        MCPServer server = AIToolServiceHandler.removeMCPServer(serverId);
        if(server!=null) try{
            server = AIToolServiceHandler.connectMCPServer(
                    serverId,server.name,conf
            );
            mcpServerConfigs.put(server.name,conf);
            writeMCPServerConfigs();
            callback.onResult(255,null,server);
//            if (toolMakerId > 0) {
//                Map<String, Object> parameters = new HashMap<>() {{
//                    put("makerId", toolMakerId);
//                    put("name", name);
//                    put("tags", tags);
//                    put("status", status);
//                }};
//                MCPServer finalServer = server;
//                hstpRequest(aitoolsManagementUSL, "tool_maker/modify", parameters,
//                        (code,message,data)-> {
//                            callback.onResult(code, message, finalServer);
//                        },
//                        (data) -> JSON.fromJson(data, new TypeReference<>() {}));
//            }
        } catch (Exception e) {
            callback.onResult(255,e.getMessage(),null);
        } else {
            callback.onResult(255,null,null);
        }
    }

    public static long accountId(){
        if(accountDetails!=null){
            return accountDetails.userInfo.id;
        }
        return 0;
    }

    public static long studioId(){
        if(serviceEngine!=null){
            return Long.parseLong(serviceEngine.getEngineId());
        }
        return 0;
    }
    public static String studioEngineId(){
        if(serviceEngine!=null){
            return serviceEngine.getEngineId();
        }
        return null;
    }
    public static long studioToolAgentId(){
        if(toolAgentDetails!=null){
            return toolAgentDetails.toolAgent.id;
        }
        return -1;
    }
    public interface EventListener{
        void onEvent(int event);
    }

    private static final HashSet<EventListener> eventListeners = new HashSet<>();
    public static void addEventListener(EventListener listener){
        if(listener!=null){
            eventListeners.add(listener);
        }
    }
    public static void removeMEventListener(EventListener listener){
        if(listener!=null){
            eventListeners.remove(listener);
        }
    }
    public static void fireEvent(int event){
        for (EventListener listener : eventListeners) {
            listener.onEvent(event);
        }
    }

    private static NotificationHandler notificationHandler = new NotificationHandler(){};
    public static void setNotificationHandler(NotificationHandler handler){
        notificationHandler = handler!=null?handler:new NotificationHandler() {};
    }
}
