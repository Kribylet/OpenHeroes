package connection;

/**
 * Interface for listening to client data changes.
 * <p>
 * Implemented by the HeroesFrame to remap listeners onto the GameModel when it is synchronized, and notify
 * clients of lost connections.
 */
public interface ClientListener
{
    public void clientUpdated();
}
