package com.jcloisterzone.ai.copy;

import com.jcloisterzone.game.Game;

//NOT finished
public class GameDuplicator {
	
	final Game original;
	
	public GameDuplicator(Game original) {
		this.original = original;
	}
	
	public Game duplicate() {
		Game copy = new Game();
		copy.setConfig(original.getConfig());
		copy.setTilePack(original.getTilePack());
		
		
		return copy;
	}

}