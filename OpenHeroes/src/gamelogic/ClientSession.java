package gamelogic;

import connection.ClientListener;
import connection.Message;
import connection.MessageType;
import connection.SynchronizationListener;
import connection.TCPClient;
import entity.Hero;
import entity.Interactable;
import entity.Mover;
import gamemodel.GameModel;
import gamemodel.InvalidMainMapStateException;
import gamemodel.Position;
import gamemodel.Team;
import gamemodel.listeners.GameEvent;
import resources.GameResourceManager;
import resources.SocketGenerationException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static gamemodel.listeners.GameEvent.GameEventType.TURN_START;

/**
 * The primary client thread. The purpose of this class is to maintain the GameModel in a synchronized
 * state and provide the view with relevant information.
 * <p>
 * It uses a TCPListener through a TCPClient to listen for new Messages. When a message comes in, is is
 * assumed to be valid and passed to the GameHandler immediately.
 * <p>
 * A ClientSession may request to synchronize with the ServerSession, but the object transfer itself is
 * handled by the TCPClient class instance generated during construction.
 * <p>
 * What primarily separates a ClientSession from a ServerSession besides not driving game logic is that the
 * ClientSession shares the GameModel with a View, in our case HeroesFrame.
 */
public class ClientSession extends GameHandler implements SynchronizationListener, Runnable
{
    private final static Logger LOGGER = Logger.getLogger(ClientSession.class.getName());
    private final TCPClient tcpClient;
    private final ReentrantLock gameModelLock = new ReentrantLock();

    private List<ClientListener> clientListeners = new ArrayList<>();
    private boolean connectionLost;


    public ClientSession(final String name, final String address, final int portNumber)
	    throws IOException, SocketGenerationException, UnknownHostException, SocketException, SocketTimeoutException,
	    ConnectException
    {
	this.tcpClient = new TCPClient(name, address, portNumber, this);
	tcpClient.addSyncListener(this);
    }

    public boolean move(final Mover hero, final Deque<Position> proposedPath) {
	Deque<Position> legalPath = getGameModel().getMainMap().legalMoves(hero, proposedPath);
	boolean legal = proposedPath.size() == legalPath.size();
	sendMessage(Message.move(hero.getInteractableID()));
	sendPath(legalPath);
	return legal;
    }

    public void addClientListener(ClientListener cl) {
	clientListeners.add(cl);
    }

    @Override public void synchronizeGameModel() {
	gameModelLock.lock();
	try {
	    GameModel newGameModel;
	    newGameModel = readGameModel();

	    if (newGameModel != null) {
		setGameModel(newGameModel);
		notifyListeners();
	    }
	} finally {
	    gameModelLock.unlock();
	}
    }

    @Override public void synchronizeBattlefield() {
	gameModelLock.lock();
	try {
	    getGameModel().setBattlefield(tcpClient.readBattlefield());
	} finally {
	    gameModelLock.unlock();
	}
    }

    public void notifyListeners() {
	for (ClientListener cl : clientListeners) {
	    cl.clientUpdated();
	    getGameModel().notifyGameEventListeners(new GameEvent(TURN_START));
	}
    }

    @Override public Deque<Position> readPath(final Team id) throws IOException {
	return tcpClient.readPath();
    }

    @Override public Hero readHero(final Team id) throws IOException {
    	return tcpClient.readHero();
    }


    public void run() {
	GameResourceManager.instance().executeParallel(tcpClient);

	while (true) {
	    try {
		Message message = messageQueue.take();
		gameModelLock.lock();
		try {
		    LOGGER.log(Level.INFO, "Client got message " + message);
		    synchronized (this) {
			processMessage(message);
			if (getGameModel().isGameOver() && !getGameModel().hasWon()) {
			    tcpClient.sendMessage(Message.stop());
			}
		    }
		    LOGGER.log(Level.INFO, "Client finished handling message.");
		    // Some commands must chain a movement first. The movement changes the GameModel hash,
		    // so we must wait with the actual interaction command until the movement has completed.
		    if (message.getHeader() == MessageType.MOVE_HERO) {
			sendChainMessage();
		    }
		} catch (InvalidMainMapStateException e) {
		    LOGGER.log(Level.WARNING, "ClientSession MainMap is corrupt. Attempting to synchronize..", e);
		    sendMessage(Message.sync());
		} catch (IOException e) {
		    LOGGER.log(Level.WARNING, "Lost connection to the host!", e);
		    closeGameSession();
		} finally {
		    gameModelLock.unlock();
		}
	    } catch (InterruptedException e) {
		LOGGER.log(Level.WARNING, "ClientSession was interrupted!", e);
		return;
	    }
	}
    }

    public void closeGameSession() {
        connectionLost = true;
        notifyListeners();
    }

    public boolean hasLostConnection() {
	return connectionLost;
    }

    private void sendChainMessage() throws IOException {
        tcpClient.sendChainMessage(getGameModel().gameBoardHashCode());
    }

    public void sendMessage(final Message message) {
	try {
	    message.updateHash(getGameModel().gameBoardHashCode());
	    tcpClient.sendMessage(message);
	} catch (IOException e) {
	    LOGGER.log(Level.WARNING, "Lost connection with the host!", e);
	    closeGameSession();
	}
    }

    public void queueChainMessage(final Message message) {
	tcpClient.queueChainMessage(message);
    }

    public void sendPath(final Deque<Position> legalPath) {
	try {
	    tcpClient.sendPath(legalPath);
	} catch (IOException e) {
	    LOGGER.log(Level.WARNING, "Lost connection with the host!", e);
	    closeGameSession();
	}
    }

    public void closeConnection() {
	try {
	    tcpClient.terminate();
	} catch (IOException e) {
	    LOGGER.log(Level.WARNING, "Attempted to terminate connection, but it was already lost!", e);
	}
    }

    protected GameModel readGameModel() {
	    try {
		GameModel newGameModel = (GameModel) tcpClient.readData();
		newGameModel.setCurrentTeam(tcpClient.getConnectedTeam());
		return newGameModel;
	    } catch (IOException e) {
		LOGGER.log(Level.SEVERE, "Failed to read object on client. Unable to recover dataSocket.", e);
		System.exit(ExitCode.CONNECTION.ordinal());
	    } catch (ClassNotFoundException e) {
		LOGGER.log(Level.SEVERE, "Unknown object class read on client. Game version mismatch?", e);
		System.exit(ExitCode.CONNECTION.ordinal());
	    } catch (ClassCastException e) {
		LOGGER.log(Level.WARNING, "Wrong object class read on client. Attempting to synchronize again..", e);
		sendMessage(Message.sync());
		return null;
	    }
	    return null;
    }

    public void buyHero(final Interactable interactable) {
        sendMessage(Message.buyHero(interactable));
    }

    public void openTradeScreen() {
	sendMessage(Message.tradeConcluded());
    }

    public void closeBattleScreen() {
	sendMessage(Message.endBattle());
    }

    public boolean affordsHero() {
    	return getGameModel().affordsHero();
    }

    public void sendInteractMessage(Hero actor, final Interactable target, Deque<Position> proposedPath) {
        Message interaction = target.interaction(actor);
	if (interaction.getHeader() == MessageType.MOVE_HERO) {
	    move(actor, proposedPath);
	    return;
	}
	if (!proposedPath.isEmpty()) {
	    if (move(actor, proposedPath)) {
		queueChainMessage(interaction);
	    }
	} else if (getGameModel().getMainMap().unitCanInteractWith(actor, target)) {
	    sendMessage(interaction);
	}
    }

    public void endTurn() {
	sendMessage(Message.endTurn());
    }

    public void synchronizeClient() {
	sendMessage(Message.sync());
    }

    public void surrender() {
	sendMessage(Message.surrender());
    }
}
