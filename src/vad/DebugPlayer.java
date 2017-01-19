package vad;

public class DebugPlayer implements Player
{
	ChessGUI	gui;
	int			playerColor;
	
	int currentMove = 0;
	public static final Position[][] moves = {{Position.get(3, 6), Position.get(3, 4)}, {Position.get(4, 6), Position.get(4, 4)}}; 

	public DebugPlayer(int playerColor)
	{
		this.playerColor = playerColor;
		gui = new ChessGUI(null, playerColor);
	}
	
	@Override
	public void update(CompressedGameBoard board)
	{
		update(board.getGameBoard());
	}

	public void update(GameBoard board)
	{
		gui.updateBoard(board);
	}

	public Move makeMove(CompressedGameBoard cgb)
	{
		GameBoard b=cgb.getGameBoard();
		update(b);
		
		return b.getAllPossibleMoves(playerColor).get(0);
		
//		if (moves[currentMove][0] == null) return null;
//		
//		Move m = new Move(b, moves[currentMove][0], moves[currentMove][1]);
//		currentMove++;
//		if (currentMove >= moves.length)
//			currentMove = 0;
//		
//		return m;
	}
}
