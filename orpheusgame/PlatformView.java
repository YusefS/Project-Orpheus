package orpheusgame;

import java.awt.Color;
import java.awt.Graphics;

public class PlatformView {
	
	private Sprite sDirty;
	private Sprite sClean;
	
	public PlatformView() {
		// Get the sprite associated with the player character (will create image if not loaded yet)
		sDirty = SpriteManager.get().getSprite("resources/sprites/platform1.bmp");
		sClean = SpriteManager.get().getSprite("resources/sprites/platform_gold.bmp");
	}
	
	/** Draws the platform at the given coordinates */
	public void drawPlatform(Graphics g, int x, int y, int width, int height, int platformState) {
		int[] xcoords = new int[4];
		int[] ycoords = new int[4];
		xcoords[0] = x;
		ycoords[0] = y;
		xcoords[1] = x+width;
		ycoords[1] = y;
		xcoords[2] = x + (int) (0.8 * width);
		ycoords[2] = y - 4;
		xcoords[3] = x + (int) (0.2 * width);
		ycoords[3] = y - 4;
		
		if (platformState == Platform.platform_cleansed) {
			sClean.drawPart(g, x, y, 0, 0, width, height, false);
			// They need a shadow
			g.setColor(OrpheusGui.cGold);
			g.fillPolygon(xcoords, ycoords, 4);		
		} else if (platformState == Platform.platform_polluted) {
			sDirty.drawPart(g, x, y, 0, 0, width, height, false);
			g.setColor(Color.gray);
			g.fillPolygon(xcoords, ycoords, 4);
		}
		
	}

}
