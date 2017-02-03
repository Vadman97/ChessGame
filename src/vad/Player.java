package vad;

/**
 * Player interface, designed to modulize project and to fit XRMI library.
 * 
 * @author Vadim Korolik, Gary Guo
 *
 */
public interface Player
{
	Move makeMove(GameBoard board);

	void update(GameBoard board);
	
	int getColor();
}
