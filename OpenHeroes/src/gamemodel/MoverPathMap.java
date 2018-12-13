package gamemodel;

import entity.Mover;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;


/**
 * A more specialized implementation of the PathMap which adds
 * methods for interacting with moving units on the map itself.
 * <p>
 * Introduces and manages the concept of movement points.
 */

public abstract class MoverPathMap extends PathMap implements Serializable
{
    protected Mover[][] unitMap;
    protected int mapWidth;
    protected int mapHeight;

    protected MoverPathMap(final int mapWidth, final int mapHeight) {
	this.mapHeight = mapHeight;
	this.mapWidth = mapWidth;
	this.unitMap = new Mover[mapWidth][mapHeight];
    }

    public boolean unitIsAdjacent(final Mover mover, final Position position) {
	for (int x = -1; x <= 1; x++) {
	    for (int y = -1; y <= 1; y++) {
		Position tentativePosition = mover.getPosition().offset(x, y);
		if (tentativePosition.equals(position)) {
		    return true;
		}
	    }
	}
	return false;
    }

    public int pathMoveCost(final List<Position> path) {
	ListIterator<Position> steps = path.listIterator();
	int cost = 0;
	while (steps.hasNext()) {
	    cost += passCost(steps.next());
	}
	return cost;
    }

    public void moveMapUnit(Mover mover, Position pos) {
	unitMap[pos.getX()][pos.getY()] = mover;
	unitMap[mover.getPosition().getX()][mover.getPosition().getY()] = null;
	mover.setPosition(pos);
    }

    public Deque<Position> legalMoves(final Mover mover, final Deque<Position> path) {
	if (path.isEmpty()) {
	    return path;
	}
	Deque<Position> legalMoves = new LinkedList<>();

	int remainingMoves = mover.getRemainingMoveLength();
	for (Position step : path) {
	    remainingMoves -= passCost(step);
	    if (remainingMoves >= 0) {
		legalMoves.add(step);
	    } else {
		break;
	    }
	}
	return legalMoves;
    }

    public Set<Position> getNeighbors(Position pos) {
	Set<Position> neighbors = new HashSet<>();

	for (int x = -1; x <= 1; x++) {
	    for (int y = -1; y <= 1; y++) {
		if (x == 0 && y == 0) {continue;}
		Position newPosition = pos.offset(x, y);
		neighbors.add(newPosition);
	    }
	}
	return neighbors;
    }

    public int getMapHeight() {
	return mapHeight;
    }

    public int getMapWidth() {
	return mapWidth;
    }
}
