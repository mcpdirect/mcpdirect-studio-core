package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortToolMakerTeam {
    public long toolMakerId;
    public long teamId;
    public Integer status;
    public long created;
    public long lastUpdated;

    public AIPortToolMakerTeam toolMakerId(long toolMakerId) {
        this.toolMakerId = toolMakerId;
        return this;
    }

    public AIPortToolMakerTeam teamId(long teamId) {
        this.teamId = teamId;
        return this;
    }

    public AIPortToolMakerTeam status(Integer status) {
        this.status = status;
        return this;
    }

    public AIPortToolMakerTeam created(long created) {
        this.created = created;
        return this;
    }

    public AIPortToolMakerTeam lastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
    public AIPortToolMakerTeam copy(){
        return build().toolMakerId(toolMakerId).teamId(teamId).status(status).created(created).lastUpdated(lastUpdated);
    }
    public static AIPortToolMakerTeam build(){
        return new AIPortToolMakerTeam();
    }
}
