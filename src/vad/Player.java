package vad;

/**
 * Player interface, designed to modulize project and to fit XRMI library.
 * 
 * @author Gary Guo, Vadim Korolik
 *
 */
public interface Player
{
	Move makeMove(CompressedGameBoard b);

	void update(CompressedGameBoard board);
	
	int getColor();
}
