package connection;

import gamelogic.GameHandler;

import java.io.IOException;

/**
 * MessageHandler class for server-specific messages.
 *
 * Uses subtype-polymorphism to assign the proper method for each message,
 * as an alternative to a Switch statement.
 * */
public class ServerMessageHandler extends MessageManager
{
    private final TCPServer tcpServer;

    public ServerMessageHandler(TCPServer tcpServer, GameHandler session) {
	super(session);
	this.tcpServer = tcpServer;
	buildMessageHandlers();
    }

    private void buildMessageHandlers() {
	messageHandlers.put(MessageType.HANDSHAKE, new ServerHandleHandshakeMessageHandler());
	messageHandlers.put(MessageType.SYNC, new ServerSyncMessageHandler());
	messageHandlers.put(MessageType.STOP, new ServerStopMessageHandler());
	messageHandlers.put(MessageType.STOP_ACK, new ServerStopAckMessageHandler());
	messageHandlers.put(MessageType.CONNECTION_LOST, new ConnectionLostMessageHandler());
    }

    public void handleMessage(final Message message) throws IOException {
	messageHandlers.getOrDefault(message.getHeader(), defaultMessageHandler).handleMessage(message);
    }

    public void sendGameModel() throws IOException {
	synchronized (tcpServer.dataOut) {
	    tcpServer.dataOut.reset();
	    tcpServer.sendMessage(Message.syncSent());
	    tcpServer.dataOut.writeObject(tcpServer.serverSession.getGameModel());
	}
    }

    public void sendBattlefield() throws IOException {
	tcpServer.dataOut.reset();
	tcpServer.sendMessage(Message.getBattlefield());
	tcpServer.dataOut.writeObject(tcpServer.serverSession.getGameModel().getBattlefield());
    }

    private class ServerSyncMessageHandler implements MessageHandler
    {

	public void handleMessage(final Message message) throws IOException {
	    tcpServer.serverMessageHandler.sendGameModel();
	}
    }

    private class ServerStopMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) throws IOException {
	    tcpServer.sendMessage(Message.stopAck());
	    tcpServer.getServerSession().removeConnection(tcpServer.getConnectedTeam());
	}
    }

    private class ServerHandleHandshakeMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) throws IOException {
	    tcpServer.setConnectedTeam(tcpServer.getServerSession().nextClientID());
	    tcpServer.getServerSession().addPlayer(tcpServer, message.getArgs()[0], tcpServer.getConnectedTeam());
	    tcpServer.sendMessage(Message.assignID(tcpServer.getConnectedTeam()));
	    tcpServer.serverMessageHandler.sendGameModel();
	}
    }

    private class ServerStopAckMessageHandler implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    tcpServer.getServerSession().removeConnection(tcpServer.getConnectedTeam());
	}
    }

    private class ConnectionLostMessageHandler implements MessageHandler
    {
	@Override public void handleMessage(final Message message) {
	    message.rebrand(tcpServer.getConnectedTeam());
	    tcpServer.serverSession.addMessage(message);
	}
    }
}