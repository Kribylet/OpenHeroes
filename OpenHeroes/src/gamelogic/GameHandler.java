package gamelogic;

import connection.Message;
import entity.Hero;
import entity.ResourceMine;
import entity.Town;
import gamemodel.GameModel;
import gamemodel.InvalidMainMapStateException;
import gamemodel.Position;
import gamemodel.Team;
import gamemodel.listeners.GameEvent;
import gamemodel.listeners.GameEvent.GameEventType;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GameHandler handles confirmed game model changes. Once a server acknowledges
 * the validity of a client request, the game handler processes it.
 * <p>
 * This centralizes most of the code used to keep the server and clientside in a
 * synchronized state which minimizes clutter and code duplication.
 * <p>
 * The server still has the responsibility of driving the game state based on the
 * changes that occur in the GameHandler, so all game logic can not be moved here.
 */
public abstract class GameHandler
{
    private final static Logger LOGGER = Logger.getLogger(GameHandler.class.getName());
    private volatile GameModel gameModel = null;
    protected Deque<Position> movePath = null;
    private final ReentrantLock gameModelLock = new ReentrantLock();
    protected final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();


    protected GameHandler() {}

    public void setGameModel(final GameModel gameModel) {
	gameModelLock.lock();
	try {
	    this.gameModel = gameModel;
	    LOGGER.log(Level.FINER, "Replaced GameModel in GameHandler.");
	} finally {
	    gameModelLock.unlock();
	}
    }

    public void addMessage(final Message message) {
        	messageQueue.add(message);
            }

    public GameModel getGameModel() {
	gameModelLock.lock();
	try {
	    LOGGER.log(Level.FINEST, "GameHandler GameModel was read.");
	    return gameModel;
	} finally {
	    gameModelLock.unlock();
	}

    }

    public abstract Deque<Position> readPath(final Team id) throws IOException;

    public abstract Hero readHero(final Team id) throws IOException;

    public void processMessage(Message message) throws InvalidMainMapStateException, IOException {
	gameModelLock.lock();
	try {
	    LOGGER.log(Level.FINE, "Received message as follows: " + message);
	    switch (message.getHeader()) {
		case MOVE_HERO:
		    handleMoveHeroMessage(message);
		    break;
		case HERO_BATTLE:
		    handleHeroBattleMessage();
		    break;
		case HERO_TRADE:
		    handleHeroTradeMessage();
		    break;
		case TRADE_CONCLUDED:
		    handleTradeConcludedMessage();
		    break;
		case TOWN_BATTLE:
		    handleTownBattleMessage(message);
		    break;
		case END_BATTLE_TURN:
		    handleEndBattleTurnMessage();
		    break;
		case HERO_DEFEATED:
		    handleHeroDefeatedMessage(message);
		    break;
		case CAPTURE_RESOURCE:
		    handleCaptureResourceMessage(message);
		    break;
		case TEAM_DEFEATED:
		    handleTeamDefeatedMessage(message);
		    break;
		case TURN_END:
		    handleTurnEndMessage();
		    break;
		case ADD_HERO:
		    handleAddHero(message);
		    break;
		case PLAYER_DISCONNECTED:
		    handlePlayerDisconnectMessage(message);
		    break;
		default:
	    }
	    LOGGER.log(Level.FINER, "GameHandler completed " + message.getHeader().name() + "instructions.");
	} finally {
	    gameModelLock.unlock();
	}
    }

    private void handleTeamDefeatedMessage(final Message message) throws InvalidMainMapStateException {
	gameModel.defeatTeam(Team.valueOf(message.getArgs()[0]));
    }

    private void handleAddHero(final Message message) throws InvalidMainMapStateException, IOException {
	Town town = gameModel.getTownByID(message.getArgs()[1]);
	Hero hero = readHero(message.getSender());
	Team team = Team.valueOf(message.getArgs()[0]);
	gameModel.buyHero(team, hero, town);
    }

    private void handlePlayerDisconnectMessage(final Message message) {
        getGameModel().notifyGameEventListeners(new GameEvent(Team.valueOf(message.getArgs()[0]),
							      GameEventType.PLAYER_DISCONNECTED));
    }

    private void handleHeroDefeatedMessage(final Message message) throws InvalidMainMapStateException {
	Hero hero = gameModel.getHeroByID(message.getArgs()[0]);
	gameModel.killHero(hero);
    }

    private void handleEndBattleTurnMessage() {
	gameModel.battleConcluded();
    }

    private void handleHeroTradeMessage() {
	gameModel.trade();
    }

    private void handleTownBattleMessage(final Message message) throws InvalidMainMapStateException {

	gameModelLock.lock();
	try {
	    Team team = gameModel.getHeroByID(message.getArgs()[0]).getOwner();
	    String townID = message.getArgs()[1];
	    Town town = gameModel.getTownByID(townID);
	    town.setOwner(team);
	    gameModel.getMainMap().notifyMainMapListeners();
	    gameModel.checkDefeat();
	} finally {
	    gameModelLock.unlock();
	}
    }

    private void handleCaptureResourceMessage(final Message message) throws InvalidMainMapStateException {

	gameModelLock.lock();
	try {
	    Team team = gameModel.getHeroByID(message.getArgs()[0]).getOwner();
	    ResourceMine mine = gameModel.getResourceMineByID(message.getArgs()[1]);
	    mine.setOwner(team);
	    gameModel.getMainMap().notifyMainMapListeners();
	} finally {
	    gameModelLock.unlock();
	}
    }

    private void handleMoveHeroMessage(final Message message) throws InvalidMainMapStateException, IOException {
	gameModelLock.lock();
	try {
	    Hero hero = gameModel.getHeroByID(message.getArgs()[0]);
	    movePath = readPath(message.getSender());
	    gameModel.moveHero(hero, movePath);
	} finally {
	    gameModelLock.unlock();
	}
    }

    private void handleHeroBattleMessage() {
	gameModelLock.lock();
	try {
	    gameModel.startBattle();

	} finally {
	    gameModelLock.unlock();
	}
    }

    private void handleTradeConcludedMessage() {
	gameModel.tradeConcluded();
    }

    private void handleTurnEndMessage() {
	gameModel.endTurn();
    }
}
