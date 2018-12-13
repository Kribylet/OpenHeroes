package connection;

import entity.Hero;
import gamelogic.ExitCode;
import gamemodel.Position;
import gamemodel.Team;
import resources.GameResourceManager;
import resources.SocketGenerationException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MessageProtocol standardizes communication behaviour shared by both client- and serverside interfaces.
 * <p>
 * Socket connections are polled from the GRM during construction, and presupposes that a connection
 * has already been established.
 * See the GameResourceManager documentation for more details on the socket connections.
 * <p>
 * Messages are serialized properly using the Message object structure, but the contents of a Message
 * must be ordered in accordance with the receiver-side decoding of its corresponding MessageType. The
 * overflow arguments to the Message constructor itself imposes no restrictions on argument type or
 * order, and as such the default Message constructor has been made private.
 */
public abstract class MessageProtocol implements Runnable
{
    private final static Logger LOGGER = Logger.getLogger(MessageProtocol.class.getName());
    protected final String playerName;
    protected BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> chainQueue = new LinkedBlockingQueue<>();


    private final ObjectOutputStream dataOut;
    private final DataOutputStream messageOut;
    protected final ObjectInputStream dataIn;
    private final String socketID;

    protected Team connectedTeam = Team.NEUTRAL;

    protected MessageProtocol(final String socketID, final String playerName) throws IOException, SocketGenerationException
    {
        this.playerName = playerName;

	/* The constructor caller is responsible for opening the sockets. */
	this.socketID = socketID;

	/* Socket resource management is left to the GameResourceManager. */
	this.messageOut = GameResourceManager.instance().getMessageOut(socketID);
	this.dataOut = GameResourceManager.instance().getDataOut(socketID);
	this.dataIn = GameResourceManager.instance().getDataIn(socketID);

	final TCPListener listener = new TCPListener();

	/* We listen for new messages asynchronously; subclasses can access this listener
	 * and query it for new messages as a blocking action. */
	GameResourceManager.instance().executeParallel(listener);
    }

    public Team getConnectedTeam() {
	return connectedTeam;
    }

    protected void setConnectedTeam(final Team connectedTeam) {
	this.connectedTeam = connectedTeam;
    }

    public String getSocketID() {
    	return socketID;
    }

    protected Message readMessage() throws InterruptedException {
        return messageQueue.take();
    }


    public void sendMessage(Message message) throws IOException {
	message.rebrand(connectedTeam);
	LOGGER.log(Level.INFO, "Sending message " + message);
	messageOut.writeBytes(message.getHeader() + ";" + connectedTeam + ";" + message.getHash());
	for (String arg : message.getArgs()) {
	    messageOut.writeBytes(";" + arg);
	}
	messageOut.writeBytes("\n");
    }

    public void terminate() throws IOException {
        sendMessage(Message.stop());
    }

    protected void closeConnection() {
	LOGGER.log(Level.INFO, "Closing sockets!");
	GameResourceManager.instance().closeSockets(socketID);
    }

    public void sendPath(Deque<Position> path) throws IOException {
	dataOut.writeObject(path);
    }

    public void sendHero(Hero hero) throws IOException {
    	dataOut.writeObject(hero);
    }

    public Deque<Position> readPath() throws IOException {
	try {
	    /*
	     * Stepped casting for a collection ensures type safety.
	     */
	    Iterable<?> tentativePath = (LinkedList<?>) dataIn.readObject();
	    Deque<Position> path = new LinkedList<>();
	    for (Object tentativePosition : tentativePath) {
		path.add((Position) tentativePosition);
	    }
	    return path;
	} catch (IOException e) {
	    LOGGER.log(Level.SEVERE, "Failed to read path object on server. Unable to recover dataSocket.", e);
	    System.exit(ExitCode.CONNECTION.ordinal());
	} catch (ClassNotFoundException e) {
	    LOGGER.log(Level.SEVERE, "Unknown object type detected. Probably game version mismatch, unable to recover.", e);
	    System.exit(ExitCode.CONNECTION.ordinal());
	} catch (ClassCastException e) {
	    LOGGER.log(Level.WARNING, "Erroneous object type detected. Attempting to synchronize.", e);
	    sendMessage(Message.sync());
	}
	return null;
    }

    public void queueChainMessage(Message message) {
	chainQueue.add(message);
    }

    public void sendChainMessage(final int newHash) throws IOException {
	Message chainMessage = chainQueue.poll();
	if (chainMessage != null) {
	    chainMessage.updateHash(newHash);
	    sendMessage(chainMessage);
	}
    }

    public Hero readHero() throws IOException {
	try {
	    Hero newHero = (Hero) dataIn.readObject();
	    return newHero;
	} catch (IOException e) {
	    LOGGER.log(Level.SEVERE, "Failed to read object on client. Unable to recover dataSocket.", e);
	    System.exit(ExitCode.CONNECTION.ordinal());
	} catch (ClassNotFoundException e) {
	    LOGGER.log(Level.SEVERE, "Unknown object class read on client. Game version mismatch?", e);
	    System.exit(ExitCode.CONNECTION.ordinal());
	} catch (ClassCastException e) {
	    LOGGER.log(Level.WARNING, "Wrong object class read on client. Attempting to synchronize again..", e);
	    sendMessage(Message.sync());
	}
	return null;
    }

    /**
     * The TCPListener class listens for String-formatted Message objects on a
     * BufferedReader.
     * <p>
     * If a line is read, we attempt to parse it as a Message. If successful, we deposit
     * it into a BlockingQueue. The BlockingQueue can then be polled by an underlying
     * TCPServer or TCPClient as a blocking action.
     * <p>
     * If we fail to recognize the read String as a Message, it is simply discarded
     * and the TCPListener resumes listening.
     */
    private final class TCPListener implements Runnable
    {
	private final BufferedReader messageIn;

	private TCPListener() throws IOException, SocketGenerationException {
	    this.messageIn = GameResourceManager.instance().getMessageIn(socketID);
	}

	@Override public void run() {
	    try {
		while (!Thread.interrupted()) {
		    String s = messageIn.readLine();

		    if (s == null) {
			break;
		    }

		    Message m = Message.parseMessage(s);
		    if (m != null) {
			LOGGER.log(Level.FINER, "Adding message on TCPListener interface " + connectedTeam);
			messageQueue.add(m);
			LOGGER.log(Level.FINER, "Successfully added message on TCPListener interface " + connectedTeam);
		    } else {
			LOGGER.log(Level.INFO, "Discarded bad messagetype in TCPListener interface " + connectedTeam);
		    }
		}
	    } catch (IOException e) {

		/*
		 * Because of how BufferedReader is implemented it is not possible to interrupt it normally using
		 * interrupt(). The only way to halt the TCPListener thread once it has entered the readLine()
		 * call is to close the socket providing the underlying stream, which will throw an IOException.
		 */

		LOGGER.log(Level.INFO, "BufferedReader socket was closed suddenly. This is okay at the end of a game.", e);
		Message lostConnectionMessage = Message.connectionLost();
		lostConnectionMessage.rebrand(connectedTeam);
		messageQueue.add(lostConnectionMessage);
		Thread.currentThread().interrupt();
	    }
	}
    }
}
