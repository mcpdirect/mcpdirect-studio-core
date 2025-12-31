package ai.mcpdirect.studio;


import ai.mcpdirect.backend.dao.entity.account.*;
import ai.mcpdirect.backend.dao.entity.aitool.*;
import ai.mcpdirect.backend.util.AIPortAccessKeyValidator;
import ai.mcpdirect.studio.dao.entity.*;
import ai.mcpdirect.studio.handler.*;
import ai.mcpdirect.studio.service.AIPortServiceResponse;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import ai.mcpdirect.studio.tool.openapi.OpenAPITool;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import appnet.communicator.ssl.SSLContextGenerator;
import appnet.hstp.*;
import appnet.hstp.annotation.ServiceScan;
import appnet.hstp.engine.HstpServiceEngine;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.exception.ServiceException;
import appnet.hstp.exception.ServiceNotFoundException;
import ai.mcpdirect.studio.service.AIToolServiceHandler;
import appnet.hstp.labs.util.http.HstpHttpClient;
import appnet.hstp.labs.util.http.HttpClient;
import appnet.util.crypto.SHA256;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker.*;
import static ai.mcpdirect.studio.service.AIPortServiceResponse.TOOL_MAKER_EXISTS;
import static ai.mcpdirect.studio.service.AIPortServiceResponse.TOOL_MAKER_NOT_EXISTS;

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
    private static final Map<String,AIPortAccessKeyCredential> accessKeyCredentials = new ConcurrentHashMap<>();

    private static MCPDirectStudioDBHelper dbHelper;
    private static ScheduledExecutorService keepaliveService;

    static{
        Properties props = new Properties();
        try(InputStream resourceAsStream = MCPDirectStudio.class.getResourceAsStream("/mcpdirect-studio.properties")){
            props.load(resourceAsStream);
        }catch (Exception ignore){}

        String webportURL = props.getProperty("ai.mcpdirect.hstp.webport");
        if(webportURL==null||webportURL.trim().isEmpty()){
            webportURL = System.getProperty("ai.mcpdirect.hstp.webport");
        }
        if(webportURL==null||webportURL.trim().isEmpty()){
            webportURL = System.getenv("AI_MCPDIRECT_HSTP_WEBPORT");
        }
        if(webportURL==null||(webportURL=webportURL.trim()).isEmpty()){
            throw new RuntimeException("Please set 'ai.mcpdirect.hstp.webport' properties in mcpdirect-studio.properties\n" +
                    "or set environment variable 'AI_MCPDIRECT_HSTP_WEBPORT'");
        }
        hstpWebport = webportURL;

        String serviceGateway = props.getProperty("ai.mcpdirect.hstp.service.gateway");
        if(serviceGateway==null||serviceGateway.isEmpty()){
            serviceGateway = System.getProperty("ai.mcpdirect.hstp.service.gateway");
        }

        if(serviceGateway==null||serviceGateway.isEmpty()){
            serviceGateway = System.getenv("AI_MCPDIRECT_HSTP_SERVICE_GATEWAY");
        }

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
        accountServiceUSL = new USL("account.management", adminProvider);
        aitoolsManagementUSL = new USL("aitools.management", adminProvider);
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
                            mid = parts[2];
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
    private static void start(String keySeed,Callback<AIPortUser> callback) throws Exception {
        dbHelper = new MCPDirectStudioDBHelper();
        serviceEngine = new HstpServiceEngine(engineConfig,null,
                "ai.mcpdirect.studio."+machineId+"."+keySeed);
        AtomicBoolean init = new AtomicBoolean(false);
        serviceEngine.addServiceRegisterListener(((serviceName, serviceDomain, engineId, registeredOrUnregistered)->{
            System.out.println(serviceName+"@"+serviceDomain+":"+engineId+","+registeredOrUnregistered);
            if(!init.get()) {
                init.set(true);
                try {
                    new Thread(() -> {
                        initToolAgent();
                        callback.onResult(0, null, accountDetails.userInfo);
                    }).start();
                } catch (Exception ignore) {
                }
            }
        }));
        LOG.info("ServiceEngine {} started", serviceEngine);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logout();
            } catch (Exception ignore) {
            }
        }));
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

    private static void onLoginHttpResponse(
            SimpleServiceResponseMessage<AccountDetails> httpResp,String userDevice,
            Callback<AIPortUser> callback
    ) throws Exception {
        if(httpResp.code== Service.SERVICE_SUCCESSFUL){
            accountDetails = httpResp.data;
            if(accountDetails.userInfo.name==null){
                accountDetails.userInfo.name = accountDetails.account;
            }

            authHeaders = new ServiceHeaders()
                    .addHeader("hstp-auth", accountDetails.accessToken)
                    .addHeader("mcpdirect-device",userDevice);
            start(accountDetails.accountKeySeed,callback);
//            initToolAgent();
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
        onLoginHttpResponse(httpResp,userDevice,(code,message,data)->{

        });
        return accountDetails !=null;
    }

    public static void login(String account, String password,Callback<AIPortUser> callback){
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
            onLoginHttpResponse(httpResp, userDevice,callback);
        } catch (Exception e) {
            callback.onResult(-1,e.getMessage(),null);
        }
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
        onLoginHttpResponse(httpResp,userDevice,(code,message,data)->{

        });
        return accountDetails !=null;
    }
    public static void logout() throws Exception {
        if(serviceEngine==null){
            return;
        }
        if(keepaliveService!=null) try{
            keepaliveService.shutdown();
        } catch (Exception ignore) {
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
//            mcpServerConfigs.clear();
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
//                mcpServerConfigs.clear();
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

    public static Collection<? extends MCPServer> getMCPServers(){
        return AIToolServiceHandler.getMCPServers();
    }
    public static MCPServer getMCPServer(long serverId){
        return AIToolServiceHandler.getMCPServer(serverId);
    }
    public static class ToolAgentDetails {
        public AIPortToolAgent toolAgent;
        public List<AIPortToolMaker> makers;
//        public List<AIPortMCPServerConfig> mcpServerConfigs;
        public List<AIPortTool> tools;
    }

    private static Service hstpRequest(USL baseUsl,String path,Map<String,Object> parameters) throws Exception {
        return baseUsl.appendPath(path).createServiceClient()
                .headers(authHeaders)
                .content(parameters)
                .request(serviceEngine);
    }

    public static void initToolAgent(){
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
                    notificationHandler.onToolAgentNotification(toolAgentDetails.toolAgent);
                    if (toolAgentDetails.makers != null) {
                        if(toolAgentDetails.tools==null) toolAgentDetails.tools = new ArrayList<>();
                        for (AIPortToolMaker toolMaker : toolAgentDetails.makers) {
                            ToolMakerDetails details = new ToolMakerDetails();
                            details.maker = toolMaker;
                            details.tools = toolAgentDetails.tools;
                            if(toolMaker.mcp()){
//                                MCPServerConfig config = null;
//                                if(toolMaker.templateId>0){
//                                    ToolMakerTemplate template = dbHelper.selectToolMakerTemplate(toolMaker.templateId);
//                                    ToolMakerTemplateConfig templateConfig = dbHelper.selectToolMakerTemplateConfig(toolMaker.id);
//                                    if(template!=null&&templateConfig!=null) {
//                                        config = JSON.fromJson(template.config, MCPServerConfig.class);
//                                        config.fillInputs(templateConfig.inputs);
//                                    }
//                                } else config = dbHelper.selectMCPServerConfig(toolMaker.id);
//                                if(config!=null){
//                                    MCPServer noti = new MCPServer(config) {{
//                                        id = toolMaker.id;
//                                        name = toolMaker.name;
//                                        status = STATUS_WAITING;
//                                        agentId = studioToolAgentId();
//                                        userId = accountId();
//                                        templateId = toolMaker.templateId;
//                                    }};
//                                    notificationHandler.onToolMakerNotification(noti);
//                                    config.status = toolMaker.status;
//                                    MCPServerConfig finalConfig = config;
//                                    new Thread(() -> {
//                                        try {
//                                            MCPServer mcpServer = AIToolServiceHandler.connectMCPServer(
//                                                    toolMaker.id, toolMaker.name, finalConfig);
//                                            mcpServer.merge(toolMaker, toolAgentDetails.tools);
////                                            mcpServer.id = config.id;
////                                            mcpServer.tags = maker.tags;
//                                            notificationHandler.onToolMakerNotification(mcpServer);
//                                        } catch (Exception e) {
//                                            noti.errorCode = 1;
//                                            noti.errorMessage = e.getMessage();
//                                            notificationHandler.onToolMakerNotification(noti);
//                                        }
//                                    }).start();
//                                }else{
//                                    notificationHandler.onToolMakerNotification(new MCPServer(toolMaker.id) {{
//                                        name = toolMaker.name;
//                                        status = toolMaker.status;
//                                        errorCode = ERROR;
//                                        userId = toolMaker.userId;
//                                        errorMessage = "config not found";
//                                        agentId = studioToolAgentId();
//                                        templateId = toolMaker.templateId;
//                                    }});
//                                }
                                new Thread(()->{
                                    connectMCPServer(details,(error,message,server)->{

                                    });
                                }).start();
                            }else if(toolMaker.openapi()){
//                                OpenAPIServerConfig config = dbHelper.selectOpenAPIServerConfig(toolMaker.id);
//                                if(config!=null){
//                                    OpenAPIServer server = AIToolServiceHandler.connectOpenAPIServer(
//                                            toolMaker.id,toolMaker.name,config);
//                                    server.merge(toolMaker, toolAgentDetails.tools);
//                                    notificationHandler.onToolMakerNotification(server);
//                                }else{
//                                    notificationHandler.onToolMakerNotification(new OpenAPIServer() {{
//                                        id = toolMaker.id;
//                                        name = toolMaker.name;
//                                        status = toolMaker.status;
//                                        errorCode = ERROR;
//                                        userId = toolMaker.userId;
//                                        errorMessage = "config not found";
//                                        agentId = studioToolAgentId();
//                                        templateId = toolMaker.templateId;
//                                    }});
//                                }
                                new Thread(()->{
                                    connectOpenAPIServer(details,(error,messge,server)->{

                                    });
                                }).start();
                            }
                        }
                    }
                    keepaliveService = Executors.newSingleThreadScheduledExecutor();
                    keepaliveService.scheduleWithFixedDelay(()->{
                        if(serviceEngine!=null){
                            Callback<Long> callback = new Callback<Long>() {
                                @Override
                                public void onResult(int code, String message, Long data) {
                                    if(code==0&&data!=null){
                                        toolAgentDetails.toolAgent.lastKeepalive = data;
                                        notificationHandler.onToolAgentNotification(toolAgentDetails.toolAgent);
                                    }
                                }
                            };
                            hstpRequest(aitoolsManagementUSL, "tool_agent/keepalive",
                                    Map.of(), callback,
                                    (data) -> JSON.fromJson(data, new TypeReference<>() {
                                    }));
                        }
                    },5,5, TimeUnit.MINUTES);
                }
                break;
            }
            queryAccessKeys();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<AIPortTool> getAIPortTools(MCPServer mcpServer){
        List<AIPortTool> tools = new ArrayList<>();
        for (MCPTool tool : mcpServer.getTools()) {
            tools.add(tool.duplicate());
        }
        return tools;
    }
    public static List<AIPortTool> getAIPortTools(OpenAPIServer server) {
        List<AIPortTool> tools = new ArrayList<>();
        for (OpenAPITool tool : server.getTools()) {
            tools.add(tool.duplicate());
        }
        return tools;
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

    public static class RequestOfPublishTools{
        public AIPortToolMaker maker = new AIPortToolMaker();
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
    public static void publishTools(MCPServer mcpServer,Callback<AIPortToolMaker> callback) throws Exception {
        long oldMCPServerId = mcpServer.id;
        String name = mcpServer.name;
        if(name==null||(name=name.trim()).isEmpty()||name.length()>32){
            throw new Exception("The name must not be empty and the max length is 32");
        }
        AtomicInteger code = new AtomicInteger(255);
        AtomicReference<String> message = new AtomicReference<>();
//        if(mcpServer.id<0) createToolMaker(name,"",mcpServer,
//                (c,m,d)->{
//            code.set(c);
//            message.set(m);
//            if(c==0){
//                mcpServer.merge(d,null);
//                MCPServerConfig config = dbHelper.selectMCPServerConfig(oldMCPServerId);
//                if(config!=null) {
//                    config.id = mcpServer.id;
//                    dbHelper.setMCPServerConfig(oldMCPServerId,config);
//                }
//                AIToolServiceHandler.remapMCPServer(oldMCPServerId);
//                MCPDirectStudio.notificationHandler()
//                        .onToolMakerNotification(MCPServer.deprecated(oldMCPServerId));
//            }
//        }); else code.set(0);

        if(code.get()==0){
            List<AIPortTool> publishingTools = new ArrayList<>();
            List<AIPortTool> tools = new ArrayList<>();
            for (MCPTool tool : mcpServer.getTools()) if(tool.lastUpdated!=0){
                AIPortTool duplicate = tool.duplicate();
                duplicate.metaData = tool.metaData();
                duplicate.hash = duplicate.metaData.hashCode();
                publishingTools.add(duplicate);
                tools.add(tool);
            }
            if(publishingTools.isEmpty()){
                callback.onResult(0,"no tools updated",mcpServer);
                return;
            }
            RequestOfPublishTools req = new RequestOfPublishTools();
            req.maker.id = mcpServer.id;
            req.maker.name = name;
            req.maker.type = TYPE_MCP;
            req.maker.agentId = toolAgentDetails.toolAgent.id;
            req.maker.tags="";
            req.tools = publishingTools;
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
                    MCPDirectStudio.notificationHandler().onToolMakerNotification(mcpServer);
                }
            }
        }
        callback.onResult(code.get(),message.get(),mcpServer);
    }

    public static void publishTools(OpenAPIServer server,Callback<OpenAPIServer> callback) throws Exception {
        long oldServerId = server.id;
        String name = server.name;
        if(name==null||(name=name.trim()).isEmpty()||name.length()>32){
            throw new Exception("The name must not be empty and the max length is 32");
        }
        AtomicInteger code = new AtomicInteger(255);
        AtomicReference<String> message = new AtomicReference<>();
//        if(server.id<0) createToolMaker(TYPE_OPENAPI,name,"",
//                (c,m,d)->{
//                    code.set(c);
//                    message.set(m);
//                    if(c==0){
//                        server.merge(d,null);
//                        OpenAPIServerConfig config = dbHelper.selectOpenAPIServerConfig(oldServerId);
//                        if(config!=null) {
//                            config.id = server.id;
//                            dbHelper.setOpenAPIServerConfig(oldServerId,config);
//                        }
//                        AIToolServiceHandler.remapOpenAPIServer(oldServerId);
//                        MCPDirectStudio.notificationHandler().onToolMakerNotification(
//                                OpenAPIServer.deprecated(oldServerId)
//                        );
//                    }
//                }); else code.set(0);
//
//        if(code.get()==0){
            List<AIPortTool> publishingTools = new ArrayList<>();
            List<AIPortTool> tools = new ArrayList<>();
            for (OpenAPITool tool : server.getTools()) if(tool.lastUpdated!=0){
                AIPortTool duplicate = tool.duplicate();
                if(duplicate.status>-1) {
                    duplicate.metaData = tool.metaData();
                    duplicate.hash = duplicate.metaData.hashCode();
                }
                publishingTools.add(duplicate);
                tools.add(tool);
            }
            if(publishingTools.isEmpty()){
                callback.onResult(0,"no tools updated",server);
                return;
            }
            Service service = aitoolsManagementUSL
                    .appendPath("tool/publish")
                    .createServiceClient()
                    .headers(authHeaders)
                    .content(JSON.toJson(Map.of(
                            "toolAgentId",toolAgentDetails.toolAgent.id,
                            "toolMakerId",server.id,
                            "tools",publishingTools
                    )))
                    .request(serviceEngine);
            code.set(service.getErrorCode());
            message.set(service.getErrorMessage());
            if(code.get() ==0) {
                SimpleServiceResponseMessage<List<AIPortTool>> resp
                        = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
                code.set(resp.code);
                message.set(resp.message);
                if(resp.code==0){
                    server.merge(null,resp.data);
                    MCPDirectStudio.notificationHandler().onToolMakerNotification(server);
                }
            }
//        }
        callback.onResult(code.get(),message.get(),server);
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
        String host = System.getProperty("ai.mcpdirect.gateway.endpoint");
        if(host==null||host.trim().isEmpty()) {
            host = System.getenv("AI_MCPDIRECT_GATEWAY_ENDPOINT");
        }
        return "{\"mcpServers\":{\""+credential.name
                +"\":{\"url\":\""+host+"/sse\",\"env\":{\"X-MCPdirect-Key\":"
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
    public static void httpRequest(String uslStr,String parameters,
                                   HstpResponseHandler handler){
        try {
            USL usl = USL.create(uslStr);
            if(usl.getDomainName().equals(studioEngineId())){
                hstpRequest(uslStr,parameters,handler);
            }else handler.onResponse(HttpClient.doPost(hstpWebport, Map.of(
                    "hstp-usl", uslStr,
                    "hstp-auth", accountDetails!=null?accountDetails.accessToken:"",
                    "mcpdirect-device",Long.toString(machineId)
            ), parameters));
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
    }
//    public static void createVirtualToolMaker(String name, String tags,
//                                              Callback<AIPortToolMaker> callback) throws Exception{
//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/create")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(Map.of(
//                        "name",name,
//                        "type", TYPE_VIRTUAL,
//                        "tags",tags
//                )))
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
//    public static void createToolMaker(String name, String tags,MCPServer server,
//                                       Callback<AIPortToolMaker> callback) throws Exception{
//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/create")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(Map.of(
//                        "name",name,
//                        "type", TYPE_MCP,
//                        "tags",tags
//                )))
//                .request(serviceEngine);
//        if(service.getErrorCode()==0) {
//            SimpleServiceResponseMessage<AIPortToolMaker> resp
//                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
//            callback.onResult(resp.code,resp.message,resp.data);
//        }else{
//            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
//        }
//    }
//
//    public static void createToolMaker(int type,String name, String tags,
//                                       Callback<AIPortToolMaker> callback) throws Exception{
//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/create")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(Map.of(
//                        "name",name,
//                        "type", type,
//                        "tags",tags
//                )))
//                .request(serviceEngine);
//        if(service.getErrorCode()==0) {
//            SimpleServiceResponseMessage<AIPortToolMaker> resp
//                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
//            callback.onResult(resp.code,resp.message,resp.data);
//        }else{
//            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
//        }
//    }

    public static AIPortServiceResponse<AIPortToolMaker> createToolMaker(int type,String name,String tags) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool_maker/create")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of(
                        "name",name,
                        "type", type,
                        "tags",tags
                )))
                .request(serviceEngine);
        AIPortServiceResponse<AIPortToolMaker> resp;
        if(service.getErrorCode()==0) {
            resp = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
        }else{
            resp = new AIPortServiceResponse<>();
            resp.code = service.getErrorCode();
            resp.message = service.getErrorMessage();
        }
        return resp;
    }

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

    public static void modifyMCPServerConfig(long serverId,String serverName,
                                             Integer status,MCPServerConfig conf,
                                             Callback<AIPortToolMaker> callback){
        MCPServer server = AIToolServiceHandler.getMCPServer(serverId);
        try{
            if(server==null) {
                AIPortToolMaker first = toolAgentDetails.makers.stream()
                        .filter(toolMaker -> toolMaker.id == serverId)
                        .findFirst().get();
                if (first!=null&&conf!=null) {
                    if(first.templateId>0){
                        callback.onResult(255,"canot modify templated MCP server",null);
                        return;
                    }
                    conf.id = first.id;
                    conf.name = first.name;
                    conf.status = first.status;
                    dbHelper.insertMCPServerConfig(conf);
                    ToolMakerDetails details = new ToolMakerDetails();
                    details.maker = first;
                    details.tools = toolAgentDetails.tools;
                    connectMCPServer(details,callback);
                } else {
                    callback.onResult(255,null,null);
                }
            } if(server.templateId>0){
                callback.onResult(255,"canot modify templated MCP server",null);
                return;
            } else {
                Map<String,Object> map = new HashMap<>();
                if(serverName==null) serverName = server.name;
                else if(!server.name.equals(serverName)) {
//                    MCPServer exists = AIToolServiceHandler.getMCPServer(serverName);
                    if(AIToolServiceHandler.toolMakerExists(serverId,serverName)){
                        callback.onResult(TOOL_MAKER_EXISTS,serverName+" exists",null);
                        return;
                    }
                    map.put("name", serverName);
                }

                if(status!=null&&status!=server.status) map.put("status",status);
                if(!map.isEmpty()){
                    map.put("makerId",server.id);
                    Service service = aitoolsManagementUSL
                            .appendPath("tool_maker/modify")
                            .createServiceClient()
                            .headers(authHeaders)
                            .content(JSON.toJson(map))
                            .request(serviceEngine);
                    if(service.getErrorCode()==0) {
                        AIPortServiceResponse<AIPortToolMaker> resp
                                = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
                        if(resp.code!=Service.SERVICE_SUCCESSFUL||resp.data==null){
                            callback.onResult(resp.code, resp.message, null);
                            return;
                        }else if(serverName!=null) server.name = serverName;
                    }else{
                        callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
                        return;
                    }
                }
                MCPServer finalServer = server;
                if(conf!=null){
                    notificationHandler.onToolMakerNotification(new MCPServer(conf) {{
                        id = finalServer.id;
                        name = finalServer.name;
                        agentId = finalServer.agentId;
                        status = STATUS_WAITING;
                        userId = finalServer.userId;
                        templateId = finalServer.templateId;
                    }});
                    conf.id = serverId;
                    conf.status = server.status;
                    if(status!=null) {
                        conf.status = status;
                    }
                    dbHelper.insertMCPServerConfig(conf);
                    connectToolMaker(server.id,callback);
                }else{
                    if(status!=null&&server.status!=status) {
                        conf = dbHelper.selectMCPServerConfig(serverId);;
                        notificationHandler.onToolMakerNotification(new MCPServer(conf) {{
                            id = finalServer.id;
                            name = finalServer.name;
                            agentId = finalServer.agentId;
                            status = STATUS_WAITING;
                            userId = finalServer.userId;
                            templateId = finalServer.templateId;
                        }});
                        server.name = serverName;
                        conf.status = status;
                        server.status = status;
                        dbHelper.insertMCPServerConfig(conf);
                        if(status==1) AIToolServiceHandler.startMCPServer(serverId);
                        else if(status==0) AIToolServiceHandler.stopMCPServer(serverId);
                    }
                    callback.onResult(0,null,server);
                    notificationHandler.onToolMakerNotification(server);
                }
            }
        } catch (Exception e) {
            callback.onResult(255,e.getMessage(),null);
        }
    }
    public static void queryOpenAPIServerConfig(
            long serverId,
            Callback<OpenAPIServerConfig> callback){
        callback.onResult(0,"",dbHelper.selectOpenAPIServerConfig(serverId));
    }
    public static void modifyOpenAPIServerConfig(
            long serverId,String serverName,Integer serverStatus,
            OpenAPIServerConfig serverConfig,
            Callback<AIPortToolMaker> callback){
        if(serverId<Integer.MAX_VALUE&&serverName==null
                &&serverStatus==null&&serverConfig==null){
            callback.onResult(255,"Invalid config",null);
            return;
        }
        OpenAPIServer server = AIToolServiceHandler.getOpenAPIServer(serverId);
        if(server!=null) try{
            if(serverConfig==null){
                serverConfig = new OpenAPIServerConfig();
                serverConfig.status = server.status;
                serverConfig.name = server.name;
            }
            serverConfig.id = serverId;
            if(serverStatus!=null){
                serverConfig.status = serverStatus;
            }
            if(serverName!=null){
                serverConfig.name = serverName;
            }
            Map<String,Object> map = new HashMap<>();
            if(!server.name.equals(serverConfig.name)) {
//                MCPServer exists = AIToolServiceHandler.getOpenAPIServer(serverName);
                if(AIToolServiceHandler.toolMakerExists(serverId,serverConfig.name)){
                    callback.onResult(TOOL_MAKER_EXISTS,serverConfig.name+" exists",null);
                    return;
                }
                map.put("name", serverConfig.name);
            }

            if(serverConfig.status!=server.status) map.put("status",serverConfig.status);
            if(!map.isEmpty()){
                map.put("makerId",server.id);
                Service service = aitoolsManagementUSL
                        .appendPath("tool_maker/modify")
                        .createServiceClient()
                        .headers(authHeaders)
                        .content(JSON.toJson(map))
                        .request(serviceEngine);
                if(service.getErrorCode()==0) {
                    AIPortServiceResponse<AIPortToolMaker> resp
                            = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
                    if(resp.code!=Service.SERVICE_SUCCESSFUL||resp.data==null){
                        callback.onResult(resp.code, resp.message, null);
                        return;
                    }else server.name = serverConfig.name;
                }else{
                    callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
                    return;
                }
            }
            OpenAPIServerConfig config = dbHelper.selectOpenAPIServerConfig(serverId);
            config.id = serverId;
            config.name = serverConfig.name;
            config.status = serverConfig.status;
            boolean changed = false;
            if(serverConfig.doc!=null&&!serverConfig.doc.equals(config.doc)){
                changed = true;
                config.doc = serverConfig.doc;
            }
            if(serverConfig.url!=null&&!serverConfig.url.equals(config.url)){
                changed = true;
                config.url = serverConfig.url;
            }
            if(serverConfig.securities!=null){
                changed = true;
                config.securities = serverConfig.securities;
            }
            dbHelper.insertOpenAPIServerConfig(config);
            if(changed){
                notificationHandler.onToolMakerNotification(new OpenAPIServer() {{
                    id = server.id;
                    name = server.name;
                    agentId = server.agentId;
                    status = STATUS_WAITING;
                    userId = server.userId;
                    templateId = server.templateId;
                }});
                connectToolMaker(server.id,callback);
            }
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

    public static String studioId(){
        if(serviceEngine!=null){
            return serviceEngine.getEngineId();
        }
        return "";
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
    public static NotificationHandler notificationHandler(){
        return notificationHandler;
    }
    public static class ToolMakerDetails{
        public AIPortToolMaker maker;
//        public AIPortMCPServerConfig config;
        public List<AIPortTool> tools;
    }
    public static void connectToolMaker(long makerId,Callback<AIPortToolMaker> callback){
        hstpRequest(aitoolsManagementUSL, "tool_maker/details/get", Map.of(
                "makerId", makerId
        ), (int code, String message, ToolMakerDetails details) -> {
            AIPortToolMaker maker;
            if(code==0&&details!=null&&(maker=details.maker)!=null) try {
                if(maker.mcp()) connectMCPServer(details,callback);
                else if(maker.openapi()) connectOpenAPIServer(details,callback);
            } catch (Exception e) {
                callback.onResult(255,e.getMessage(),null);
            } else callback.onResult(code,message,null);
        }, (data) -> JSON.fromJson(data, new TypeReference<>() {}));
    }
    private static void connectMCPServer(
            ToolMakerDetails details,
            Callback<AIPortToolMaker> callback
    ){
        AIPortToolMaker maker = details.maker;
        long templateId = maker.templateId;
        MCPServerConfig mcpServerConfig=null;
        String errorMessage = null;
        if(templateId>Integer.MAX_VALUE){
            ToolMakerTemplate template = dbHelper.selectToolMakerTemplate(templateId);
            if(template!=null)try{
                mcpServerConfig = JSON.fromJson(template.config, MCPServerConfig.class);
                ToolMakerTemplateConfig templateConfig = dbHelper.selectToolMakerTemplateConfig(maker.id);
                mcpServerConfig.fillInputs(templateConfig.inputs);
            }catch (Exception e){
                mcpServerConfig = null;
                errorMessage = e.getMessage();
            }
        }else{
            mcpServerConfig = dbHelper.selectMCPServerConfig(maker.id);
        }
        if(mcpServerConfig==null){
            notificationHandler.onToolMakerNotification(new MCPServer(mcpServerConfig) {{
                id = maker.id;
                name = maker.name;
                agentId = maker.agentId;
                status = STATUS_OFF;
                userId = maker.userId;
                templateId = maker.templateId;
                errorCode = ERROR;
                errorMessage = errorMessage==null?"config not found":errorMessage;
            }});
            return;
        }
        notificationHandler.onToolMakerNotification(new MCPServer(mcpServerConfig) {{
            id = maker.id;
            name = maker.name;
            agentId = maker.agentId;
            status = STATUS_WAITING;
            userId = maker.userId;
            templateId = maker.templateId;
        }});
        MCPServer mcpServer = AIToolServiceHandler.connectMCPServer(
                maker.id, maker.name, mcpServerConfig);
        if(mcpServer==null){
            notificationHandler.onToolMakerNotification(new MCPServer(mcpServerConfig) {{
                id = maker.id;
                name = maker.name;
                agentId = maker.agentId;
                status = STATUS_OFF;
                userId = maker.userId;
                templateId = maker.templateId;
                errorCode = ERROR;
                errorMessage = maker.name+" exists";
            }});
            callback.onResult(TOOL_MAKER_EXISTS,null,null);
            return;
        }
        mcpServer.merge(maker, details.tools);
        if(mcpServer.errorCode>0){
            callback.onResult(0,null,mcpServer);
            notificationHandler.onToolMakerNotification(mcpServer);
            return;
        }
        notificationHandler.onToolMakerNotification(mcpServer);

        List<AIPortTool> publishingTools = new ArrayList<>();
        List<AIPortTool> oldTools = new ArrayList<>();
        for (MCPTool tool : mcpServer.getTools())
            if (tool.lastUpdated != 0) {
                AIPortTool duplicate = tool.duplicate();
                duplicate.metaData = tool.metaData();
                duplicate.hash = duplicate.metaData.hashCode();
                publishingTools.add(duplicate);
                oldTools.add(tool);
            }
        for (AIPortTool tool : details.tools) {
            if(tool.makerId==maker.id&&mcpServer.getTool(tool.name)==null){
                AIPortTool deprecated = new AIPortTool();
                deprecated.id = tool.id;
                deprecated.name = tool.name;
                deprecated.status = -1;
                publishingTools.add(deprecated);
            }
        }
        if (!publishingTools.isEmpty()) hstpRequest(
                aitoolsManagementUSL, "tool/publish",
                Map.of(
                        "toolAgentId",toolAgentDetails.toolAgent.id,
                        "toolMakerId",maker.id,
                        "tools",publishingTools
                ),
                (int code, String message, List<AIPortTool> data) ->{
                    if(code==0){
                        mcpServer.merge(maker,data);
                        callback.onResult(code, message, mcpServer);
                    }else {
                        callback.onResult(code, message, null);
                    }
                },
                (data) -> JSON.fromJson(data, new TypeReference<>() {})
        ); else callback.onResult(0,null,mcpServer);
    }

    public static void createToolMakerTemplate(
            String name, int type,String config,String inputs,
            Callback<AIPortToolMakerTemplate> callback) throws Exception{
        Service service = aitoolsManagementUSL
                .appendPath("tool_maker/template/create")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of(
                        "name",name,
                        "type", type,
                        "inputs",inputs,
                        "agentId",studioToolAgentId()
                )))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            AIPortServiceResponse<AIPortToolMakerTemplate> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==Service.SERVICE_SUCCESSFUL&&resp.data!=null){
                ToolMakerTemplate template = new ToolMakerTemplate();
                template.id = resp.data.id;
                template.type = type;
                template.config = config;
                template.inputs = inputs;
                dbHelper.insertToolMakerTemplate(template);
            }
            callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }
    public static void modifyToolMakerTemplate(
            long templateId,String name,int type,String config,String inputs,
            Callback<Boolean> callback) throws Exception{
        if (config != null && !(config = config.trim()).isEmpty()
                && inputs != null && !(inputs = inputs.trim()).isEmpty()) {
            ToolMakerTemplate template = dbHelper.selectToolMakerTemplate(templateId);
            if(template==null){
                callback.onResult(255,"Tool maker template not exists",null);
                return;
            }
            template.type = type;
            template.config = config;
            template.inputs = inputs;
            dbHelper.insertToolMakerTemplate(template);
            callback.onResult(0,null,true);
        }else callback.onResult(255,"Tool maker template config is invalid",null);
    }
    public static void connectToolMakerTemplate(
            long userId,long templateId,String name, String inputs,
            Callback<AIPortToolMaker> callback) throws Exception{
        ToolMakerTemplate template = dbHelper.selectToolMakerTemplate(templateId);
        if(template==null){
            callback.onResult(255,"Tool maker template not exists",null);
            return;
        }

        Service service = aitoolsManagementUSL
                .appendPath("tool_maker/create")
                .createServiceClient()
                .headers(authHeaders)
                .content(JSON.toJson(Map.of(
                        "userId",userId,
                        "templateId",templateId,
                        "agentId",studioToolAgentId(),
                        "name",name,
                        "type", template.type
                )))
                .request(serviceEngine);
        if(service.getErrorCode()==0) {
            AIPortServiceResponse<AIPortToolMaker> resp
                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
            if(resp.code==0&&resp.data!=null){
                ToolMakerTemplateConfig templateConfig = new ToolMakerTemplateConfig();
                templateConfig.id = resp.data.id;
                templateConfig.inputs = inputs;
                dbHelper.insertToolMakerTemplateConfig(templateConfig);
                ToolMakerDetails details = new ToolMakerDetails();
                details.maker = resp.data;
                if(resp.data.mcp()){
                    connectMCPServer(details,callback);
                }
            }else callback.onResult(resp.code,resp.message,resp.data);
        }else{
            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
        }
    }

    public static ToolMakerTemplate getToolMakerTemplate(long templateId){
        return dbHelper.selectToolMakerTemplate(templateId);
    }
    public static ToolMakerTemplateConfig getToolMakerTemplateConfig(long toolMakerId){
        return dbHelper.selectToolMakerTemplateConfig(toolMakerId);
    }
    public static void modifyToolMakerTemplateConfig(long toolMakerId,String inputs,Callback<AIPortToolMaker> callback){
        ToolMakerTemplateConfig templateConfig = dbHelper.selectToolMakerTemplateConfig(toolMakerId);
        if(templateConfig==null){
            templateConfig = new ToolMakerTemplateConfig();
            templateConfig.id = toolMakerId;
            templateConfig.inputs = inputs;
        }
        dbHelper.insertToolMakerTemplateConfig(templateConfig);
        connectToolMaker(toolMakerId,callback);
    }


    public static AIPortServiceResponse<AIPortToolMaker> removeToolMaker(long serverId) throws Exception{
        Service service = aitoolsManagementUSL.appendPath("tool_maker/remove")
                .createServiceClient()
                .headers(authHeaders)
                .content(Map.of(
                        "makerId", serverId
                ))
                .request(serviceEngine);
        int code = service.getErrorCode();
        String message = service.getErrorMessage();
        AIPortServiceResponse<AIPortToolMaker> resp;
        if(code==0){
            resp = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
        } else {
            resp = new AIPortServiceResponse<>();
            resp.code = code;
            resp.message = message;
        }
        return resp;
    }

    public static void removeMCPServer(long serverId, Callback<Boolean> callback) throws Exception {
        if(serverId>Integer.MAX_VALUE){
            AIPortServiceResponse<AIPortToolMaker> resp = removeToolMaker(serverId);
            if(resp.code!=0){
                callback.onResult(resp.code,resp.message,null);
                return;
            }
        }
        MCPServer server = AIToolServiceHandler.removeMCPServer(serverId);
        if(server!=null) {
            dbHelper.deleteMCPServerConfig(server.id);
            server.status = STATUS_ABANDONED;
            notificationHandler.onToolMakerNotification(server);
            if(server.templateId>0) dbHelper.deleteToolMakerTemplateConfig(server.id);
            callback.onResult(0, null, true);
        }else{
            callback.onResult(TOOL_MAKER_NOT_EXISTS,null,null);
        }
    }

    public static void removeOpenAPIServer(long serverId, Callback<Boolean> callback) throws Exception {
        if(serverId>Integer.MAX_VALUE){
            AIPortServiceResponse<AIPortToolMaker> resp = removeToolMaker(serverId);
            if(resp.code!=0){
                callback.onResult(resp.code,resp.message,null);
                return;
            }
        }

        OpenAPIServer server = AIToolServiceHandler.removeOpenAPIServer(serverId);
        if (server != null) {
            server.status = STATUS_ABANDONED;
            dbHelper.deleteOpenAPIServerConfig(server.id);
            notificationHandler.onToolMakerNotification(server);
            if(server.templateId>0) dbHelper.deleteToolMakerTemplateConfig(server.id);
            callback.onResult(0, null, true);
        } else {
            callback.onResult(TOOL_MAKER_NOT_EXISTS, null, null);
        }
    }

    public static void connectMCPServer(
            MCPServerConfig config,
            Callback<AIPortToolMaker> callback
    ) throws Exception {
        if(config==null){
            callback.onResult(255,"invalid MCP server config",null);
            return;
        }
        if(config.name.isBlank()){
            callback.onResult(255,"MCP server name can't be empty",null);
            return;
        }
        if(config.transport==0&&(config.command==null||config.command.isBlank())){
            callback.onResult(255,"MCP server command can't be empty",null);
            return;
        }
        if (config.transport>0&&(config.url==null||config.url.isBlank())){
            callback.onResult(255,"MCP server url can't be empty",null);
            return;
        }
        config.name = config.name.trim();
        if(AIToolServiceHandler.toolMakerExists(0,config.name)){
            callback.onResult(TOOL_MAKER_EXISTS,config.name+" exists",null);
            return;
        }

//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/create")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(Map.of(
//                        "userId",accountId(),
//                        "agentId",studioToolAgentId(),
//                        "name",config.name,
//                        "type", TYPE_MCP
//                )))
//                .request(serviceEngine);
//        if(service.getErrorCode()==0) {
//            AIPortServiceResponse<AIPortToolMaker> resp
//                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
//            if(resp.code==0&&resp.data!=null){
//                config.id = resp.data.id;
//                config.status = resp.data.status;
//                dbHelper.insertMCPServerConfig(config);
//                ToolMakerDetails details = new ToolMakerDetails();
//                details.maker = resp.data;
//                connectMCPServer(details,callback);
//            }else callback.onResult(resp.code,resp.message,null);
//        }else{
//            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
//        }
        AIPortServiceResponse<AIPortToolMaker> resp = createToolMaker(TYPE_MCP,config.name,"");
        if(resp.code==0&&resp.data!=null){
            config.id = resp.data.id;
            config.status = resp.data.status;
            dbHelper.insertMCPServerConfig(config);
            ToolMakerDetails details = new ToolMakerDetails();
            details.maker = resp.data;
            connectMCPServer(details,callback);
        }else callback.onResult(resp.code,resp.message,null);
    }
    private static void connectOpenAPIServer(
            ToolMakerDetails details,
            Callback<AIPortToolMaker> callback
    ){
        AIPortToolMaker maker = details.maker;
        long templateId = maker.templateId;
        OpenAPIServerConfig config = dbHelper.selectOpenAPIServerConfig(maker.id);
        if(config==null){
            notificationHandler.onToolMakerNotification(new OpenAPIServer() {{
                id = maker.id;
                name = maker.name;
                agentId = maker.agentId;
                status = STATUS_OFF;
                userId = maker.userId;
                templateId = maker.templateId;
                errorCode = ERROR;
                errorMessage = "config not found";
            }});
            return;
        }
        notificationHandler.onToolMakerNotification(new OpenAPIServer() {{
            id = maker.id;
            name = maker.name;
            agentId = maker.agentId;
            status = STATUS_WAITING;
            userId = maker.userId;
            templateId = maker.templateId;
        }});
        OpenAPIServer server = AIToolServiceHandler.connectOpenAPIServer(
                maker.id, maker.name, config);
        if(server==null){
            if(callback!=null)callback.onResult(TOOL_MAKER_EXISTS,null,null);
            notificationHandler.onToolMakerNotification(new OpenAPIServer() {{
                id = maker.id;
                name = maker.name;
                agentId = maker.agentId;
                status = STATUS_WAITING;
                userId = maker.userId;
                templateId = maker.templateId;
                errorCode = ERROR;
                errorMessage = maker.name +" exists";
            }});
            return;
        }
        server.merge(maker, details.tools);
        if(server.errorCode>0){
            if(callback!=null)callback.onResult(0,null,server);
            notificationHandler.onToolMakerNotification(server);
            return;
        }
        notificationHandler.onToolMakerNotification(server);

        List<AIPortTool> publishingTools = new ArrayList<>();
        List<AIPortTool> oldTools = new ArrayList<>();
        for (OpenAPITool tool : server.getTools())
            if (tool.lastUpdated != 0) {
                AIPortTool duplicate = tool.duplicate();
                duplicate.metaData = tool.metaData();
                duplicate.hash = duplicate.metaData.hashCode();
                publishingTools.add(duplicate);
                oldTools.add(tool);
            }
        for (AIPortTool tool : details.tools) {
            if(tool.makerId==maker.id&&server.getTool(tool.name)==null){
                AIPortTool deprecated = new AIPortTool();
                deprecated.id = tool.id;
                deprecated.name = tool.name;
                deprecated.status = -1;
                publishingTools.add(deprecated);
            }
        }
        if (!publishingTools.isEmpty()) hstpRequest(
                aitoolsManagementUSL, "tool/publish",
                Map.of(
                        "toolAgentId",toolAgentDetails.toolAgent.id,
                        "toolMakerId",maker.id,
                        "tools",publishingTools
                ),
                (int code, String message, List<AIPortTool> data) ->{
                    if(code==0){
                        server.merge(maker,data);
                        callback.onResult(code, message, server);
                    }else {
                        callback.onResult(code, message, null);
                    }
                }, (data) -> JSON.fromJson(data, new TypeReference<>() {
                }));
        else if(callback!=null) callback.onResult(0,null,server);
    }

    public static void connectOpenAPIServer(
            OpenAPIServerConfig config,
            Callback<AIPortToolMaker> callback
    ) throws Exception {
        if(config==null||config.name.isBlank()){
            callback.onResult(255,"invalid OpenAPI server config",null);
            return;
        }
        if(AIToolServiceHandler.toolMakerExists(0L,config.name)){
            callback.onResult(TOOL_MAKER_EXISTS,config.name+" exists",null);
            return;
        }
//        Service service = aitoolsManagementUSL
//                .appendPath("tool_maker/create")
//                .createServiceClient()
//                .headers(authHeaders)
//                .content(JSON.toJson(Map.of(
//                        "userId",accountId(),
//                        "agentId",studioToolAgentId(),
//                        "name",config.name,
//                        "type", TYPE_OPENAPI
//                )))
//                .request(serviceEngine);
//        if(service.getErrorCode()==0) {
//            AIPortServiceResponse<AIPortToolMaker> resp
//                    = JSON.fromJson(service.getResponseMessage(), new TypeReference<>() {});
//            if(resp.code==0&&resp.data!=null){
//                config.id = resp.data.id;
//                dbHelper.insertOpenAPIServerConfig(config);
//                ToolMakerDetails details = new ToolMakerDetails();
//                details.maker = resp.data;
//                connectOpenAPIServer(details,callback);
//            }else callback.onResult(resp.code,resp.message,null);
//        }else{
//            callback.onResult(service.getErrorCode(),service.getErrorMessage(),null);
//        }

        AIPortServiceResponse<AIPortToolMaker> resp = createToolMaker(TYPE_OPENAPI,config.name,"");
        if(resp.code==0&&resp.data!=null){
            config.id = resp.data.id;
            dbHelper.insertOpenAPIServerConfig(config);
            ToolMakerDetails details = new ToolMakerDetails();
            details.maker = resp.data;
            connectOpenAPIServer(details,callback);
        }else callback.onResult(resp.code,resp.message,null);
    }
    private static String runCommand(File workDir,String... command){
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir);
        try {
            Process process = processBuilder.start();
            return readStream(process.getInputStream());
        } catch (IOException ignore) {}
        return null;
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader processReader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = processReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }
    public static String installNpm() throws InstallationException {
        String nodeVersion = "v22.17.0";
        String npmVersion = "provided";
        String nodeDownloadRoot = System.getProperty("user.home")+"/Downloads/mcpdirect/rtm/";
        File workDir = new File(System.getProperty("user.home"),".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36));
        File installDir = new File(System.getProperty("user.home")+"/.mcpdirect/rtm/");
        ProxyConfig proxyConfig = new ProxyConfig(List.of());
        FrontendPluginFactory factory = new FrontendPluginFactory(workDir, installDir);
        factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
//                .setNodeDownloadRoot(nodeDownloadRoot)
                .setNpmVersion(npmVersion)
                .install();
        factory.getNPMInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
                .setNpmVersion(npmVersion)
//                .setNpmDownloadRoot(nodeDownloadRoot)
                .install();
        return checkNpm();
    }
    private static Map<String,String> nodejsCommands = new HashMap<>();
    private static String checkNodejs(String command){
        File workDir = new File(System.getProperty("user.home"),".mcpdirect/studio/"+Long.toString(accountDetails.userInfo.id,36));
        String commandPath = System.getProperty("user.home")+"/.mcpdirect/rtm/node/"+command;
        String result = null;
        if(new File(commandPath).exists()){
            result = runCommand(workDir, commandPath,"-v");
        }
        if(result==null){
            commandPath = "npx";
            result = runCommand(workDir, commandPath,"-v");
        }
        nodejsCommands.put(command,commandPath);
        return result;
    }

    public static String getNpxPath(){
        String npxPath = nodejsCommands.get("npx");
        if(npxPath==null) {
            checkNpx();
            npxPath = nodejsCommands.get("npx");
        }
        return npxPath;
    }
    public static String checkNpx(){
        return checkNodejs("npx");
    }
    public static String getNpmPath(){
        String npmPath = nodejsCommands.get("npm");
        if(npmPath==null) {
            checkNpm();
            npmPath = nodejsCommands.get("npm");
        }
        return npmPath;
    }
    public static String checkNpm(){
        return checkNodejs("npm");
    }
    public static String getNodePath(){
        String path = nodejsCommands.get("node");
        if(path==null) {
            checkNode();
            path = nodejsCommands.get("node");
        }
        return path;
    }
    public static String checkNode(){
        return checkNodejs("node");
    }
    public static String parseOpenAPIDoc(String yaml) throws Exception{
        OpenAPIServerDoc serverDoc = new OpenAPIServerDoc();
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(yaml);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        List<Server> servers = openAPI.getServers();
        if(servers!=null) {
            for (Server server :servers) {
                AtomicReference<String> url = new AtomicReference<>(server.getUrl());
                ServerVariables variables = server.getVariables();
                if(variables!=null) variables.forEach((k, v) -> {
                    String value = v.getDefault();
                    if (value == null && v.getEnum() != null) {
                        for (String s : v.getEnum()) {
                            value = s;
                            break;
                        }
                    }
                    if (value != null) {
                        url.set(url.get().replace("{" + k + "}", value));
                    }
                });
                serverDoc.addServer(server.getDescription(), url.get());
            }
        }
        List<SecurityRequirement> securities = openAPI.getSecurity();
        Components components = openAPI.getComponents();
        Map<String, SecurityScheme> schemes;
        if(securities!=null&&components!=null&&(schemes=components.getSecuritySchemes())!=null) {
            for (SecurityRequirement requirement : securities) {
                for (String keyName : requirement.keySet()) {
                    SecurityScheme scheme = schemes.get(keyName);
                    if(scheme!=null){
                        serverDoc.addSecurity(scheme.getDescription(),keyName);
                    }
                }
            }
        }
        io.swagger.v3.oas.models.Paths paths = openAPI.getPaths();
        if(paths!=null)for (Map.Entry<String, PathItem> e : paths.entrySet()) {
            String path = e.getKey();
            PathItem i = e.getValue();
            createToolName(serverDoc,"get",path,i.getGet());
            createToolName(serverDoc,"post",path,i.getPost());
            createToolName(serverDoc,"delete",path,i.getDelete());
            createToolName(serverDoc,"patch",path,i.getPatch());
            createToolName(serverDoc,"put",path,i.getPut());
        }
        return JSON.toJson(serverDoc);
    }
    private static void createToolName(OpenAPIServerDoc auth, String method, String path, Operation operation){
        if(operation==null){
            return;
        }
        String operationId = operation.getOperationId();
        String name = OpenAPITool.name(method,path);
        if(operationId!=null&&operationId.length()<name.length()){
            name = operationId;
        }
        auth.addPath(name,method,path);
    }
}
