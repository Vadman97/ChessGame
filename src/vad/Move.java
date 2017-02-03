package vad;

import java.io.Serializable;

import com.nwgjb.commons.util.BitField;

public class Move implements Serializable {
	private static final long serialVersionUID = 8443779978202842017L;

	private Position start;
	private Position dest;
	private Piece startPiece;
	private Piece killedPiece;
	private byte flags;

	public static final int KING_MOVED_FLAG = 0;
	public static final int L_ROOK_FLAG = 1;
	public static final int R_ROOK_FLAG = 2;
	public static final int PAWN_PROMOTION_FLAG = 3;

	public Move(GameBoard b, Position start, Position dest) {
		this.startPiece = b.getPiece(start);
		this.start = start;
		this.dest = dest;

		killedPiece = b.getPiece(dest);

		if (startPiece.getType() == Piece.KING && !b.hasKingMoved(startPiece.getColor())) {
			flags = (byte) BitField.setBit(flags, KING_MOVED_FLAG);
		}
		if (startPiece.getType() == Piece.ROOK) {
			if (start.getColumn() == 0 && !b.hasLRookMoved(startPiece.getColor())) {
				// TODO Check whether it's the bottom line
				// TODO We need to set L_ROOK_FLAG if l rook is killed
				flags = (byte) BitField.setBit(flags, L_ROOK_FLAG);
			} else if (start.getColumn() == 7 && start.getRow() == 0 && !b.hasRRookMoved(startPiece.getColor())) {
				flags = (byte) BitField.setBit(flags, R_ROOK_FLAG);
			}
		}
		if ((dest.getRow() == 0 || dest.getRow() == 7) && startPiece.getType() == Piece.PAWN) {
			flags = (byte) BitField.setBit(flags, PAWN_PROMOTION_FLAG);
		}
	}

	public String toString() {
		return "Move " + startPiece.getType() + " figure from " + start.getRow() + "R " + start.getColumn() + "C to "
				+ dest.getRow() + "R " + dest.getColumn() + "C";
	}

	public Position getStartPosition() {
		return start;
	}

	public Position getDestPosition() {
		return dest;
	}

	public Piece getKilledPiece() {
		return killedPiece;
	}

	public boolean isFirstKingMove() {
		return BitField.getBit(flags, KING_MOVED_FLAG);
	}

	public boolean isFirstLRookMove() {
		return BitField.getBit(flags, L_ROOK_FLAG);
	}

	public boolean isFirstRRookMove() {
		return BitField.getBit(flags, R_ROOK_FLAG);
	}
	
	public boolean isPawnPromotion() {
		return BitField.getBit(flags, PAWN_PROMOTION_FLAG);
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		Move m = (Move) o;
		if (getStartPosition().equals(m.getStartPosition())) {
			if (getDestPosition().equals(m.getDestPosition())) {
				if (startPiece == null ? m.startPiece == null : startPiece.equals(m.startPiece)) {
					if (killedPiece == null ? m.killedPiece == null : killedPiece.equals(m.killedPiece)) {
						return flags == m.flags;
					}
				}
			}
		}
		return false;
	}

	public int hashCode() {
		int sphc = startPiece != null ? startPiece.hashCode() : 0;
		int kphc = killedPiece != null ? killedPiece.hashCode() : 0;
		int hash = 17;
		hash = hash * 31 + getStartPosition().hashCode();
		hash = hash * 31 + getDestPosition().hashCode();
		hash = hash * 31 + sphc;
		hash = hash * 31 + kphc;
		hash = hash * 31 + flags;
		return hash % Integer.MAX_VALUE;
	}
}
