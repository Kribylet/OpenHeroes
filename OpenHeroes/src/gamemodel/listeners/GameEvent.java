package gamemodel.listeners;

import gamemodel.Team;

/**
 * GameEvents are simple objects carrying information about events that occur in the GameModel.
 * <p>
 * They're used to provide specific information about game events to the View through the
 * GameEventListener interface.
 */
public class GameEvent
{
    private Team team = null;
    private GameEventType type;

    public GameEvent(final Team team, final GameEventType type) {
	this.team = team;
	this.type = type;
    }

    public GameEvent(final GameEventType event) {
	this.type = event;
    }

    public GameEvent() {
	this.type = GameEventType.DEFAULT;
    }

    public Team getTeam() {
	return team;
    }

    public GameEventType getType() {
	return type;
    }


    public enum GameEventType
    {
	/** A context change has occured */
	DEFAULT,
	/** Another player was defeated */
    	DEFEAT_NOTIFICATION,
	/** Our turn begun. */
    	TURN_START,
	/** Notify the view tha a client has disconnected.*/
	PLAYER_DISCONNECTED,
	/** A battle started. */
    	BATTLE_START
    }
}
