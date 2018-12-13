package gamemodel;

import entity.Hero;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Unfortunately there was not enough time to expand this part of the game model.
 * <p>
 * This class is intended to handle army battles between heroes. It extends the
 * MoverPathMap abstract class, meaning we ccould re-use the pathfinding logic from
 * the PathFinder class to navigate this map.
 * <p>
 * In its current state it holds some basic information about two contending heroes and
 * rolls some dice to settle battle outcomes.
 */


public class Battlefield extends MoverPathMap
{
    private static final int BATTLEFIELD_WIDTH = 20;
    private static final int BATTLEFIELD_HEIGHT = 20;
    private int defenderResult = 0;
    private int challengerResult = 0;
    private Hero challenger;
    private Hero defender;


    public Battlefield(final Hero challenger, final Hero defender) {
	super(BATTLEFIELD_WIDTH, BATTLEFIELD_HEIGHT);
	this.challenger = challenger;
	this.defender = defender;

	final int upperdicecap = 101;

	while (defenderResult == challengerResult) {
	    defenderResult = ThreadLocalRandom.current().nextInt(1, upperdicecap);
	    challengerResult = ThreadLocalRandom.current().nextInt(1, upperdicecap);
	}
    }

    @Override public boolean isPassable(final Position pos) {
	return false;
    }

    @Override public int passCost(final Position pos) {
	return 0;
    }

    @Override public int heuristicCostEstimate(final Position start, final Position goal) {
	return 0;
    }

    @Override public Set<Position> getNeighbors(final Position pos) {
	return null;
    }

    public int getDefenderResult() {
	return defenderResult;
    }

    public int getChallengerResult() {
	return challengerResult;
    }

    public Hero getChallenger() {
	return challenger;
    }

    public Hero getDefender() {
	return defender;
    }
}
