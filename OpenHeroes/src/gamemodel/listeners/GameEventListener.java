package gamemodel.listeners;

/** Listener interface for View objects interested in GameEvents. */
public interface GameEventListener
{
    public void gameModelChanged(GameEvent e);
}
