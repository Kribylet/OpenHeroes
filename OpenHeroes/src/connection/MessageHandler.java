package connection;

import java.io.IOException;


/**
 * Interface for classes that interpret and perform operations according to Message objects.
 *
 * This was fully implemented down to the GameHandler level but caused code duplication and
 * special cases compared to the Switch solution there, so it was ultimately discarded.
 */
public interface MessageHandler
{
    public void handleMessage(final Message message) throws IOException;
}
