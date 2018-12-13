package gamelogic;

import connection.Message;
import connection.MessageType;
import connection.TCPServer;
import entity.Hero;
import entity.HeroFactory;
import entity.Town;
import gamemodel.GameModel;
import gamemodel.InvalidMainMapStateException;
import gamemodel.Position;
import gamemodel.Team;
import resources.GameResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The ServerSession class is responsible for driving the GameModel logic
 * when a change occurs, and propagating valid GameModel changes to connected
 * client sessions.
 * <p>
 * After construction the ServerSession begins listening for connection requests
 * and accepting them until the expected number of clients have connected.
 * <p>
 * Once a client is defeated, the ServerSession will disconnect them.
 * <p>
 * Since the GameModel behaviour is largely deterministic, many commands need only
 * be validated and propagated. The GameHandler superclass handles the shared
 * GameModel behaviour.
 * <p>
 * The ServerSession studies the GameModel after changes if necessary and propagates
 * any changes that occur.
 * <p>
 * Message validation is implemented rather uniformly:
 * If a client is considered to be on turn by the server GameModel, and they supply a
 * valid gameBoardHashCode for the current GameBoard, their actions are considered legitimate.
 * <p>
 * ** This offers no real protection against a malicious client. A more thorough validity
 * check of client commands would be necessary to detect fraudulent commands.
 * <p>
 * If they are on turn but fail to supply a valid hashcode, they are told to synchronize
 * with the ServerSession GameModel.
 */

public class ServerSession extends GameHandler implements Runnable
{
    private final Logger serverSessionLogger = Logger.getLogger(ServerSession.class.getName());
    private final int expectedClients;

    private final Map<Team, TCPServer> connectionID = new EnumMap<>(Team.class);
    private final List<TCPServer> connections = new ArrayList<>();
    private final ReentrantLock gameModelLock = new ReentrantLock();
    private int clientIDCounter = 0;

    public ServerSession(final GameModel gameModel) {
	setGameModel(gameModel);
	this.expectedClients = gameModel.getHumanPlayers();
    }

    @Override public Deque<Position> readPath(final Team id) throws IOException {
	return connectionID.get(id).readPath();
    }

    @Override public Hero readHero(final Team id) throws IOException {
	return connectionID.get(id).readHero();
    }

    @Override public void run() {
	ServerSocketListener[] serverSocketListeners = new ServerSocketListener[expectedClients];
	for (int i = 0; i < serverSocketListeners.length; i++) {
	    serverSocketListeners[i] = new ServerSocketListener(this);
	}
	GameResourceManager.instance().executeSequentialThreads(serverSocketListeners);

	while (true) {
	    try {
		Message message = messageQueue.take();
		gameModelLock.lock();
		try {
		    serverSessionLogger.log(Level.INFO, "[Q] Server got message " + message);
		    if (message.getHeader() == MessageType.CONNECTION_LOST) {
		        killDisconnectedTeam(message.getSender());
		    }
		    if (validateMessage(message)) {
			processMessage(message);
			driveGameLogic(message);
		    } else {
		        connectionID.get(message.getSender()).sendGameModel();
		    }
		    serverSessionLogger.log(Level.INFO, "[Q] Server finished handling message.");
		} catch (InvalidMainMapStateException e) {
		    serverSessionLogger.log(Level.SEVERE, "Map is corrupted on server, unable to recover.", e);
		    System.exit(ExitCode.MAP.ordinal());
		} catch (IOException e) {
		    serverSessionLogger.log(Level.WARNING, "Lost connection with a client!", e);
		} finally {
		    gameModelLock.unlock();
		}
	    } catch (InterruptedException e) {
		serverSessionLogger.log(Level.WARNING, "ServerSession was interrupted. This is only intended if the game has ended.",
					e);
		return;
	    }
	}
    }

    protected boolean validateMessage(final Message message) {
	if (message.getHeader() == MessageType.SURRENDER) {return true;} // Always permitted.
	return verifyGameBoard(message.getHash()) && getGameModel().getTurnTaker().equals(message.getSender());
    }

    private void driveGameLogic(final Message message) throws InvalidMainMapStateException, IOException {
	Team team = message.getSender();
	switch (message.getHeader()) {
	    case MOVE_HERO:
		messageAllClients(message);
		sendAllMovePath(movePath);
		break;
	    case BUY_HERO:
	        buyHeroServerLogic(message, team);
	        break;
	    case TRADE_CONCLUDED:
		connectionID.get(team).sendMessage(message);
		break;
	    case HERO_BATTLE:
		heroBattleServerLogic(message, team);
		break;
	    case HERO_TRADE:
		connectionID.get(team).sendMessage(message);
		break;
	    case SURRENDER:
		handleSurrenderServerLogic(team);
		break;
	    default:
		messageAllClients(message);
		break;
	}
    }

    private void buyHeroServerLogic(final Message message, final Team team)
	    throws InvalidMainMapStateException, IOException
    {
    	Town town = getGameModel().getTownByID(message.getArgs()[0]);
    	Hero hero = HeroFactory.makeHero();
	getGameModel().buyHero(team, hero, town);
    	messageAllClients(Message.addHero(team, message.getArgs()[0]));
    	sendAllHero(hero);
    }

    private void handleSurrenderServerLogic(final Team team) throws InvalidMainMapStateException {
	teamDefeated(team);
    }
    public void teamDefeated(final Team defeatedTeam) throws InvalidMainMapStateException {
	getGameModel().defeatTeam(defeatedTeam);
	messageAllClients(Message.teamDefeated(defeatedTeam));
    }

    private void heroBattleServerLogic(Message message, Team team) throws InvalidMainMapStateException, IOException {
	String challengerID = message.getArgs()[0];
	String defenderID = message.getArgs()[1];
	Hero challenger = getGameModel().getHeroByID(challengerID);
	Hero defender = getGameModel().getHeroByID(defenderID);
	Team defendingTeam = defender.getOwner();
	getGameModel().createBattlefield(challenger, defender);
	connectionID.get(team).sendBattlefield();
	connectionID.get(team).sendMessage(message);
	connectionID.get(defendingTeam).sendBattlefield();
	connectionID.get(defendingTeam).sendMessage(message);
	int challengerResult = getGameModel().getBattlefield().getChallengerResult();
	int defenderResult = getGameModel().getBattlefield().getDefenderResult();
	if (challengerResult > defenderResult) {
	    getGameModel().killHero(defender);
	    messageAllClients(Message.heroDefeated(defenderID));
	} else {
	    getGameModel().killHero(challenger);
	    messageAllClients(Message.heroDefeated(challengerID));
	}
    }

    public void messageAllClients(Message message) {
	try {
	    message.updateHash(getGameModel().gameBoardHashCode());
	    synchronized (connections) {
		for (TCPServer connection : connections) {

		    connection.sendMessage(message);
		}
	    }
	} catch (IOException e) {
	    serverSessionLogger.log(Level.WARNING, "Lost connection with a client!", e);
	    cleanDeadConnections();
	    synchronizeAllClients();
	}
    }

    private void cleanDeadConnections() {
	synchronized (connections) {
	    List<TCPServer> deadConnections = new ArrayList<>();
	    for (TCPServer connection : connections
		 ) {
		if (GameResourceManager.instance().isSocketDead(connection.getSocketID())) {
		    deadConnections.add(connection);
		}
	    }
	    for ( TCPServer deadConnection : deadConnections
		 ) {
		killDisconnectedTeam(deadConnection.getConnectedTeam());
	    }
	}
    }

    private void synchronizeAllClients() {
	synchronized (connections) {
	    for (TCPServer connection : connections) {
		try {
		    connection.sendGameModel();
		} catch (IOException e) {
		    serverSessionLogger.log(Level.WARNING, "Lost connection with a client!", e);
		    killDisconnectedTeam(connection.getConnectedTeam());
		    synchronizeAllClients();
		}
	    }
	}
    }

    public void sendAllMovePath(Deque<Position> path) throws IOException {
	for (TCPServer connection : connections) {
	    connection.sendPath(path);
	}
    }
    public void sendAllHero(Hero hero) throws IOException {
    	for (TCPServer connection : connections) {
    	    connection.sendHero(hero);
    	}
    }


    private boolean verifyGameBoard(final String clientGameBoardHash) {
	return (Integer.parseInt(clientGameBoardHash) == getGameModel().gameBoardHashCode());
    }

    public void addPlayer(final TCPServer newTCPServer, final String playerID, final Team team) throws IOException {
	gameModelLock.lock();
	try {
	    serverSessionLogger.log(Level.INFO, "added playerid " + playerID);

	    getGameModel().addPlayer(team, playerID);
	    for (TCPServer oldTCPServer : connections) {
		if (!oldTCPServer.equals(newTCPServer)) {
		    oldTCPServer.sendGameModel();
		}
	    }
	    connectionID.put(team, newTCPServer);
	} finally {
	    gameModelLock.unlock();
	}
    }

    public void removeConnection(final Team team) {
	synchronized (connections) {
	    connections.remove(connectionID.get(team));
	    connectionID.remove(team);
	}
    }

    public int getConnectedClients() {
	return connections.size();
    }

    public void disconnectRemainingPlayers() {
	try {
	    for (TCPServer connection : connections) {

		connection.terminate();
	    }
	} catch (IOException e) {
	    serverSessionLogger.log(Level.WARNING, "Found dead connection when attempting to disconnect remaining players!", e);
	    disconnectRemainingPlayers();
	}
    }

    public void killDisconnectedTeam(final Team team) {
        removeConnection(team);
	try {
	    if (getGameModel().playerAlive(team)) {
		getGameModel().defeatTeam(team);
		messageAllClients(Message.teamDefeated(team));
	    }
	    messageAllClients(Message.playerDisconnected(team));
	} catch (InvalidMainMapStateException e) {
	    serverSessionLogger.log(Level.SEVERE, "Serverside mapstate is corrupt, can't recover.", e);
	    Thread.currentThread().interrupt();
	}
    }

    private final class ServerSocketListener implements Runnable
    {
	private ServerSession parentThread;

	private ServerSocketListener(final ServerSession parentThread) {
	    this.parentThread = parentThread;
	}

	@Override public void run() {

	    serverSessionLogger.log(Level.INFO, "ServerSocket has begun listening..");
	    try {
		final String serverID = "SERVER-";
		TCPServer connection = new TCPServer(parentThread, serverID + clientIDCounter);
		connections.add(connection);
		GameResourceManager.instance().executeParallel(connection);
		final int connectedClients = clientIDCounter - 1;
		if (expectedClients == connectedClients) {
		    // We've connected all the players we were supposed to.
		    try {
			GameResourceManager.instance().closeServerSocket();
		    } catch (IOException e) {
		        serverSessionLogger.log(Level.WARNING, "Failed to close ServerSocket cleanly. " +
							       "Resources may be leaking..", e);
		    }
		    serverSessionLogger.log(Level.INFO, "Server is done accepting client connections.");
		}

	    } catch (IOException e) {
		serverSessionLogger.log(Level.WARNING, "Failed to build serverside communication interface. " +
						       "Did the host exit prematurely?", e);
		Thread.currentThread().interrupt();
	    }
	}
    }

    public Team nextClientID() {
	Team team = Team.values()[clientIDCounter];
	clientIDCounter++;
	return team;
    }
}
