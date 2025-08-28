package ai.mcpdirect.studio;

import appnet.hstp.Service;
import appnet.hstp.ServiceEngine;
import appnet.hstp.ServiceEngineFactory;
import appnet.hstp.USL;
import appnet.hstp.annotation.ServiceName;
import appnet.hstp.exception.ServiceEngineException;
import appnet.hstp.exception.ServiceException;
import appnet.hstp.exception.USLSyntaxException;
import junit.framework.TestCase;

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

}