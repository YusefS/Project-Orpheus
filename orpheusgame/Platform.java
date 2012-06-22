package orpheusgame;

/** A single platform which Orpheus may (or may not) jump upon. The attributes of a
 *  particular platform depend upon the musical note from which it was created. */
public class Platform {

	/** The width, in pixels, of this platform. */
	private int width;
	/** The x and y coordinate of this platform, relative to it's parent Layer. */
	private int x, y;
	/** The musical note represented by this platform. Follows MIDI specifications, i.e. 0-128 (middle c is
	 *  note number 60. */
	private int note;
	
	/** The state of the platform. A platform may be polluted or cleansed. */
	private int state;
	/** Platforms start out polluted, and must be cleansed by the player. */
	public static final int platform_polluted = 0;
	/** A cleansed platform makes a pleasing sound when it reaches the end of the screen. Otherwise, 
	 *  it will make a discordant sound. */
	public static final int platform_cleansed = 1;
	/** The note has been sounded and left the screen. */
	public static final int platform_played = 2;
	/** There may be other states that the platform can be in -- perhaps spiked, or ghostly. */
	
	/** Width, initial x and y position - in pixels.*/
	public Platform(int width, int x, int y, int pitch){
		this.width = width;
		this.x = x;
		this.y = y;
		state = platform_polluted;
		this.note = pitch;
	}

	/** Returns the width, in pixels, of the platform. */
	public int getWidth(){
		return width;
	}
	
	/** Returns the x coordinate, in pixels, of the platform. */
	public int getX(){
		return x;
	}

	/** Returns the y coordinate, in pixels, of the platform. */
	public int getY(){
		return y;
	}
	
	/** Returns the musical note that is represented by this platform. */
	public int getNote(){
		return note;
	}
	
	/** Sets the musical note that is represented by this platform. Must be within the range
	 *  of 0 - 128. */
	public void setNote(int n){
		// Constrain 'note' to values from 0 to 127.
		note = n % 128; 
	}
	
	/** Returns the current state of the platform. */
	public int getState(){
		return state;	
	}
	
	/** Changes the state of the platform. */
	public void setState(int new_state){
		state = new_state;
	}
	
}



