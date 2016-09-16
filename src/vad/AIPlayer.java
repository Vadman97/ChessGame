package vad;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AIPlayer implements Player {
	public static final int CACHE_INITIAL_SIZE = 2000003;
	public static final float CACHE_LOAD_FACTOR = 0.9f;
	public static final int CACHE_NUM_SHARDS = 4;

	public static final int MAX = Integer.MAX_VALUE;
	public static final int MIN = -MAX;
	public static final int SEARCH_PRINT_DELAY = 2000; // ms
	
	public static final int REPEATED_MOVE_PENALTY = 10000;

	private static final boolean UI_ENABLED = false;

	int playerColor;
	int depth = 6;
	GameBoard realBoard;
	ConcurrentHashMap<CompressedGameBoard, TranspositionTableEntry> cache = new ConcurrentHashMap<>(CACHE_INITIAL_SIZE, CACHE_LOAD_FACTOR, CACHE_NUM_SHARDS);
	
	int globalAlpha = MAX, globalBeta = MIN;

	ChessGUI gui;
	GameBoard lastBoardConfig;

	int protectedValWeight = 2;
	int benchMark;
	
	Random r = new Random();

	public boolean thinking = false;
	
	public synchronized void updateGlobalAlpha(int alpha) {
		System.out.println(this.globalAlpha + " " + alpha);
		this.globalAlpha = alpha;
	}
	
	public synchronized void updateGlobalBeta(int beta) {
		this.globalBeta = beta;
	}

	public AIPlayer(int playerColor) {
		this.playerColor = playerColor;
		if (UI_ENABLED)
			gui = new ChessGUI(null, playerColor);
	}

	@Override
	public Move makeMove(CompressedGameBoard cpdboard) {
		GameBoard board = cpdboard.getGameBoard();
		realBoard = board;
		long start = System.currentTimeMillis();
		thinking = true;
		lastBoardConfig = board.copy(); // apply past config
		Move move = getBestMove(board, depth);
		long end = System.currentTimeMillis();
		double timeSec = (end - start) / 1000.;

		if (board.getNumAllPieces() < 20) {
			// increase search depth if our search space gets smaller
			if (timeSec < 0.75 && depth < 10) {
				depth += 2;
			} else if (timeSec > 15.) {
				depth -= 2;
			}
		}

		System.out.println("AI Think time: " + timeSec + " depth: " + depth + " pieces: " + board.getNumAllPieces());
		thinking = false;
		return move;
	}

	@Override
	public void update(CompressedGameBoard board) {
		update(board.getGameBoard());
	}

	public void update(GameBoard board) {
		if (UI_ENABLED)
			gui.updateBoard(board);
	}
	
	public int negascout(GameBoard board, int alpha, int beta, int d) {
		CompressedGameBoard cgb = new CompressedGameBoard(board);
		// check cache in case this board was already evaluated
		if (cache.containsKey(cgb)) {
			TranspositionTableEntry entry = cache.get(cgb);
			if (entry.isLowerBound()) {
				if (entry.getValue() > alpha) {
					alpha = entry.getValue();
				}
			} else if (entry.isUpperBound()) {
				if (entry.getValue() < beta) {
					beta = entry.getValue();
				}
			} else {
				return entry.getValue();
			}
			if (alpha >= beta) {
				return entry.getValue();
			}
		}
		
		// if we are at the bottom of the tree, return the board score
		if (d == 0) {
			int score = evaluateBoard(board);
				
			if (board.currentColor != playerColor)
				score = -score;

			cache.put(cgb, new TranspositionTableEntry(TranspositionTableEntry.PRECISE, score, 0));
			benchMark++;
			return score;
		}
		
		// if we are not at the bottom of the tree, continue down the tree to return score
		boolean first = true;
		int originalAlpha = alpha;
		for (Move child : board.getAllPossibleMoves(board.currentColor)) {
			int score = 0;
			
			if (first) {
				first = false;
				board.apply(child);
				score = -negascout(board, -alpha - 1, -alpha, d - 1);
				if (alpha < score && score < beta) {
					score = -negascout(board, -beta, -score, d - 1);
				}
				board.undo(child);
			} else {
				board.apply(child);
				score = -negascout(board, -beta, -alpha, d - 1);
				board.undo(child);
			}
			if (score > alpha) {
				alpha = score;
				updateGlobalAlpha(alpha);
			}
			if (alpha >= beta) {
				break;
			}
		}
		int cacheFlag = alpha <= originalAlpha ? TranspositionTableEntry.UPPER_BOUND : (alpha >= beta ? TranspositionTableEntry.LOWER_BOUND : TranspositionTableEntry.PRECISE);
		cache.put(cgb, new TranspositionTableEntry(cacheFlag, alpha, d));
		
//		if (cacheFlag == TranspositionTableEntry.UPPER_BOUND)
//			updateGlobalAlpha(alpha);
//		else if (cacheFlag == TranspositionTableEntry.LOWER_BOUND)
//			updateGlobalBeta(beta);
		return alpha;
	}

	public Move getBestMoveNegamaxNoThreads(GameBoard board, int d)
	{
		Move best = null;
		int alpha = MIN;
		boolean first=true;
		for (Move child : board.getAllPossibleMoves(board.currentColor))
		{
			int score;
			if(first){
				first=false;
				board.apply(child);
				score = -negascout(board, -alpha-1, -alpha, d - 1);
				if(alpha<score){
					score = -negascout(board, MIN, -score, d - 1);
				}
				board.undo(child);
			}else{
				board.apply(child);
				score = -negascout(board, MIN, -alpha, d - 1);
				board.undo(child);
			}
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
	
	private class NegascoutParallel extends Thread {
		int d;
		GameBoard board;

		public int score;
		Move move = null;
		public boolean finished = false;

		public NegascoutParallel(Move move, GameBoard board, int startD) {
			this.move = move;
			this.board = board.copy();
			this.d = startD;
		}

		public void run() {
			board.apply(move);
			score = -negascout(board, -globalBeta, -globalAlpha, d - 1);
			board.undo(move);
			finished = true;
			
			if (score < globalAlpha)
				updateGlobalAlpha(score);
		}
	}	

	public Move getBestMoveNegascout(GameBoard board, int d) {
		ArrayList<NegascoutParallel> threads = new ArrayList<NegascoutParallel>();
		
		ArrayList<Move> moves =  board.getAllPossibleMoves(board.currentColor);
		
		boolean first = true;
		
		updateGlobalAlpha(MIN);
		for (Move child : moves) {
			int score = 0;
			
			if (first) {
				first = false;
				board.apply(child);
				score = -negascout(board, -globalAlpha - 1, -globalAlpha, d - 1);
				if (globalAlpha < score && score < globalBeta) {
					score = -negascout(board, -globalBeta, -score, d - 1);
				}
				board.undo(child);
				System.out.println("First A: " + globalAlpha + " B: " + globalBeta);
			} else {
				NegascoutParallel t = new NegascoutParallel(child, board, d);
				threads.add(t);
				t.start();
			}
		}

		long start = System.currentTimeMillis();
		long lastPrint = start;

		while (true) {
			boolean finished = true;
			for (NegascoutParallel t : threads) {
				if (!t.finished) {
					finished = false;
					break;
				}
			}
			if (!finished) {
				try {
					if (System.currentTimeMillis() - lastPrint >= SEARCH_PRINT_DELAY) {
						System.out.print("Elapsed: " + (System.currentTimeMillis() - start) / 1000. + "s. Searched "
								+ benchMark + " nodes. ");
						DecimalFormat f = new DecimalFormat(".#");
						System.out.println(f.format(1.0e6 / benchMark * (System.currentTimeMillis() - start) / 1000.) + "s per 1M nodes.");
						lastPrint = System.currentTimeMillis();
					}
						
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		System.out.println("Final A: " + globalAlpha + " B: " + globalBeta);

		Move best = null;
		int score = MIN;

		for (NegascoutParallel t : threads) {
			if (t.score >= score) {
				score = t.score;
				best = t.move;
			}
		}

		System.out.println("Best score: " + score);
		cache.clear();
		return best;
	}

	public Move getBestMoveMTDF(GameBoard board, int d) {
		int score = 0;
		int lb = MIN;
		int ub = MAX;
		do {
			int beta = score == lb ? score + 1 : score;
			score = negascout(board, beta - 1, beta, d);
			if (score < beta) {
				ub = score;
			} else {
				lb = score;
			}
		} while (lb < ub);
		return getBestMoveNegascout(board, d);
	}

	public Move getBestMove(GameBoard board, int d) {
		/*
		 * Due to unknown reason, negascout search more nodes than negamax(why?)
		 * but MTD-F search less nodes All these three should give same results
		 * - which is the optimal result.
		 */
		System.out.println("Thinking.....");
		benchMark = 0;
		//Move ret = getBestMoveNegamaxNoThreads(board, d);
		Move ret = getBestMoveNegascout(board, d);
		System.out.println(benchMark + " nodes searched");
		return ret;
	}

	public int evaluateBoard(GameBoard board) {
//		if (board.isCheckMate(playerColor))
//			return -10000;
//		else if (board.isCheckMate(Piece.getOppositeColor(playerColor)))
//			return 10000;

		// TODO(vadim): change the GameBoard to save an array of which squares
		// are under attack/defended etc
		int score = 0;

		 int numK = 0, numEK = 0;
		int numQ = 0, numEQ = 0;
		int numR = 0, numER = 0;
		int numB = 0, numEB = 0;
		int numN = 0, numEN = 0;
		int numP = 0, numEP = 0;
		int doubledPawns = 0;
		int doubledEPawns = 0;
		// int blockedPawns = 0;
		// int blockedEPawns = 0;
		int isolatedPawns = 0;
		int isolatedEPawns = 0;
//		int mobility = 0;
//		int EMobility = 0;
		int fProtected = 0;
		int eProtected = 0;
		int castleVal = 0;
		int castleEVal = 0;
		int piecesOnStartRow = 0;
		int piecesEOnStartRow = 0;

		// ArrayList<Move> fMoves =
		// board.getAllPossibleMovesWithDefend(playerColor);
		// ArrayList<Move> eMoves =
		// board.getAllPossibleMovesWithDefend(Piece.getOppositeColor(playerColor));

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
					if (j == 0 && p.getType() != Piece.KING && p.getType() != Piece.ROOK && p.getType() != Piece.QUEEN)
						piecesOnStartRow += 3;
					else if (j == 0 && p.getType() == Piece.QUEEN)
						piecesOnStartRow += 1;
					//mobility += MoveHelper.getAllMoves4PieceWithoutValidation(board, pos).size();
					switch (p.getType()) {
					case Piece.KING:
						castleVal = board.hasCastled(playerColor) ? 1 : 0;
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
						break;
					}

					// if (MoveHelper.isProtected(board, Position.get(i,j),
					// fMoves))
					// fProtected++;

				} else {
					if (j == 0 && p.getType() != Piece.KING && p.getType() != Piece.ROOK && p.getType() != Piece.QUEEN)
						piecesEOnStartRow += 3;
					else if (j == 0 && p.getType() == Piece.QUEEN)
						piecesEOnStartRow += 1;
					//EMobility += MoveHelper.getAllMoves4PieceWithoutValidation(board, pos).size();
					switch (p.getType()) {
					case Piece.KING:
						castleEVal = board.hasCastled(Piece.getOppositeColor(playerColor)) ? 1 : 0;
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
						break;
					}

					// if (MoveHelper.isProtected(board, Position.get(i,j),
					// eMoves))
					// eProtected++;
				}
			}
		}

		score = 30 * (
						10000 * (numK - numEK) +
						9 * (numQ - numEQ) + 
						5 * (numR - numER) + 
						3 * (numB - numEB + numN - numEN) + 
						1 * (numP - numEP)
					) + 
				//2 * (mobility - EMobility) + 
				5 * (doubledEPawns - doubledPawns) + 
				5 * (isolatedEPawns - isolatedPawns) + 
				30 * (castleVal - castleEVal) + 
				5 * (piecesEOnStartRow - piecesOnStartRow) + 
				0 * (fProtected - eProtected);

//		if (board.currentColor == playerColor)
//			if (board != null && lastBoardConfig != null) {
//				if (new CompressedGameBoard(board).equals(new CompressedGameBoard(lastBoardConfig)))
//					score -= REPEATED_MOVE_PENALTY;
//			}
		
		return score;
	}

	public int oldEvaluateBoard(GameBoard board) {
		// if can kill, good
		// for each piece close to king, good
		// if can kill better piece very good
		// TODO add more incentive to kill enemy.
		// TODO add attack weight
		// TODO Make sure protectedVal and attackValFriendly work
		// TODO Punish for pieces on edges, good to move out pieces
		// TODO if queen attacker is protected, god otherwise bad
		int score = 0;

		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (board.getPiece(Position.get(i, j)) == null)
					continue;
				ArrayList<Move> allPossibleMovesDef = board
						.getAllPossibleMovesWithDefend(board.getPiece(Position.get(i, j)).getColor());
				ArrayList<Move> allPossibleMovesReg = board
						.getAllPossibleMoves(board.getPiece(Position.get(i, j)).getColor());

				boolean isProtected = MoveHelper.isProtected(board, Position.get(i, j), allPossibleMovesDef);
				int protectedVal = MoveHelper.protectedVal(board, Position.get(i, j), allPossibleMovesDef);

				boolean isUnderAttack = MoveHelper.isUnderAttack(board, Position.get(i, j), allPossibleMovesReg);
				int attackValFriendly = MoveHelper.attackVal(board, Position.get(i, j), allPossibleMovesReg,
						playerColor);

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

					if (isProtected) {
						if (numEnemyPieces <= 7) {
							protectedValWeight = 15;
						} else {
							protectedValWeight = 6;
						}
						score += protectedVal * protectedValWeight;
					} else {
						if (!isUnderAttack)
							score += 2;
					}
					if (isUnderAttack) {
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

					if (isProtected) {
						if (numPieces <= 7) {
							protectedValWeight = 14;
						} else {
							protectedValWeight = 3;
						}
						score -= protectedVal * protectedValWeight;
					} else {
						if (!isUnderAttack)
							score -= 10;
					}
					if (isUnderAttack) {
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
