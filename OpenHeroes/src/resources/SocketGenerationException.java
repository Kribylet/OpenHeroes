package resources;

import java.io.IOException;


/**
 * Thrown to indicate the GameResourceManager was not properly initialized before socket requests.
 */
public class SocketGenerationException extends IOException
{
    public SocketGenerationException(final String s) {
	super(s);
    }
}