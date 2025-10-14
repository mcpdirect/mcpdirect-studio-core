package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortTeamToolMaker {
    public long toolMakerId;
    public long teamId;
    public Integer status;
    public long created;
    public long lastUpdated;

    public AIPortTeamToolMaker toolMakerId(long toolMakerId) {
        this.toolMakerId = toolMakerId;
        return this;
    }

    public AIPortTeamToolMaker teamId(long teamId) {
        this.teamId = teamId;
        return this;
    }

    public AIPortTeamToolMaker status(Integer status) {
        this.status = status;
        return this;
    }

    public AIPortTeamToolMaker created(long created) {
        this.created = created;
        return this;
    }

    public AIPortTeamToolMaker lastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
    public AIPortTeamToolMaker copy(){
        return build().toolMakerId(toolMakerId).teamId(teamId).status(status).created(created).lastUpdated(lastUpdated);
    }
    public static AIPortTeamToolMaker build(){
        return new AIPortTeamToolMaker();
    }
}
