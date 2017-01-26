package vad;

public class ScoredMove {
	public Move move;
	public int score;
	
	public ScoredMove(Move move, int score) {
		this.move = move;
		this.score = score;
	}
}
