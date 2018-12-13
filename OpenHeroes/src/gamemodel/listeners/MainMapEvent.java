package gamemodel.listeners;

import entity.Mover;

import static gamemodel.listeners.MainMapEvent.MapEventType.NONE;

/**
 * MainMapEvents are simple objects containing View-specific information
 * regarding the main map, meant to be sent to a MainMapListener.
 */
public class MainMapEvent
{
    private Mover mover;
    private MapEventType type;

    public MainMapEvent(final Mover mover, final MapEventType type) {
	this.mover = mover;
	this.type = type;
    }

    public MainMapEvent() {
	mover = null;
        this.type = NONE;
    }

    public Mover getMover() {
	return mover;
    }

    public MapEventType getType() {
	return type;
    }

    public enum MapEventType
    {
        /** A generic change of the main map, typically something requiring a repaint() call. */
	NONE,
	/** A mover has performed an action deserving focus. */
	HERO_MOVED,
	/** A mover was killed. */
	HERO_KILLED
    }

}
