package gamemodel;

/**
 * Passability is a property of terrain and interactables. For example, towns allow
 * heroes to stand at their gates.
 * <p>
 * The values here are what the MainMap passes on to the PathFinder class through its
 * passCost method.
 */
public enum Passability
{
    /** */
    IMPASSABLE(Integer.MAX_VALUE),
    /** */
    NORMAL(1),
    /** */
    ROUGH(2);

    private final int moveCost;

    Passability(final int moveCost) {
	this.moveCost = moveCost;
    }

    public int getMoveCost() {
	return moveCost;
    }
}
