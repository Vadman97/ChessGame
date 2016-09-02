package vad;

import java.io.Serializable;

import com.nwgjb.commons.util.BitField;

public class Move implements Serializable
{
	private static final long serialVersionUID = 8443779978202842017L;
	
	private Position start;
	private Position dest;
	private Piece startPiece;
	private Piece killedPiece;
	private byte flags;
	
	public static final int KING_MOVED_FLAG=0;
	public static final int L_ROOK_FLAG=1;
	public static final int R_ROOK_FLAG=2;
	
	
	public Move(GameBoard b, Position start, Position dest)
	{
		this.startPiece=b.getPiece(start);
		this.start = start;
		this.dest = dest;
		
		killedPiece=b.getPiece(dest);
		
		if(startPiece.getType()==Piece.KING&&!b.hasKingMoved(startPiece.getColor())){
			flags=(byte)BitField.setBit(flags, KING_MOVED_FLAG);
		}
		if(startPiece.getType()==Piece.ROOK){
			if(start.getColumn()==0&&!b.hasLRookMoved(startPiece.getColor())){
				//TODO Check whether it's the bottom line
				//TODO We need to set L_ROOK_FLAG if l rook is killed
				flags=(byte)BitField.setBit(flags, L_ROOK_FLAG);
			}else if(start.getColumn()==7&&start.getRow()==0&&!b.hasRRookMoved(startPiece.getColor())){
				flags=(byte)BitField.setBit(flags, R_ROOK_FLAG);
			}
		}
	}
	
	public String toString() {
		return "Move " + startPiece.getType() + " figure from " + start.getRow() + "R " + start.getColumn() + "C to " + start.getRow() + "R " + start.getColumn() + "C";
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
		return BitField.getBit(flags, KING_MOVED_FLAG);
	}
	
	public boolean isFirstLRookMove()
	{
		return BitField.getBit(flags, L_ROOK_FLAG);
	}
	
	public boolean isFirstRRookMove()
	{
		return BitField.getBit(flags, R_ROOK_FLAG);
	}
}
