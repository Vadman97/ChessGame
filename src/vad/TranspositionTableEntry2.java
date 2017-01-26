package vad;

public class TranspositionTableEntry2 {
	private int lower;
	private int upper;
	
	public TranspositionTableEntry2(int lower, int upper){
		this.lower=lower;
		this.upper=upper;
	}
	
	public int getLower() {
		return this.lower;
	}
	
	public int getUpper() {
		return this.upper;
	}
}
