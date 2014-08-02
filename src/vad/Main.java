package vad;

import java.io.IOException;
import java.net.ServerSocket;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.nwgjb.xrmi.RMIConnection;

public class Main
{

	static
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
		{
			e.printStackTrace();
		}
	}

	public static void startGame(Player p1, Player p2)
	{
		GameBoard board = new GameBoard();
		while (true)
		{

			if (board.currentColor == Piece.WHITE)
			{
				CompressedGameBoard b=new CompressedGameBoard(board);
				p2.update(b);
				board.apply(p1.makeMove(b));
			} else
			{
				CompressedGameBoard b=new CompressedGameBoard(board);
				p1.update(b);
				board.apply(p2.makeMove(b));
			}

		}
	}

	public static void main(String[] args) throws IOException
	{
		ServerSocket socket = new ServerSocket(12345);
		Player p1 =((ClientPlayerFactory) new RMIConnection(socket.accept()).getBind()).create(Piece.WHITE);
		Player p2 = ((ClientPlayerFactory) new RMIConnection(socket.accept()).getBind()).create(Piece.BLACK);
		socket.close();
		startGame(p1, p2);
	}
}
