package orpheusgame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;

/** This class represents the active content area of a program, upon which drawing can be done. Additionally, it
 *  listens to mouse and key events. */
public class Panel extends JPanel implements KeyListener {

	private static final long serialVersionUID = 1L;
	/** The owner of this content Panel. Is the target of callbacks. */
	private PanelOwner parent;
	/** An integer array that keeps track of the state of the keyboard. */
	private int[] keys;
	
	/** Defaults to a 600x600 screen. */
	public Panel(PanelOwner parent){
		this(600, 600, parent);
	}
	
	/** Constructs the Panel with width and height in pixels. The last argument, parent, is a reference to 
	 * the owner of this GameEngine and is necessary for callback functions. */
	public Panel(int width, int height, PanelOwner parent){
		this.parent = parent;
		setPreferredSize(new Dimension(width, height));
		setBackground(Color.black);
		setDoubleBuffered(true);
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);
		setIgnoreRepaint(true); // Tell the AWT to ignore us, b/c WE'LL repaint ourselves
		addKeyListener(this);
		keys = new int[256];
		requestFocusInWindow();
	}
	
	
	/** This will be called by Java's AWT whenever a repaint() is called. */
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		parent.drawGame(g);
		g.dispose();
	}
	
	/** Updates the keys[] array. This allows the panel to keep track of the length
	 *  of time that a key has been held down. */
	public void updateKeys(){
		for (int i = 0; i < keys.length; i++){
			if (keys[i] != 0) {keys[i]++;}
		}
		
	}
	
	/** Sets respective elements of keys[] array. If the key is held down, this is checked
	 *  for in handleKeys(). */
	public void keyPressed(KeyEvent arg0) {
				
		int index = arg0.getKeyCode();
		if (index > 255) {return;} // Don't want to go out of bounds of array
		
		// If the key had been up, then set it down
		// If the keys[index] isn't zero, then it was down last
		// cycle
		if (keys[index]==0) {keys[index] = 1;} 
	}

	/** Resets respective elements of keys[] array. */
	public void keyReleased(KeyEvent arg0) {
		int index = arg0.getKeyCode();
		if (index > 255) {return;} // Don't want to go out of bounds of array
		
		keys[index] = 0; // Reset the key
	}

	/** Unused in Panel, but required by interface. */
	public void keyTyped(KeyEvent arg0) {
	}
	
	//=================/ Getters & Setters /==============================/
	/** Gets the current state of a keyboard key (use KeyEvent.VK_XXXX for index). A state 
	 *  of 0 indicates a key that is up, while a state of 1 or greater indicates 
	 *  a key that has been down for that many cycles. */
	public int getKey(int index){
		if (index >= 0 && index < 256){ return keys[index];}
		return 0;
	}

}
