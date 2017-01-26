package tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vad.CompressedGameBoard;
import vad.GameBoard;
import vad.Move;
import vad.Position;

public class TestGameBoard {
	private GameBoard gb;
	private CompressedGameBoard cb1 = null, cb2 = null, cb3 = null;
	
	public TestGameBoard() {
		gb = new GameBoard();
		GameBoard copy = gb.copy();

		GameBoard diff = new GameBoard();
		diff.apply(new Move(diff, Position.get(0, 1), Position.get(0, 3)));
		
		cb1 = new CompressedGameBoard(gb);
		cb2 = new CompressedGameBoard(copy);
		cb3 = new CompressedGameBoard(diff);
	}
	
	@Test
	public void testHashCode() {
		assertTrue(cb1.hashCode() == cb2.hashCode());
		assertTrue(cb3.hashCode() != cb1.hashCode());
		assertTrue(cb3.hashCode() != cb2.hashCode());
	}
	
	@Test
	public void testEquals() {
		assertTrue(cb1.equals(cb2));
		assertTrue(cb2.equals(cb1));
		assertTrue(!cb3.equals(cb1));
		assertTrue(!cb3.equals(cb2));
	}
}
