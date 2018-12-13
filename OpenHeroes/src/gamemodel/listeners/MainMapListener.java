package gamemodel.listeners;

/**
 * Listener interface for objects that need to know about changes to the main map.
 */
public interface MainMapListener
{
    public void mapChanged(MainMapEvent e);
}


