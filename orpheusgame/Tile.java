package orpheusgame;

import java.awt.Graphics;

public class Tile {
	
	private Sprite sprite;
	private int x;
	private int y;
	
	public Tile() {
	}
	
	public void drawTile(Graphics g, int xOffset){
		g.drawImage(sprite.getImage(), x + xOffset, y, null);
	}
	
	public void setSprite(Sprite s){
		this.sprite = s;
	}
	
	public void setX(int x){
		this.x = x;
	}
	
	public void setY(int y){
		this.y = y;
	}
	
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
}
