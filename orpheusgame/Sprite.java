package orpheusgame;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Transparency;

/** The image class used by the Sprite Manager. */
public class Sprite {

	private String ref; // The unique name of the file associated with this Sprite
	private Image image; // The reference to a Sprite Manager image;

	
	/** To construct a Sprite, one must give the file name of the image. */
	public Sprite(String ref, Image image){
		this.ref = ref;
		this.image = image;
	}

	/** Returns the filename associated with this sprite's image. */
	public String getRef(){
		return ref;
	}
	
	/** Returns the image associated with this sprite. */
	public Image getImage(){
		return image;
	}
	
	/** Sets the image. */
	public void setImage(Image img){
		this.image = img;
	}
	
	/** Returns the width of the Sprite, in pixels. */
	public int getWidth(){
		return image.getWidth(null);
	}
	
	/** Returns the height of the Sprite, in pixels. */
	public int getHeight(){
		return image.getHeight(null);
	}
	
	/** Draws this sprite at the specified coordinates and graphics context. */
	public void draw(Graphics g, int x, int y){
		g.drawImage(image, x, y, null);
	}
	
	/** Draws this sprite at the specified coordinates and graphics context except that it will be drawn flipped horizontally!. */
	public void drawFlipped(Graphics g, int x, int y){
		g.drawImage(image, x + image.getWidth(null), y, x, y + image.getWidth(null), 0, 0, image.getWidth(null), image.getHeight(null), null);
	}
	
	/** Draws a rectangular portion of the image held within this sprite. 
	 *  @param g The graphics context upon which the image will be drawn. 
	 *  @param dx destination x coordinate of the top left corner
	 *  @param dy destination y coordinate of the top left corner
	 *  @param sx source coordinate (top left corner)
	 *  @param sy source coordinate (top left corner)
	 *  @param width the width of the image
	 *  @param height the height of the image.
	 *  @param flipped whether or not the image is to be flipped horizontally */
	public void drawPart(Graphics g, int dx, int dy, int sx, int sy, int width, int height, boolean flipped) { 
		if (!flipped) {
			g.drawImage(image, dx, dy, dx+width, dy+height, sx, sy, sx+width, sy+height, null);
		} else {
			g.drawImage(image, dx+width, dy, dx, dy+height, sx, sy, sx+width, sy+height, null);
		}
	}

}
