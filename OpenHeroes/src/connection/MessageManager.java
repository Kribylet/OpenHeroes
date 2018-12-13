package connection;

import gamelogic.GameHandler;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** MessageManager provides the base methods necessary to handle messages using
 * subtype polymorphism.
 *
 * This was meant to replace a switch statement, but didn't really reduce complexity.
 * */
public abstract class MessageManager
{
    protected GameHandler gameHandler;
    protected  Map<MessageType, MessageHandler> messageHandlers = new EnumMap<>(MessageType.class);

    protected final MessageHandler defaultMessageHandler = new DefaultMessageManager();

    protected MessageManager(final GameHandler gameHandler) {
	this.gameHandler = gameHandler;
    }

    protected void handleMessage(final Message message) throws IOException {
	messageHandlers.getOrDefault(message.getHeader(), defaultMessageHandler).handleMessage(message);
    }

    private class DefaultMessageManager implements MessageHandler
    {
	public void handleMessage(final Message message) {
	    gameHandler.addMessage(message);
	}
    }
}