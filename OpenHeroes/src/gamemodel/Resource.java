package gamemodel;

/**
 * The resource enum keeps track of resource specific information,
 * including its graphical representations in game.
 */
public enum Resource
{
    /** */
    GOLD(1000, "img/goldmine.png", "img/gold.png"),
    /** */
    MERCURY(5, "img/goldmine.png", "img/mercury.png"),
    /** */
    GEMS(5, "img/goldmine.png", "img/gems.png"),
    /** */
    WOOD(5, "img/goldmine.png", "img/wood.png"),
    /** */
    ORE(5, "img/goldmine.png", "img/ore.png"),
    /** */
    SULFUR(5, "img/goldmine.png", "img/sulfur.png"),
    /** */
    CRYSTAL(5, "img/goldmine.png", "img/crystal.png");

    private final int mineAmountPerTurn;
    private final String resourceMineImagePath;
    private final String resourceImagePath;

    Resource(final int mineAmountPerTurn, final String resourceMineImagePath, final String resourceImagePath) {
	this.mineAmountPerTurn = mineAmountPerTurn;
	this.resourceMineImagePath = resourceMineImagePath;
	this.resourceImagePath = resourceImagePath;
    }

    public int getMineAmountPerTurn() {
	return mineAmountPerTurn;
    }

    public String getResourceMineImagePath() {
	return resourceMineImagePath;
    }

    public String getResourceImagePath() {
	return resourceImagePath;
    }
}
