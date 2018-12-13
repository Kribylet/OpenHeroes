package entity;

import connection.Message;
import gamemodel.Passability;
import gamemodel.Position;
import gamemodel.Team;

import java.util.Objects;

/**
 * Heroes are the primary agents of the game. In the full scope of the project they contain information
 * regarding owned battlefield units, artifacts, skills, experience and more.
 * <p>
 * In the current implementation, Hero objects are the only ones that can move on the MainMap
 * and interact with objects there.
 */
public class Hero extends Mover
{

    private String imagePath;
    private String heroName;

    public Hero(final String imagePath, final int maxMoveLength, final String heroName) {
	super(imagePath, new Passability[][] { { Passability.IMPASSABLE } }, new Position(0, 0), maxMoveLength);
	this.heroName = heroName;
	this.imagePath = imagePath;
    }

    public String getHeroName() {
	return heroName;
    }

    @Override public InteractionType leftClick(final Team team) {
	if (isFriendly(team)) {
	    return InteractionType.SELECT;
	}
	return InteractionType.NONE;
    }

    @Override public Message interaction(Interactable other) {
	if (isFriendly(other)) {
	    return Message.heroTrade(other.getInteractableID(), this.getInteractableID());
	} else {
	    return Message.heroBattle(other.getInteractableID(), this.getInteractableID());
	}
    }

    @Override public boolean equals(final Object o) {
	if (this == o) return true;
	if (o == null || !Objects.equals(getClass(), o.getClass())) return false;
	final Mover hero = (Mover) o;
	return Objects.equals(getInteractableID(), hero.getInteractableID());
    }

    @Override public int hashCode() {

	int result = super.hashCode();
	result = 31 * result + imagePath.length() * heroName.length() * remainingMoveLength;
	return result;
    }

    @Override public String toString() {
	return heroName;
    }
}


