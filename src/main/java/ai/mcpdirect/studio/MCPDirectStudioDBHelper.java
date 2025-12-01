package ai.mcpdirect.studio;

import ai.mcpdirect.studio.dao.entity.ToolMakerTemplate;
import ai.mcpdirect.studio.dao.entity.ToolMakerTemplateConfig;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
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
    private final Table<MCPServerConfig> mcpServerConfigTable;
    private final Table<ToolMakerTemplate> templateTable;
    private final Table<ToolMakerTemplateConfig> templateConfigTable;
    public MCPDirectStudioDBHelper() throws Exception {
        String userHome = System.getProperty("user.home");
        File file = new File(userHome, ".mcpdirect/studio/"+Long.toString(MCPDirectStudio.accountId(),36));
        if(file.exists()||(!file.exists()&&file.mkdirs())){
            sqliteHelper = new SQLiteHelper(new File(file, "mcpdirect-studio.db").getPath());
            openAPIServerConfigTable = sqliteHelper.table("openapi_server_config", OpenAPIServerConfig.class)
                    .column("id","int",true)
                    .column("config","text",false)
                    .create();
            mcpServerConfigTable = sqliteHelper.table("mcp_server_config", MCPServerConfig.class)
                    .column("id","int",true)
                    .column("config","text",false)
                    .create();
            templateTable = sqliteHelper.table("tool_maker_template", ToolMakerTemplate.class)
                    .column("id","int",true)
                    .column("type","int",false)
                    .column("config","text",false)
                    .column("inputs","text",false)
                    .create();
            templateConfigTable = sqliteHelper.table("tool_maker_template_config", ToolMakerTemplateConfig.class)
                    .column("id","int",true)
                    .column("inputs","text",false)
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
            LOG.error("deleteOpenAPIServerConfig()",e);
        }
    }

    public List<MCPServerConfig> selectMCPServerConfigs(){
        try {
            TableResultSet<MCPServerConfig> execute = mcpServerConfigTable
                    .select().execute();
            return execute.getList("config");
        } catch (Exception e) {
            LOG.error("selectMCPServerConfigs()",e);
            return List.of();
        }
    }
    public Long selectMCPServerConfigId(long id){
        try {
            TableResultSet<MCPServerConfig> execute = mcpServerConfigTable
                    .select().and(ComparisonOperator.eq("id",id)).execute();
            return execute.getLong("id");
        } catch (Exception e) {
            LOG.error("selectMCPServerConfigId()",e);
            return null;
        }
    }

    public MCPServerConfig selectMCPServerConfig(long id){
        try {
            TableResultSet<MCPServerConfig> execute = mcpServerConfigTable
                    .select().and(ComparisonOperator.eq("id",id)).execute();
            return execute.get("config");
        } catch (Exception e) {
            LOG.error("selectMCPServerConfigId()",e);
            return null;
        }
    }

    public boolean insertMCPServerConfig(MCPServerConfig config){
        try {
            return mcpServerConfigTable
                    .insert("id", config.id)
                    .value("config", config)
                    .replaceIfExists()
                    .execute();
        }catch (Exception e){
            LOG.error("insertMCPServerConfig()",e);
            return false;
        }
    }
    public boolean setMCPServerConfig(long id,MCPServerConfig config){
        try {
            return mcpServerConfigTable
                    .update("id",config.id)
                    .set("config",config)
                    .and(ComparisonOperator.eq("id",id))
                    .execute().updatedRows()>0;
        }catch (Exception e){
            LOG.error("insertOpenAPIServerConfig()",e);
            return false;
        }
    }
    public void deleteMCPServerConfig(long id){
        try {
            mcpServerConfigTable
                    .delete(ComparisonOperator.eq("id",id))
                    .execute()
                    .updatedRows();
        }catch (Exception e){
            LOG.error("deleteMCPServerConfig()",e);
        }
    }
    public boolean insertToolMakerTemplate(ToolMakerTemplate template){
        try {
            return templateTable
                    .insert("id", template.id)
                    .value("type", template.type)
                    .value("config", template.config)
                    .value("inputs",template.inputs)
                    .replaceIfExists()
                    .execute();
        }catch (Exception e){
            LOG.error("insertToolMakerTemplate()",e);
            return false;
        }
    }
    public ToolMakerTemplate selectToolMakerTemplate(long id){
        try {
            TableResultSet<ToolMakerTemplate> execute = templateTable
                    .select().and(ComparisonOperator.eq("id",id)).execute();
            return execute.get();
        } catch (Exception e) {
            LOG.error("selectToolMakerTemplate()",e);
            return null;
        }
    }
    public boolean insertToolMakerTemplateConfig(ToolMakerTemplateConfig config){
        try {
            return templateConfigTable
                    .insert("id", config.id)
                    .value("inputs",config.inputs)
                    .replaceIfExists()
                    .execute();
        }catch (Exception e){
            LOG.error("insertToolMakerTemplateConfig()",e);
            return false;
        }
    }
    public ToolMakerTemplateConfig selectToolMakerTemplateConfig(long id){
        try {
            TableResultSet<ToolMakerTemplateConfig> execute = templateConfigTable
                    .select().and(ComparisonOperator.eq("id",id)).execute();
            return execute.get();
        } catch (Exception e) {
            LOG.error("selectToolMakerTemplateConfig()",e);
            return null;
        }
    }
}
