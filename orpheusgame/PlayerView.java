package orpheusgame;

import java.awt.Color;
import java.awt.Graphics;

/** Takes care of displaying the graphical elements of the main character of the game Orpheus. */
public class PlayerView {

	private Sprite sprite;
	// Our internal counter for time. Used for animations.
	private long timer;
	
	public PlayerView() {
		// Get the sprite associated with the player character (will create image if not loaded yet)
		sprite = SpriteManager.get().getSprite("resources/sprites/Orpheus 24x24.bmp");
		// Now make the color magenta transparent on this image
		sprite.setImage(SpriteManager.get().makeColorTransparent(sprite.getImage(), Color.magenta));
		timer = 0;
	}
	
	/** Draws the player, with the given coordinates describing the top left corner of the bounding box. */
	public void drawPlayer(Graphics g, int x, int y, int playerState, float vx, float vy) {
		// Draw the images flipped if the player is travelling left
		boolean flipped = false;
		if (vx < 0) {flipped = true;}
		
		if (playerState == Orpheus.PLAYER_STANDING) {
			if (vx != 0) {
				// Walking animation;
				if (timer < 200 ){
					sprite.drawPart(g, x, y, 24, 0, 24, 24, flipped);
				} else {
					sprite.drawPart(g, x, y, 0, 0, 24, 24, flipped);
					timer = 0; // Reset animation sequence
				}
			} else {
				// Standing still
				sprite.drawPart(g, x, y, 0, 0, 24, 24, flipped);
				timer = 0;
			}
		} else if (playerState == Orpheus.PLAYER_FALLING) {
			// Cycle between falling animations
			// Velocity positive when FALLING DOWN!
			if (vy <= 0) {
				// Jumping or reached apex of jump
				sprite.drawPart(g, x, y, 2*24, 0, 24, 24, flipped);
			} else {
				// Actually falling towards bottom of screen
				if (timer < 100) {
					sprite.drawPart(g, x, y, 3*24, 0, 24, 24, flipped);
				} else {
					sprite.drawPart(g, x, y, 4*24, 0, 24, 24, flipped);
					timer = 0; // Reset falling animation
				}
			}
		}
		
	}
	
	/** Synchs the PlayerView object. This should be called every cycle. It allows the PlayerView to know when to animate and how long to
	 * delay between each frame. */
	public void update(long delta){
		timer += delta;
	}
	
}
