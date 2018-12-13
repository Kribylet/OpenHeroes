package view;

import entity.Interactable;
import gamemodel.GameModel;
import gamemodel.Position;
import gamemodel.TerrainType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Abstract class containing shared methods for rendering a MainMap.
 */
public abstract class MainMapViewer extends JComponent
{
    protected int gridSize;
    protected int zoomOffsetX = 0;
    protected int zoomOffsetY = 0;

    /** Draws the map terrain for a given position. If it was visible it returns true, otherwise false. */
    protected void drawVisibleMap(final Graphics graphics, GameModel gameModel, final int x, final int y) {
	if (!gameModel.getMainMap().playerSeesMapTile(x, y)) {
	    drawFog(graphics, x, y);
	    return;
	}
	drawTerrain(graphics, gameModel.getMainMap().getTerrainMap(), x, y);

	Interactable feature = gameModel.getMainMap().getFeatureMap()[x][y];
	if (feature != null) {
	    drawInteractable(graphics, feature, x, y);
	}
    }

    protected void drawImageSquare(Graphics graphics, Position pos, BufferedImage image) {
	graphics.drawImage(image, pos.getX() * gridSize + zoomOffsetX, pos.getY() * gridSize + zoomOffsetY,
			   (pos.getX() + 1) * gridSize + zoomOffsetX, (pos.getY() + 1) * gridSize + zoomOffsetY, 0, 0,
			   image.getWidth(this), image.getHeight(this), this);
    }

    protected void drawFog(final Graphics graphics, final int x, final int y) {
	graphics.setColor(Color.BLACK);
	graphics.fillRect(x * gridSize + zoomOffsetX, y * gridSize + zoomOffsetY, gridSize, gridSize);
    }

    protected void drawInteractable(final Graphics graphics, final Interactable interactable, final int x, final int y) {
	int xOffset = x - interactable.getPosition().getX();
	int yOffset = y - interactable.getPosition().getY();
	Image image = interactable.getImageSlice(xOffset, yOffset);
	drawImage(graphics, image, x, y);
    }

    protected void drawInteractable(final Graphics graphics, final Interactable interactable) {
    	Image image = interactable.getImage();
    	drawImage(graphics,
		  image,
		  interactable.getPosition().getX(),
		  interactable.getPosition().getY());
    }

    protected void drawImage(final Graphics graphics, Image image, final int x, final int y) {

	int imageWidth = image.getWidth(this);
	int imageHeight = image.getHeight(this);
	int startX = x * gridSize + zoomOffsetX;
	int startY = y * gridSize + zoomOffsetY;
	graphics.drawImage(image, startX, startY, startX + gridSize,
			   startY + gridSize, 0, 0, imageWidth, imageHeight, this);
    }

    protected void drawTerrain(final Graphics g, final TerrainType[][] terrainMap, final int x, final int y) {
	if (terrainMap[x][y].hasImage()) {
	    drawImageSquare(g, new Position(x, y), terrainMap[x][y].getImage());
	} else {
	    g.setColor(terrainMap[x][y].getColor());
	    g.fillRect(x * gridSize + zoomOffsetX, y * gridSize + zoomOffsetY, gridSize, gridSize);
	}
    }
}
