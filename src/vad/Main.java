package vad;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main
{

	static{
		try
		{
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		GameBoard board = new GameBoard();
		Player p1 = new UserPlayer(Piece.WHITE);
		Player p2 = new AIPlayer(Piece.BLACK);
		//board=new CompressedGameBoard(board).getGameBoard();
		p1.update(board);
		p2.update(board);
		while (true)
		{
			
			if (board.currentColor == Piece.WHITE)
			{
				board.apply(p1.makeMove(board));
				p1.update(board);
				p2.update(board);
			}
			else
			{
				board.apply(p2.makeMove(board));
				p1.update(board);
				p2.update(board);
			}
			
		}
	}
}
