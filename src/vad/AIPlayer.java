package vad;

import java.util.ArrayList;
import java.util.HashMap;

public class AIPlayer implements Player
{
	int										playerColor;
	int										depth				= 3;
	GameBoard								realBoard;
	HashMap<CompressedGameBoard, Integer>	cache				= new HashMap<>();
	
	ChessGUI gui;

	int										protectedValWeight	= 2;

	public static final int					MAX					= Integer.MAX_VALUE;
	public static final int					MIN					= -MAX;
	
	private static final boolean UI_ENABLED=false;

	public AIPlayer(int playerColor)
	{
		this.playerColor = playerColor;
		if(UI_ENABLED)
			gui=new ChessGUI(null);
	}

	@Override
	public Move makeMove(GameBoard board)
	{
		realBoard = board;
		long start = System.currentTimeMillis();
		Move move = getBestMove(board, depth);
		long end = System.currentTimeMillis();
		System.out.println("AI Think time: " + (end - start) / 1000.);
		return move;
	}

	@Override
	public void update(GameBoard board)
	{
		if(UI_ENABLED)
			gui.updateBoard(board);
	}

	public int negamax(GameBoard board, int alpha, int beta, int d)
	{
		CompressedGameBoard cgb = new CompressedGameBoard(board);
		if (cache.containsKey(cgb))
		{
			return cache.get(cgb);
		}
		if (d == 0)
		{
			int score = evaluateBoard(board);
			if (board.currentColor != playerColor)
				score = -score;
			cache.put(cgb, score);
			return score;
		}
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			board.apply(child);
			int score = -negamax(board, -beta, -alpha, d - 1);
			board.undo(child);
			if (score > alpha)
				alpha = score;
			if (alpha >= beta)
			{
				break;
			}
		}

		cache.put(cgb, alpha);
		return alpha;
	}

	public Move getBestMove(GameBoard board, int d)
	{
		Move best = null;
		int alpha = MIN;
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			board.apply(child);
			int score = -negamax(board, MIN, -alpha, d - 1);
			board.undo(child);
			if (score > alpha)
			{
				alpha = score;
				best = child;
			}
		}
		System.out.println("Best score: " + alpha);
		cache.clear();
		return best;
	}

	public int evaluateBoard(GameBoard board)
	{
		int score = 0;

		int numK = 0, numEK = 0;
		int numQ = 0, numEQ = 0;
		int numR = 0, numER = 0;
		int numB = 0, numEB = 0;
		int numN = 0, numEN = 0;
		int numP = 0, numEP = 0;
		int doubledPawns = 0;
		int doubledEPawns = 0;
		int blockedPawns = 0;
		int blockedEPawns = 0;
		int isolatedPawns = 0;
		int isolatedEPawns = 0;
		int mobility = 0;
		int EMobility = 0;
		int fProtected = 0;
		int eProtected = 0;

		mobility = board.getAllPossibleMoves(playerColor).size();
		EMobility = board.getAllPossibleMoves(playerColor == Piece.BLACK ? Piece.WHITE : Piece.BLACK).size();

		for (int i = 0; i < 8; i++)
		{
			boolean columnHasPawnF = false;
			boolean columnHasPawnE = false;
			for (int j = 0; j < 8; j++)
			{
				if (board.isEmpty(i, j))
					continue;
				Piece p = board.getPiece(i, j);

				if (p.getColor() == playerColor) // piece of computer
				{
					switch (p.getType())
					{
						case Piece.KING:
							numK++;
						case Piece.QUEEN:
							numQ++;
						case Piece.ROOK:
							numR++;
						case Piece.BISHOP:
							numB++;
						case Piece.KNIGHT:
							numN++;
						case Piece.PAWN:
							numP++;
							if (columnHasPawnF)
								doubledPawns++;
							columnHasPawnF = true;
							if (i == 0 || i == 7 || j == 0 || j == 7)
								isolatedPawns++;
							/*
							 * if (MoveHelper.getAllMoves4Piece(board, Position.get(i, j), false).size() == 0) blockedPawns++;
							 */
							break;
					}
					/*
					 * if (MoveHelper.isProtected(board, Position.get(i,j))) fProtected++;
					 */
				}
				else
				{
					switch (p.getType())
					{
						case Piece.KING:
							numEK++;
						case Piece.QUEEN:
							numEQ++;
						case Piece.ROOK:
							numER++;
						case Piece.BISHOP:
							numEB++;
						case Piece.KNIGHT:
							numEN++;
						case Piece.PAWN:
							numEP++;
							if (columnHasPawnE)
								doubledEPawns++;
							columnHasPawnE = true;
							if (i == 0 || i == 8 || j == 0 || j == 8)
								isolatedEPawns++;
							/*
							 * if (MoveHelper.getAllMoves4Piece(board, Position.get(i, j), false).size() == 0) blockedEPawns++;
							 */
							break;
					}
					/*
					 * if (MoveHelper.isProtected(board, Position.get(i,j))) eProtected++;
					 */
				}
			}
		}

		score = (int) (200 * (numK - numEK) + 9 * (numQ - numEQ) + 5 * (numR - numER) + 3 * (numB - numEB + numN - numEN) + 1 * (numP - numEP) - 0.5
				* (doubledPawns - doubledEPawns + blockedPawns - blockedEPawns + isolatedPawns - isolatedEPawns) + 0.25 * (mobility - EMobility))
				+ 1 * (fProtected - eProtected);
		// TODO maybe cast is bad
		return score;
	}

	public int oldEvaluateBoard(GameBoard board)
	{
		// if can kill, good
		// for each piece close to king, good
		// if can kill better piece very good
		// TODO add more incentive to kill enemy.
		// TODO add attack weight
		// TODO Make sure protectedVal and attackValFriendly work
		// TODO Punish for pieces on edges, good to move out pieces
		// TODO if queen attacker is protected, god otherwise bad
		int score = 0;

		for (int i = 0; i < 8; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				if (board.getPiece(Position.get(i, j)) == null)
					continue;
				ArrayList<Move> allPossibleMovesDef = board.getAllPossibleMovesWithDefend(board.getPiece(Position.get(i, j)).getColor());
				ArrayList<Move> allPossibleMovesReg = board.getAllPossibleMoves(board.getPiece(Position.get(i, j)).getColor());

				boolean isProtected = MoveHelper.isProtected(board, Position.get(i, j), allPossibleMovesDef);
				int protectedVal = MoveHelper.protectedVal(board, Position.get(i, j), allPossibleMovesDef);

				boolean isUnderAttack = MoveHelper.isUnderAttack(board, Position.get(i, j), allPossibleMovesReg);
				int attackValFriendly = MoveHelper.attackVal(board, Position.get(i, j), allPossibleMovesReg, playerColor);

				int numPieces = board.getNumPieces(playerColor);
				int numEnemyPieces = board.getNumPieces(Piece.getOppositeColor(playerColor));

				int trueBoardPieces = realBoard.getNumPieces(playerColor);
				int trueBoardEnemyPieces = realBoard.getNumPieces(Piece.getOppositeColor(playerColor));

				Piece curPiece = board.getPiece(Position.get(i, j));

				// System.out.println(protectedVal + " " + attackValFriendly);

				if (board.currentColor == playerColor) // ai player
				{
					if (trueBoardPieces > numPieces)
						score -= 20;
					if (trueBoardEnemyPieces < trueBoardPieces)
						score += 75;

					if (isProtected)
					{
						if (numEnemyPieces <= 7)
						{
							protectedValWeight = 15;
						}
						else
						{
							protectedValWeight = 6;
						}
						score += protectedVal * protectedValWeight;
					}
					else
					{
						if (!isUnderAttack)
							score += 2;
					}
					if (isUnderAttack)
					{
						/*
						 * if (curPiece.getType() == Piece.QUEEN) { if (!isProtected) score -= 350; else score -= 125; }
						 */
						score += (attackValFriendly * 20);
						if (isProtected)
							score += 10;
					}
					score += (numPieces * 3);
					score += 2 * (6 - curPiece.getType());
				}
				else
				// enemy
				{
					if (trueBoardPieces < numPieces)
						score += 15;
					if (trueBoardEnemyPieces > trueBoardPieces)
						score -= 35;

					if (isProtected)
					{
						if (numPieces <= 7)
						{
							protectedValWeight = 14;
						}
						else
						{
							protectedValWeight = 3;
						}
						score -= protectedVal * protectedValWeight;
					}
					else
					{
						if (!isUnderAttack)
							score -= 10;
					}
					if (isUnderAttack)
					{
						/*
						 * if (curPiece.getType() == Piece.QUEEN) { if (!isProtected) score += 100; else score += 25; }
						 */
						score -= (attackValFriendly * 20);
						if (isProtected)
							score -= 10;
					}
					score -= (numEnemyPieces * 3);
					score -= 2 * (6 - board.getPiece(Position.get(i, j)).getType());
				}

			}
		}
		return score;
	}
}
