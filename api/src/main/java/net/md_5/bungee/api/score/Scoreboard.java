package net.md_5.bungee.api.score;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class Scoreboard {

    /**
     * Objectives for this scoreboard.
     */
    private final Map<String, Objective> objectives = new HashMap<>();
    /**
     * Scores for this scoreboard.
     */
    private final Map<String, Score> scores = new HashMap<>();
    /**
     * Teams on this board.
     */
    private final Map<String, Team> teams = new HashMap<>();
    /**
     * Unique name for this scoreboard.
     */
    private String name;
    /**
     * Position of this scoreboard.
     */
    private Position position;

    public Collection<Objective> getObjectives() {
        return Collections.unmodifiableCollection(objectives.values());
    }

    public Collection<Score> getScores() {
        return Collections.unmodifiableCollection(scores.values());
    }

    public Collection<Team> getTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public void addObjective(Objective objective) {
        Preconditions.checkNotNull(objective, "objective");
        Preconditions.checkArgument(!objectives.containsKey(objective.getName()), "Objective %s already exists in this scoreboard", objective.getName());
        objectives.put(objective.getName(), objective);
    }

    public void addScore(Score score) {
        Preconditions.checkNotNull(score, "score");
        scores.put(score.getItemName(), score);
    }

    public Score getScore(String name) {
        return scores.get(name);
    }

    public void addTeam(Team team) {
        Preconditions.checkNotNull(team, "team");
        if (teams.containsKey(team.getName())) return;
        teams.put(team.getName(), team);
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

    public Objective getObjective(String name) {
        return objectives.get(name);
    }

    public void removeObjective(String objectiveName) {
        objectives.remove(objectiveName);
    }

    public void removeScore(String scoreName) {
        scores.remove(scoreName);
    }

    public void removeTeam(String teamName) {
        teams.remove(teamName);
    }

    public void clear() {
        name = null;
        position = null;
        objectives.clear();
        scores.clear();
        teams.clear();
    }
}
