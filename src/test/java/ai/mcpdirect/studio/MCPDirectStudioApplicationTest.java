package ai.mcpdirect.studio;

import appnet.hstp.Service;
import appnet.hstp.ServiceEngine;
import appnet.hstp.ServiceEngineFactory;
import appnet.hstp.USL;
import appnet.hstp.annotation.ServiceName;
import appnet.hstp.exception.ServiceEngineException;
import appnet.hstp.exception.ServiceException;
import appnet.hstp.exception.USLSyntaxException;
import appnet.hstp.labs.util.http.HstpHttpClient;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@ServiceName
public class MCPDirectStudioApplicationTest extends TestCase {
    public static void main(String[] args) throws ServiceEngineException, USLSyntaxException, ServiceException, InterruptedException {
        ServiceEngine serviceEngine = ServiceEngineFactory.getServiceEngine();
        Thread.sleep(5000);
        Service service = USL
                .createServiceClient("aitools@local" + "/call/12345")
                .content("{}")
                .request(serviceEngine);
        System.out.println(service.getErrorCode());
    }
    public void testOsName() throws Exception {
        System.out.println(System.getProperty("os.name").toLowerCase().contains("linux"));
        Process process = Runtime.getRuntime().exec("cat /etc/machine-id");
        process.waitFor();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        System.out.println(reader.readLine());
    }
    public void test() throws Exception {
        String s = HstpHttpClient.doPost(
                "http://192.168.1.7:8081/tool/list",
                Map.of(
                        "_Authorization","aik-mVIhlp725ZUf2FcxV5SyYbHo4LVoDAiWddbdcrfqayx811s5",
                        "X-MCPdirect-Key","aik-mVIhlp725ZUf2FcxV5SyYbHo4LVoDAiWddbdcrfqayx811s5"
                ),
                Map.of(),
                Map.of()
        );
        System.out.println(s);
    }
    public void testToolCall() throws Exception {
        String s = HstpHttpClient.doPost(
                "http://192.168.1.7:8081/tool/_obsidian_list_files_in_vault_3fvd5v1",
                Map.of(
                        "Authorization","aik-mVIhlp725ZUf2FcxV5SyYbHo4LVoDAiWddbdcrfqayx811s5"
                ),
                Map.of(),
                Map.of()
        );
        System.out.println(s);
    }
    public void testMachineName() throws Exception{
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            System.out.println("主机名: " + hostname);
        } catch (UnknownHostException e) {
            System.err.println("无法获取主机名: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            String path = "/sys/class/dmi/id/product_name";
            if (Files.exists(Paths.get(path))) {
                String model = Files.readString(Paths.get(path)).trim();
                if (!model.isEmpty() && !model.equals("To be filled by O.E.M.")) {
                    System.out.println(model);
                }
            }
        } catch (Exception ignored) {}
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "/bin/sh", "-c", "dmidecode -s system-product-name 2>/dev/null"
            });
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String model = reader.readLine();
            if (model != null && !model.trim().isEmpty()) {
                System.out.println(model);
            }
        } catch (Exception ignored) {}
        Process process = Runtime.getRuntime().exec(
                new String[]{"/bin/sh", "-c", "system_profiler SPHardwareDataType"});

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        String modelName = null;
        String mid = null;
        while ((line = reader.readLine()) != null) {
//            mid = line.split(":")[1].trim();
            line = line.trim();
            if(line.startsWith("Model Name")){
                modelName = line.split(":")[1].trim();
            }else if(line.startsWith("Hardware UUID")){
                mid = line.split(":")[1].trim();
            }

        }
        System.out.println(modelName);
        System.out.println(mid);
        process = Runtime.getRuntime().exec("sysctl -n hw.model");
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

//        String model = reader.readLine();
//        System.out.println(model);
        while ((line = reader.readLine()) != null) {
//            mid = line.split(":")[1].trim();
            System.out.println(line);
        }
    }
}