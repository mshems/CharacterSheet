package app.core.items;

import app.CharacterCommand;

public class Weapon extends Equippable{
	private static final long serialVersionUID = CharacterCommand.VERSION;
	private DiceRoll damage;
	
	public Weapon(String name){
		super(name);
	}
	
	public Weapon(String name, int count){
		super(name, count);
	}
	
	public Weapon(String name, DiceRoll dmg){
		super(name);
		this.damage = dmg;
	}

	public Weapon setDamage(DiceRoll roll){
		this.damage = roll;
		return this;
	}
	
	@Override
	public String toString(){
		String s =super.toString();
		if (damage!=null){
			s+=" ["+damage+"]";
		}
		return s;
	}
}
