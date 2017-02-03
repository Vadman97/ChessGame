package vad;

import java.util.ArrayList;
import java.util.Random;

public class DebugPlayer implements Player {
	ChessGUI gui;
	int playerColor;
	AIPlayer opponent;
	Random r;

	int movesMade = 0;
	int currentMove = 0;
	public static final Position[][] moves = { { Position.get(3, 6), Position.get(3, 4) },
			{ Position.get(4, 6), Position.get(4, 4) } };

	public DebugPlayer(int playerColor, AIPlayer opponent) {
		this.playerColor = playerColor;
		this.opponent = opponent;
		gui = new ChessGUI(null, playerColor);
		r = new Random();
	}

	public void update(GameBoard board) {
		gui.updateBoard(board);
	}

	public Move makeMove(GameBoard b) {
		if (movesMade == 11) {
			System.out.println("============ 10 MOVES HAVE BEEN PLAYED===========");
			System.out.println(opponent.totalNodes + " " + (opponent.totalTime / 1e9));
			//System.exit(0);
		}

		movesMade++;

		ArrayList<Move> moves = b.getAllPossibleMoves(playerColor);
		//return moves.get(0);
		if (moves.size() == 0)
			return null;
		
		return moves.get(r.nextInt(moves.size()));

		// if (moves[currentMove][0] == null) return null;
		//
		// Move m = new Move(b, moves[currentMove][0], moves[currentMove][1]);
		// currentMove++;
		// if (currentMove >= moves.length)
		// currentMove = 0;
		//
		// return m;
	}

	@Override
	public int getColor() {
		return playerColor;
	}
}
