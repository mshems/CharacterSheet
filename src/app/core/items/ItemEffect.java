package app.core.items;

import app.CharacterCommand;
import app.core.character.Stat;

import java.io.Serializable;

public class ItemEffect implements Serializable{
	private static final long serialVersionUID = CharacterCommand.VERSION;
	private Stat target;
	private int bonus;
	
	public ItemEffect(Stat target, int bonus){
		this.target = target;
		this.bonus= bonus;
	}
	
	public Stat getTarget() {
		return target;
	}
	public void setTarget(Stat target) {
		this.target = target;
	}
	
	public int getBonus() {
		return bonus;
	}
	public void setBonus(int bonus) {
		this.bonus = bonus;
	}
	
	
}
