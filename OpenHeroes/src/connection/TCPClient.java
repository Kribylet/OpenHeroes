package connection;


import gamelogic.ClientSession;
import gamelogic.ExitCode;
import gamemodel.Battlefield;
import resources.GameResourceManager;
import resources.SocketGenerationException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles clientside communication. Large parts of the functionality is implemented through the superclass MessageProtocol.
 * <p>
 * A TCPClient is instantiated by a ClientSession when it first attempts to connect to a host. The TCPClient performs
 * an initial handshake with the server and then proceeds to wait for messages through its TCPListener via BlockingQueue.
 *
 * Note that this TCPListener is actually created by the MessageProtocol superconstructor.
 * <p>
 * If a message it network related the TCPClient class handles it directly, otherwise it delegates to the
 * ClientSession through a BlockingQueue.
 *
 * See the ClientMessageHandler class for specifics on the message handling.
 */
public class TCPClient extends MessageProtocol
{
    private final static Logger LOGGER = Logger.getLogger(TCPClient.class.getName());
    private final ClientMessageHandler clientMessageHandler;

    private List<SynchronizationListener> syncListeners = new ArrayList<>();
    private ClientSession clientSession;

    public TCPClient(final String playerName, final String address, final int portNumber, final ClientSession clientSession)
	    throws IOException, SocketGenerationException, UnknownHostException, SocketException, SocketTimeoutException,
	    ConnectException
    {
	super(GameResourceManager.instance().requestNewConnection(address, portNumber), playerName);
	this.clientSession = clientSession;
	clientMessageHandler = new ClientMessageHandler(this, this.clientSession);
    }

    public void run() {
	try {
	    while (!Thread.interrupted()) {
		clientMessageHandler.handleMessage(readMessage());
	    }
	} catch (InterruptedException e) {
	    LOGGER.log(Level.WARNING,
		       "Client connection thread was interrupted. This should only occur at the end of the game.", e);
	    Thread.currentThread().interrupt();
	} catch (IOException e) {
	    LOGGER.log(Level.WARNING, "Lost connection to the host!", e);
	    clientSession.closeGameSession();
	}
    }



    public void notifyNewGameModel() {
	for (SynchronizationListener sl : syncListeners) {
	    sl.synchronizeGameModel();
	}
    }

    public void notifyNewBattlefield() {
	for (SynchronizationListener sl : syncListeners) {
	    sl.synchronizeBattlefield();
	}
    }

    public void addSyncListener(SynchronizationListener sl) {
	syncListeners.add(sl);
    }

    public Object readData() throws IOException, ClassNotFoundException {
        return dataIn.readObject();
    }

    public ClientSession getClientSession() {
	return clientSession;
    }

    public Battlefield readBattlefield() {
	    try {
		Battlefield newBattleField = (Battlefield) readData();
		return newBattleField;
	    } catch (IOException e) {
		LOGGER.log(Level.SEVERE, "Failed to read object on client. Unable to recover dataSocket.", e);
		System.exit(ExitCode.CONNECTION.ordinal());
	    } catch (ClassNotFoundException e) {
		LOGGER.log(Level.SEVERE, "Unknown object class read on client. Game version mismatch?", e);
		System.exit(ExitCode.CONNECTION.ordinal());
	    }
	    return null;
    }
}
