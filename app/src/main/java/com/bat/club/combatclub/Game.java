package com.bat.club.combatclub;

import java.util.Date;

/**
 * Created by Noyloy on 4/4/2016.
 */
public class Game {

    public int gameID;
    public String gameName;

    public Date gameStart;
    public int gameMinutes;

    public int playersCap;
    public int ticketsCap;

    public int[] teamsCount = new int[2];
    public int[] ticketsCount = new int[2];

    public Game(int gameID, String gameName, int playersCap, int ticketsCap, int[] teamsCount, int[] ticketsCount) {
        this.gameID = gameID;
        this.gameName = gameName;
        this.playersCap = playersCap;
        this.ticketsCap = ticketsCap;
        this.teamsCount = teamsCount;
        this.ticketsCount = ticketsCount;
    }
}
