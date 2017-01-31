package vad;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.nwgjb.xrmi.RMIConnection;

public class Main {

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	static final boolean NETWORKING = false;

	public static void startGame(Player p1, Player p2) {
		GameBoard board = new GameBoard();
		int currentTurn = Piece.WHITE;
		Player[] players = new Player[Piece.COLORS.length];
		players[0] = p1.getColor() == Piece.WHITE ? p1 : p2;
		players[1] = p2.getColor() == Piece.BLACK ? p2 : p1;
		while (true) {
			currentTurn = (currentTurn + 1) % 2;
			CompressedGameBoard b = new CompressedGameBoard(board);
			p1.update(b);
			p2.update(b);
			Move m = players[currentTurn].makeMove(b);
			if (m == null) {
				System.out.println("~~~~~~~" + players[currentTurn].getClass().getName() + " Defeated~~~~~~~~~");
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			board.apply(m);
		}
	}

	public static void main(String[] args) throws IOException {
		Random r = new Random();
		int col = r.nextInt(2);
		Player p2;
		if (NETWORKING) {
			ServerSocket socket = new ServerSocket(12345);
			p2 = ((ClientPlayerFactory) new RMIConnection(socket.accept()).getBind()).create(Piece.BLACK);
			socket.close();
		} else {
			// p2 = new UserPlayer(Piece.getOppositeColor(col));
			p2 = new AIPlayer(Piece.getOppositeColor(col));
			// p2 = new OldAIPlayer(Piece.getOppositeColor(col));
		}
		// Player p1 = new DebugPlayer(col, (AIPlayer) p2);
		// Player p1 = new UserPlayer(col);
		Player p1 = new OldAIPlayer(col);
		// Player p1 = new AIPlayer(col);

		startGame(p1, p2);
	}
}
