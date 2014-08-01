package vad;

import java.util.ArrayList;

public class MoveHelper
{
	public static ArrayList<Position> getReachablePosition(GameBoard board, int col, int row, boolean defend)
	{
		Piece p = board.getPiece(col, row);
		switch (p.getType())
		{
			case Piece.ROOK:
				return getReachableRookPosition(board, p, col, row, defend);
			case Piece.KNIGHT:
				return getReachableKnightPosition(board.board, col, row, defend);
			case Piece.BISHOP:
				return getReachableBishopPosition(board.board, col, row, defend);
			case Piece.KING:
				return getReachableKingPosition(board, col, row, defend);
			case Piece.QUEEN:
				return getReachableQueenPosition(board, p, col, row, defend);
			default:
				return getReachablePawnPosition(board.board, col, row, defend);
		}

	}

	public static ArrayList<Move> getAllMoves4PieceWithoutValidation(GameBoard board, Position pos)
	{
		ArrayList<Move> moves = new ArrayList<>();
		for (Position p : getReachablePosition(board, pos.col, pos.row, false))
		{
			moves.add(new Move(board, pos, p));
		}
		return moves;
	}

	public static ArrayList<Move> getAllMoves4Piece(GameBoard board, Position pos, boolean defend)
	{
		ArrayList<Move> moves = new ArrayList<>();
		Piece piece = board.getPiece(pos);
		for (Position p : getReachablePosition(board, pos.col, pos.row, defend))
		{
			Move m = new Move(board, pos, p);
			board.apply(m);
			if (!board.isCheck(piece.getColor()))
				moves.add(m);
			board.undo(m);
		}
		return moves;
	}

	private static ArrayList<Position> getReachableRookPosition(GameBoard board, Piece piece, int col, int row,
			boolean defend)
	{
		ArrayList<Position> position = new ArrayList<>();
		for (int c = col - 1; c >= 0; c--)
		{
			if (defend)
				checkDefend(board.board, position, c, row, piece);
			if (!checkFreeOrEatAndAdd(board.board, position, c, row, piece))
				break;
		}
		for (int c = col + 1; c < 8; c++)
		{
			if (defend)
				checkDefend(board.board, position, c, row, piece);
			if (!checkFreeOrEatAndAdd(board.board, position, c, row, piece))
				break;
		}
		for (int r = row - 1; r >= 0; r--)
		{
			if (defend)
				checkDefend(board.board, position, col, r, piece);
			if (!checkFreeOrEatAndAdd(board.board, position, col, r, piece))
				break;
		}
		for (int r = row + 1; r < 8; r++)
		{
			if (defend)
				checkDefend(board.board, position, col, r, piece);
			if (!checkFreeOrEatAndAdd(board.board, position, col, r, piece))
				break;
		}
		return position;
	}

	private static ArrayList<Position> getReachableKnightPosition(Piece[][] board, int col, int row, boolean defend)
	{
		ArrayList<Position> position = new ArrayList<>();
		Piece p = board[col][row];

		checkFreeOrEatAndAdd(board, position, col - 2, row - 1, p);
		checkFreeOrEatAndAdd(board, position, col - 2, row + 1, p);
		checkFreeOrEatAndAdd(board, position, col + 2, row - 1, p);
		checkFreeOrEatAndAdd(board, position, col + 2, row + 1, p);
		checkFreeOrEatAndAdd(board, position, col - 1, row - 2, p);
		checkFreeOrEatAndAdd(board, position, col - 1, row + 2, p);
		checkFreeOrEatAndAdd(board, position, col + 1, row - 2, p);
		checkFreeOrEatAndAdd(board, position, col + 1, row + 2, p);

		if (defend)
		{
			checkDefend(board, position, col - 2, row - 1, p);
			checkDefend(board, position, col - 2, row + 1, p);
			checkDefend(board, position, col + 2, row - 1, p);
			checkDefend(board, position, col + 2, row + 1, p);
			checkDefend(board, position, col - 1, row - 2, p);
			checkDefend(board, position, col - 1, row + 2, p);
			checkDefend(board, position, col + 1, row - 2, p);
			checkDefend(board, position, col + 1, row + 2, p);
		}
		return position;
	}

	private static ArrayList<Position> getReachableBishopPosition(Piece[][] board, int col, int row, boolean defend)
	{
		ArrayList<Position> position = new ArrayList<>();
		Piece p = board[col][row];
		for (int i = 1;; i++)
		{
			if (defend)
				checkDefend(board, position, col + i, row + i, p);
			if (!checkFreeOrEatAndAdd(board, position, col + i, row + i, p))
				break;
		}
		for (int i = 1;; i++)
		{
			if (defend)
				checkDefend(board, position, col - i, row + i, p);
			if (!checkFreeOrEatAndAdd(board, position, col - i, row + i, p))
				break;
		}
		for (int i = 1;; i++)
		{
			if (defend)
				checkDefend(board, position, col - i, row - i, p);
			if (!checkFreeOrEatAndAdd(board, position, col - i, row - i, p))
				break;
		}
		for (int i = 1;; i++)
		{
			if (defend)
				checkDefend(board, position, col + i, row - i, p);
			if (!checkFreeOrEatAndAdd(board, position, col + i, row - i, p))
				break;
		}
		return position;
	}

	private static ArrayList<Position> getReachableKingPosition(GameBoard board, int col, int row, boolean defend)
	{
		ArrayList<Position> position = new ArrayList<>();
		Piece p = board.board[col][row];
		checkFreeOrEatAndAdd(board.board, position, col - 1, row - 1, p);
		checkFreeOrEatAndAdd(board.board, position, col - 1, row + 1, p);
		checkFreeOrEatAndAdd(board.board, position, col + 1, row - 1, p);
		checkFreeOrEatAndAdd(board.board, position, col + 1, row + 1, p);
		checkFreeOrEatAndAdd(board.board, position, col + 1, row, p);
		checkFreeOrEatAndAdd(board.board, position, col - 1, row, p);
		checkFreeOrEatAndAdd(board.board, position, col, row - 1, p);
		checkFreeOrEatAndAdd(board.board, position, col, row + 1, p);

		if (!board.hasKingMoved(p.getColor()))
		{
			if (!board.hasLRookMoved(p.getColor()))
			{
				if (!isUnderAttack(board, Position.get(col, row)) && !isUnderAttack(board, Position.get(col - 1, row))
						&& !isUnderAttack(board, Position.get(col - 2, row)) && board.isEmpty(col - 1, row)
						&& board.isEmpty(col - 2, row) && board.isEmpty(col - 3, row))
				{
					position.add(Position.get(col-3, row));
				}
			}
			if (!board.hasRRookMoved(p.getColor()))
			{
				if (!isUnderAttack(board, Position.get(col, row)) && !isUnderAttack(board, Position.get(col + 1, row))
						&& board.isEmpty(col + 1, row) && board.isEmpty(col + 2, row))
				{
					position.add(Position.get(col+2, row));
				}
			}
		}

		if (defend)
		{
			checkDefend(board.board, position, col - 1, row - 1, p);
			checkDefend(board.board, position, col - 1, row + 1, p);
			checkDefend(board.board, position, col + 1, row - 1, p);
			checkDefend(board.board, position, col + 1, row + 1, p);
			checkDefend(board.board, position, col + 1, row, p);
			checkDefend(board.board, position, col - 1, row, p);
			checkDefend(board.board, position, col, row - 1, p);
			checkDefend(board.board, position, col, row + 1, p);
		}
		return position;
	}

	private static ArrayList<Position> getReachableQueenPosition(GameBoard board, Piece piece, int col, int row,
			boolean defend)
	{
		ArrayList<Position> position = getReachableRookPosition(board, piece, col, row, defend);
		position.addAll(getReachableBishopPosition(board.board, col, row, defend));
		return position;
	}

	private static ArrayList<Position> getReachablePawnPosition(Piece[][] board, int col, int row, boolean defend)
	{
		ArrayList<Position> position = new ArrayList<>();
		Piece p = board[col][row];
		if (p.getColor() == Piece.BLACK)
		{
			checkEatAndAdd(board, position, col + 1, row + 1, p);
			checkEatAndAdd(board, position, col - 1, row + 1, p);
			if (defend)
			{
				checkDefend(board, position, col + 1, row + 1, p);
				checkDefend(board, position, col - 1, row + 1, p);
			}
			if (checkFree(board, col, row + 1))
			{
				position.add(Position.get(col, row + 1));
				if (row == 1)
					checkFreeAndAdd(board, position, col, row + 2);
			}
		}
		else
		{
			checkEatAndAdd(board, position, col + 1, row - 1, p);
			checkEatAndAdd(board, position, col - 1, row - 1, p);
			if (defend)
			{
				checkDefend(board, position, col + 1, row - 1, p);
				checkDefend(board, position, col - 1, row - 1, p);
			}
			if (checkFree(board, col, row - 1))
			{
				position.add(Position.get(col, row - 1));
				if (row == 6)
					checkFreeAndAdd(board, position, col, row - 2);
			}
		}
		return position;
	}

	public static int protectedVal(GameBoard board, Position targetPos, ArrayList<Move> allPossibleMoves)
	{
		if (board.isEmpty(targetPos))
			return 0;
		int score = 0;
		for (Move m : allPossibleMoves)
		{
			if (m.getDestPosition() == targetPos)
			{
				Piece defending = board.getPiece(m.getStartPosition());
				Piece defended = board.getPiece(targetPos);
				score = (defended.getType() - defending.getType());
			}
		}
		return score;
	}

	public static int attackVal(GameBoard board, Position targetPos, ArrayList<Move> allPossibleMoves, int goodForColor)
	{
		if (board.isEmpty(targetPos))
			return 0;
		int score = 0;
		boolean queen = false;
		for (Move m : allPossibleMoves)
		{
			if (m.getDestPosition() == targetPos)
			{
				Piece killed = m.getKilledPiece();
				if (killed.getType() == Piece.QUEEN)
				{
					queen = true;
				}
				Piece attacker = board.getPiece(m.getStartPosition());
				System.out.println(killed.getType() + " " + attacker.getType());

				if (killed.getColor() != goodForColor)
				{
					if (queen)
						score -= 5;
					score = (attacker.getType() - killed.getType());
				}
				else
				{
					if (queen)
						score += 5;
					score = (killed.getType() - attacker.getType());
				}
			}
		}
		return score;
	}

	public static int protectedVal(GameBoard board, Position targetPos)
	{
		if (board.isEmpty(targetPos))
			return 0;
		return protectedVal(board, targetPos, board.getAllPossibleMovesWithDefend(board.getPiece(targetPos).getColor()));
	}

	public static int attackVal(GameBoard board, Position targetPos, int goodForColor)
	{
		if (board.isEmpty(targetPos))
			return 0;
		return attackVal(board, targetPos, board.getAllPossibleMoves(board.getPiece(targetPos).getColor()),
				goodForColor);
	}

	public static boolean isProtected(GameBoard board, Position targetPos, ArrayList<Move> allPossibleMoves)
	{
		if (board.isEmpty(targetPos))
			return false;
		for (Move m : allPossibleMoves)
		{
			if (m.getDestPosition() == targetPos)
				return true;
		}
		return false;
	}

	public static boolean isProtected(GameBoard board, Position targetPos)
	{
		if (board.isEmpty(targetPos))
			return false;
		return isProtected(board, targetPos, board.getAllPossibleMovesWithDefend(board.getPiece(targetPos).getColor()));
	}

	public static boolean isUnderAttack(GameBoard board, Position targetPos, ArrayList<Move> allPossibleMoves)
	{
		if (board.isEmpty(targetPos))
			return false;
		for (Move m : allPossibleMoves)
		{
			if (m.getDestPosition() == targetPos)
				return true;
		}
		return false;
	}

	public static boolean isUnderAttack(GameBoard board, Position targetPos)
	{
		if (board.isEmpty(targetPos))
			return false;
		return isUnderAttack(board, targetPos, board.getAllPossibleMoves(board.getPiece(targetPos).getColor()));
	}

	private static boolean checkFree(Piece[][] board, int col, int row)
	{
		if (col < 0 || col > 7 || row < 0 || row > 7)
			return false;
		return board[col][row] == null;
	}

	private static boolean checkFreeAndAdd(Piece[][] board, ArrayList<Position> arr, int col, int row)
	{
		if (col < 0 || col > 7 || row < 0 || row > 7)
			return false;
		boolean ret = board[col][row] == null;
		if (ret)
		{
			arr.add(Position.get(col, row));
		}
		return ret;
	}

	private static boolean checkDefend(Piece[][] board, ArrayList<Position> arr, int col, int row, Piece c)
	{
		if (col < 0 || col > 7 || row < 0 || row > 7)
			return false;
		if (board[col][row] == null)
		{
			return true;
		}
		if (board[col][row].getColor() == c.getColor())
		{
			arr.add(Position.get(col, row));
		}
		return false;
	}

	private static boolean checkFreeOrEatAndAdd(Piece[][] board, ArrayList<Position> arr, int col, int row, Piece c)
	{
		if (col < 0 || col > 7 || row < 0 || row > 7)
			return false;
		if (board[col][row] == null)
		{
			arr.add(Position.get(col, row));
			return true;
		}
		if (board[col][row].getColor() != c.getColor())
		{
			arr.add(Position.get(col, row));
		}
		return false;
	}

	private static boolean checkEatAndAdd(Piece[][] board, ArrayList<Position> arr, int col, int row, Piece c)
	{
		if (col < 0 || col > 7 || row < 0 || row > 7)
			return false;
		if (board[col][row] == null)
		{
			return true;
		}
		if (board[col][row].getColor() != c.getColor())
		{
			arr.add(Position.get(col, row));
		}
		return false;
	}
}
