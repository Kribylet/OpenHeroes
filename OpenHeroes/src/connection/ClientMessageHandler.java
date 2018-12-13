package connection;

import gamelogic.GameHandler;
import gamemodel.Team;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Message Handler class for client-specific messages.
 *
 * Uses subtype-polymorphism to assign the proper method for each message,
 * as an alternative to a Switch statement.
 *
 * Didn't really turn out better than a simple Switch statement in this
 * case, unfortunately!
 *
 * See the project description for some notes on it.
 * */
public class ClientMessageHandler extends MessageManager
{
    private final TCPClient tcpClient;
    private final static Logger LOGGER = Logger.getLogger(ClientMessageHandler.class.getName());

    public ClientMessageHandler(TCPClient tcpClient, GameHandler session) throws IOException
    {
        super(session);
        this.tcpClient = tcpClient;
	buildMessageHandlers();
	tcpClient.sendMessage(Message.handshake(tcpClient.playerName));
    }

    private void buildMessageHandlers() {
	messageHandlers.put(MessageType.CONNECTION_LOST, new ConnectionLostMessageHandler());
	messageHandlers.put(MessageType.STOP_ACK, new StopAckMessageHandler());
	messageHandlers.put(MessageType.GET_BATTLEFIELD, new GetBattlefieldMessageHandler());
	messageHandlers.put(MessageType.STOP, new StopMessageHandler());
	messageHandlers.put(MessageType.SYNC_SENT, new SyncSentMessageHandler());
	messageHandlers.put(MessageType.ASSIGN_ID, new AssignIDMessageHandler());
    }

    private class ConnectionLostMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    tcpClient.getClientSession().closeGameSession();
	}
    }

    private class StopAckMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    tcpClient.closeConnection();
	    Thread.currentThread().interrupt();
	}
    }

    private class GetBattlefieldMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    tcpClient.notifyNewBattlefield();
	}
    }

    private class StopMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) throws IOException {
	    tcpClient.sendMessage(Message.stopAck());
	    tcpClient.closeConnection();
	    Thread.currentThread().interrupt();
	}
    }

    private class SyncSentMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    tcpClient.notifyNewGameModel();
	}
    }

    private class AssignIDMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    Team clientID = Team.valueOf(message.getArgs()[0]);
	    tcpClient.setConnectedTeam(clientID);
	    LOGGER.log(Level.FINE, "Was assigned new connection ID: " + tcpClient.getConnectedTeam());
	}
    }
}