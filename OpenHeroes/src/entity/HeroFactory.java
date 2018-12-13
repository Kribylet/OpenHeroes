package entity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Heroes are intended to have unique properties such as specialized talents, skills and item inventories. This class would
 * be responsible for generating all the unique properties of the heroes in the full scope of the project.
 *
 * Right now, it sets their max movement points and name.
 */
public final class HeroFactory
{

    private static final int STANDARD_MAX_MOVE = 20;

    private HeroFactory() {}

    private static String[] heroNames =
	    new String[] { "Foulque the Giant", "Gawin the Angel", "Radulf the Twisted", "Christofur the Bruiser",
		    "Gifardus the Loyal", "Geffrey the Cautious", "Remont the Great", "Carac the Wild", "Ulric the Harbinger",
		    "Kit the Hungry" };

    public static Hero makeHero() {
	return new Hero("img/horse.png", STANDARD_MAX_MOVE, heroNames[ThreadLocalRandom.current().nextInt(heroNames.length)]);
    }
}
