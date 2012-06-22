package orpheusgame;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/** The SpriteManager allows us to load a lot of image data, and have individual
 *  game objects then reference this data.
 *  For ease of access, the SpriteManager is singleton. We really only need one instance.
 * */
public class SpriteManager {

	private static final SpriteManager SPRT_MNGR = new SpriteManager();

	// An ArrayList containing all the loaded image data. Note that this ArrayList is of type Sprite.
	// A word to the wise: since ArrayList is not thread-safe, we must be sure to do any structural
	// modification in ONE thread. This means adding or removing objects to the list.
	private ArrayList<Sprite> data = new ArrayList<Sprite>();
	
	private SpriteManager() {} // Prevents instantiation of SpriteManager by other classes.

	/** Allows access to the singleton Sprite Manager */
	public static SpriteManager get(){
		return SPRT_MNGR;
	}
	
	/** Redirects to the other getSprite method, defaulting to Transparency.BITMASK. */
	public Sprite getSprite(String ref){
		return getSprite(ref, Transparency.BITMASK);
	}
	
	/** Given a filename, returns a reference to a Sprite with that filename (or else creates a new one if
	 * none exists). Ultimately, we should only ever load a file ONCE. This whole class and method ensures
	 * that this will be so.*/
	public Sprite getSprite(String ref, int transparency){
		
		// Check to see if the Image is already part of the ArrayList...technically a Hash Map would
		// be better for this, but this is a simple implementation and won't likely have to deal with
		// very large ArrayLists.
		
		// Loop through our ArrayList
		for (int i = 0; i < data.size(); i++){
			// If we already have such a file loaded, just return a reference to the object:
			if (data.get(i).getRef() == ref) { return data.get(i);}
		}
		
		// Okay, we don't have this image loaded yet, let's do so:
		BufferedImage source = null;
		try {
			//URL url = this.getClass().getClassLoader().getResource(ref);
			URL url = this.getClass().getResource(ref);
			if (url == null) { System.out.println("Can't find image resource: " + ref); System.exit(1);}
			source = ImageIO.read(url);
		}
		catch (IOException e) {
			System.out.println("Failed to load image resource: " + ref);
			e.printStackTrace();
			System.exit(1);
		}
		
		// Now here's the fun part. We get to create a hardware graphics accelerated image. This should
		// be loads better than a standard BufferedImage.
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		Image gc_image = gc.createCompatibleImage(source.getWidth(), source.getHeight(), transparency);
		
		// Good, we've got a device compatible, but blank (and transparent!), image. Now let's copy our source image to it!
		Graphics2D g2d = (Graphics2D) gc_image.getGraphics();
		g2d.drawImage(source, 0, 0, null);
		
		// Now we've got to add it to our repository: the ArrayList called data:
		Sprite the_new_guy = new Sprite(ref, gc_image);
		data.add(the_new_guy);
		
		// Finally, return
		return the_new_guy;
	}
	
	/** Returns a string containing filenames of all elements of the data list. */
	@Override
	public String toString(){
		String result = "";	
		for (int i = 0; i < data.size(); i++){
			result += data.get(i).getRef() + "\n";
		}
		return result;
	}
	
	/** Takes an image and transforms it so that all pixels of the given color become fully transparent. */
	// Does this need to be static?? I.E public static Image makeColorTransparent...
	public Image makeColorTransparent(Image im, final Color color) {

		ImageFilter filter = new RGBImageFilter() {
			// the color we are looking for... Alpha bits are set to opaque
			public int markerRGB = color.getRGB() | 0xFF000000;

			public final int filterRGB(int x, int y, int rgb) {
				if ((rgb | 0xFF000000) == markerRGB) {
					// Mark the alpha bits as zero - transparent
					return 0x00FFFFFF & rgb;
				} else {
					// nothing to do
					return rgb;
				}
			}
		};

		ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}
	
	/** A fun little image filter shifts the image to look more red, green, blue or some combination thereof. The brighter the given
	 *  color is from middle gray (128, 128, 128) so shall the resultant image be brighter. Any darker from middle gray and the resultant image
	 *  will be darker too. */
	public Image mutateRGB(Image im, final Color color){
		ImageFilter filter = new RGBImageFilter() {

			float rFactor = 1 + (color.getRed() - 128) / 128.0f;
			float gFactor = 1 + (color.getGreen() - 128) / 128.0f;
			float bFactor = 1 + (color.getBlue() - 128) / 128.0f;
			
			// CAN'T ignore alpha channel. You hafta do the full four bytes
			public final int filterRGB(int x, int y, int rgb) {
				int a = rgb & 0xff000000;
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				r = clamp((int)(r * rFactor));
				g = clamp((int)(g * gFactor));
				b = clamp((int)(b * bFactor));
				return a | (r << 16) | (g << 8) | b;
			}
		};

		ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}
	
	/** Clamps the color to between 0 and 255. */
	public int clamp(int c){
		if (c > 255) {c = 255;}
		if (c < 0) {c = 0;}
		return c;
	}
}
