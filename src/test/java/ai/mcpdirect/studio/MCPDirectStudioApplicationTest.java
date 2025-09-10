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
}