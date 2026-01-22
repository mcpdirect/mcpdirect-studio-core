package ai.mcpdirect.studio;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import java.io.*;
import java.util.List;
import java.util.Map;

public class InstallNpxTest {
    public static void main(String[] args) throws InstallationException, TaskRunnerException, IOException {
        String nodeVersion = "v22.17.0";
        String npmVersion = "provided";
        String nodeDownloadRoot = System.getProperty("user.home")+"/Downloads/";
        ProxyConfig proxyConfig = new ProxyConfig(List.of());
        FrontendPluginFactory factory = new FrontendPluginFactory(
                new File("./")
                , new File(System.getProperty("user.home"),"/.mcpdirect/studio/"));
//        factory.getNodeInstaller(proxyConfig)
//                .setNodeVersion(nodeVersion)
////                .setNodeDownloadRoot(nodeDownloadRoot)
//                .setNpmVersion(npmVersion)
//                .install();
//        factory.getNPMInstaller(proxyConfig)
//                .setNodeVersion(nodeVersion)
//                .setNpmVersion(npmVersion)
////                .setNpmDownloadRoot(nodeDownloadRoot)
//                .install();

        factory.getNpxRunner(proxyConfig, "")
                .execute("-v", Map.of());

        ProcessBuilder processBuilder = new ProcessBuilder(
                "/home/robin/.mcpdirect/studio/node/npx","-v"
        );
        Process process = processBuilder.start();
        System.out.println(bound(process.getInputStream()));
        System.out.println(bound(process.getErrorStream()));

        factory.getNpmRunner(proxyConfig, "")
                .execute("-v", Map.of());

        processBuilder = new ProcessBuilder(
                "/home/robin/.mcpdirect/studio/node/npm","-v"
        );
        process = processBuilder.start();
        System.out.println(bound(process.getInputStream()));
        System.out.println(bound(process.getErrorStream()));
    }

    public static String bound(InputStream in){
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader processReader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = processReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        catch (IOException e) {
            stringBuilder.append(e.getMessage());
        }
        return stringBuilder.toString();
    }
}
