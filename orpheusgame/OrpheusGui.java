package orpheusgame;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class OrpheusGui implements TimerOwner, PanelOwner {

	
	public static void main(String[] args) {
		new OrpheusGui();
	}
	
	private JFrame jWnd;
	private Timer timer;
	private Panel panel;
	
	private Orpheus game;
	
	// Random number generator for general use
	private Random RNG;
	
	private PlayerView pView;
	private PlatformView platView;
	
	private ArrayList<Tile> tArray; // Stores all the background tiles
	private float tileOffset; // Changes as the background moves
	private Sprite[] sWater; // For the background tiles
	private Sprite[] sBottom; // Goes above the water tiles
	private float tileSpeed; // The speed at which tiles move from right to left
	
	// The next game state to move into - for the MENU system
	private int menuSelection;
	// The splash screen title text
	private Sprite sprMain;
	// The cursor used to select menu items
	private Sprite cursor;
	// The font used for most of the text within the game
	private Font fOrpheus;
	// The gold awesome color
	public static final Color cGold = new Color(0xFF, 0xDC, 0x20);
	// The background for most of the game. It's grayscale so we can add cool colors to it later
	private Sprite cave;
	
	public OrpheusGui(){
		jWnd = new JFrame("Project Orpheus");
		jWnd.setResizable(false);
		jWnd.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jWnd.setLocationRelativeTo(null);
		jWnd.setIgnoreRepaint(true);	
		panel = new Panel(800, 600, this);
		jWnd.add(panel); // Since panel is a JPanel, we must add() it in order for it to be visible!	
		jWnd.pack();
		jWnd.setVisible(true);	
		game = new Orpheus(800, 600);
		fOrpheus = new Font("Blackmoor LET", Font.PLAIN, 30);
		
		// Initialize the random number generator
		RNG = new Random();
		
		// Make the color
		//cGold = new Color(0xFF, 0xDC, 0x20);
		// Load up splash art (for menu)
		sprMain = SpriteManager.get().getSprite("resources/sprites/Splash Screen Lucida.png", Transparency.TRANSLUCENT);
		// Load cursor
		cursor = SpriteManager.get().getSprite("resources/sprites/cursor.png", Transparency.TRANSLUCENT);
		// Load the cave background
		cave = SpriteManager.get().getSprite("resources/sprites/cave.bmp", Transparency.OPAQUE);
		cave.setImage(SpriteManager.get().mutateRGB(cave.getImage(), new Color(32, 18, 0)));
		
		// Set up main menu splash screen cursor to point at the first object
		menuSelection = 0;

		pView = new PlayerView();
		game.setPlatformSpeed(75.0f);				
		platView = new PlatformView();
		
		// Set up the background tiles
		Color cMutate = new Color(90, 115, 64);
		tileSpeed = -1.0f;
		sWater = new Sprite[3];
		sWater[0] = SpriteManager.get().getSprite("resources/sprites/Water 1.bmp");
		sWater[0].setImage(SpriteManager.get().mutateRGB(sWater[0].getImage(), cMutate));
		
		sWater[1] = SpriteManager.get().getSprite("resources/sprites/Water 2.bmp");
		sWater[1].setImage(SpriteManager.get().mutateRGB(sWater[1].getImage(), cMutate));
		
		sWater[2] = SpriteManager.get().getSprite("resources/sprites/Water 3.bmp");
		sWater[2].setImage(SpriteManager.get().mutateRGB(sWater[2].getImage(), cMutate));
		
		sBottom = new Sprite[2];
		sBottom[0] = SpriteManager.get().getSprite("resources/sprites/Cave Bottom 1.png");
		sBottom[0].setImage(SpriteManager.get().mutateRGB(sBottom[0].getImage(), cMutate));
		
		sBottom[1] = SpriteManager.get().getSprite("resources/sprites/Cave Bottom 2.png");
		sBottom[1].setImage(SpriteManager.get().mutateRGB(sBottom[1].getImage(), cMutate));
		
		tArray = new ArrayList<Tile>();
		// Create water tiles
		for (int i = 0; i < panel.getWidth()/64 + 2; i++){
			Tile t = new Tile();
			t.setX(i*64);
			t.setY(panel.getHeight() - 64);
			t.setSprite(sWater[RNG.nextInt(3)]);
			tArray.add(t);
		}
		// Create more tiles
		for (int i = 0; i < panel.getWidth()/64 + 2; i++){
			Tile t = new Tile();
			t.setX(i*64);
			t.setY(panel.getHeight() - 128);
			t.setSprite(sBottom[RNG.nextInt(2)]);
			tArray.add(t);
		}
		
		// Harp instrument
		game.getSong().setChannelInstrument(0, 46);
		
		// TODO Implement a quick loading screen...it takes a few seconds to get anything on the screen
		// Let's have a nice menu song too
		if (game.loadSong(new File("bin/orpheusgame/resources/music/Epica.mid"))){
			game.getSong().play();
		}
		
		
		timer = new Timer(25, this);
		timer.start();
	}
	
	public void cycle(long delta) {
		
		tileOffset += tileSpeed;
		
		if (game.getGameState() == Orpheus.GAME_PLAYING) {
			game.updatePlatforms(delta);
			game.updatePlayer(delta);
			//Update the PlayerView
			pView.update(delta);
			// Move the tile background
		}
		
		// Allow the music to update itself, e.g. turn off notes and such
		game.getSong().updateNotes(System.currentTimeMillis());
		
		HandleKeys();
		
		
		// Did the player win?
		if (game.getGameState() == Orpheus.GAME_PLAYING && game.getSong().isOver() == true) {;
			game.setGameState(Orpheus.GAME_WON);
		}
		
		// Trigger a repaint
		panel.repaint();
	}

	/** Whenever a repaint of the panel is called, it will respond by giving us a
	 *  graphics object to control drawing with. */
	public void drawGame(Graphics g) {
		
		// Always draw the cave background
		cave.draw(g, 0, 0);		
		
		// Always draw the background tiles
		Tile t;
		for (int i = 0; i < tArray.size(); i++) {
			t = tArray.get(i);
			t.drawTile(g, (int) tileOffset);
			// If a tile goes off the screen, we need to reset it
			if (t.getX() + tileOffset + 64 < 0){
				t.setX(t.getX() + 64*(panel.getWidth()/64 + 2));
				// Should also reset the image to something random for fun
			}
		}
		
		g.setFont(fOrpheus);
		g.setColor(cGold);
		
		if (game.getGameState() == Orpheus.GAME_MAIN_MENU) {
			sprMain.draw(g, (game.getWidth() - sprMain.getWidth()) / 2, 100);
			g.setColor(cGold);
			g.drawString("Play", 365, 300);
			g.drawString("About", 355, 375);
			g.drawString("Quit", 365, 450);
			cursor.draw(g, 280, 275 + 75*menuSelection);//
			cursor.drawFlipped(g, 470, 275 + 75*menuSelection);
		} else if (game.getGameState() == Orpheus.GAME_ABOUT) {
			sprMain.draw(g, (game.getWidth() - sprMain.getWidth()) / 2, 100);
			g.drawString("HOW TO PLAY", 300, 210);
			g.drawString("Use the arrow keys to move and the Z key to jump.", 90, 300);
			g.drawString("Touch the dead notes to enliven them - and score points!", 40, 350);
		} else if (game.getGameState() == Orpheus.GAME_LVL_SELECT) {
			sprMain.draw(g, (game.getWidth() - sprMain.getWidth()) / 2, 100);
			g.drawString("SELECT LEVEL", 300, 225);
			File[] fList = game.enumerateSongs();
			//Will only display the first 10 songs! Otherwise they'd trail off the screen
			for (int i = 0; i < 10; i++) {
				if (fList.length <= i) {
					g.drawString("...", 280, 300 + i*25);
				} else {
				g.drawString(fList[i].getName(), 280, 300 + i*25);
				}
			}
			// The cursor
			cursor.draw(g, 200, 275 + 25*menuSelection);
			//cursor.drawFlipped(g, 555, 275 + 25*menuSelection);
		} else if (game.getGameState() == Orpheus.GAME_PLAYING) {
			// All drawing can be done here.
			ArrayList<Platform> level = game.getLevel();
			Platform p;
			for (int i = 0; i < level.size(); i++){
				p = level.get(i);
				platView.drawPlatform(g, p.getX() + (int) game.getPlatformOffsetX(), p.getY(), p.getWidth(), game.getPlatformHeight(), p.getState());
			}
			
			// Draw the character
			pView.drawPlayer(g, (int) game.getX() - 3, (int) game.getY(), game.getPlayerState(), game.getVX(), game.getVY());
			// Bounding boxes for debugging
//			g.setColor(Color.yellow);
//			g.drawRect((int)game.getX(), (int)game.getY(), game.getPlayerWidth(), game.getPlayerHeight());
			
			// Draw the status bar (displays health)
			g.setColor(Color.yellow);
			g.drawRect(10, 10, 30, 60);
			g.setColor(Color.cyan);
			int height = (int) (1.0 * game.getHealth() / game.max_health * 57);
			g.fillRect(12, 12 + 57 - height, 27, height);
			g.setColor(Color.white);
			g.drawLine(15, 15, 15, 55);
			
			// Display the score
			g.setColor(Color.yellow);
			g.drawString("Score: " + game.getScore(), 550, 50);
		} else if (game.getGameState() == Orpheus.GAME_OVER) {
			g.setColor(Color.RED);
			g.drawString("GAME OVER", 330, 250);
			g.drawString("Your score: " + game.getScore() + "/" + game.getLevelSize(), 285, 300);
		} else if (game.getGameState() == Orpheus.GAME_WON) {
			g.drawString("YOU WIN!", 330, 250);
			g.drawString("Your score: " + game.getScore() + "/" + game.getLevelSize(), 285, 300);
		}
	}
	
	
	/** Takes care of keyboard input and figures out what to do when a button is pressed. */
	public void HandleKeys(){
		if (game.getGameState() == Orpheus.GAME_MAIN_MENU){
			if (panel.getKey(KeyEvent.VK_ENTER) == 1 || panel.getKey(KeyEvent.VK_Z) == 1) {
				switch (menuSelection) {
				case 0:
					// Level Select
					game.setGameState(Orpheus.GAME_LVL_SELECT);
					menuSelection = 0;
					break;
				case 1:
					// How to play
					game.setGameState(Orpheus.GAME_ABOUT);
					menuSelection = 0;
					break;
				case 2:
					// Quit
					game.setGameState(Orpheus.GAME_QUIT);
					timer.stop();
					System.exit(0);
					break;
				}
			}
			if (panel.getKey(KeyEvent.VK_DOWN) == 1) {
				// Only allow values of 0, 1, or 2
				menuSelection = ( menuSelection + 1) % 3;
				game.getSong().noteOn(60, 127, System.currentTimeMillis() + 50);
			}
			if (panel.getKey(KeyEvent.VK_UP) == 1) {
				// Only allow values of 0, 1, or 2
				menuSelection = ( menuSelection - 1 + 3) % 3;
				game.getSong().noteOn(60, 127, System.currentTimeMillis() + 50);
			}
			if (panel.getKey(KeyEvent.VK_ESCAPE) == 1) {
				game.quit();
				timer.stop();
				System.exit(0);
			}
		} else if (game.getGameState() == Orpheus.GAME_LVL_SELECT) {
			File[] fList = game.enumerateSongs();
			
			if (panel.getKey(KeyEvent.VK_DELETE) == 1 || panel.getKey(KeyEvent.VK_ESCAPE) == 1) {
				game.setGameState(Orpheus.GAME_MAIN_MENU);
				menuSelection = 0;
				game.getSong().stop();
			}
			if (panel.getKey(KeyEvent.VK_ENTER) == 1 || panel.getKey(KeyEvent.VK_Z) == 1) {
				// Select the level to play here
				if (fList.length > menuSelection && game.loadSong(fList[menuSelection])){
					// Successfully loaded the file
					game.resetPlayer();
					game.setGameState(Orpheus.GAME_PLAYING);
					game.getSong().play();
				}
			}
			if (panel.getKey(KeyEvent.VK_DOWN) == 1) {
				// Only allow values of 0, 1, or 2
				menuSelection = ( menuSelection + 1) % 10; // Allow 0-9 possible songs
				game.getSong().noteOn(60, 127, System.currentTimeMillis() + 50);
			}
			if (panel.getKey(KeyEvent.VK_UP) == 1) {
				// Only allow values of 0, 1, or 2
				menuSelection = ( menuSelection + 9) % 10;
				game.getSong().noteOn(60, 127, System.currentTimeMillis() + 50);
			}
		} else if (game.getGameState() == Orpheus.GAME_ABOUT) {
			if (panel.getKey(KeyEvent.VK_ENTER) == 1 || panel.getKey(KeyEvent.VK_ESCAPE) == 1 || panel.getKey(KeyEvent.VK_Z) == 1){
				menuSelection = 0;
				game.setGameState(game.GAME_MAIN_MENU);
			}
			
		} else if (game.getGameState() == Orpheus.GAME_PLAYING) {
			if (panel.getKey(KeyEvent.VK_Z) > 0) {
				game.jump();
			}
			if (panel.getKey(KeyEvent.VK_LEFT) > 0) {
				game.moveLeft();
			} else if (panel.getKey(KeyEvent.VK_RIGHT)  > 0) {
				game.moveRight();
			}
			if (panel.getKey(KeyEvent.VK_DOWN)  > 0) {
				game.moveDown();  
			}
			if (panel.getKey(KeyEvent.VK_DELETE) == 1 || panel.getKey(KeyEvent.VK_ESCAPE) == 1) {
				game.setGameState(Orpheus.GAME_MAIN_MENU);
				menuSelection = 0;
				game.getSong().stop();
			}
		} else if (game.getGameState() == Orpheus.GAME_OVER) {
			if (panel.getKey(KeyEvent.VK_ENTER) == 1 || panel.getKey(KeyEvent.VK_Z) == 1) {
				game.setGameState(Orpheus.GAME_MAIN_MENU);
				menuSelection = 0;
			}
		} else if (game.getGameState() == Orpheus.GAME_WON) {
			if (panel.getKey(KeyEvent.VK_ENTER) == 1 || panel.getKey(KeyEvent.VK_Z) == 1) {
				game.setGameState(Orpheus.GAME_MAIN_MENU);
				menuSelection = 0;
			}
		}
		
		panel.updateKeys();
	}
}
