package character;

public class CounterStat extends Stat {
	private static final long serialVersionUID = 1L;
	private double currVal;
	
	public CounterStat(){
		super();
		this.currVal = this.getMaxVal();
	}
	public CounterStat(String name, double baseVal){
		super(name, baseVal);
		this.currVal = this.getMaxVal();
	}
	
	public void countUp(){
		if(this.currVal<getMaxVal()){
			this.currVal++;
		}
	}
	public void countDown(){
		if(this.currVal>0){
			this.currVal--;
		}
	}
	public void countUp(double amt){
		if(this.currVal+amt<getMaxVal()){
			this.currVal += amt;
		} else {
			currVal = getMaxVal();
		}
	}
	public void countDown(double amt){
		if(this.currVal-amt>0){
			this.currVal -= amt;
		} else {
			this.currVal = 0;
		}
	}

	public void setCurrVal(double val){
		this.currVal = val;
	}
	public double getCurrVal(){
		return this.currVal;
	}

	public double getMaxVal(){
		return this.getBaseVal();
	}
	public void setMaxVal(double val){
		this.setBaseVal(val);
	}
	
	@Override 
	public String toString(){
		return String.format("%s: %.0f/%.0f", this.getName(), this.getCurrVal(), this.getMaxVal());
	}
}

