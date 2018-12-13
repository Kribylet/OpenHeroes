package connection;

/**
 * Observer type interface used to notify a ClientSession when the
 * GameModel has been synchronized, or a new Battlefield has been sent.
 */
public interface SynchronizationListener
{
    public void synchronizeGameModel();

    public void synchronizeBattlefield();
}
