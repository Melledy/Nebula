package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.ScoreBossLevel;
import emu.nebula.proto.ScoreBossInfoOuterClass.ScoreBossInfo;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.score_boss_info_req)
public class HandlerScoreBossInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = ScoreBossInfo.newInstance()
                .setControlId(session.getPlayer().getScoreBossManager().getControlId());
        
        // Get rank entry
        var rankEntry = session.getPlayer().getScoreBossManager().getRankEntry();
        if (rankEntry != null) {
            // Set total score
            rsp.setScore(rankEntry.getScore());
            rsp.setStar(rankEntry.getStars());
            
            // Encode team data to proto
            for (var teamEntry : rankEntry.getTeams().entrySet()) {
                int id = teamEntry.getKey();
                var team = teamEntry.getValue();
                
                var info = ScoreBossLevel.newInstance()
                        .setBuildId(team.getBuildId())
                        .setLevelId(id)
                        .setScore(team.getLevelScore())
                        .setSkillScore(team.getSkillScore())
                        .setStar(team.getStars());
                
                for (var charEntry : team.getCharacters()) {
                    info.addCharIds(charEntry.getId());
                }
                
                rsp.addLevels(info);
            }
            
            // Add claimed rewards
            for (int id : rankEntry.getClaimedRewards()) {
                rsp.addStarRewards(id);
            }
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.score_boss_info_succeed_ack, rsp);
    }

}
