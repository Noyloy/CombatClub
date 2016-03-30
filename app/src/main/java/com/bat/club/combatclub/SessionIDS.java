package com.bat.club.combatclub;

/**
 * Created by Noyloy on 3/30/2016.
 */
public class SessionIDS {
    public static String[] KEYS = {"P_ID","P_NAME","G_ID","T_ID"};

    public int playerID;
    public String playerName;
    public int gameID;
    public int teamID;

    public SessionIDS(int playerID, String playerName) {
        this.playerID = playerID;
        this.playerName = playerName;
    }

    public SessionIDS(int playerID, String playerName, int gameID, int teamID) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.gameID = gameID;
        this.teamID = teamID;
    }

    public int getPlayerID() {
        return playerID;
    }

    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getGameID() {
        return gameID;
    }

    public void setGameID(int gameID) {
        this.gameID = gameID;
    }

    public int getTeamID() {
        return teamID;
    }

    public void setTeamID(int teamID) {
        this.teamID = teamID;
    }
}
