package vad;

public interface Player
{
	Move makeMove(GameBoard b);
	void update(GameBoard board);
	
	/*
	public void deselectPiece()
	{
		this.selectedPiece = null;
	}

	public int getColor()
	{
		return this.playerColor;
	}

	public Piece getSelectedPiece()
	{
		return this.selectedPiece;
	}

	public boolean hasSelectedPiece()
	{
		if (this.selectedPiece == null) return false;
		if (!this.selectedPiece.isValidPiece()) return false;
		return true;
	}

	public void selectPiece(Piece select)
	{
		this.selectedPiece = select;
	}

	public void runMove(Piece[][] board)
	{
		System.out.println("Running default movement for class: " + this);
	}*/
	
}
