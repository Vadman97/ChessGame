package vad;

import java.util.ArrayList;
import java.util.HashMap;

public class AIPlayer implements Player
{
	int playerColor;
	int depth = 4;
	GameBoard realBoard;
	HashMap<CompressedGameBoard, TranspositionTableEntry> cache = new HashMap<>();

	ChessGUI gui;

	int protectedValWeight = 2;

	public static final int MAX = Integer.MAX_VALUE;
	public static final int MIN = -MAX;
	
	public boolean thinking = false;

	private static final boolean UI_ENABLED = false;
	
	int benchMark;

	public AIPlayer(int playerColor)
	{
		this.playerColor = playerColor;
		if (UI_ENABLED)
			gui = new ChessGUI(null, playerColor);
	}

	@Override
	public Move makeMove(CompressedGameBoard cpdboard)
	{
		GameBoard board=cpdboard.getGameBoard();
		realBoard = board;
		long start = System.currentTimeMillis();
		thinking = true;
		Move move = getBestMove(board, depth);
		long end = System.currentTimeMillis();
		double timeSec = (end - start) / 1000.;
		
		// increase search depth if our search space gets smaller
		if (board.getNumAllPieces() < 18) {
			if (depth >= 4 && board.getNumAllPieces() < 12) {
				depth += 2;
			} else {
				depth = 6;
			}
		} else if (timeSec < 0.25) {
			depth += 2;
		} else if (timeSec > 15.) {
			depth -= 2;
		}
		
		System.out.println("AI Think time: " + timeSec + " depth: " + depth);
		thinking = false;
		return move;
	}

	@Override
	public void update(CompressedGameBoard board)
	{
		update(board.getGameBoard());
	}
	
	public void update(GameBoard board)
	{
		if (UI_ENABLED)
			gui.updateBoard(board);
	}

	public int negamax(GameBoard board, int alpha, int beta, int d)
	{
		CompressedGameBoard cgb = new CompressedGameBoard(board);
		if (cache.containsKey(cgb))
		{
			TranspositionTableEntry entry=cache.get(cgb);
			if(entry.isLowerBound()){
				if(entry.getValue()>alpha){
					alpha=entry.getValue();
				}
			}else if(entry.isUpperBound()){
				if(entry.getValue()<beta){
					beta=entry.getValue();
				}
			}else{
				return entry.getValue();
			}
			if(alpha>=beta){
				return entry.getValue();
			}
		}
		int originalAlpha=alpha;
		if (d == 0)
		{
			int score = evaluateBoard(board);
			if (board.currentColor != playerColor)
				score = -score;
			cache.put(cgb, new TranspositionTableEntry(TranspositionTableEntry.PRECISE, score, 0));
			benchMark++;
			return score;
		}
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			board.apply(child);
			int score = -negamax(board, -beta, -alpha, d - 1);
			
			board.undo(child);
			if (score > alpha)
				alpha = score;
			if (alpha >= beta) // was >=
			{
				break;
			}
		}
		int cacheFlag=alpha<=originalAlpha?TranspositionTableEntry.UPPER_BOUND:(alpha>=beta?TranspositionTableEntry.LOWER_BOUND:TranspositionTableEntry.PRECISE);
		cache.put(cgb, new TranspositionTableEntry(cacheFlag, alpha, d));
		return alpha;
	}
	
	public int negascout(GameBoard board, int alpha, int beta, int d)
	{
		CompressedGameBoard cgb = new CompressedGameBoard(board);
		if (cache.containsKey(cgb))
		{
			synchronized (this) {
				TranspositionTableEntry entry=cache.get(cgb);
				if(entry.isLowerBound()){
					if(entry.getValue()>alpha){
						alpha=entry.getValue();
					}
				}else if(entry.isUpperBound()){
					if(entry.getValue()<beta){
						beta=entry.getValue();
					}
				}else{
					return entry.getValue();
				}
				if(alpha>=beta){
					return entry.getValue();
				}
			}
		}
		int originalAlpha=alpha;
		if (d == 0)
		{
			int score = evaluateBoard(board);
			if (board.currentColor != playerColor)
				score = -score;
			synchronized (this) {
				cache.put(cgb, new TranspositionTableEntry(TranspositionTableEntry.PRECISE, score, 0));
			}
			if (benchMark % 20000 == 0)
				System.out.println("Searched " + benchMark + " nodes.");
			benchMark++;
			return score;
		}
		boolean first=true;
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			int score;
			if(first){
				first=false;
				board.apply(child);
				score = -negascout(board, -alpha-1, -alpha, d - 1);
				if(alpha<score&&score<beta){
					score=-negascout(board, -beta, -score, d-1);
				}
				board.undo(child);
			}else{
				board.apply(child);
				score = -negascout(board, -beta, -alpha, d - 1);
				board.undo(child);
			}
			if (score > alpha)
				alpha = score;
			if (alpha >= beta)
			{
				break;
			}
		}
		int cacheFlag=alpha<=originalAlpha?TranspositionTableEntry.UPPER_BOUND:(alpha>=beta?TranspositionTableEntry.LOWER_BOUND:TranspositionTableEntry.PRECISE);
		synchronized (this) {
			cache.put(cgb, new TranspositionTableEntry(cacheFlag, alpha, d));
		}
		return alpha;
	}
	
	private class NegascoutThread extends Thread {
		Move startMove = null;
		int d;
		boolean first=true;
		GameBoard board;

		public int alpha = MIN;
		public boolean finished = false;
		public Move best = null;
		
		public NegascoutThread(Move startMove, GameBoard board, int startD) {
			this.startMove = startMove;
			this.board = board.copy();
			this.d = startD;
		}
		
		public void run() {
			int score;
			if(first){
				first=false;
				board.apply(startMove);
				score = -negascout(board, -alpha-1, -alpha, d - 1);
				if(alpha<score){
					score=-negascout(board, MIN, -score, d-1);
				}
				board.undo(startMove);
			}else{
				board.apply(startMove);
				score = -negascout(board, MIN, -alpha, d - 1);
				board.undo(startMove);
			}
			if (score > alpha)
			{
				alpha = score;
				best = startMove;
			}
			finished = true;
		}
	}
	
	public Move getBestMoveNegamax(GameBoard board, int d)
	{
		ArrayList<NegascoutThread> threads = new ArrayList<NegascoutThread>();
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			NegascoutThread t = new NegascoutThread(child, board, d);
			t.start();
			threads.add(t);
		}
		System.out.println(threads.size() + " threads started!");
		
		while (true) {
			boolean finished = true;
			for (NegascoutThread t: threads) {
				if (!t.finished) {
					finished = false;
					break;
				}
			}
			if (!finished) {
				try {
					if (benchMark % 20000 == 0)
						System.out.println("Searched " + benchMark + " nodes.");
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		
		Move best = null;
		int alpha = MIN;
		
		for (NegascoutThread t: threads) {
			if (t.alpha >= alpha) {
				alpha = t.alpha;
				best = t.best;
			}
		}
		
		System.out.println("Best score: " + alpha);
		cache.clear();
		return best;
	}

//	public Move getBestMoveNegamax(GameBoard board, int d)
//	{
//		Move best = null;
//		int alpha = MIN;
//		boolean first=true;
//		for (Move child : board.getAllPossibleMoves(board.currentColor))
//		{
//			int score;
//			if(first){
//				first=false;
//				board.apply(child);
//				score = -negascout(board, -alpha-1, -alpha, d - 1);
//				if(alpha<score){
//					score=-negascout(board, MIN, -score, d-1);
//				}
//				board.undo(child);
//			}else{
//				board.apply(child);
//				score = -negascout(board, MIN, -alpha, d - 1);
//				board.undo(child);
//			}
//			if (score > alpha)
//			{
//				alpha = score;
//				best = child;
//			}
//		}
//		System.out.println("Best score: " + alpha);
//		cache.clear();
//		return best;
//	}
	
	public Move getBestMoveNegascout(GameBoard board, int d)
	{
		Move best = null;
		int alpha = MIN;
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			board.apply(child);
			
			int score = -negascout(board, MIN, -alpha, d - 1);
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
	
	public Move getBestMoveMTDF(GameBoard board, int d){
//		int score=0;
//		int lb=MIN;
//		int ub=MAX;
//		do{
//			int beta=score==lb?score+1:score;
//			score=negamax(board, beta-1, beta, d);
//			if(score<beta){
//				ub=score;
//			}else{
//				lb=score;
//			}
//		}while(lb<ub);
		return getBestMoveNegamax(board, d);
	}
	
	public Move getBestMove(GameBoard board, int d)
	{
		/*
		 * Due to unknown reason, 
		 * negascout search more nodes than negamax(why?)
		 * but MTD-F search less nodes
		 * All these three should give same results - 
		 * which is the optimal result.
		 */
		System.out.println("Thinking.....");
		benchMark=0;
		Move ret= getBestMoveMTDF(board, d);
		System.out.println(benchMark+" nodes searched");
		return ret;
	}

	public int evaluateBoard(GameBoard board)
	{
//		long start = System.nanoTime();
		// TODO(vadim): change the GameBoard to save an array of which squares are under attack/defended etc
		int score = 0;

		int numK = 0, numEK = 0;
		int numQ = 0, numEQ = 0;
		int numR = 0, numER = 0;
		int numB = 0, numEB = 0;
		int numN = 0, numEN = 0;
		int numP = 0, numEP = 0;
		int doubledPawns = 0;
		int doubledEPawns = 0;
//		int blockedPawns = 0;
//		int blockedEPawns = 0;
		int isolatedPawns = 0;
		int isolatedEPawns = 0;
		int mobility = 0;
		int EMobility = 0;
		int fProtected = 0;
		int eProtected = 0;
		int castleVal = 0;
		int castleEVal = 0;
		int piecesOnStartRow = 0;
		int piecesEOnStartRow = 0;

//		mobility = board.getAllPossibleMoves(playerColor).size();
//		EMobility = board.getAllPossibleMoves(playerColor == Piece.BLACK ? Piece.WHITE : Piece.BLACK).size();
		
//		ArrayList<Move> fMoves = board.getAllPossibleMovesWithDefend(playerColor);
//		ArrayList<Move> eMoves = board.getAllPossibleMovesWithDefend(Piece.getOppositeColor(playerColor));

		for (int i = 0; i < 8; i++) // col
		{
			boolean columnHasPawnF = false;
			boolean columnHasPawnE = false;
			for (int j = 0; j < 8; j++) // row
			{
				Position pos = Position.get(i, j);
				if (board.isEmpty(pos))
					continue;
				Piece p = board.getPiece(pos);

				if (p.getColor() == playerColor) // piece of computer
				{
					if (j == 0 && p.getType() != Piece.KING)
						piecesOnStartRow += 3;
//					mobility += MoveHelper.getAllMoves4PieceWithoutValidation(board, pos).size();
					switch (p.getType())
					{
					case Piece.KING:
//						castleVal = MoveHelper.canCastleLeft(board, playerColor, i, j) ? 1 : 0; //hasCastled
						numK++;
						break;
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
						if (i == 0 || i == 7)
							isolatedPawns++;
						if (j == 1) {
							piecesOnStartRow++;
						}
						/*
						 * if (MoveHelper.getAllMoves4Piece(board,
						 * Position.get(i, j), false).size() == 0)
						 * blockedPawns++;
						 */
						mobility += MoveHelper.getAllMoves4PieceWithoutValidation(board, pos).size();
						break;
					}
					
//					 if (MoveHelper.isProtected(board, Position.get(i,j), fMoves))
//						 fProtected++;
					 
				} else
				{
					if (j == 7 && p.getType() != Piece.KING)
						piecesEOnStartRow += 3;
//					EMobility += MoveHelper.getAllMoves4PieceWithoutValidation(board, pos).size();
					switch (p.getType())
					{
					case Piece.KING:
//						castleEVal = MoveHelper.canCastleLeft(board, playerColor, i, j) ? 1 : 0;
						numEK++;
						break;
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
						if (i == 0 || i == 7)
							isolatedEPawns++;
						if (j == 6) {
							piecesEOnStartRow++;
						}
						/*
						 * if (MoveHelper.getAllMoves4Piece(board,
						 * Position.get(i, j), false).size() == 0)
						 * blockedEPawns++;
						 */
						EMobility += MoveHelper.getAllMoves4PieceWithoutValidation(board, pos).size();
						break;
					}
					
//					 if (MoveHelper.isProtected(board, Position.get(i,j), eMoves))
//						 eProtected++;
				}
			}
		}
		// 200, 9, 5, 3, 1, 0.5, 0.25, 1
		// 800, 36, 20, 12, 4, 2, 1, 4
//		score = 1600 * (numK - numEK) + 
//				800 * (numQ - numEQ) + 
//				320 * (numR - numER) + 
//				192 * (numB - numEB + numN - numEN) + 
//				2 * (numP - numEP) +
////				-1 * (doubledPawns - doubledEPawns + blockedPawns - blockedEPawns + isolatedPawns - isolatedEPawns) + 
////				1 * (mobility - EMobility) + 
//				// 8 * (fProtected - eProtected)
//				- 2 * (castleVal - castleEVal);
		score = 50 * (
					32 * (numK - numEK) + 
					9 * (numQ - numEQ) + 
					5 * (numR - numER) + 
					3 * (numB - numEB + numN - numEN) + 
					1 * (numP - numEP)
					) + 
				5 * (mobility - EMobility) + 
				5 * (doubledEPawns - doubledPawns) + 
				1 * (isolatedEPawns - isolatedPawns) +
				0 * (castleVal - castleEVal) +
				2 * (piecesEOnStartRow - piecesOnStartRow) + 
				0 * (fProtected - eProtected)
				;
//		System.out.println(((double)(System.nanoTime() - start)) * 1e-9);
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
				ArrayList<Move> allPossibleMovesDef = board.getAllPossibleMovesWithDefend(board.getPiece(Position.get(i, j))
						.getColor());
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
						} else
						{
							protectedValWeight = 6;
						}
						score += protectedVal * protectedValWeight;
					} else
					{
						if (!isUnderAttack)
							score += 2;
					}
					if (isUnderAttack)
					{
						/*
						 * if (curPiece.getType() == Piece.QUEEN) { if
						 * (!isProtected) score -= 350; else score -= 125; }
						 */
						score += (attackValFriendly * 20);
						if (isProtected)
							score += 10;
					}
					score += (numPieces * 3);
					score += 2 * (6 - curPiece.getType());
				} else
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
						} else
						{
							protectedValWeight = 3;
						}
						score -= protectedVal * protectedValWeight;
					} else
					{
						if (!isUnderAttack)
							score -= 10;
					}
					if (isUnderAttack)
					{
						/*
						 * if (curPiece.getType() == Piece.QUEEN) { if
						 * (!isProtected) score += 100; else score += 25; }
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
