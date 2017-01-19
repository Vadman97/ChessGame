package vad;

import java.util.ArrayList;

import com.nwgjb.commons.util.BitField;

/**
 * Game board data structure. Provide a lot of helper functions. Do not transfer
 * this in networking because in Java, object streams will cache transferred
 * object. Since we only have one game board instance, do not pass this as an
 * argument, but you can use this class freely if you are sure that this class
 * will only be used locally.
 * 
 * @author Gary Guo, Vadim Korolik
 *
 */
public class GameBoard
{
	/*
	 * Column, Row
	 */
	Piece[][] board;
	int currentColor = Piece.WHITE;
	byte whiteFlags;
	byte blackFlags;

	public static final int KING_MOVED_FLAG = 0;
	public static final int L_ROOK_FLAG = 1;
	public static final int R_ROOK_FLAG = 2;
	public static final int CASTLED = 3;
	public static final short WIDTH = 8;
	public static final short HEIGHT = WIDTH;

	public static final int[] STARTING_ROW =
	{ Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK };

	public GameBoard()
	{
		board = new Piece[WIDTH][HEIGHT];
		for (int i = 0; i < WIDTH; i++)
		{
			setPiece(Position.get(i, 0), Piece.get(Piece.BLACK, STARTING_ROW[i]));
			setPiece(Position.get(i, 7), Piece.get(Piece.WHITE, STARTING_ROW[i]));
			setPiece(Position.get(i, 1), Piece.get(Piece.BLACK, Piece.PAWN));
			setPiece(Position.get(i, 6), Piece.get(Piece.WHITE, Piece.PAWN));
		}
	}

	public GameBoard(boolean dummy)
	{
		board = new Piece[8][8];
	}
	
	public GameBoard copy() {
		GameBoard copy = new GameBoard(true);
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				copy.setPiece(Position.get(i, j), getPiece(Position.get(i, j)));
			}
		}
		copy.currentColor = currentColor;
		copy.whiteFlags = whiteFlags;
		copy.blackFlags = blackFlags;
		return copy;
	}

	public void setPiece(Position pos, Piece piece)
	{
		board[pos.col][pos.row] = piece;
	}

	public boolean isEmpty(Position pos)
	{
		return board[pos.col][pos.row] == null;
	}
	
	public int getNumAllPieces() 
	{
		int count = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (!isEmpty(Position.get(i, j)))
					count++;
			}
		}
		return count;
	}

	public Piece getPiece(Position loc)
	{
		return board[loc.col][loc.row];
	}

	public void apply(Move m)
	{
		if (m == null) {
			System.out.println("Move is null cannot apply");
			return;
		}
		
		Position start = m.getStartPosition();
		Position dest = m.getDestPosition();
		Piece startPiece = getPiece(start);
		
		if (startPiece == null) {
			System.out.println("Error no piece at start location");
			return;
		}

		if (m.isFirstKingMove())
			setHasKingMoved(currentColor, true);
		
		if (m.isFirstLRookMove())
			setHasLRookMoved(currentColor, true);
		if (m.isFirstRRookMove())
			setHasRRookMoved(currentColor, true);

		/* Check if it's castling */
		if (startPiece.getType() == Piece.KING)
		{
			if (start.getColumn() - 2 == dest.getColumn())
			{
				// L Castle
				Position rookPosition = Position.get(0, dest.getRow());
				setPiece(dest.getRight(), getPiece(rookPosition));
				setPiece(rookPosition, null);
				setCastled(currentColor, true);
			} else if (start.getColumn() + 2 == dest.getColumn())
			{
				// R Castle
				Position rookPosition = Position.get(7, dest.getRow());
				setPiece(dest.getLeft(), getPiece(rookPosition));
				setPiece(rookPosition, null);
				setCastled(currentColor, true);
			}
		} else if (startPiece.getType() == Piece.PAWN) {
			//Pawn promotion
			if (startPiece.getColor() == Piece.WHITE) {
				if (dest.getRow() == 0)
					startPiece = new Piece(startPiece.getColor(), Piece.QUEEN);
			} else if (startPiece.getColor() == Piece.BLACK) {
				if (dest.getRow() == 7)
					startPiece = new Piece(startPiece.getColor(), Piece.QUEEN);
			}
		}

		setPiece(start, null);
		setPiece(dest, startPiece);

		// System.out.println("Move apply: " + m.getKilledPiece());
		
		currentColor = Piece.getOppositeColor(currentColor); // change whose turn it is

	}

	public void undo(Move move)
	{
		currentColor = Piece.getOppositeColor(currentColor); // undo whose turn it is
		Position start = move.getStartPosition();
		Position dest = move.getDestPosition();
		Piece movedPiece = getPiece(dest);
		if (move.isFirstKingMove())
		{
			setHasKingMoved(movedPiece.getColor(), false);
		}
		if (move.isFirstLRookMove())
		{
			setHasLRookMoved(movedPiece.getColor(), false);
			// System.out.println("LROOK UNDO");
		}
		if (move.isFirstRRookMove())
		{
			setHasRRookMoved(movedPiece.getColor(), false);
		}
		// System.out.println("Move undo: " + move.getKilledPiece());

		if (movedPiece.getType() == Piece.KING)
		{
			if (start.getColumn() - 2 == dest.getColumn())
			{
				// L Castle
				Position rookOriginalPosition = Position.get(0, dest.getRow());
				Position rookCurrentPosition = dest.getRight();
				setPiece(rookOriginalPosition, getPiece(rookCurrentPosition));
				setPiece(rookCurrentPosition, null);
				setCastled(movedPiece.getColor(), false);
			} else if (start.getColumn() + 2 == dest.getColumn())
			{
				// R Castle
				Position rookOriginalPosition = Position.get(7, dest.getRow());
				Position rookCurrentPosition = dest.getLeft();
				setPiece(rookOriginalPosition, getPiece(rookCurrentPosition));
				setPiece(rookCurrentPosition, null);
				setCastled(movedPiece.getColor(), false);
			}
		} else if (movedPiece.getType() == Piece.PAWN) {
			//Pawn promotion undo
			if (movedPiece.getColor() == Piece.WHITE) {
				if (dest.getRow() == 0)
					movedPiece = new Piece(movedPiece.getColor(), Piece.PAWN);
			} else if (movedPiece.getColor() == Piece.BLACK) {
				if (dest.getRow() == 7)
					movedPiece = new Piece(movedPiece.getColor(), Piece.PAWN);
			}
		}

		setPiece(move.getStartPosition(), movedPiece);
		setPiece(move.getDestPosition(), move.getKilledPiece());
	}
	
	public ArrayList<Move> getAllPossibleMovesWithoutValidation(int color) {
		return getAllPossibleMovesWithoutValidation(color, false);
	}

	public ArrayList<Move> getAllPossibleMovesWithoutValidation(int color, boolean ignoreEKing)
	{
		ArrayList<Move> moves = new ArrayList<>();
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				if (isEmpty(Position.get(c, r)))
					continue;
				
				Piece piece = getPiece(Position.get(c, r));
				
				if (ignoreEKing && piece.getType() == Piece.KING)
					continue;
				if (piece.getColor() == color)
				{
					moves.addAll(MoveHelper.getAllMoves4PieceWithoutValidation(this, Position.get(c, r)));
				}
			}
		}
		return moves;
	}

	public ArrayList<Move> getAllPossibleMoves(int color)
	{
		ArrayList<Move> moves = new ArrayList<>();
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				if (isEmpty(Position.get(c, r)))
					continue;
				if (getPiece(Position.get(c, r)).getColor() == color)
				{
					moves.addAll(MoveHelper.getAllMoves4Piece(this, Position.get(c, r), false));
				}
			}
		}
		return moves;
	}

	public ArrayList<Move> getAllPossibleMovesWithDefend(int color)
	{
		ArrayList<Move> moves = new ArrayList<>();
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				if (isEmpty(Position.get(c, r)))
					continue;
				if (getPiece(Position.get(c, r)).getColor() == color)
				{
					moves.addAll(MoveHelper.getAllMoves4Piece(this, Position.get(c, r), true));
				}
			}
		}
		return moves;
	}

	public int getNumPieces(int color)
	{
		int count = 0;
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				if (isEmpty(Position.get(c, r)))
					continue;
				if (getPiece(Position.get(c, r)).getColor() == color)
					count++;
			}
		}
		return count;
	}

	public boolean isCheck(int kingColor)
	{
		for (Move m : getAllPossibleMovesWithoutValidation(Piece.getOppositeColor(kingColor)))
		{
			if (m.getKilledPiece() != null && m.getKilledPiece().getType() == Piece.KING)
			{
				return true;
			}
		}
		return false;
	}

	public boolean isCheckMate(int kingColor)
	{
		if (isCheck(kingColor))
		{
			if (getAllPossibleMoves(kingColor).size() == 0)
				return true;
		}
		return false;
	}
	
	public boolean hasKingMoved(int color)
	{
		if (color == Piece.BLACK)
		{
			return BitField.getBit(blackFlags, KING_MOVED_FLAG);
		} else
		{
			return BitField.getBit(whiteFlags, KING_MOVED_FLAG);
		}
	}

	public boolean hasLRookMoved(int color)
	{
		if (color == Piece.BLACK)
		{
			return BitField.getBit(blackFlags, L_ROOK_FLAG);
		} else
		{
			return BitField.getBit(whiteFlags, L_ROOK_FLAG);
		}
	}

	public boolean hasRRookMoved(int color)
	{
		if (color == Piece.BLACK)
		{
			return BitField.getBit(blackFlags, R_ROOK_FLAG);
		} else
		{
			return BitField.getBit(whiteFlags, R_ROOK_FLAG);
		}
	}
	
	public boolean hasCastled(int color)
	{
		if (color == Piece.BLACK)
		{
			return BitField.getBit(blackFlags, CASTLED);
		} else
		{
			return BitField.getBit(whiteFlags, CASTLED);
		}
	}

	public void setHasKingMoved(int color, boolean moved)
	{
		if (color == Piece.BLACK)
		{
			blackFlags=(byte) BitField.changeBit(blackFlags, KING_MOVED_FLAG, moved);
		} else
		{
			whiteFlags=(byte) BitField.changeBit(whiteFlags, KING_MOVED_FLAG, moved);
		}
	}

	public void setHasLRookMoved(int color, boolean moved)
	{
		if (color == Piece.BLACK)
		{
			blackFlags=(byte) BitField.changeBit(blackFlags, L_ROOK_FLAG, moved);
		} else
		{
			whiteFlags=(byte) BitField.changeBit(whiteFlags, L_ROOK_FLAG, moved);
		}
	}

	public void setHasRRookMoved(int color, boolean moved)
	{
		if (color == Piece.BLACK)
		{
			blackFlags=(byte) BitField.changeBit(blackFlags, R_ROOK_FLAG, moved);
		} else
		{
			whiteFlags=(byte) BitField.changeBit(whiteFlags, R_ROOK_FLAG, moved);
		}
	}
	
	public void setCastled(int color, boolean castled)
	{
		if (color == Piece.BLACK)
		{
			blackFlags=(byte) BitField.changeBit(blackFlags, CASTLED, castled);
		} else
		{
			whiteFlags=(byte) BitField.changeBit(whiteFlags, CASTLED, castled);
		}
	}
	
	public String toString() {
		String out = "";
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (!isEmpty(Position.get(i, j))) {
					out += getPiece(Position.get(i, j)).getType();
				} else {
					out += " ";
				}
				if (j != 7)
					out += "|";
			}
			out += "\n";
		}
		return out;
	}
}
