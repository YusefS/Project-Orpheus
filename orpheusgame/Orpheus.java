package orpheusgame;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Random;

import javax.sound.midi.*;

import ljing.*;

/** Orpheus is a platform-jumping game wherein one plays the role of the hero Orpheus
 *  from Grecian myth. This is the main class, representing the controller portion
 *  of the model. */
public class Orpheus extends Program {

	public static void main(String[] args) {
		new Orpheus(600, 600).run();
	}

	//====================/ Fields /===================/

	/** The width (in pixels) of the game world/screen. */
	private int world_width;
	/** The height (in pixels) of the game world/screen. */
	private int world_height;
	
	/** Keeps track of the last known update time. Basically it's how long a frame is. */
	private long avgDelay;
	
	/** The player's health. Since max_health will be 100, this will be a %. */
	private int health;
	/** Maximum health should be 100. */
	public final int max_health;
	/** The current score of the player. */
	private int score;
	/** The player's x and y location (measured from the top left corner of the player's rectangle), and x and y velocities.*/
	private float x, y, vx, vy;
	/** The width of the player, in pixels. */
	private int player_width;
	/** The height of the player, in pixels. */
	private int player_height;
	/** This is a downward acceleration, applied per frame to falling objects. Measured in pixels/second*second. */
	private float gravity;
	/** The speed at which the player walks, in pixels per second.*/
	private float walk_speed;
	/** The speed at which the player moves while falling or jumping, in pixels per second. */
	private float air_influence;
	/** How much to propel the player in the air when they jump. */
	private float jump_velocity;
	/** The maximum speed in any cardinal direction (in pixels per second). */
	public final int max_speed;
	/** The state of the player, e.g. WALK_RIGHT, FALLING, JUMPING, etc. */
	private int player_state;
	/** How to enumerate constants again? */
	public static final int PLAYER_STANDING = 0;
	//public static final int PLAYER_WALK_LEFT = 1;
	//public static final int PLAYER_WALK_RIGHT = 2;
	public static final int PLAYER_FALLING = 3;
	//public static final int PLAYER_JUMPING = 4;
	public static final int PLAYER_DEAD = 5;
	
	/** The current display state of the game, e.g. main menu, playing, etc. */
	private int game_state;
	/** The main screen of the game. */
	public static final int GAME_MAIN_MENU = 0;
	/** The player is playing the game. */
	public static final int GAME_PLAYING = 1;
	/** The player has lost all health. */
	public static final int GAME_OVER = 2;
	/** The player has defeated a level. */
	public static final int GAME_WON = 3;
	/** Signal to exit the program. */
	public static final int GAME_QUIT = 4;
	/** Displaying "How-to-play" screen. */
	public static final int GAME_ABOUT = 5;
	/** Selecting a MIDI file to play. */
	public static final int GAME_LVL_SELECT = 6;
	
	
	/** The song that holds the data for the currently selected level. */
	private Song song;
	
	/** The level data, transformed into platform objects. */
	private ArrayList<Platform> level;
	private int levelSize; // The number of platforms in the level
	/** The offset of the platforms. sAs they scroll by, px will become more and more negative. 'py' will
	 *  normally remain 0, but is included here for completion. */
	private float plat_x, plat_y;
	/** Speed at which the platforms move -- this depends on the tempo of the music.*/
	private float platform_speed;
	/** What fraction of the note's duration is occupied by a physical note. */
	public float platform_fill_factor;
	/** The height (in pixels) of all platforms. */
	private int platform_height;
	
	/** Just for fluff, this counts the number of times you select 'move' in the game. */
	int movecount = 0;
	
	//Random number generator
	private Random RNG;
	
	//===================/ Methods /===================/
	
	/** Create a new game of Orpheus with the given width and height. */
	public Orpheus(int width, int height){
		// Initialize random number generator
		RNG = new Random();
		
		// Set up the size of the world/screen
		world_width = width;
		world_height = height;
		
		platform_height = 16;
		
		platform_fill_factor = 0.8f;
		platform_speed = 75.0f;
		
		player_width = 18;
		player_height = 24;
		
		health = 100;
		max_health = 100;
		
		// Arbitrary values that need to be tweaked to find the most satisfying/realistic.
		gravity = 100.0f;
		walk_speed = 125.0f;
		air_influence = 75.0f;
		jump_velocity = 200.0f;
		max_speed = 175;
		//
		
		level = new ArrayList<Platform>();
		
		game_state = GAME_MAIN_MENU;
		// Create a new midi-handling song object
		song = new Song();
		if (!song.loadDevices()){
			printLine("Failed to set up MIDI devices!");
			quit();
		}
	}
	
	/** The main game loop. */
	public void run(){
		
		int input = 0;
		do {
			switch(game_state){
				case GAME_MAIN_MENU:
					printLine("+-~-~-~-~-~-~-~-~-~+ Main Menu +~-~-~-~-~-~-~-~-~-~-~+");
					printLine("Welcome to Orpheus. Select an option below:");
					printLine("1) Play Game");
					printLine("2) How to Play");
					printLine("3) Quit");
					input = readInt("What would you like to do?\n>");
						switch (input){
						case 1:
							// Play
							game_state = GAME_LVL_SELECT;
							break;
						case 2:
							// How to Play
							game_state = GAME_ABOUT;
							break;
						case 3:
							// Quit
							game_state = GAME_QUIT;
						default:
						}
					break;
				case GAME_LVL_SELECT:
					printLine("+-~-~-~-~-~-~-~-~-~+ Level Select +~-~-~-~-~-~-~-~-~-~-~+");
					printLine("Available songs:");
					// We need to list all available midi files in /resources/music/
					// and enumerate them (0, 1, 2, 3, etc).
					
					File songs[] = enumerateSongs();
					
					// Print the contents to the screen
					if (songs.length == 0) {
						printLine("There are no available songs to play!\n");
						game_state = GAME_MAIN_MENU;
						break;
					}
					
					for (int i = 0; i < songs.length; i++){
						if (songs[i].isFile()){
							printLine("    " + i + ") " + songs[i].getName());
						}
					}
					
					// Get user input (repeat question until answer is in list).
					do {
						input = readInt("Select a song number from above:\n>");
					} while (input >= songs.length || input < 0);
					
					// Okay, now we should have a selected file. We need to load it and play the game.
					if (loadSong(songs[input])) {
						printLine("Successfully loaded " + song.getSource().getName());
						printLine("Length of song(ms) = " + song.getLength());
						printLine("Tempo of song (ms per tick) = " + song.getTempo());
						
						Platform p;
						for (int j = 0; j < level.size(); j++) {
							p = level.get(j);
							printLine("Creating new platform at x = " + p.getX() + ", y = " + p.getY() + " with pitch " + p.getNote());
						}
						
						game_state = GAME_PLAYING;
					}
					else {
						printLine("An error occurred while loading song!");
						game_state = GAME_MAIN_MENU;
					}
					break;
				case GAME_ABOUT:
					printLine("+-~-~-~-~-~-~-~-~-~+ How to Play +~-~-~-~-~-~-~-~-~-~-~+");
					printLine("Here's how to play the game of Orpheus! This is a stub for now.");
					readLine(">Return to Main Menu...");
					// Go back to menu
					game_state = GAME_MAIN_MENU;
					break;
				case GAME_PLAYING:
					printLine("Here is where you would play the game. For now, just listen to the music" +
							" and imagine how much fun you would be having!");
						
					// Set up the player
					resetPlayer();
					// Start the song
					song.play();
					
					while (game_state == GAME_PLAYING) {
						// Gives a text version of the level. If something happens to change the game_state
						// then we stop doing this and continue on.
						simulatePlayingTheGame();
					}
					break;
				case GAME_OVER:
					printLine("+-~-~-~-~-~-~-~-~-~+ Game Over +~-~-~-~-~-~-~-~-~-~-~+");
					printLine("FAILURE!");
					printLine("You have succumbed to the cruel hand of Fate, and must now reside in the" +
							" Underworld forever.");
					readLine(">Return to Main Menu...");
					// Go back to menu
					game_state = GAME_MAIN_MENU;
					break;
				case GAME_WON:
					printLine("+-~-~-~-~-~-~-~-~-~+ Level Complete! +~-~-~-~-~-~-~-~-~-~-~+");
					printLine("SUCCESS!");
					printLine("You defeated the level with the following statistics:");
					printLine("Score: " + score);
					readLine(">Return to Main Menu...");
					// Go back to menu
					game_state = GAME_MAIN_MENU;
					break;
				case GAME_QUIT:
					// No need to do anything here. It's handled after this loop.
					break;
				default:
			}
		} while (game_state != GAME_QUIT);
		// End of loop
		quit();
	}
	
	
	/** Lists the songs available for play in resources/music. At present, this WILL list files which
	 *  are not of type .mid, so be careful! */
	public File[] enumerateSongs(){
		/* Hall of Shame: All the file loaders that didn't work...
		File dir = new File(System.getProperty("user.dir"));
		String path = this.getClass().getResource("").getPath();
		*/
		
		// Get the location of our running class
		String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// Decode out of spaces and other special characters
		try {path = URLDecoder.decode(path, "UTF-8");} 
		catch (UnsupportedEncodingException e) {
			printLine("UnsupportedEncodingException");
			e.printStackTrace();
			System.exit(1);
		}
		// Add on the location of the package and the resources folder.
		path += this.getClass().getPackage().getName() + "/resources/music/";
		File dir = new File(path);
		
		if (!dir.exists()) {
			printLine("Error! Directory '/resources/music/' doesn't exist!"); 
			System.exit(1);
		}
		
		// List all the files in the directory
		File songs[] = dir.listFiles();
		return songs;
	}
	
	/** Loads the specified midi file into memory and sets up the game world. Returns true
	 *  if the operation succeeded; false otherwise. */
	public boolean loadSong(File source){
		
		// If the data is loaded, returns true. If a problem occurred, returns false.
		if (!song.loadData(source)){return false;}
		
		// Turn the song into platforms (first destroying the previous incarnation, if it exists):
		level.clear();
		plat_x = 0;
		plat_y = 0;
		
		// The naive way to create a level is to just look through the MIDI sequence and make a platform
		// for every NoteOn message, and to make the platforms all the same width;
		MidiEvent mEvent;
		MidiMessage mMSG;
		int iStatus; // The status byte of the message
		int iNote; // The data byte of the message
		int iVelocity = 0; // The 'pressure' of the note. If this is zero then this is technically a NoteOff message
		long lTick; // The timestamp of the message, i.e. when the message is sent.
		Track[] tracks = song.getTracks();
		int activeTrack = 0; // The track with the data
		int platWidth = 20; // The width of the platform
		
		if (tracks.length == 1) {
			activeTrack = 0;
		}
		else if (tracks[0].ticks() < tracks[1].ticks()) {
			activeTrack = 1;
		}
		
		// Data is usually stored on the second track, not the first. Note that size() is the number of
		// MIDI events in the track. (We could loop through all tracks -- this would probably be a good idea)
		for (int i = 0; i < tracks[activeTrack].size(); i++){
			mEvent = tracks[activeTrack].get(i);
			mMSG = mEvent.getMessage();
			iStatus = (int) (mMSG.getMessage()[0] & 0xFF);
			iNote = (int) (mMSG.getMessage()[1] & 0xFF);
			
			// We can only access velocity in certain messages - this code avoids ArrayIndexOutOfBounds
			if (mMSG.getMessage().length > 2) {
				iVelocity = (int) (mMSG.getMessage()[2] & 0xFF);
			}
			
			lTick = mEvent.getTick();
			// 144 = NoteOn message (MIDI Specification)
			if (iStatus == 144 && iVelocity != 0) {
				// TODO The size and position of the platform will depend on many factors
				
				// Only allow musical notes within the correct octaves
				if (iNote > 88 || iNote < 60) {continue;}
				
				// Okay, let's make the notes the RIGHT width! Check until we get to the next message
				for (int j = (i+1); j < tracks[activeTrack].size(); j++) {
					MidiMessage jMSG = tracks[activeTrack].get(j).getMessage();
					int jStatus = (int) (jMSG.getMessage()[0] & 0xFF);
					int jNote = (int) (jMSG.getMessage()[1] & 0xFF);
					// if this is a note on or off message referring to the same note
					if (jStatus == 128 || jStatus == 144 && jNote == iNote) {
						// Then we now know the width of the previous note
						// This is the time difference between the two note events
						long diff = tracks[activeTrack].get(j).getTick() - lTick;
						platWidth = (int) (platform_fill_factor * diff * song.getTempo() / 1000.0 * platform_speed);
						break; // Done looking!
					}
				}
				// TODO only add TOP notes, i.e. notes with nothing below them
				// To do this, we need only look at notes at the same midi tick
				level.add(new Platform(platWidth, (int) (lTick * song.getTempo() / 1000.0 * platform_speed), world_height - 50 - (iNote - 60) * platform_height, iNote));
			}
		}
		
		// TODO Song tempo needs to be analyzed in order to determine speed of notes
		//platform_speed = 10.0f;
		levelSize = level.size();
		
		return true;
	}
	
	/** Returns the Song object which controls sound in the game. */
	public Song getSong(){
		return song;
	}
	
	/** Releases resources, stops music, ends the game, and closes down the program. */
	public void quit(){
		//printLine("+-~-~-~-~-~-~-~-~-~+ Exit Game +~-~-~-~-~-~-~-~-~-~-~+");
		//printLine("Thanks for playing!");
		song.stop();
		System.exit(0);
	}
	
	
	/** Just gives a text version of the game. */
	public void simulatePlayingTheGame(){
		printLine("+-~-~-~-~-~-~-~-~-~+ Game World +~-~-~-~-~-~-~-~-~-~-~+");
		printLine("You are currently playing the song " + song.getSource().getName());
		printLine("Health: " + health);
		printLine("Score: " + score);
		printLine("Your options are:");
		printLine("1) Jump to my death");
		printLine("2) Move around a bit");
		printLine("3) Play a note");
		printLine("4) Stop playing");
		int input = readInt("What would you like to do?\n>");
			switch (input){
			case 1:
				// Hurt the player
				printLine("Geronimooooo! You smile and leap off the platform with gusto...\n");
				damagePlayer(25);
				if (health > 0) {
					printLine("...Ouch that hurt! But you manage to muster up enough willpower to continue on.");
				}
				else {
					printLine("...and you die! Congratulations!");
					// Don't need to manually change game_state here; damagePlayer will do that.
					song.stop();
				}
				break;
			case 2:
				// Move
				movecount++;
				if (movecount == 0){
					printLine("You test the strength of your limbs. Yep, flexible and robust.");
				} else if (movecount == 1){
					printLine("You jump deftly from one platform to another: this is fun!");
				} else if (movecount == 2){
					printLine("You can see the light of day just a bit ahead - you're almost there!");
				} else {
					printLine("Huzzah! With one last bound, you escape the darkness behind you and" +
							" emerge into the light of a fine Summer day.");
					song.stop();
					game_state = GAME_WON;
				}
				break;
			case 3:
				// Play a note
				printLine("You strum a few chords on your lyre...");
				// access song and have it play a chord...
				song.noteOn(60, 127, 0);
				song.noteOn(65, 127, 0);
				song.noteOn(72, 127, 0);
				try { Thread.sleep(1000);}
				catch (InterruptedException e) {e.printStackTrace();}
				printLine("My, doesn't that sound great?");
				song.noteOff(60, 127);
				song.noteOff(65, 127);
				song.noteOff(72, 127);
				// etc...
				break;
			case 4:
				// Return to main menu
				printLine("Well, that was fun. But you're not really into it, so you decide to quit.");
				song.stop();
				game_state = GAME_MAIN_MENU;
				break;
			default:
			}
	}
	
	
	//=========================/ Getters & Setters /========================================/
	
	/** Returns the ArrayList containing the current level data. */
	public ArrayList<Platform> getLevel(){
		return level;
	}
	
	/** Returns the number of notes in the current level. */
	public int getLevelSize(){
		return levelSize;
	}
	
	/** Returns the width (in pixels) of the game world/screen. */
	public int getWidth(){
		return world_width;
	}
	
	/** Returns the height (in pixels) of the game world/screen. */
	public int getHeight(){
		return world_height;
	}
	
	
	/** Sets the state of the game, i.e. GAME_PLAYING, GAME_LVL_SELECT etc. */
	public void setGameState(int new_state){
		this.game_state = new_state;
	}
	
	/** Returns the current state of the game, as enumerated in Orpheus.java */
	public int getGameState(){
		return game_state;
	}
	
	//================================/ Player Methods /=============================/
	
	/** Returns the width of the player (in pixels). */
	public int getPlayerWidth(){
		return player_width;
	}

	/** Returns the height of the player (in pixels). */
	public int getPlayerHeight(){
		return player_height;
	}
	
	/** Adds the value to the player's current score. */
	public void modScore(int value){
		score += value;
	}
	
	/** Returns the player's current score. */
	public int getScore(){
		return score;
	}
	
	/** Resets the player to pristine state, ready to play a level from the start. */
	public void resetPlayer(){
		health = max_health;
		score = 0;
		player_state = PLAYER_STANDING;
		x = 0;
		y = 0;
	}
	
	/** Returns the current health of the player. Since max_health is 100, this number is a percent. */
	public int getHealth(){
		return health;
	}

	/** Damages the player. If the player has no life left, this kills the player.*/
	public void damagePlayer(int percent){
		health -= percent;
		if (health <= 0) {
			// Need to kill the player here and end the current game
			game_state = GAME_OVER;
			song.stop();
		}
	}
	
	/** Heals the player for the given amount. A player cannot have more than 100% health. */
	public void healPlayer(int percent){
		health = Math.min(health + percent, max_health);
	}
	
	/** Returns the x position of the player (measured from the top left corner of the player rectangle). */
	public float getX(){
		return x;
	}

	/** Returns the y position of the player (measured from the top left corner of the player rectangle). */
	public float getY(){
		return y;
	}
	
	
	/** Returns the velocity of the player in the x-direction. */
	public float getVX(){
		return vx;
	}
	
	/** Returns the velocity of the player in the x-direction. */
	public float getVY(){
		return vy;
	}
	
	/** Called when the user tries to press the move right key. */
	public void moveRight(){
		if (player_state == PLAYER_STANDING) {
			vx = walk_speed;
		} else {
			// We're in the air, need to enact "air influence" as it's called
			vx = air_influence;
		}
	}
	
	/** Called when the user tries to press the move left key. */
	public void moveLeft(){
		if (player_state == PLAYER_STANDING) {
			vx = -walk_speed;
		} else {
			// We're in the air, need to enact "air influence" as it's called
			vx = -air_influence;
		}
	}
	
	/** Called when the user presses the jump key. */
	public void jump(){
		// Can't jump when you're falling!
		if (player_state == PLAYER_STANDING){
			vy -= jump_velocity;
			player_state = PLAYER_FALLING;
		}
	}
	
	/** When a player presses the down button. If the player is on a platform, this would cause them to fall through
	 *  it. This is currently disabled as a design decision. */
	public void moveDown(){
		if (player_state == PLAYER_STANDING) {
			// Nothing to do here
		} else if (player_state == PLAYER_FALLING) {
			vy += air_influence;
		}
	}
	
	/** Called when the player presses the shoot button. Currently disabled. */
	public void shoot(){
		// TODO Need to create a PROJECTILE class and incorporate it here
		// Set up some kind of max_firing_rate too
	}
	
	/** Returns the current state of the player. */
	public int getPlayerState(){
		return player_state;
	}
	
	/** Updates the player by the amount determined by delta -- the time which has passed.*/
	public void updatePlayer(long delta){
		// Keep track of how long delay was this frame
		avgDelay = delta;
		// Assume the player is falling...if they are standing on something then this will be changed.
		player_state = PLAYER_FALLING;
		// Check for collision
		// For now, just loop through the entire level and see if we hit any platforms
		// In a song with >1000 platforms, this could be slow. Before we optimize let's get it working.
		
		// Collision with sides of the screen
		// Left and right
		if (x < 0) {
			// Stop player movement
			vx = 0;
			// If they managed to get past the screen a bit, let's pop them back into the world
			x = 0;
		} else if ((x + player_width) > world_width) {
			x = world_width - player_width;
			vx = 0;
		}
		
		// Top and bottom
		if (y < 0) {
			if (vy < 0) {vy = 0;}
			y = 0;
		} else if ((y) > world_height) {
			// Damage the player and reset them
			damagePlayer(25);
			// We need to set them atop a safe platform somehow
			// For now:
			vy = 0;
			y = 0;
		}
		
		// Collision with platforms
		Platform p;
		
		for (int i = 0; i < level.size(); i++){
			p = level.get(i);
			playerCollisionWithPlatform(p);
		}
		
		// Move the player based on state
		if (player_state == PLAYER_FALLING) {
			// acceleration due to gravity, limited by max_speed
			vy = vy + Math.min((gravity) * (delta/1000.0f), max_speed);
			y += vy * (delta/1000.0f);
		} 
		// Falling, jumping, or on the ground: move the player left/right.
		x += vx * (delta/1000.0f);
		vx= 0; // Reset the horizontal velocity; The game is more satisfying this way.
		
		// This makes the player move along with the platforms when they're standing on them, rather than slipping
		// But I've found it's more fun without
//		if (player_state == PLAYER_STANDING) {
//			x -= (platform_speed * delta / 1000.0f); 
//		}
//		
		// Limit vertical speed
		vy = Math.min(vy, max_speed);
		vy = Math.max(vy, -max_speed);
	}
	
	public void playerCollisionWithPlatform(Platform p){
		
		// Is the platform within the bounds of the screen? If it isn't, then the player can't collide with it
		if (!platformInWorld(p)) {return;}			
		
		float left_overlap = 0;
		float top_overlap = 0;
		float right_overlap = 0;
		float bottom_overlap = 0;
		
		right_overlap = (p.getX() + p.getWidth() + plat_x) - x; // How much to the left of the right side of the platform we are (positive if overlap)
		top_overlap = (y + player_height) - (p.getY() + plat_y); // How much below the surface of the platform we are (positive if overlap)
		
		// Extend the bounding box a little according to how fast we are moving; if we are falling faster we need to check for collisions a little less restrictively
		// in order to catch them all!
		// How much are we going to fall in the next frame???
		int displacement = (int) (vy * avgDelay / 1000.0);
		top_overlap += displacement;
		
		left_overlap = (x + player_width) - (p.getX() + plat_x); // How much to the right of the left side of the platform we are (positive if overlap)
		bottom_overlap = (p.getY() + platform_height + plat_y) - (y); // How much above the underside of the platform we are (positive if overlap)
		
		// Add one to the top_overlap for the purposes of calculating if PLAYER_STANDING. Otherwise the player goes spastic
		float top_overlap_adj = top_overlap + 1;
		
		if (top_overlap_adj <= 0) {return;}
		if (bottom_overlap <= 0) {return;}
		if (right_overlap <= 0) {return;}
		if (left_overlap <= 0) {return;}
		
		// There has been some kind of collision
		if (p.getState() == Platform.platform_polluted) {
			score += 1;
			p.setState(Platform.platform_cleansed);
			song.noteOn(45, 100, System.currentTimeMillis() + 500);
			// Increase the player's willpower/health
			healPlayer(1);
		}
		
		// Find the smallest overlap and take care of that (it represents the most efficient way to move the player such that they are no longer intersecting the rectangle):
		
		if (top_overlap_adj <= left_overlap && top_overlap_adj <= right_overlap && top_overlap_adj <= bottom_overlap) {
			// We have collided with the top of the platform
			// Now the player is standing
			player_state = PLAYER_STANDING;
			vy = 0;
			y = p.getY() - player_height + plat_y;
		} else if (right_overlap <= left_overlap && right_overlap <= top_overlap && right_overlap <= bottom_overlap) {
			// We have collided with the right side of the platform
			vx = 0;
			x = p.getX() + p.getWidth() + plat_x;
		} else if (left_overlap <= top_overlap && left_overlap <= right_overlap && left_overlap <= bottom_overlap) {
			// We have collided with the left side of the platform
			vx = 0;
			x = p.getX() + plat_x - player_width;
		} else {
			// We have collided with the bottom of the platform
			// Do nothing for now
		}
		

	}
	
	//===================/ /===========================/
	
	/** Runs one cycle of the game, of length delta (in milliseconds). */
	public void cycle(long delta){
		// Update the player
		updatePlayer(delta);
		// Move the platforms
		updatePlatforms(delta);
	}
	
	
	//============================================/ Platform Methods /=============================/
	
	/** Moves the platforms, and plays the note if it reaches the left edge of the screen. */
	public void updatePlatforms(long delta){
		plat_x = plat_x - (platform_speed) * (delta / 1000.0f);
		Platform p;
		for (int i = 0; i < level.size(); i++){
			p = level.get(i);
			// If the rightmost edge of the platform has left the screen
			if ((p.getX() + plat_x + p.getWidth()) < 0) {
				// Was the platform activated?
				if (p.getState() == Platform.platform_cleansed) {
					// Increase the score of the player and play the sound

				} else if (p.getState() == Platform.platform_polluted) {
					// The player gets no points, and some discordant sound is produced
					// TODO consider whether or not to leave out this line
					//song.noteOn((p.getNote() + 6 + RNG.nextInt(3)) % 127, 127, System.currentTimeMillis() + 50);
				}
				// Kill the platform
				level.remove(i);
			}
		}
	}
	
	
	/** Returns true if the given platform is currently within screen coordinates. */
	public boolean platformInWorld(Platform p) {
		
		Rectangle r = new Rectangle();
		r.setBounds(0, 0, world_width, world_height);		
		
		if (!r.contains((int) (p.getX() + plat_x), (int) (p.getY() + plat_y)) &&
				!r.contains((int) (p.getX() + plat_x), (int) (p.getY() + plat_y + platform_height)) &&
				!r.contains((int) (p.getX() + plat_x + p.getWidth()), (int) (p.getY() + plat_y)) &&
				!r.contains((int) (p.getX() + plat_x + p.getWidth()), (int) (p.getY() + plat_y + platform_height))) {
					// This platform is not in the world
					return false;
				}
		return true;
	}
	
	/** Gets the height (in pixels) of all platforms. */
	public int getPlatformHeight(){
		return platform_height;
	}
	
	/** Returns the offset of the platforms in the x-axis; this is how far left/right the platforms have moved (in pixels).*/
	public float getPlatformOffsetX(){
		return plat_x;
	}
	
	/** Returns platform speed (in pixels per second) */
	public float getPlatformSpeed(){
		return platform_speed;
	}
	
	/** Sets the platform speed (in pixels per second). */
	public void setPlatformSpeed(float speed){
		platform_speed = speed;
	}
	
	/** Returns the current gravity of the game world (in pixels per second per second). */
	public float getGravity() {
		return gravity;
	}
	
	
	//========================================/ Miscellaneous /========================================/
	
	/** Gives a String representation of the current state of the player. */
	public String toString(){
		String out = "Player: at (" + x + ", " + y + ")\n";
		out += "Velocity: (" + vx + ", " + vy + ")\n";
		out += "Player State = " + player_state;
		return out;
	}
}
