package vad;

public class TranspositionTableEntry {
	private int lower;
	private int upper;
	private Move bestMove;
	
	public TranspositionTableEntry(int lower, int upper, Move best){
		this.lower=lower;
		this.upper=upper;
		this.bestMove = best;
	}
	
	public int getLower() {
		return this.lower;
	}
	
	public int getUpper() {
		return this.upper;
	}
	
	public Move getMove() {
		return this.bestMove;
	}
}
