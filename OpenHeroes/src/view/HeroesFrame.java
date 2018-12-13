package view;

import connection.ClientListener;
import entity.Hero;
import entity.Interactable;
import entity.InteractionType;
import entity.Mover;
import entity.ResourceMine;
import entity.Town;
import gamelogic.ClientSession;
import gamelogic.ExitCode;
import gamelogic.ServerSession;
import gamemodel.GameModel;
import gamemodel.GameModelFactory;
import gamemodel.GameState;
import gamemodel.Position;
import gamemodel.Resource;
import gamemodel.Team;
import gamemodel.UnknownTownTypeException;
import gamemodel.UnsupportedPlayerAmountException;
import gamemodel.listeners.GameEvent;
import gamemodel.listeners.GameEventListener;
import gamemodel.listeners.MainMapEvent;
import gamemodel.listeners.MainMapEvent.MapEventType;
import gamemodel.listeners.MainMapListener;
import net.miginfocom.swing.MigLayout;
import resources.FileIO;
import resources.GameResourceManager;
import resources.ImageReadException;
import resources.SocketGenerationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static gamemodel.GameModel.*;

/**
 * The primary view of the Heroes game. Allows the user to start a game, join a waiting game
 * and generally interact with the game itself.
 * <p>
 * The HeroesFrame requests changes to the gamestate from the ClientSession. It alters its own
 * view context and displays event prompts by listening to the GameModel state.
 *
 * MainMapInterface clicks are context sensitive and use the clicked Interactables to determine what
 * interactions should occur.
 */
public class HeroesFrame extends JFrame implements ClientListener, GameEventListener
{

    /**
     * The preferred size of a main map tile in pixels. This is used to
     * subdivide game images when they cover multiple tiles.
     * <p>
     * As such, game image resources must be:
     * PIXEL_TILE_SIZE*TILE_WIDTH wide
     * PIXEL_TILE_SIZE*TILE*HEIGHT tall
     * <p>
     * Images that are too small to satisfy that requirement will cause the game
     * to crash.
     * <p>
     * Images that are too big will not render properly.
     * <p>
     * Project default is 32.
     */
    public static final int PIXEL_TILE_SIZE = 32;
    private static final int GENERIC_FRAME_BORDER_COLOR = 0x261310;
    private static final int GENERIC_FRAME_BACKGROUND_COLOR = 0x4e2910;
    private static final int GENERIC_FRAME_LARGE_TEXT_SIZE = 60;
    private static final int GENERIC_FRAME_BORDER_SIZE = 32;
    private static final int VICTORY_TEXT_COLOR = 0xe0ce3d;
    private static final int GENERIC_FRAME_TEXT_COLOR = 0x140a0a;
    private final static Logger LOGGER = Logger.getLogger(HeroesFrame.class.getName());
    private final CardLayout cardLayout = new CardLayout();
    private final BattlefieldView battlefieldView;

    private static final String WAITING_FOR_PLAYER_STRING = "Waiting for players..";
    private GameInfoPanel gameInfoPanel;
    private final GameControlPanel gameControlPanel;

    private Container contentPane;
    private MainMenu mainMenu;
    private MainMapInterface mainMapInterface;
    private JScrollPane scrollPane;
    private ClientSession clientSession = null;
    private ServerSession serverSession = null;
    private boolean gameRunning = false;

    private Dimension resolution = new Dimension(1024, 720);
    private MiniMap miniMap = null;
    private GameOverScreen gameOverScreen;

    public HeroesFrame() throws HeadlessException, ImageReadException {
	super("OpenHeroes");

	this.addWindowListener(new CleanupWindowAdapter());

	Thread.currentThread().getThreadGroup().list();
	this.setBackground(Color.BLACK);
	this.setOpacity(1.0f);
	setResizable(false);
	mainMenu = new MainMenu("img/mainmenu_bg.jpg");

	Action hostAction = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		try {
		    if (!mainMenu.isValidPort()) {
			JOptionPane.showMessageDialog(HeroesFrame.this, "The port number is invalid.");
			return;
		    }
		    createServerSession();
		    createClientSession();
		} catch (UnsupportedPlayerAmountException e) {
		    LOGGER.log(Level.SEVERE, "Map could not be generated.", e);
		    System.exit(ExitCode.MAP.ordinal());
		} catch (UnknownHostException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.WARNING, "Unable to resolve host!", e);
		    initializationFailureDialog("Unable to resolve host!");
		} catch (SocketGenerationException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Sockets were not initialized properly!", e);
		    initializationFailureDialog("Sockets were not initialized properly!");
		} catch (SocketTimeoutException | ConnectException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.WARNING, "Unable to connect to specified server!", e);
		    initializationFailureDialog("Unable to connect to the specified server!");
		} catch (SocketException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Failed to bind sockets!", e);
		    initializationFailureDialog("Failed to bind sockets!");
		} catch (IOException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Could not initialize sockets.", e);
		    initializationFailureDialog("Could not initialize sockets.");
		} catch (UnknownTownTypeException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Unknown town type supplied to TownFactory.", e);
		    initializationFailureDialog("Failed to generate map!");
		}

	    }
	};
	mainMenu.getHostButton().setAction(hostAction);
	mainMenu.getHostButton().setText("Host");

	Action joinAction = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		if (!mainMenu.isValidPort()) {
		    JOptionPane.showMessageDialog(HeroesFrame.this, "The port number is invalid.");
		    return;
		} else if (!mainMenu.isValidIP()) {
		    JOptionPane.showMessageDialog(HeroesFrame.this, "The IP address is invalid.");
		    return;
		}
		try {
		    createClientSession();
		} catch (UnknownHostException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.WARNING, "Unable to resolve host!", e);
		    JOptionPane.showMessageDialog(HeroesFrame.this, "Unable to resolve host!");
		} catch (SocketGenerationException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Sockets were not initialized properly!", e);
		    JOptionPane.showMessageDialog(HeroesFrame.this, "Sockets were not initialized properly!");
		} catch (SocketTimeoutException | ConnectException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.WARNING, "Unable to connect to specified server!", e);
		    JOptionPane.showMessageDialog(HeroesFrame.this, "Unable to connect to the specified server!");
		} catch (SocketException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Failed to bind sockets!", e);
		    JOptionPane.showMessageDialog(HeroesFrame.this, "Failed to bind sockets!");
		} catch (IOException e) {
		    postGameCleanUp();
		    LOGGER.log(Level.SEVERE, "Failed to bind sockets!", e);
		    JOptionPane.showMessageDialog(HeroesFrame.this, "Could not initialize sockets.");
		    System.exit(ExitCode.IO.ordinal());
		}
	    }
	};

	Action zoomIn = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		mainMapInterface.increaseGridSize();
		mainMapInterface.revalidate();
	    }
	};

	Action zoomOut = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		mainMapInterface.decreaseGridSize();
		mainMapInterface.revalidate();
	    }
	};

	final MouseListener mainMapMouseListener = new MouseListener()
	{
	    @Override public void mouseClicked(final MouseEvent mouseEvent) {
		if (!mainMapInterface.isEnabled()) {return;}

		if (mouseEvent.getButton() == MouseEvent.BUTTON3) {

		    gameBoardClickHandler(InteractionType.DESELECT, null);

		}
	    }

	    @Override public void mousePressed(final MouseEvent mouseEvent) {

		if (!mainMapInterface.isEnabled()) {return;}
		Position pos = mainMapInterface.getGrid(mouseEvent.getPoint().x, mouseEvent.getPoint().y);
		if (invalidClick(pos, mouseEvent)) {return;}
		handleInteraction(mouseEvent, pos);
	    }

	    @Override public void mouseReleased(final MouseEvent mouseEvent) {

	    }

	    @Override public void mouseEntered(final MouseEvent mouseEvent) {

	    }

	    @Override public void mouseExited(final MouseEvent mouseEvent) {

	    }
	};
	MouseMotionListener doScrollRectToVisible = new MouseMotionAdapter()
	{
	    public void mouseDragged(MouseEvent e) {
		if (!mainMapInterface.isEnabled() || !SwingUtilities.isRightMouseButton(e)) {return;}
		Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
		((JComponent) e.getSource()).scrollRectToVisible(r);
	    }
	};

	this.battlefieldView = new BattlefieldView(resolution);

	JButton battlefieldEndTurnButton = battlefieldView.getEndBattleButton();


	Action endBattleTurnAction = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		clientSession.closeBattleScreen();
	    }
	};

	battlefieldEndTurnButton.setAction(endBattleTurnAction);
	battlefieldEndTurnButton.setText("Done");

	this.mainMapInterface = new MainMapInterface();
	this.gameControlPanel = new GameControlPanel();
	this.gameInfoPanel = new GameInfoPanel();
	gameOverScreen = new GameOverScreen();
	this.scrollPane = new JScrollPane(mainMapInterface);
	final TradeScreen tradeScreen = new TradeScreen();

	InputMap inputMap = mainMapInterface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	ActionMap actionMap = mainMapInterface.getActionMap();

	inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0), "zoomIn");
	inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "zoomOut");
	actionMap.put("zoomIn", zoomIn);
	actionMap.put("zoomOut", zoomOut);

	mainMapInterface.addMouseListener(mainMapMouseListener);
	mainMapInterface.addMouseMotionListener(doScrollRectToVisible);
	mainMenu.getJoinButton().setAction(joinAction);
	mainMenu.getJoinButton().setText("Join");

	contentPane = this.getContentPane();
	contentPane.setLayout(cardLayout);
	JPanel gameView = new JPanel()
	{
	    @Override public Dimension getPreferredSize() {
		return resolution;
	    }
	};
	gameView.setLayout(new BorderLayout());
	gameView.add(scrollPane, BorderLayout.CENTER);
	gameView.add(gameControlPanel, BorderLayout.EAST);
	gameView.add(gameInfoPanel, BorderLayout.SOUTH);

	contentPane.add(mainMenu, GameState.MAIN_MENU.name());
	contentPane.add(gameView, GameState.MAIN_MAP.name());
	contentPane.add(gameOverScreen, GameState.GAME_OVER.name());
	contentPane.add(tradeScreen, GameState.TRADE.name());
	contentPane.add(battlefieldView, GameState.BATTLEFIELD.name());
    }

    private void handleInteraction(final MouseEvent mouseEvent, final Position pos) {
	Interactable topLevelInteractable = getMainMap().getTopLevelInteractable(pos);
	boolean clickedInteractionPoint =  topLevelInteractable != null &&
					   topLevelInteractable.getInteractionPoint().is(pos);

	if (mainMapInterface.getSelectedUnit() == null) {
	    gameBoardClickHandler(resolveInteractionType(topLevelInteractable, mouseEvent, pos),
				  topLevelInteractable);
	} else {
	    if (mainMapInterface.moveTarget == null || !mainMapInterface.moveTarget.is(pos)) {
		if (clickedInteractionPoint) {
		    mainMapInterface.setTarget(topLevelInteractable);
		} else {
		    mainMapInterface.setTarget(pos);
		}
	    } else if (clickedInteractionPoint && !topLevelInteractable.equals(mainMapInterface.selectedUnit)) {
		clientSession.sendInteractMessage(mainMapInterface.getSelectedUnit(), topLevelInteractable,
						  mainMapInterface.getProposedPath());
	    } else if (!mainMapInterface.proposedPath.isEmpty()) {
		clientSession.move(mainMapInterface.selectedUnit, mainMapInterface.getProposedPath());
	    }

	}
    }

    private void initializationFailureDialog(final String s) {
	SwingUtilities.invokeLater(new Runnable()
	{
	    @Override public void run() {
		JOptionPane.showMessageDialog(HeroesFrame.this, s);
	    }
	});
    }

    public void start() {
	this.setVisible(true);
	this.revalidate();
	this.pack();
    }

    private void createServerSession()
	    throws IOException, SocketException, UnsupportedPlayerAmountException,
	    UnknownTownTypeException
    {
        GameModel newGameModel = GameModelFactory.generateGameModel(mainMenu.getNumberOfPlayers(), mainMenu.getFogOfWar());
	GameResourceManager.instance().startServer(mainMenu.getConnectionAddress(), mainMenu.getPort());
	serverSession = new ServerSession(newGameModel);
	GameResourceManager.instance().executeParallel(serverSession);
    }

    private void createClientSession()
	    throws IOException, SocketGenerationException, UnknownHostException, SocketException, SocketTimeoutException,
	    ConnectException
    {
	this.clientSession = new ClientSession(mainMenu.getPlayerName(), mainMenu.getConnectionAddress(), mainMenu.getPort());
	clientSession.addClientListener(this);
	GameResourceManager.instance().executeParallel(clientSession);
    }

    public void buildGameView() {
	mainMapInterface.prepareView();

	SwingUtilities.invokeLater(new Runnable()
	{
	    @Override public void run() {
		mainMapInterface.enableInputMethods(true);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(mainMapInterface.getResolution());
		mainMapInterface.setAutoscrolls(true);
		mainMapInterface.centerCameraOnFriendlyHero();
		miniMap.updateDisplayData();
	    }
	});
    }

    @Override public void gameModelChanged(GameEvent e) {

	switch (e.getType()) {
	    case PLAYER_DISCONNECTED:

	        SwingUtilities.invokeLater(new Runnable() {
		    @Override public void run() {
		        String playerName = getGameModel().getPlayerNames().get(e.getTeam());
			JOptionPane.showMessageDialog(HeroesFrame.this, playerName + " has disconnected abruptly!");
		    }
		});
	        break;
	    case DEFEAT_NOTIFICATION:
	        if (!getGameModel().getGameState().equals(GameState.GAME_OVER)) {
		    clientDefeat(e.getTeam());
		}
		break;
	    case BATTLE_START:
		battlefieldView.loadBattleField(getGameModel());
		break;
	    case TURN_START:
		mainMapInterface.centerCameraOnFriendlyHero();
		break;
	    default:
	}

	if (getGameModel().getGameState() == GameState.GAME_OVER && serverSession != null) {
	    gameOverScreen.connectionRedrawTimer.start();
	}
	LOGGER.log(Level.FINE, "UI set context to " + getGameModel().getGameState().name());
	switchToPane(getGameModel().getGameState().name());
    }

    public void switchToPane(final String paneKey) {
	SwingUtilities.invokeLater(new Runnable()
	{
	    @Override public void run() {
		HeroesFrame.this.cardLayout.show(contentPane, paneKey);
		mainMapInterface.clearHeroTarget();
	    }
	});
    }

    private InteractionType resolveInteractionType(final Interactable topLevelInteractable,
						   final MouseEvent mouseEvent,
						   final Position pos)
    {
	boolean leftClick = mouseEvent.getButton() == MouseEvent.BUTTON1;
	boolean clickedInteractable =
		topLevelInteractable != null && !topLevelInteractable.equals(mainMapInterface.getSelectedUnit());

	if (!leftClick) {
	    assert topLevelInteractable != null; // invalidClick() ensures this!
	    return topLevelInteractable.rightClick(getGameModel().getCurrentTeam());
	}
	if (clickedInteractable && topLevelInteractable.getInteractionPoint().is(pos)) {
	    return topLevelInteractable.leftClick(getGameModel().getCurrentTeam());
	}
	return InteractionType.NONE;
    }

    private boolean invalidClick(final Position pos, final MouseEvent mouseEvent) {


	if (pos == null) {
	    return true; // clicked outside the map itself (When map border is visible)
	}
	boolean leftClick = mouseEvent.getButton() == MouseEvent.BUTTON1;
	boolean rightClick = mouseEvent.getButton() == MouseEvent.BUTTON1;

	if (!leftClick && !rightClick) {
	    return true; // Only left and right clicks allowed
	}

	if (!getMainMap().playerSeesMapTile(pos)) {
	    return true; // Clicked on the fog of war.
	}

	if (leftClick && !getGameModel().onTurn()) {
	    return true;
	}

	// Right clicks are meaningless outside of interactables.
	return !leftClick && getMainMap().getTopLevelInteractable(pos) == null;
    }

    private void gameBoardClickHandler(final InteractionType interaction, final Interactable interactable)
    {
	switch (interaction) {
	    case OWNER_STATVIEW:
		return;
	    case HOSTILE_STATVIEW:
		return;
	    case SELECT:
		mainMapInterface.selectHero((Hero) interactable);
		return;
	    case DESELECT:
		mainMapInterface.clearSelectedHero();
		return;
	    case BUY_HERO:
		buyHeroDialog(interactable);
	}
    }

    private void buyHeroDialog(final Interactable interactable) {
	SwingUtilities.invokeLater(new Runnable()
	{
	    @Override public void run() {
		if (JOptionPane.showConfirmDialog(HeroesFrame.this, "Do you want to buy a hero for 500 gold?", "Buy Hero",
						  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
		    if (clientSession.affordsHero()) {
			clientSession.buyHero(interactable);
		    } else {
			JOptionPane.showMessageDialog(HeroesFrame.this, "You don't have enough gold!");
		    }
		}
	    }
	});

    }

    @Override public void clientUpdated() {
	if (clientSession.hasLostConnection()) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override public void run() {
		    switchToPane(GameState.MAIN_MENU.name());
		    JOptionPane.showMessageDialog(HeroesFrame.this,
						  "The server connection was lost abruptly!",
						  "Connection lost!",
						  JOptionPane.ERROR_MESSAGE);
		    postGameCleanUp();
		}
	    });
	    return;
	}

	LOGGER.log(Level.FINE, "About to hook listeners to the GameModel.");
	getGameModel().addGameModelListeners(this);
	if (!gameRunning) {
	    gameRunning = true;
	    buildGameView();
	}
	getMainMap().addMainMapListener(mainMapInterface);
	getMainMap().addMainMapListener(gameInfoPanel);
	getMainMap().addMainMapListener(gameControlPanel);
	mainMapInterface.clearSelectedHero();
	getMainMap().notifyMainMapListeners();
	LOGGER.log(Level.FINE, "Finished hooking listeners to the GameModel.");
    }

    public void clientDefeat(final Team defeatedTeam) {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override public void run() {
		String defeatedPlayerName = getGameModel().getPlayerNames().get(defeatedTeam);
		JOptionPane
				.showMessageDialog(HeroesFrame.this, defeatedPlayerName + " was defeated!", "DEFEAT", JOptionPane.INFORMATION_MESSAGE);

	    }
	});
    }

    public void postGameCleanUp() {
        SwingUtilities.invokeLater(new Runnable() {
	    @Override public void run() {
		switchToPane(GameState.MAIN_MENU.name());
		if (clientSession != null) {
		    clientSession.closeConnection();
		    clientSession = null;
		}
		if (serverSession != null) {
		    serverSession = null;
		}
		gameRunning = false;
		gameControlPanel.turnLabel.setText(WAITING_FOR_PLAYER_STRING);
		try {
		    GameResourceManager.instance().purgeGameResources();
		} catch (IOException e) {
		    LOGGER.log(Level.WARNING, "May have failed to completely reset game state!", e);
		}
	    }
	});
    }

    private final class GameControlPanel extends JComponent implements MainMapListener
    {
	private BufferedImage bgImage;
	private JLabel turnLabel;

	private GameControlPanel() throws ImageReadException {

	    this.bgImage = FileIO.readImage("img/large_wood_bg.png");

	    GridBagLayout gridbag = new GridBagLayout();
	    GridBagConstraints c = new GridBagConstraints();
	    this.setLayout(gridbag);

	    JButton endTurnButton = new JButton();
	    final Action endTurnAction = new AbstractAction()
	    {
		@Override public void actionPerformed(final ActionEvent actionEvent) {
		    mainMapInterface.clearSelectedHero();
		    clientSession.endTurn(); // We'll be told to end our own turn!
		}
	    };
	    endTurnButton.setAction(endTurnAction);
	    endTurnButton.setText("End Turn");
	    endTurnButton.setPreferredSize(new Dimension(100, 60));

	    JButton syncButton = new JButton();
	    final Action syncAction = new AbstractAction()
	    {
		@Override public void actionPerformed(final ActionEvent actionEvent) {
		    clientSession.synchronizeClient();
		}
	    };
	    syncButton.setAction(syncAction);
	    syncButton.setText("Sync");
	    syncButton.setPreferredSize(new Dimension(100, 60));

	    JButton surrenderButton = new JButton();
	    final Action closeAction = new AbstractAction()
	    {
		@Override public void actionPerformed(final ActionEvent actionEvent) {
		    if (JOptionPane.showConfirmDialog(HeroesFrame.this, "Are you sure you want to surrender?", "Surrender",
						      JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			clientSession.surrender();
		}
	    };

	    surrenderButton.setAction(closeAction);
	    surrenderButton.setText("Surrender");
	    surrenderButton.setPreferredSize(new Dimension(100, 60));

	    turnLabel = new JLabel(WAITING_FOR_PLAYER_STRING);
	    turnLabel.setForeground(Color.WHITE);
	    turnLabel.setPreferredSize(new Dimension(50, 50));

	    JLabel helpLabel = new JLabel();
	    helpLabel.setForeground(Color.WHITE);
	    helpLabel.setPreferredSize(new Dimension(50, 50));
	    helpLabel.setText("'O' & 'P' zoom the map in and out.");

	    miniMap = new MiniMap();

	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.gridx = 0;
	    c.gridy = 0;
	    gridbag.setConstraints(miniMap, c);
	    this.add(miniMap);
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.LINE_START;
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 0;
	    c.weighty = 0;
	    gridbag.setConstraints(turnLabel, c);
	    this.add(turnLabel);
	    c.gridx = 0;
	    c.gridy = 2;
	    gridbag.setConstraints(helpLabel, c);
	    this.add(helpLabel);
	    c.anchor = GridBagConstraints.LAST_LINE_START;
	    c.gridx = 0;
	    c.gridy = 4;
	    gridbag.setConstraints(surrenderButton, c);
	    this.add(surrenderButton);
	    c.gridx = 0;
	    c.gridy = 5;
	    gridbag.setConstraints(syncButton, c);
	    this.add(syncButton);
	    c.gridx = 0;
	    c.gridy = 6;
	    gridbag.setConstraints(endTurnButton, c);
	    this.add(endTurnButton);
	}

	@Override public Dimension getPreferredSize() {
	    return new Dimension(resolution.width / 4, resolution.height);
	}

	@Override protected void paintComponent(final Graphics graphics) {
	    super.paintComponent(graphics);
	    drawRepeatedBackground(graphics, bgImage);
	}

	@Override public void mapChanged(final MainMapEvent e) {
	    if (getGameModel().gameStarted()) {
	        SwingUtilities.invokeLater(new Runnable() {
		    @Override public void run() {
			String turnText =
				getGameModel().getPlayerNames().get(getGameModel().getTurnTaker());
			if (getGameModel().onTurn()) {
			    turnText += " (You!)";
			}
			turnLabel.setText(turnText);
			GameControlPanel.this.repaint();
		    }
		});

	    }
	}
    }

    private final class MiniMap extends MainMapViewer implements MainMapListener
    {
	private int mapHeight;
	private int mapWidth;

	private MiniMap() {}

	@Override public Dimension getPreferredSize() {
	    return new Dimension(resolution.width / 4, resolution.width/4);
	}

	@Override protected void paintComponent(final Graphics graphics) {
	    super.paintComponent(graphics);

	    for (int x = 0; x < mapWidth; x++) {
		for (int y = 0; y < mapHeight; y++) {
		    drawVisibleMap(graphics, getGameModel(), x, y);
		}
	    }
	}



	public void updateDisplayData() {
	    mapHeight = getMainMap().getMapHeight();
	    mapWidth = getMainMap().getMapHeight();
	    gridSize = Integer.max(resolution.width / (4*mapWidth), resolution.width / (4*mapHeight));
	}

	@Override public void mapChanged(final MainMapEvent e) {
	    if (e.getType() == MapEventType.HERO_MOVED) {
		this.repaint();
	    }
	}


    }


    private void drawRepeatedBackground(Graphics graphics, Image bgImage) {
	for (int x = 0; x < getPreferredSize().width; x += bgImage.getWidth(this)) {
	    for (int y = 0; y < getPreferredSize().height; y += bgImage.getHeight(this)) {
		graphics.drawImage(bgImage, x, y, this);
	    }
	}
    }

    private final class GameOverScreen extends JPanel
    {
	public static final int DEFEAT_TEXT_COLOR = 0xe01919;
	public static final int REMAINING_PLAYER_FONT_SIZE = 45;
	public Timer connectionRedrawTimer;

	private GameOverScreen() {
	    this.setLayout(new MigLayout("gap 0px 0px, fill", "80%[grow]", "80%[grow]"));

	    JButton gameOverButton = new JButton();
	    Action gameOverAction = new AbstractAction()
	    {
		@Override public void actionPerformed(final ActionEvent actionEvent) {

		    /*
		     * Slightly complex boolean:
		     * The idea is to notify a host that they might be about to disconnect
		     * a game that is still underway after they've lost.
		     *
		     * If they're not hosting, or they've won, the game is over and we
		     * can safely close it down.
		     *
		     * If they are hosting and there are no players left, there's no reason
		     * ask for confirmation.
		     *
		     * Otherwise, ask for confirmation before shutting the game down.
		     */

		    if (serverSession == null || getGameModel().hasWon()) {
			postGameCleanUp();
			return;
		    }

		    confirmDisconnect();
		}
	    };

	    Action redraw = new AbstractAction()
	    {
		@Override public void actionPerformed(final ActionEvent actionEvent) {
		    GameOverScreen.this.repaint();
		}
	    };

	    connectionRedrawTimer = new Timer(1000, redraw);
	    gameOverButton.setAction(gameOverAction);
	    gameOverButton.setText("Done");

	    this.add(gameOverButton);
	}

	private void confirmDisconnect() {
	    SwingUtilities.invokeLater(new Runnable()
	    {

		@Override public void run() {
		    boolean noPlayersLeft = serverSession.getConnectedClients() < 2;
		    if (noPlayersLeft || JOptionPane.YES_OPTION == JOptionPane
			    .showConfirmDialog(HeroesFrame.this, "You're hosting the game!\n" +
								 "Are you sure you want to leave the game?\n\n" +
								 "All " + serverSession.getConnectedClients() +
								 " other players will be disconnected!",
					       "Host warning!", JOptionPane.YES_NO_OPTION)) {
			serverSession.disconnectRemainingPlayers();
			postGameCleanUp();
		    }
		}
	    });
	}

	@Override protected void paintComponent(final Graphics graphics) {
	    super.paintComponent(graphics);
	    drawGenericFrame(graphics);
	    Font gameOverFont = new Font("MonoSpaced", Font.BOLD, GENERIC_FRAME_LARGE_TEXT_SIZE);
	    String gameOverText;
	    boolean victory = getGameModel().hasWon();
	    if (victory) {
		gameOverText = "VICTORY";
		graphics.setColor(new Color(VICTORY_TEXT_COLOR));
	    } else {
		gameOverText = "DEFEAT";
		graphics.setColor(new Color(DEFEAT_TEXT_COLOR));
	    }
	    graphics.setFont(gameOverFont);
	    int textWidth = getFontMetrics(gameOverFont).stringWidth(gameOverText);
	    int textHeight = getFontMetrics(gameOverFont).getHeight();
	    graphics.drawString(gameOverText, (resolution.width - textWidth) / 2, (resolution.height - textHeight) / 2);

	    if (serverSession != null && !victory) {
		Font remainingPlayerFont = new Font("MonoSpaced", Font.BOLD, REMAINING_PLAYER_FONT_SIZE);
		textWidth = getFontMetrics(remainingPlayerFont).stringWidth("Remaining players: " + serverSession.getConnectedClients());
		textHeight = getFontMetrics(remainingPlayerFont).getHeight();
		graphics.setFont(remainingPlayerFont);
		graphics.setColor(new Color(GENERIC_FRAME_TEXT_COLOR));
		int remainingPlayers = serverSession.getConnectedClients();
		if (remainingPlayers == 1) {
		    remainingPlayers = 0;
		}
		graphics.drawString("Remaining players: " + remainingPlayers, (resolution.width - textWidth) / 2,
				    (resolution.height * 3) / 4 - textHeight / 2);
	    }
	}



	@Override public Dimension getPreferredSize() {
	    return resolution;
	}
    }

    private final class GameInfoPanel extends JPanel implements MainMapListener
    {
	private static final int BORDER = 4;
	public static final int RESOURCE_LABEL_TEXT_COLOR = 0xc0c0c;
	private final BufferedImage[] resourceIcons = new BufferedImage[Resource.values().length];
	private final JLabel[] resourceAmountLabels = new JLabel[Resource.values().length];
	private final BufferedImage bgImage;

	private GameInfoPanel() throws ImageReadException {
	    this.setLayout(new MigLayout("gap 0px 0px, fill", "[80px!,grow]", "[]"));

	    this.bgImage = FileIO.readImage("img/wood_bg_offset.png");

	    for (int i = 0; i < resourceAmountLabels.length; i++) {
		resourceIcons[i] = FileIO.readImage(Resource.values()[i].getResourceImagePath());
		resourceAmountLabels[i] = new JLabel();
		ImageIcon icon = new ImageIcon(resourceIcons[i]);
		resourceAmountLabels[i].setIcon(icon);
		resourceAmountLabels[i].setForeground(Color.WHITE);
		resourceAmountLabels[i].setBackground(new Color(RESOURCE_LABEL_TEXT_COLOR));
		this.add(resourceAmountLabels[i]);
	    }
	}

	@Override public void mapChanged(final MainMapEvent e) {
	    SwingUtilities.invokeLater(new Runnable() {

		@Override public void run() {
		    int[] playerResourceList = getGameModel().getClientResourceList();
		    for (int i = 0; i < resourceAmountLabels.length; i++) {
			String resourceAmount = Integer.toString(playerResourceList[i]);
			resourceAmountLabels[i].setText(resourceAmount);
		    }
		    GameInfoPanel.this.revalidate();
		}
	    });

	}


	@Override protected void paintComponent(final Graphics graphics) {
	    super.paintComponent(graphics);

	    super.paintComponent(graphics);
	    drawRepeatedBackground(graphics, bgImage);
	}

	@Override public Dimension getPreferredSize() {
	    return new Dimension(resolution.width, PIXEL_TILE_SIZE + BORDER * 2);
	}

    }

    private final class TradeScreen extends JPanel
    {
	private TradeScreen() {
	    this.setLayout(new MigLayout("gap 0px 0px, fill", "80%[grow]", "80%[grow]"));

	    JButton doneButton = new JButton();
	    Action doneAction = new AbstractAction()
	    {
		@Override public void actionPerformed(final ActionEvent actionEvent) {
		    clientSession.openTradeScreen();
		}
	    };
	    doneButton.setAction(doneAction);
	    doneButton.setText("Done");

	    this.add(doneButton);
	}

	@Override protected void paintComponent(final Graphics graphics) {
	    super.paintComponent(graphics);
	    drawGenericFrame(graphics);
	    Font gameOverFont = new Font("MonoSpaced", Font.BOLD, GENERIC_FRAME_LARGE_TEXT_SIZE);
	    String tradeText = "HEJ!";
	    graphics.setFont(gameOverFont);
	    int textWidth = getFontMetrics(gameOverFont).stringWidth(tradeText);
	    int textHeight = getFontMetrics(gameOverFont).getHeight();
	    graphics.setColor(new Color(GENERIC_FRAME_TEXT_COLOR));
	    graphics.drawString(tradeText, (resolution.width - textWidth) / 2, (resolution.height - textHeight) / 2);
	}
    }

    private void drawGenericFrame(final Graphics graphics) {
	graphics.setColor(new Color(GENERIC_FRAME_BORDER_COLOR));
	graphics.fillRect(0, 0, resolution.width, resolution.height);
	graphics.setColor(new Color(GENERIC_FRAME_BACKGROUND_COLOR));
	graphics.fillRect(GENERIC_FRAME_BORDER_SIZE, GENERIC_FRAME_BORDER_SIZE, resolution.width - GENERIC_FRAME_BORDER_SIZE * 2, resolution.height - GENERIC_FRAME_BORDER_SIZE * 2);
    }

    private class CleanupWindowAdapter extends WindowAdapter
    {
	@Override public void windowClosing(final WindowEvent windowEvent) {
	    postGameCleanUp();
	}
    }

    /**
     * The MainMapInterface class is responsible for the graphical representation of the MainMap, as well
     * as handling parts of the user interactions.
     * <p>
     * It can "zoom" in and out by changing its grid size, and translates mouse clicks on the screen
     * into the proper game model coordinates regardless of zoom level.
     * <p>
     * The UI aspect of the MainMapInterface involves keeping track of proposed hero paths, hero selections,
     * target selections and similar.
     * <p>
     * The scrolling functionality is actually provided by the ScrollPane which the MainMapInterface
     * is a subcomponent of.
     */
    private final class MainMapInterface extends MainMapViewer implements MainMapListener
    {

	private static final int MAX_GRID_SIZE = 256;
	private static final int MIN_GRID_SIZE = 16;
	private LinkedList<Position> proposedPath = new LinkedList<>();
	private Hero selectedUnit = null;
	private Position moveTarget = null;

	private int mapWidth;
	private int mapHeight;

	private MainMapInterface() {}

	public void prepareView() {
	    this.mapWidth = getMainMap().getMapWidth();
	    this.mapHeight = getMainMap().getMapHeight();
	    gridSize = PIXEL_TILE_SIZE;
	    clearSelectedHero();
	    setPreferredSize(new Dimension(mapWidth * gridSize, mapHeight * gridSize));
	    updateDisplayData();
	}

	@Override protected void paintComponent(final Graphics graphics) {
	    super.paintComponent(graphics);

	    if (zoomOffsetX > 0) {
		Image image = UIElements.BG_IMAGE.getMapImage();
		if (image == null) {
		    // The program will actually close before a null pointer could occur here..
		    return;
		}
		int imageWidth = image.getWidth(this);
		int imageHeight = image.getHeight(this);
		for (int x = 0; x < resolution.width; x += imageWidth) {
		    for (int y = 0; y < resolution.height; y += imageHeight) {
			// Loops a smaller background image over the entire background if we've zoomed out far enough.
			graphics.drawImage(UIElements.BG_IMAGE.getMapImage(), x, y, this);
		    }
		}
	    }

	    for (int x = 0; x < mapWidth; x++) {
		for (int y = 0; y < mapHeight; y++) {
		    drawVisibleMap(graphics, clientSession.getGameModel(), x, y);
		}
	    }

	    for (Hero hero : getGameModel().getHeroes()
		 ) {
		if (getMainMap().playerSeesMapTile(hero.getPosition())) {
		    drawInteractable(graphics, hero);
		}
	    }

	    drawOwnerFlags(graphics);

	    if (!proposedPath.isEmpty()) {
		drawPath(graphics);
	    }
	    if (selectedUnit != null) {
		drawHeroSelection(graphics);
	    }
	}

	private void shortenProposedPath() {
	    if (!proposedPath.isEmpty()) {
		proposedPath.removeFirst();
	    }
	}

	public void selectHero(Hero hero) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override public void run() {
		    clearProposedPath();
		    MainMapInterface.this.moveTarget = null;
		    MainMapInterface.this.selectedUnit = hero;
		    MainMapInterface.this.repaint();
		}
	    });

	}

	public void clearProposedPath() {
	    proposedPath.clear();
	}

	public void proposePath(final Position pos) {
	    proposedPath = getGameModel().findPath(selectedUnit.getPosition(), pos);
	}

	public void proposePath(final Interactable interactable) {

	    LinkedList<Position> cheapestPath = new LinkedList<>();
	    if (getMainMap().unitCanInteractWith(selectedUnit, interactable)) {
		proposedPath = cheapestPath;
		return;
	    }
	    Iterable<Position> possibleDestinations = getMainMap().passableInteractionPositions(interactable);
	    int cheapestPathCost = Integer.MAX_VALUE;
	    for (Position destination : possibleDestinations) {
		LinkedList<Position> tentativePath = getGameModel().findPath(selectedUnit.getPosition(), destination);
		if (tentativePath.isEmpty()) {
		    continue;
		}
		int currentMoveCost = getMainMap().pathMoveCost(tentativePath);
		if (currentMoveCost < cheapestPathCost) {
		    cheapestPathCost = currentMoveCost;
		    cheapestPath = tentativePath;
		}
	    }
	    proposedPath = cheapestPath;
	}

	public Deque<Position> getProposedPath() {
	    return proposedPath;
	}

	public void clearSelectedHero() {
	    selectHero(null);
	}

	public void setTarget(Position pos) {
	    SwingUtilities.invokeLater(new Runnable()
	    {
		@Override public void run() {
		    if (getMainMap().isPassable(pos)) {
			proposePath(pos);
			moveTarget = pos;
		    } else {
		        clearHeroTarget();
		    }
		}
	    });
	    this.repaint();
	}
	public void setTarget(Interactable interactable) {
	    SwingUtilities.invokeLater(new Runnable()
	    {
		@Override public void run() {
		    if (getMainMap().unitCanInteractWith(selectedUnit, interactable)) {
			clearProposedPath();
		    } else {
			proposePath(interactable);
		    }
		    moveTarget = interactable.getInteractionPoint();
		}
	    });
	    this.repaint();
	}

	public void clearHeroTarget() {
	    moveTarget = null;
	    clearProposedPath();
	}

	private void drawOwnerFlags(final Graphics graphics) {
	    for (Hero hero : getGameModel().getHeroes()) {
		Position heroPos = hero.getPosition();
		if (getMainMap().playerSeesMapTile(heroPos)) {
		    drawImageSquare(graphics, heroPos, hero.getOwner().getFlagImage());
		}
	    }
	    for (Town town : getGameModel().getTowns()) {
		Position townPos = town.getInteractionPoint();
		if (getMainMap().playerSeesMapTile(townPos)) {
		    drawImageSquare(graphics, townPos, town.getOwner().getFlagImage());
		}
	    }

	    for (ResourceMine mine : getGameModel().getMines()) {
		Position minePos = mine.getInteractionPoint();
		if (getMainMap().playerSeesMapTile(minePos)) {
		    drawImageSquare(graphics, minePos, mine.getOwner().getFlagImage());
		}
	    }
	}

	private void drawHeroSelection(final Graphics graphics) {

	    Position heroPos = selectedUnit.getPosition();
	    Image image = UIElements.SEL_FRAME.getMapImage();
	    // Technically an unnecessary null check. The program will actually
	    // close before a null pointer could occur here.
	    if (image != null) {
		drawImage(graphics, image, heroPos.getX(), heroPos.getY());
	    }
	}


	public void centerCameraOnHero(Mover hero) {
	    if (getMainMap().playerSeesMapTile(hero.getPosition())) {

		Point heroPoint = new Point(hero.getPosition().getX() * gridSize - resolution.width / 2,
					    hero.getPosition().getY() * gridSize - resolution.height / 2);
		SwingUtilities.invokeLater(new Runnable()
		{
		    @Override public void run() {
			scrollRectToVisible(new Rectangle(heroPoint, resolution));
		    }
		});
	    }

	}

	@Override public void mapChanged(final MainMapEvent e) {

	    // InvokeLater ensures we're operating from the Swing EventDispatchThread.
	    // Not using it could have unpredictable results on the Swing thread.
	    switch (e.getType()) {
		case HERO_MOVED:
		    SwingUtilities.invokeLater(new Runnable()
		    {
			@Override public void run() {
			    shortenProposedPath();
			    centerCameraOnHero(e.getMover());
			    MainMapInterface.this.repaint();
			}
		    });
		    break;
		case HERO_KILLED:
		    // If the selected hero was killed, clear the selection.
		    if (selectedUnit != null && e.getMover().equals(selectedUnit)) {
			SwingUtilities.invokeLater(this::clearSelectedHero);
		    }
		    break;
	    }
	    this.repaint();
	}

	private void drawPath(final Graphics graphics) {
	    Position[] pathArray = proposedPath.toArray(new Position[proposedPath.size()]);
	    int remainingMoves = selectedUnit.getRemainingMoveLength();
	    for (Position pos : pathArray) {
		remainingMoves -= getMainMap().passCost(pos);
		BufferedImage image =
			(remainingMoves < 0) ? UIElements.NM_STEP.getMapImage() : UIElements.STEP.getMapImage();
		drawImageSquare(graphics, pos, image);
	    }
	    BufferedImage image = (remainingMoves < 0) ? UIElements.NM_END.getMapImage() : UIElements.END.getMapImage();
	    drawImageSquare(graphics, proposedPath.getLast(), image);
	}

	public void increaseGridSize() {
	    if (gridSize < MAX_GRID_SIZE) {
		SwingUtilities.invokeLater(new Runnable()
		{
		    @Override public void run() {
			gridSize *= 2;
			updateDisplayData();
		    }
		});
		this.repaint();
	    }
	}

	public void decreaseGridSize() {

	    SwingUtilities.invokeLater(new Runnable() {
	    		    		@Override public void run() {

	    		    		}
	    		    	    });

		if (gridSize > MIN_GRID_SIZE) {

		    SwingUtilities.invokeLater(new Runnable()
		    {
			@Override public void run() {
			    gridSize /= 2;
			    updateDisplayData();
			}
		    });
		    this.repaint();
		}
	}

	public void updateDisplayData() {
	    int visibleWidth = this.getVisibleRect().width;
	    int visibleHeight = this.getVisibleRect().height;
	    if (gridSize * mapWidth < visibleWidth || gridSize * mapHeight < visibleHeight) {
		zoomOffsetX = (visibleWidth - gridSize * mapWidth) / 2;
		zoomOffsetY = (visibleHeight - gridSize * mapHeight) / 2;
	    } else {
		zoomOffsetX = 0;
		zoomOffsetY = 0;
	    }
	    setPreferredSize(new Dimension(mapWidth * gridSize, mapHeight * gridSize));

	}

	public Position getGrid(final int x, final int y) {
	    if (x - zoomOffsetX < 0 || y - zoomOffsetY < 0) {
		return null;
	    }
	    int gridX = (x - zoomOffsetX) / gridSize;
	    int gridY = (y - zoomOffsetY) / gridSize;
	    if (gridX >= mapWidth || gridY >= mapHeight) {
		return null;
	    }
	    return new Position(gridX, gridY);
	}

	public Dimension getResolution() {
	    return resolution;
	}

	public Hero getSelectedUnit() {
	    return selectedUnit;
	}

	public void centerCameraOnFriendlyHero() {
	    if (selectedUnit != null) {
		centerCameraOnHero(selectedUnit);
	    } else if (getGameModel().playerHasHeroes()) {
		centerCameraOnHero(getGameModel().getOwnedHeroes().get(0));
	    }
	}
    }

    private GameModel getGameModel() {
	return clientSession.getGameModel();
    }

    private MainMap getMainMap() {
	return clientSession.getGameModel().getMainMap();
    }
}
