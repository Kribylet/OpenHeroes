package view;


import gamelogic.ExitCode;
import net.miginfocom.swing.MigLayout;
import resources.FileIO;
import resources.ImageReadException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main menu UI Component. Gathers some game setup details such as IP address,
 * port number, player name and number of players for hosted games. A check box
 * toggles the Fog of War.
 *
 * IP addresses are sanity checked.
 */
public class MainMenu extends JComponent
{
    private static final Logger LOGGER = Logger.getLogger(MainMenu.class.getName());
    private static final int FONT_SIZE = 16;
    private static final int PORT_NUMBER_MAX = 65536;
    private static final int IP_NUMBER_MAX = 255;
    private JTextPane playerNamePane;
    private JTextPane connectionAddressPane;
    private JTextPane portPane;
    private JButton hostButton;
    private JButton joinButton;
    private JComboBox<Integer> playerSelect;
    private JCheckBox resolveDNSCheckBox;
    private JCheckBox fogOfWarCheckBox;
    private BufferedImage backgroundImage;

    public MainMenu(final String backgroundImagePath) throws ImageReadException {
	this.backgroundImage = FileIO.readImage(backgroundImagePath);

	this.setLayout(new MigLayout("nogrid", "30%[][][][]", "40%[]3%[][]6%[][]6%[]12%[]"));

	connectionAddressPane = new JTextPane();
	connectionAddressPane.setText("localhost");
	connectionAddressPane.setMinimumSize(new Dimension(140, 20));
	connectionAddressPane.setMaximumSize(new Dimension(140, 20));
	portPane = new JTextPane();
	portPane.setText("6789");
	portPane.setMinimumSize(new Dimension(50, 20));
	portPane.setMaximumSize(new Dimension(50, 20));
	playerNamePane = new JTextPane();
	playerNamePane.setMinimumSize(new Dimension(140, 20));
	playerNamePane.setMaximumSize(new Dimension(140, 20));
	playerNamePane.setText("Player");

	JTextArea playersText = new JTextArea("Players:");
	playersText.setDisabledTextColor(Color.BLACK);
	playersText.setEnabled(false);
	JTextArea playerNameText = new JTextArea("Name:");
	playerNameText.setDisabledTextColor(Color.BLACK);
	playerNameText.setEnabled(false);

	playerSelect = new JComboBox<>();
	for (int i = 2; i < 9; i++) {
	    playerSelect.addItem(Integer.valueOf(i));
	}

	playersText.setFont(new Font("MonoSpaced", Font.BOLD, FONT_SIZE));
	playerNameText.setFont(new Font("MonoSpaced", Font.BOLD, FONT_SIZE));
	playersText.setOpaque(false);
	playerNameText.setOpaque(false);

	JButton questionButton = new JButton();
	JButton resolveButton = new JButton();
	hostButton = new JButton();
	joinButton = new JButton();

	JButton quitButton = new JButton();
	Action quitAction = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		SwingUtilities.invokeLater(new Runnable() {
		    @Override public void run() {
			int choice = JOptionPane
				.showConfirmDialog(MainMenu.this, "Are you sure?", "You're about to quit!", JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
			    System.exit(ExitCode.OK.ordinal());
			}
		    }
		});

	    }
	};

	Action questionAction = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		SwingUtilities.invokeLater(new Runnable() {
		    @Override public void run() {
			JOptionPane.showMessageDialog(MainMenu.this, "Note that the IP address you bind the server to matters!\n" +
										     "\nIf you intend to host a game, bind to the IP address you will be receiving packets on.");
		    }
		});

	    }
	};

	Action resolveAction = new AbstractAction()
	{
	    @Override public void actionPerformed(final ActionEvent actionEvent) {
		try {
		    connectionAddressPane.setText(InetAddress.getLocalHost().toString().split("/")[1]);
		} catch (UnknownHostException e) {
		    LOGGER.log(Level.WARNING, "Couldn't resolve local address!", e);
		    SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
			    JOptionPane.showMessageDialog(MainMenu.this, "Couldn't resolve local address!");
			}
		    });
		}
	    }
	};


	quitButton.setAction(quitAction);
	quitButton.setText("Quit");

	questionButton.setAction(questionAction);
	questionButton.setText("?");

	resolveButton.setAction(resolveAction);
	resolveButton.setText("(Host) Resolve my address");

	resolveDNSCheckBox = new JCheckBox("Use DNS");
	resolveDNSCheckBox.setOpaque(false);

	fogOfWarCheckBox = new JCheckBox("Fog Of War");
	fogOfWarCheckBox.setOpaque(false);
	fogOfWarCheckBox.setSelected(true);

	// First row
	this.add(playerNameText);
	this.add(playerNamePane, "wrap");

	// Second row
	this.add(hostButton);
	this.add(questionButton);
	this.add(playersText);
	this.add(playerSelect, "wrap");
	// Third row
	this.add(fogOfWarCheckBox, "wrap");
	// Fourth row
	this.add(joinButton);
	this.add(connectionAddressPane);
	this.add(portPane, "wrap");
	// Fifth row
	this.add(resolveDNSCheckBox, "cell 1 4");
	this.add(resolveButton, "wrap");
	// Sixth row
	this.add(quitButton);
	LOGGER.log(Level.FINEST, "Constructed main menu.");
    }

    public String getPlayerName() {
	return playerNamePane.getText();
    }

    public String getConnectionAddress() {
	return connectionAddressPane.getText();
    }

    public int getPort() {
	return Integer.parseInt(portPane.getText());
    }

    @Override protected void paintComponent(final Graphics graphics) {
	super.paintComponent(graphics);
	graphics.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), 0, 0, backgroundImage.getWidth(),
			   backgroundImage.getHeight(), this);
    }

    public AbstractButton getHostButton() {
	return hostButton;
    }

    public JButton getJoinButton() {
	return joinButton;
    }

    public int getNumberOfPlayers() {
	return (int) playerSelect.getSelectedItem();
    }

    public boolean isValidPort() {
	String portString = portPane.getText();
	if (!portString.matches("^([1-9][0-9]{0,4}|0)$")) {
	    return false;
	}
	int portNumber = Integer.parseInt(portPane.getText());
	return portNumber >= 0 && portNumber <= PORT_NUMBER_MAX;
    }

    public boolean isValidIP() {
	String ipAddressString = connectionAddressPane.getText();
	if (ipAddressString.equals("localhost")) {
	    return true;
	}
	if (resolveDNSCheckBox.isSelected()) {
	    InetSocketAddress inet = new InetSocketAddress(ipAddressString, getPort());
	    if (inet.isUnresolved()) {
		return false;
	    }
	    connectionAddressPane.setText(inet.getAddress().toString().split("/")[1]);
	    return true;
	}
	/*
	 * Regex string covers a lot of the formatting.
	 * 4 fields separated by dots, only numbers, no leading 0's unless it is alone.
	 * All that is left is to ensure the individual ip numbers are in valid ranges.
	 */
	if (!ipAddressString.matches("^(([1-9][0-9]{0,2}|0)[.]){3}([1-9][0-9]{0,2}|0)?$")) {
	    return false;
	}
	String[] ipAddress = ipAddressString.split("[.]");
	for (String ipNumberString : ipAddress) {
	    int ipNumber = Integer.parseInt(ipNumberString);
	    if (!(ipNumber >= 0 && ipNumber <= IP_NUMBER_MAX)) {
		return false;
	    }
	}
	return true;
    }

    public boolean getFogOfWar() {
	return fogOfWarCheckBox.isSelected();
    }
}
