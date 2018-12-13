package view;

import gamemodel.Battlefield;
import gamemodel.GameModel;
import gamemodel.listeners.BattleFieldListener;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The BattlefieldView is supposed to display and administrate user input
 * on the Battlefield map, similar to how the MainMap works. However,
 * there was not enough time to implement this functionality.
 * <p>
 * As a placeholder it simply displays the random dice roll that occurs
 * when heroes fight.
 * <p>
 * It is largely implemented in the intended fashion, and renders as a
 * separate view properly.
 */
public class BattlefieldView extends JComponent implements BattleFieldListener
{
    private static final Logger LOGGER = Logger.getLogger(BattlefieldView.class.getName());
    private static final int BORDER = 32;
    private static final int GENERIC_FRAME_BORDER_COLOR = 0x261310;
    private static final int GENERIC_FRAME_BACKGROUND_COLOR = 0x4e2910;
    private static final int BATTLE_TEXT_SIZE = 60;
    private static final int RESULT_TEXT_SIZE = 30;
    private Battlefield battleField = null;
    private final ReentrantLock battlefieldGuardLock = new ReentrantLock();
    private Dimension resolution;
    private JButton endBattleButton;
    private GameModel gameModel = null;

    public BattlefieldView(final Dimension resolution)
    {
	this.setLayout(new MigLayout("gap 0px 0px", "75%[][]", "80%[]"));


	endBattleButton = new JButton();

	this.resolution = resolution;
	setPreferredSize(new Dimension(resolution.width / 5 * 4, resolution.height));
	setBackground(Color.WHITE);
	setForeground(Color.BLACK);

	this.add(endBattleButton);
	LOGGER.log(Level.FINEST, this.getClass().getName() + " was successfully constructed.");
    }

    public JButton getEndBattleButton() {
	return endBattleButton;
    }

    public void loadBattleField(final GameModel gameModel) {
	 SwingUtilities.invokeLater(new Runnable()
	 {
	     @Override public void run() {
		 BattlefieldView.this.gameModel = gameModel;
		 BattlefieldView.this.battleField = gameModel.getBattlefield();
		 endBattleButton.setVisible(gameModel.onTurn());
		 LOGGER.log(Level.FINER, "New battlefield was loaded.");
	     }
	 });
    }

    @Override protected void paintComponent(final Graphics graphics) {
	super.paintComponent(graphics);

	battlefieldGuardLock.lock();
	try {
	    graphics.setColor(new Color(GENERIC_FRAME_BORDER_COLOR));
	    graphics.fillRect(0, 0, resolution.width, resolution.height);
	    graphics.setColor(new Color(GENERIC_FRAME_BACKGROUND_COLOR));
	    graphics.fillRect(BORDER, BORDER, resolution.width - BORDER * 2, resolution.height - BORDER * 2);

	    Font battleFieldFont = new Font("MonoSpaced", Font.BOLD, BATTLE_TEXT_SIZE);
	    String battleText = "Battle!";
	    graphics.setFont(battleFieldFont);
	    int textWidth = getFontMetrics(battleFieldFont).stringWidth(battleText);
	    int textHeight = getFontMetrics(battleFieldFont).getHeight();
	    graphics.setColor(new Color(10, 10, 10));
	    graphics.drawString(battleText, (resolution.width - textWidth) / 2, (resolution.height / 4 - textHeight / 2));
	    Font resultFont = new Font("MonoSpaced", Font.BOLD, RESULT_TEXT_SIZE);
	    graphics.setFont(resultFont);

	    String playerText = gameModel.getPlayerNames().get(battleField.getChallenger().getOwner()) + " VS. " +
				gameModel.getPlayerNames().get(battleField.getDefender().getOwner());

	    textWidth = getFontMetrics(resultFont).stringWidth(playerText);
	    textHeight = getFontMetrics(resultFont).getHeight();
	    graphics.drawString(playerText, (resolution.width - textWidth) / 2, (resolution.height - textHeight) / 2);

	    String dicerollText = battleField.getChallengerResult() + "     " + battleField.getDefenderResult();

	    textWidth = getFontMetrics(resultFont).stringWidth(dicerollText);
	    textHeight = getFontMetrics(resultFont).getHeight();
	    graphics.drawString(dicerollText, (resolution.width / 2 - (textWidth) / 2),
				(resolution.height * 5) / 8 - textHeight / 2);

	    String winner;
	    if (battleField.getChallengerResult() > battleField.getDefenderResult()) {
		winner = "Challenger " + battleField.getChallenger().getHeroName() + " wins!";
	    } else {
		winner = "Defender " + battleField.getDefender().getHeroName() + " wins!";
	    }

	    textWidth = getFontMetrics(resultFont).stringWidth(winner);
	    textHeight = getFontMetrics(resultFont).getHeight();

	    graphics.drawString(winner, (resolution.width / 2 - (textWidth) / 2), (resolution.height * 3) / 4 - textHeight / 2);


	} finally {
	    battlefieldGuardLock.unlock();
	}

    }

    @Override public void battleFieldChanged() {
	this.repaint();
    }
}
