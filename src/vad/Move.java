package vad;

import java.io.Serializable;

public class Move implements Serializable
{
	private static final long serialVersionUID = 8443779978202842017L;
	
	private Position start;
	private Position dest;
	private Piece killedPiece;
	private byte flags;
	
	public static final int KING_MOVED_FLAG=0;
	public static final int L_ROOK_FLAG=1;
	public static final int R_ROOK_FLAG=2;
	
	
	public Move(GameBoard b, Position start, Position dest)
	{
		Piece startPiece=b.getPiece(start);
		this.start = start;
		this.dest = dest;
		
		killedPiece=b.getPiece(dest);
		
		if(startPiece.getType()==Piece.KING&&!b.hasKingMoved(startPiece.getColor())){
			flags|=1<<KING_MOVED_FLAG;
		}
		if(startPiece.getType()==Piece.ROOK){
			if(start.getColumn()==0&&!b.hasLRookMoved(startPiece.getColor())){
				flags|=1<<L_ROOK_FLAG;
			}else if(start.getColumn()==7&&!b.hasRRookMoved(startPiece.getColor())){
				flags|=1<<R_ROOK_FLAG;
			}
		}
	}
	

	public Position getStartPosition()
	{
		return start;
	}
	
	public Position getDestPosition()
	{
		return dest;
	}


	public Piece getKilledPiece()
	{
		return killedPiece;
	}


	public boolean isFirstKingMove()
	{
		return (flags&(1<<KING_MOVED_FLAG))!=0;
	}
	
	public boolean isFirstLRookMove()
	{
		return (flags&(1<<L_ROOK_FLAG))!=0;
	}
	
	public boolean isFirstRRookMove()
	{
		return (flags&(1<<R_ROOK_FLAG))!=0;
	}
}
