package connection;


import gamelogic.ServerSession;
import resources.GameResourceManager;
import resources.SocketGenerationException;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles serverside communication. Large parts of the functionality is
 * implemented through its superclass MessageProtocol.
 * <p>
 * A ServerSession spawns a TCPServer instance for each client that is expected
 * to connect. When a client connects the TCPServer performs an initial
 * "handshake" where the client is assigned an ID for future communication.
 * <p>
 * The TCPServer then waits for its TCPListener to provide it a Message through
 * a BlockingQueue.
 *
 * The TCPListener is instantiated by the MessageProtocol superconstructor.
 * <p>
 * If the message is network related, the TCPServer handles it immediately and
 * resumes waiting for Messages. Otherwise, the Message is passed to its underlying
 * ServerSession through another BlockingQueue.
 * <p>
 * If necessary the ServerSession can use a clients assigned TCPServer to
 * transfer data, such as a copy of the ServerSession GameModel.
 *
 * The TCPServer uses subtype polymorphism to handle the messages.
 * See the ServerMessageHandler class for more specifics.
 */
public class TCPServer extends MessageProtocol
{
    private final static Logger LOGGER = Logger.getLogger(TCPServer.class.getName());
    protected final ServerSession serverSession;
    protected final ObjectOutputStream dataOut;
    protected final ServerMessageHandler serverMessageHandler;

    public TCPServer(final ServerSession serverSession, final String connectionID)
	    throws IOException, SocketGenerationException
    {
	super(GameResourceManager.instance().acceptNewConnection(), connectionID);
	this.serverSession = serverSession;
	this.dataOut = GameResourceManager.instance().getDataOut(getSocketID());
	serverMessageHandler = new ServerMessageHandler(this, this.serverSession);
    }

    @Override public void run() {
	try {
	    while (!Thread.interrupted()) {
	        serverMessageHandler.handleMessage(readMessage());
	    }
	} catch (InterruptedException e) {
	    LOGGER.log(Level.INFO, "TCPServer thread was interrupted.", e);
	    serverSession.killDisconnectedTeam(connectedTeam);
	} catch (IOException e) {
	    LOGGER.log(Level.WARNING, "Lost connection to the client!", e);
	    serverSession.killDisconnectedTeam(connectedTeam);
	}

    }

    public ServerSession getServerSession() {
	return serverSession;
    }

    public void sendGameModel() throws IOException {
	serverMessageHandler.sendGameModel();
    }

    public void sendBattlefield() throws IOException {
	serverMessageHandler.sendBattlefield();
    }
}
