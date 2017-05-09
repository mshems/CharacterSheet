package items;

public class Consumable extends Item{
	private static final long serialVersionUID = 1L;
	public Consumable(String name) {
		super(name);
		this.setConsumable(true);
	}
	
	public Consumable(String name, int count) {
		super(name, count);
		this.setConsumable(true);
	}
	
	public void use(int amount){
		this.addCount(-amount);
	}

}
