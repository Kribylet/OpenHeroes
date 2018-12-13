package gamemodel;

import entity.Hero;
import entity.HeroFactory;
import entity.Interactable;
import entity.Mover;
import entity.ResourceMine;
import entity.Town;
import gamemodel.listeners.GameEvent;
import gamemodel.listeners.GameEvent.GameEventType;
import gamemodel.listeners.GameEventListener;
import gamemodel.listeners.MainMapEvent;
import gamemodel.listeners.MainMapEvent.MapEventType;
import gamemodel.listeners.MainMapListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.abs;

/**
 * The core of the game logic. The game model itself focuses on game state specifics such as
 * if the game has started, whose turn it is and how many players are left in the game.
 * <p>
 * Data specifics about player team properties are left to the MainMap class.
 * <p>
 * An example of one of the GameModel's responsibilities would be the context switch from
 * the game running to the game over screen, or the proper generation of a new battlefield that
 * two Heroes battle on.
 * <p>
 * Of note is that the game events are driven by the server gameHandler, not the model itself.
 */
public class GameModel implements Serializable
{
    private static final Logger LOGGER = Logger.getLogger(GameModel.class.getName());
    private static final int HERO_GOLD_COST = 500;
    private static final int TEAM_STARTING_GOLD = 500;
    private final List<GameEventListener> gameEventListeners = new ArrayList<>();
    private final List<Town> towns = new ArrayList<>();
    private final List<Hero> heroes = new ArrayList<>();
    private final List<ResourceMine> mines = new ArrayList<>();
    private final List<Team> turnOrder = new ArrayList<>();
    private final Map<Team, String> playerNames = new EnumMap<>(Team.class);
    private final Map<Team, int[]> resourceLists = new EnumMap<>(Team.class);
    private final Map<Team, boolean[][]> fogsOfWar = new EnumMap<>(Team.class);
    private final Team[] teams;
    private GameState gameState = GameState.MAIN_MAP;
    private Team currentTeam = null;
    private MainMap mainMap;
    private Battlefield battlefield = null;

    private boolean gameStarted = false;
    private int turnTaker = 0;
    private int playerCounter = 0;
    private boolean won;
    private final int humanPlayers;


    public GameModel(final MainMap mainMap, final int humanPlayers, final boolean fog) {
	this.mainMap = mainMap;
	this.humanPlayers = humanPlayers;
	teams = new Team[humanPlayers];

	for (int id = 0; id < teams.length; id++) {
	    teams[id] = Team.values()[id];

	    resourceLists.put(Team.values()[id], new int[Resource.values().length]);
	    resourceLists.get(Team.values()[id])[Resource.GOLD.ordinal()] = TEAM_STARTING_GOLD;

	    turnOrder.add(teams[id]);
	}

	for (int teamID = 0; teamID < humanPlayers; teamID++) {
	    fogsOfWar.put(Team.values()[teamID], newFogOfWar(fog));
	}
    }

    private boolean[][] newFogOfWar(final boolean fog) {
        int mapWidth = mainMap.getMapWidth();
        int mapHeight = mainMap.getMapHeight();
	boolean[][] fow = new boolean[mapWidth][mapHeight];
	for (int x = 0; x < mapWidth; x++) {
	    for (int y = 0; y < mapHeight; y++) {
		fow[x][y] = fog;
	    }
	}
	return fow;
    }

    public void moveHero(Mover mover, Deque<Position> movePath) throws InvalidMainMapStateException {
	if (movePath == null || mover == null) {
	    LOGGER.log(Level.WARNING, "Severe desync issue when moving hero!");
	    throw new InvalidMainMapStateException("Error when moving hero: movePath = " + movePath + " hero: " + mover);
	}
	int remainingMove = mover.getRemainingMoveLength();

	for (Position pos : movePath) {
	    mainMap.moveMapUnit(mover, pos);
	    explorePosition(mover);
	    remainingMove -= mainMap.passCost(pos);
	    mover.setRemainingMoveLength(remainingMove);
	    mainMap.notifyMainMapListeners(new MainMapEvent(mover, MapEventType.HERO_MOVED));
	}
    }

    public void explorePosition(final Mover mover) {
	boolean[][] fogOfWar = fogsOfWar.get(mover.getOwner());
	int sightRange = 3;
	int posx = mover.getPosition().getX();
	int posy = mover.getPosition().getY();
	int exploreLeftBound = Integer.max(posx - sightRange, 0);
	int exploreRightBound = Integer.min(posx + sightRange, fogOfWar.length - 1);
	int exploreTopBound = Integer.max(posy - sightRange, 0);
	int exploreBottomBound = Integer.min(posy + sightRange, fogOfWar[0].length - 1);
	for (int x = exploreLeftBound; x <= exploreRightBound; x++) {
	    for (int y = exploreTopBound; y <= exploreBottomBound; y++) {
		fogOfWar[x][y] = false;
	    }
	}
    }

    public GameState getGameState() {
	return gameState;
    }

    public void addPlayer(Team team, String playerID) {
	teams[playerCounter] = team;
	playerNames.put(team, playerID);
	playerCounter++;
	if (playerCounter == humanPlayers) {startGame();}
    }

    public void startGame() {
	gameStarted = true;
    }

    protected void prepareMap() {
	Collections.shuffle(turnOrder);

	for (Team team : teams) {
	    Town randomTown = getRandomNeutralTown();
	    randomTown.setOwner(team);
	    Hero hero = HeroFactory.makeHero();
	    giveHero(team, hero, randomTown);
	    hero = HeroFactory.makeHero();
	    giveHero(team, hero, randomTown);
	}
	for (Hero hero : heroes
	     ) {
	    explorePosition(hero);
	}
    }

    public void addUnit(final Mover mover) {
    	Position pos = mover.getPosition();
    	mainMap.unitMap[pos.getX()][pos.getY()] = mover;
    }
        public boolean unitOccupies(final Position pos) {
    	return mainMap.unitMap[pos.getX()][pos.getY()] != null;
    }

    public void giveHero(final Team team, final Hero hero, final Town town) {
	if (unitOccupies(town.getInteractionPoint())) {
	    hero.setPosition(town.getInteractionPoint().offset(0, 1)); // Not allowed in gameplay, just for testing
	} else {
	    hero.setPosition(town.getInteractionPoint());
	}
	hero.setOwner(team);
	addUnit(hero);
	heroes.add(hero);
	mainMap.notifyMainMapListeners();
    }

    public void buyHero(final Team team, final Hero hero, final Town town) throws InvalidMainMapStateException {
	giveHero(team, hero, town);
	applyCost(team, Resource.GOLD, HERO_GOLD_COST);
	mainMap.notifyMainMapListeners();
    }

    public void applyCost(final Team team, final Resource resource, final int amount) throws InvalidMainMapStateException
    {
	int[] resourceList = resourceLists.get(team);
	resourceList[resource.ordinal()] -= amount;
	if (resourceList[resource.ordinal()] < 0) {
	    resourceList[resource.ordinal()] += amount;
	    throw new InvalidMainMapStateException("Attempted to spend more resources than the player has!");
	}
    }

    public void addFeature(final Town town, final int x, final int y) {
	drawFeatureOnMap(town, x, y);
	town.setPosition(new Position(x, y));
    	towns.add(town);
    }

    public void addFeature(final ResourceMine mine, final int x, final int y) {
	drawFeatureOnMap(mine, x, y);
	mine.setPosition(new Position(x, y));
	mines.add(mine);
    }

    private void drawFeatureOnMap(final Interactable interactable, final int x, final int y) {
	for (int featureX = 0; featureX < interactable.getWidth(); featureX++) {
	    for (int featureY = 0; featureY < interactable.getHeight(); featureY++) {
		mainMap.getFeatureMap()[x + featureX][y + featureY] = interactable;
	    }
	}
    }

    public void purgeTeam(final Team team) {

	neutralize(team, towns);
	neutralize(team, mines);

	// Special case because we need to actually remove
	// the hero entities on death.

	// Would cause concurrency modifications exceptions
	// if .remove() was called immediately.
	List<Hero> defeatedHeroes = new ArrayList<>();
	for (final Hero hero : heroes) {
	    if (hero.getOwner().equals(team)) {
		mainMap.clearUnitMap(hero.getPosition());
		defeatedHeroes.add(hero);
	    }
	}
	heroes.removeAll(defeatedHeroes);
    }

    private void neutralize(final Team team, List<? extends Interactable> properties) {
	for (Interactable interactable: properties
	     ) {
	    if (interactable.getOwner().equals(team)) {
	        interactable.setOwner(Team.NEUTRAL);
	    }
	}
    }

    public boolean playerHasHeroes() {
	for (Hero hero: heroes
	     ) {
	    if (hero.getOwner().equals(turnOrder.get(turnTaker))) {
	        return true;
	    }
	}
	return false;
    }

    public Team getTurnTaker() {
	return turnOrder.get(turnTaker);
    }

    public void setCurrentTeam(final Team team) {
	this.currentTeam = team;
	mainMap.setFogOfWar(fogsOfWar.get(team));
    }

    public boolean onTurn() {
	if (!gameStarted) {
	    return false;
	}
	return turnOrder.get(turnTaker).equals(currentTeam);
    }

    public void endTurn() {
	if (turnTaker == turnOrder.size() - 1) {
	    turnTaker = 0;
	    generateResources();
	} else {
	    turnTaker += 1;
	}

	if (onTurn()) {
	    notifyGameEventListeners(new GameEvent(GameEventType.TURN_START));
	}
	refreshHeroMoves(turnOrder.get(turnTaker));
    }

    public int getHumanPlayers() {
	return humanPlayers;
    }

    @Override public boolean equals(final Object o) {
	if (this == o) return true;
	if (o == null || !Objects.equals(getClass(), o.getClass())) return false;
	final GameModel gameModel = (GameModel) o;
	return Objects.equals(mainMap, gameModel.mainMap);
    }

    public int gameBoardHashCode() {
	int hash = mainMap.mainMapHashCode();
	return hash;
    }

    public MainMap getMainMap() {
	return mainMap;
    }

    public Map<Team, String> getPlayerNames() {
	return playerNames;
    }

    public void addGameModelListeners(GameEventListener gml) {
	gameEventListeners.add(gml);
    }

    public void notifyGameEventListeners() {
	for (GameEventListener gml : gameEventListeners) {
	    gml.gameModelChanged(new GameEvent());
	}
    }

    public void notifyGameEventListeners(GameEvent e) {
	for (GameEventListener gml : gameEventListeners) {
	    gml.gameModelChanged(e);
	}
    }

    public void defeatTeam(final Team team) throws InvalidMainMapStateException {
	int defeatedPlayerIndex = turnOrder.indexOf(team);
	if (defeatedPlayerIndex == -1) {
	    throw new InvalidMainMapStateException("Attempted to remove non existant player!");
	}
	if (turnTaker == defeatedPlayerIndex) {
	    endTurn();
	}
	Team currentPlayer = turnOrder.get(turnTaker);
	//Shrinks the ArrayList.
	turnOrder.subList(defeatedPlayerIndex, defeatedPlayerIndex + 1).clear();

	turnTaker = turnOrder.indexOf(currentPlayer);

	purgeTeam(team);

	if (team.equals(currentTeam)) {
	    gameState = GameState.GAME_OVER;
	} else if (turnOrder.size() == 1)
	{
	    won = true;
	    gameState = GameState.GAME_OVER;
	}
	notifyGameEventListeners(new GameEvent(team, GameEventType.DEFEAT_NOTIFICATION));
    }

    public void tradeConcluded() {
	gameState = GameState.MAIN_MAP;
	notifyGameEventListeners();
    }

    public void battleConcluded() {
	gameState = GameState.MAIN_MAP;
	notifyGameEventListeners();
    }

    public void startBattle() {
	gameState = GameState.BATTLEFIELD;
	notifyGameEventListeners(new GameEvent(GameEventType.BATTLE_START));
    }

    public void trade() {
	gameState = GameState.TRADE;
	notifyGameEventListeners();
    }

    public Battlefield getBattlefield() {
	return battlefield;
    }

    public void setBattlefield(final Battlefield battlefield) {
	this.battlefield = battlefield;
    }

    public void createBattlefield(final Hero challenger, final Hero defender) {
	this.battlefield = new Battlefield(challenger, defender);
    }

    public boolean gameStarted() {
	return gameStarted;
    }

    public boolean hasWon() {
	return won;
    }

    public boolean isGameOver() {
	return gameState == GameState.GAME_OVER;
    }

    public void checkDefeat() throws InvalidMainMapStateException {
	Team defeatedTeam = null;
	for (Team team : turnOrder) {
	    if (playerLostAllTowns(team)) {
		defeatedTeam = team;
	    }
	}
	if (defeatedTeam != null) {
	    defeatTeam(defeatedTeam);
	}
    }

    public void killHero(Hero hero) {
	mainMap.unitMap[hero.getPosition().getX()][hero.getPosition().getY()] = null;
	heroes.remove(hero);
	mainMap.notifyMainMapListeners(new MainMapEvent(hero, MapEventType.HERO_KILLED));
    }

    public Hero getHeroByID(String interactableID) throws InvalidMainMapStateException {
	for (Hero hero : heroes) {
	    if (hero.getInteractableID().equals(interactableID)) {
		return hero;
	    }
	}
	throw new InvalidMainMapStateException("Searched for non existant hero!");
    }

    public Town getTownByID(String interactableID) throws InvalidMainMapStateException {
	for (Town town : towns) {
	    if (town.getInteractableID().equals(interactableID)) {
		return town;
	    }
	}
	throw new InvalidMainMapStateException("Searched for non existant town!");
    }

    public ResourceMine getResourceMineByID(String interactableID) throws InvalidMainMapStateException {
	for (ResourceMine mine : mines) {
	    if (mine.getInteractableID().equals(interactableID)) {
		return mine;
	    }
	}
	throw new InvalidMainMapStateException("Searched for non existant hero!");
    }

    public void refreshHeroMoves(Team turnTaker) {
    	for (Hero hero : heroes) {
	    if (hero.getOwner().equals(turnTaker)) {
		hero.refreshMove();
	    }
    	}
    	mainMap.notifyMainMapListeners();
    }

    public boolean playerLostAllTowns(final Team team) {
	for (Town town : towns
	     ) {
	    if (town.getOwner().equals(team)) {
	        return false;
	    }
	}
	return true;
    }

    public Town getRandomNeutralTown() {
	List<Town> neutralTowns = new ArrayList<>();
        for (Town town : towns
	     ) {
	    if (town.getOwner().equals(Team.NEUTRAL)) {
	        neutralTowns.add(town);
	    }
	}
        return neutralTowns.get(ThreadLocalRandom.current().nextInt(neutralTowns.size()));
    }

    public List<Hero> getOwnedHeroes() {
	List<Hero> ownedHeroes = new ArrayList<>();
	for (Hero hero : heroes
	     ) {
	    if (hero.getOwner().equals(turnOrder.get(turnTaker))) {
		ownedHeroes.add(hero);
	    }
	}
	return ownedHeroes;
    }

    public void generateResources() {

	for (Town town : towns
	     ) {
	    if (town.getOwner() != Team.NEUTRAL) {
		town.addResources(resourceLists.get(town.getOwner()));
	    }
	}

	for (ResourceMine mine : mines
	     ) {
	    if (mine.getOwner() != Team.NEUTRAL) {
		mine.addResources(resourceLists.get(mine.getOwner()));
	    }
	}
    }


    public LinkedList<Position> findPath(final Position moverPosition, final Position goal) {
        return mainMap.findPath(moverPosition, goal);
    }

    public boolean playerAlive(final Team team) {
        return (turnOrder.size() > 1 && turnOrder.contains(team));
    }

    public boolean affordsHero() {
	return resourceLists.get(getTurnTaker())[Resource.GOLD.ordinal()] >= HERO_GOLD_COST;
    }

    public List<Hero> getHeroes() {
	return heroes;
    }

    public List<Town> getTowns() {
	return towns;
    }

    public List<ResourceMine> getMines() {
	return mines;
    }

    public int[] getClientResourceList() {
	return resourceLists.get(currentTeam);
    }

    public Team getCurrentTeam() {
	return currentTeam;
    }

    /**
     * The main game board.  The main map handles the game entities entering and leaving
     * the game and is queried by the GameModel as necessary.
     * <p>
     * It relies on a lot of functionality from the MoverPathMap, wrapping
     * it in places to accommodate the tasks specific to the main map display.
     * <p>
     * The MainMap specific features are:
     * <p>
     * TerrainMap: The underlying terrain of the map. A candidate for extraction into
     * the MoverPathMap class, but the intention is to use other graphics for
     * BattleField map, so another arraytype may be better.
     * <p>
     * FeatureMap: Main map features include towns, mines and any other kind of interactable
     * ground feature. A feature does not necessarily prevent Units from standing on them,
     * so they exist as a separate layer.
     *
     * Information utility methods regarding adjacencies.
     */
    public static class MainMap extends MoverPathMap
    {

	private TerrainType[][] terrainMap;
	private Interactable[][] featureMap;
	private boolean[][] fogOfWar = null;

	private List<MainMapListener> mainMapListeners = new ArrayList<>();


	public MainMap(final int mapWidth, final int mapHeight) {
	    super(mapWidth, mapHeight);
	    terrainMap = new TerrainType[mapWidth][mapHeight];
	    featureMap = new Interactable[mapWidth][mapHeight];
	}



	public Iterable<Position> passableInteractionPositions(final Interactable interactable) {
	    List<Position> validPositions = new ArrayList<>();
	    Position interactablePos = interactable.getInteractionPoint();
	    if (interactable.interactionPointIsPassable()) {
		validPositions.add(interactablePos);
		return validPositions;
	    } else {
		for (int x = -1; x < 2; x++) {
		    for (int y = -1; y < 2; y++) {
			Position tentativePosition = interactablePos.offset(x, y);
			if (isPassable(tentativePosition)) {
			    validPositions.add(tentativePosition);
			}
		    }
		}
	    }
	    return validPositions;
	}

	public void setFogOfWar(final boolean[][] fogOfWar) {
	    this.fogOfWar = fogOfWar;
	}

	public boolean playerSeesMapTile(final int x, final int y) {
	    return !fogOfWar[x][y];
	}

	public boolean playerSeesMapTile(final Position pos) {
	    return playerSeesMapTile(pos.getX(), pos.getY());
	}

	public boolean isPassable(final Position pos) {
	    int x = pos.getX();
	    int y = pos.getY();
	    if (x > mapWidth - 1 || x < 0 || pos.getY() > mapHeight - 1 || y < 0) {
		return false;
	    }

	    if (!playerSeesMapTile(pos)) {
		return false;
	    }

	    Interactable interactable = getTopLevelInteractable(x, y);

	    if (interactable != null) {
		int relativeX = x - interactable.getPosition().getX();
		int relativeY = y - interactable.getPosition().getY();
		return interactable.getPassabilityMap()[relativeX][relativeY] != Passability.IMPASSABLE;
	    } else {
		return terrainMap[x][y].getPassability() != Passability.IMPASSABLE;
	    }
	}

	public int passCost(Position pos) {
	    Interactable feature = featureMap[pos.getX()][pos.getY()];
	    if (feature != null) {
		int relativeX = pos.getX() - feature.getPosition().getX();
		int relativeY = pos.getY() - feature.getPosition().getY();
		return feature.getPassabilityMap()[relativeX][relativeY].getMoveCost();
	    }
	    return terrainMap[pos.getX()][pos.getY()].getPassability().getMoveCost();
	}

	public Interactable getTopLevelInteractable(final int x, final int y) {
	    Interactable topLevelInteractable = unitMap[x][y];
	    if (topLevelInteractable != null) {
		return topLevelInteractable;
	    }
	    topLevelInteractable = featureMap[x][y];
	    return topLevelInteractable; // null result means none!
	}

	public Interactable getTopLevelInteractable(final Position pos) {
	    if (pos == null) {
		return null;
	    }
	    return getTopLevelInteractable(pos.getX(), pos.getY());
	}

	public TerrainType[][] getTerrainMap() {
	    return terrainMap;
	}

	public Interactable[][] getFeatureMap() {
	    return featureMap;
	}

	/*
	 * This hashcode is used by the GameModel hashcode method to ensure
	 * the MainMap attributes are part of the serverside validity check.
	 * */
	public int mainMapHashCode() {
	    int hash = 0;
	    final int goodPrime = 31;
	    for (int x = 0; x < mapWidth; x++) {
		for (int y = 0; y < mapHeight; y++) {
		    hash += terrainMap[x][y].ordinal();
		    if (unitMap[x][y] != null) {
			int heroID = Integer.parseInt(unitMap[x][y].getInteractableID());
			hash += x * heroID * goodPrime;
			hash += y * heroID * 3;
		    } else {
			hash -= 5 * x * y;
		    }
		}
	    }
	    return hash;
	}

	public void addMainMapListener(MainMapListener mmL) {
	    mainMapListeners.add(mmL);
	}

	public void notifyMainMapListeners() {
	    for (MainMapListener mmL : mainMapListeners) {
		mmL.mapChanged(new MainMapEvent());

	    }
	}

	public void notifyMainMapListeners(final MainMapEvent e) {
	    for (MainMapListener mmL : mainMapListeners) {
		mmL.mapChanged(e);

	    }
	}

	public boolean unitCanInteractWith(final Mover mover, final Interactable target) {
	    if (isPassable(target.getInteractionPoint())) {
		return mover.getPosition().equals(target.getInteractionPoint());
	    } else {
		return unitIsAdjacent(mover, target.getPosition());
	    }
	}


	/*
	 * This function describes the MainMap specific best-guess
	 * at an optimal path. Since this is an admissible heuristic
	 * (it never overestimates the cost) it will allow the A*
	 * algorithm used by the PathFinder to always find the best path.
	 */
	public int heuristicCostEstimate(Position start, Position goal) {
	    int deltaX = abs(goal.getX() - start.getX());
	    int deltaY = abs(goal.getY() - start.getY());
	    // Diagonal moves cost the same as vertical/horizontal ones,
	    // so our dream scenario is something like a chain of diagonal
	    // lowest-cost moves followed by a straight line.
	    int admissibleGuess = Integer.max(deltaX, deltaY);
	    return admissibleGuess;
	}

	public void clearUnitMap(final Position pos) {
		unitMap[pos.getX()][pos.getY()] = null;
	}
    }
}
