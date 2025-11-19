package ai.mcpdirect.studio;

import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import appnet.hstp.labs.util.db.ComparisonOperator;
import appnet.hstp.labs.util.db.Table;
import appnet.hstp.labs.util.db.TableResultSet;
import appnet.hstp.labs.util.db.sqlite.SQLiteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.FileException;

import java.io.File;
import java.util.List;

public class MCPDirectStudioDBHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MCPDirectStudioDBHelper.class);
    private final SQLiteHelper sqliteHelper;
    private final Table<OpenAPIServerConfig> openAPIServerConfigTable;
    public MCPDirectStudioDBHelper() throws Exception {
        String userHome = System.getProperty("user.home");
        File file = new File(userHome, ".mcpdirect/studio/"+Long.toString(MCPDirectStudio.accountId(),36));
        if(file.exists()||(!file.exists()&&file.mkdirs())){
            sqliteHelper = new SQLiteHelper(new File(file, "mcpdirect-studio.db").getPath());
            openAPIServerConfigTable = sqliteHelper.table("openapi_server_config", OpenAPIServerConfig.class)
                    .column("id","int",true)
                    .column("config","text",false)
                    .create();

        }else throw new FileException("Initialize database file failed");
    }
    public List<OpenAPIServerConfig> selectOpenAPIServerConfigs(){
        try {
            TableResultSet<OpenAPIServerConfig> execute = openAPIServerConfigTable
                    .select().execute();
            return execute.getList("config");
        } catch (Exception e) {
            LOG.error("selectOpenAPIServerConfigs()",e);
            return List.of();
        }
    }
    public Long selectOpenAPIServerConfigId(long id){
        try {
            TableResultSet<OpenAPIServerConfig> execute = openAPIServerConfigTable
                    .select().and(ComparisonOperator.eq("id",id)).execute();
            return execute.getLong("id");
        } catch (Exception e) {
            LOG.error("selectOpenAPIServerConfigId()",e);
            return null;
        }
    }

    public OpenAPIServerConfig selectOpenAPIServerConfig(long id){
        try {
            TableResultSet<OpenAPIServerConfig> execute = openAPIServerConfigTable
                    .select().and(ComparisonOperator.eq("id",id)).execute();
            return execute.get("config");
        } catch (Exception e) {
            LOG.error("selectOpenAPIServerConfigId()",e);
            return null;
        }
    }

    public boolean insertOpenAPIServerConfig(OpenAPIServerConfig config){
        try {
            return openAPIServerConfigTable
                    .insert("id", config.id)
                    .value("config", config)
                    .replaceIfExists()
                    .execute();
        }catch (Exception e){
            LOG.error("insertOpenAPIServerConfig()",e);
            return false;
        }
    }
    public boolean setOpenAPIServerConfig(long id,OpenAPIServerConfig config){
        try {
            return openAPIServerConfigTable
                    .update("id",config.id)
                    .set("config",config)
                    .and(ComparisonOperator.eq("id",id))
                    .execute().updatedRows()>0;
        }catch (Exception e){
            LOG.error("insertOpenAPIServerConfig()",e);
            return false;
        }
    }
    public void deleteOpenAPIServerConfig(long id){
        try {
            openAPIServerConfigTable
                    .delete(ComparisonOperator.eq("id",id))
                    .execute()
                    .updatedRows();
        }catch (Exception e){
            LOG.error("insertOpenAPIServerConfig()",e);
        }
    }
}
