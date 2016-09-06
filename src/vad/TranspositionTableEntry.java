package vad;

public class TranspositionTableEntry {
	private int flag;
	private int value;
	private int depth;
	
	public static final int PRECISE=0;
	public static final int LOWER_BOUND=1;
	public static final int UPPER_BOUND=2;
	
	public TranspositionTableEntry(int flag, int value, int depth){
		this.flag=flag;
		this.value=value;
		this.depth=depth;
	}
	
	public int getDepth() {
		return depth;
	}

	public int getFlags(){
		return flag;
	}
	
	public boolean isPrecise(){
		return flag==PRECISE;
	}
	
	public boolean isLowerBound(){
		return flag==LOWER_BOUND;
	}
	
	public boolean isUpperBound(){
		return flag==UPPER_BOUND;
	}
	
	public int getValue(){
		return value;
	}
	
	
}
