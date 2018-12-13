package gamemodel;

import java.io.Serializable;
import java.util.Objects;

/**
 * The position class holds a coordinate tuple and offers some convenience methods for working on coordinates.
 */
public class Position implements Serializable
{
    private int x;
    private int y;

    public Position(final int x, final int y) {
	this.x = x;
	this.y = y;
    }

    @Override public boolean equals(final Object o) {
	if (this == o) return true;
	if (o == null || !Objects.equals(getClass(), o.getClass())) return false;
	final Position position = (Position) o;
	return x == position.x && y == position.y;
    }

    public Position offset(final int x, final int y) {
	return new Position(this.x + x, this.y + y);
    }

    public Position add(final Position pos) {
	return new Position(this.x + pos.x, this.y + pos.y);
    }

    @Override public String toString() {
	return "Position{" + "x=" + x + ", y=" + y + '}';
    }


    // Fast alternative to .equals() for use in pathfinding.
    public boolean is(Position pos) {
        return this.x == pos.x && this.y == pos.y;
    }

    public int getX() {
	return x;
    }

    public int getY() {
	return y;
    }
}
