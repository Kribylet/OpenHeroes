package entity;


import gamemodel.Resource;
import gamemodel.UnknownTownTypeException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory design pattern for various town types. Woefully underpopulated due to time constraints!
 * <p>
 * The class is meant to add the various TownStructures that towns start with, which in turn
 * determines a lot of the Town in-game functionality.
 */
public final class TownFactory
{
    private static final Logger LOGGER = Logger.getLogger(TownFactory.class.getName());
    private static final int DEFAULT_GOLD_GENERATION = 250;

    private TownFactory() {}

    public static Town makeTown(final TownType type) throws UnknownTownTypeException {
	switch (type) {


	    /* Intentional fail-over until more town types are made */
	    case SHADOW_BOG:
	    case CAVE:
	    case WIZARD_TOWER:
	    case ELVEN_TREE:
	    case HELL_PIT:
	    case CASTLE:
		return castle();
	}
	LOGGER.log(Level.SEVERE, "Unhandled TownType offered to TownFactory.");
	throw new UnknownTownTypeException("Unknown TownType offered to TownFactory!");
    }

    private static Town castle() {
	int[] resourceGeneration = new int[Resource.values().length];
	resourceGeneration[Resource.GOLD.ordinal()] = DEFAULT_GOLD_GENERATION;
	Town town = new Town("img/castle.png", resourceGeneration);
	return town;
    }

}
