package gamemodel;

import entity.ResourceMine;
import entity.TownFactory;
import entity.TownType;
import gamemodel.GameModel.MainMap;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The GameModelFactory class generates maps until the project progresses far enough that it is possible to
 * edit, save and load maps properly.
 */
/*
 * This class only generates maps. Therefore it uses a lot of sizes
 * and coordinates that aren't really useful to label.
 */
@SuppressWarnings("MagicNumber")
public final class GameModelFactory
{
    private GameModelFactory() {}

    public static GameModel generateGameModel(final int players, final boolean fogOfWar)
	    throws UnsupportedPlayerAmountException, UnknownTownTypeException
    {

	int mapWidth;
	int mapHeight;

	GameModel gameModel;
	MainMap mainMap;

	switch (players) {
	    case 2:
	    case 3:
		mapWidth = 21;
		mapHeight = 20;

		mainMap = new MainMap(mapWidth, mapHeight);
		gameModel = new GameModel(mainMap, players, fogOfWar);

		randomizeTerrain(mainMap.getTerrainMap());

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 1, 9);
		makeSitePassable(mainMap.getTerrainMap(), 1, 9);
		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 17, 9);
		makeSitePassable(mainMap.getTerrainMap(), 17, 9);
		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 9, 9);
		makeSitePassable(mainMap.getTerrainMap(), 9, 9);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 5, 5);
		makeSitePassable(mainMap.getTerrainMap(), 5, 5);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 13, 13);
		makeSitePassable(mainMap.getTerrainMap(), 13, 13);
		break;
	    case 4:
		/*
		 * Intentional fallthrough - More maps would be a luxury within the allotted time.
		 *
		 * The whole class is supposed to be replaced by a more robust map editing
		 * tool combined with a system for selecting and loading maps.
		 */
	    case 5:
	    case 6:
	    case 7:
	    case 8:
		mapWidth = 60;
		mapHeight = 60;

		mainMap = new MainMap(mapWidth, mapHeight);
		gameModel = new GameModel(mainMap, players, fogOfWar);

		randomizeTerrain(mainMap.getTerrainMap());

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 10, 15);
		makeSitePassable(mainMap.getTerrainMap(), 10, 15);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 20, 10);
		makeSitePassable(mainMap.getTerrainMap(), 20, 10);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 30, 5);
		makeSitePassable(mainMap.getTerrainMap(), 30, 5);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 40, 10);
		makeSitePassable(mainMap.getTerrainMap(), 40, 10);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 50, 15);
		makeSitePassable(mainMap.getTerrainMap(), 50, 15);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 25, 25);
		makeSitePassable(mainMap.getTerrainMap(), 25, 25);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 35, 35);
		makeSitePassable(mainMap.getTerrainMap(), 35, 35);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 10, 45);
		makeSitePassable(mainMap.getTerrainMap(), 10, 45);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 20, 50);
		makeSitePassable(mainMap.getTerrainMap(), 20, 50);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 30, 55);
		makeSitePassable(mainMap.getTerrainMap(), 30, 55);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 40, 50);
		makeSitePassable(mainMap.getTerrainMap(), 40, 50);

		gameModel.addFeature(TownFactory.makeTown(TownType.CASTLE), 50, 45);
		makeSitePassable(mainMap.getTerrainMap(), 50, 45);

		gameModel.addFeature(new ResourceMine(Resource.GOLD), 20, 20);
		makeSitePassable(mainMap.getTerrainMap(), 20, 20);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 20, 40);
		makeSitePassable(mainMap.getTerrainMap(), 20, 40);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 56, 30);
		makeSitePassable(mainMap.getTerrainMap(), 56, 30);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 1, 30);
		makeSitePassable(mainMap.getTerrainMap(), 1, 30);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 40, 20);
		makeSitePassable(mainMap.getTerrainMap(), 40, 20);
		gameModel.addFeature(new ResourceMine(Resource.GOLD), 40, 40);
		makeSitePassable(mainMap.getTerrainMap(), 40, 40);
		break;
	    default:
		throw new UnsupportedPlayerAmountException("Invalid amount of players!");
	}

	gameModel.prepareMap();
	return gameModel;
    }

    public static void randomizeTerrain(final TerrainType[][] terrainMap) {
	int width = terrainMap.length;
	int height = terrainMap[0].length;
	for (int x = 0; x < width; x++) {
	    for (int y = 0; y < height; y++) {
		if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
		    terrainMap[x][y] = TerrainType.SEA;
		} else {
		    terrainMap[x][y] = TerrainType.values()[ThreadLocalRandom.current().nextInt(3)];
		}
	    }
	}
    }

    public static void makeSitePassable(final TerrainType[][] terrainMap, final int x, final int y) {
	for (int tx = 0; tx < 3; tx++) {
	    if (terrainMap[tx + x][2 + y].equals(TerrainType.SEA)) {
		terrainMap[tx + x][2 + y] = TerrainType.GRASS;
	    }
	}
    }
}
