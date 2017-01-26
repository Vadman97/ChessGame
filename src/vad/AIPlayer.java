package vad;

import java.util.HashMap;
import java.util.Random;

public class AIPlayer implements Player {
	public static final int CACHE_INITIAL_SIZE = 2000003;
	public static final float CACHE_LOAD_FACTOR = 0.8f;
	public static final int CACHE_NUM_SHARDS = 4;

	public static final int MAX = Integer.MAX_VALUE;
	public static final int MIN = -MAX;
	public static final int SEARCH_PRINT_DELAY = 2000; // ms

	public static final int REPEATED_MOVE_PENALTY = 10000;

	private static final boolean UI_ENABLED = false;

	private int playerColor; // ROW 7
	private int enemyColor; // ROW 0
	int depth = 4;
	GameBoard realBoard;
	HashMap<CompressedGameBoard, TranspositionTableEntry> cache = new HashMap<>(CACHE_INITIAL_SIZE, CACHE_LOAD_FACTOR);
	HashMap<CompressedGameBoard, TranspositionTableEntry2> cache2 = new HashMap<>(CACHE_INITIAL_SIZE,
			CACHE_LOAD_FACTOR);

	ChessGUI gui;
	GameBoard lastBoardConfig;

	int protectedValWeight = 2;
	int benchMark;

	Random r = new Random();

	public boolean thinking = false;

	public long totalTime = 0;
	public long totalNodes = 0;
	/* values from 1-8 */
	private int aggrMult, defMult;

	public AIPlayer(int playerColor) {
		this.playerColor = playerColor;
		this.enemyColor = Piece.getOppositeColor(this.playerColor);
		aggrMult = r.nextInt(4) + 1;
		defMult = r.nextInt(4) + 1;

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
			if (timeSec < 1. && depth < 10) {
				depth += 2;
			} else if (timeSec > 30.) {
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

	public ScoredMove AlphaBetaWithMemory(GameBoard board, int alpha, int beta, int d) {
		CompressedGameBoard cb = new CompressedGameBoard(board);
		if (cache2.containsKey(cb)) {
			TranspositionTableEntry2 entry = cache2.get(cb);
			if (entry.getLower() >= beta) {
				return new ScoredMove(entry.getMove(), entry.getLower());
			}
			if (entry.getUpper() <= alpha)
				return new ScoredMove(entry.getMove(), entry.getUpper());
			alpha = Math.max(alpha, entry.getLower());
			beta = Math.min(beta, entry.getUpper());
		}

		Move best = null;
		int score = 0;
		if (d == 0) {
			score = evaluateBoard(board);
			benchMark++;
		} else if (board.currentColor == playerColor) {
			// This is a max node
			score = MIN;
			int a = alpha;
			for (Move child : board.getAllPossibleMoves(board.currentColor)) {
				if (score >= beta)
					break;
				board.apply(child);
				ScoredMove val = AlphaBetaWithMemory(board, a, beta, d - 1);
				if (val.score > score) {
					score = val.score;
					best = child;
				}
				board.undo(child);
				a = Math.max(a, score);
			}
		} else {
			// This is a min node
			score = MAX;
			int b = beta;
			for (Move child : board.getAllPossibleMoves(board.currentColor)) {
				if (score <= alpha)
					break;
				board.apply(child);
				ScoredMove val = AlphaBetaWithMemory(board, alpha, b, d - 1);
				if (val.score < score) {
					score = val.score;
					best = child;
				}
				board.undo(child);
				b = Math.min(b, score);
			}
		}
		if (score <= alpha) {
			cache2.put(cb, new TranspositionTableEntry2(MIN, score, best));
		}
		if (score > alpha && score < beta) {
			cache2.put(cb, new TranspositionTableEntry2(score, score, best));
		}
		if (score >= beta) {
			cache2.put(cb, new TranspositionTableEntry2(score, MAX, best));
		}
		return new ScoredMove(best, score);
	}

	public int negascout(GameBoard board, int alpha, int beta, int d) {
//		CompressedGameBoard cgb = new CompressedGameBoard(board); // check cache
//		// in case this board was already evaluated
//		if (cache.containsKey(cgb)) {
//			TranspositionTableEntry entry = cache.get(cgb);
//			if (entry.isLowerBound()) {
//				if (entry.getValue() > alpha) {
//					System.out.println("Setting alpha from cache");
//					alpha = entry.getValue();
//				}
//			} else if (entry.isUpperBound()) {
//				if (entry.getValue() < beta) {
//					System.out.println("Setting beta from cache");
//					beta = entry.getValue();
//				}
//			} else {
//				System.out.println("Retrieving exact value from cache");
//				return entry.getValue();
//			}
//			if (alpha >= beta) {
//				System.out.println("alpha more than beta, returning score from cache");
//				return entry.getValue();
//			}
//		}

		// if we are at the bottom of the tree, return the board score
		if (d == 0) {
			int score = evaluateBoard(board);

			if (board.currentColor != playerColor)
				score = -score;

//			cache.put(cgb, new TranspositionTableEntry(TranspositionTableEntry.PRECISE, score, 0));
			benchMark++;
			return score;
		}

		// if we are not at the bottom of the tree, continue down the tree to
		// return score
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
			}
			if (alpha >= beta) {
				break;
			}
		}
		int cacheFlag = alpha < originalAlpha ? TranspositionTableEntry.UPPER_BOUND
				: (alpha >= beta ? TranspositionTableEntry.LOWER_BOUND : TranspositionTableEntry.PRECISE);
//		cache.put(cgb, new TranspositionTableEntry(cacheFlag, alpha, d));

		return alpha;
	}

	public Move getBestMoveNegamaxNoThreads(GameBoard board, int d) {
		Move best = null;
		int alpha = MIN;
		boolean first = true;
		for (Move child : board.getAllPossibleMoves(playerColor)) {
			int score;
			if (first) {
				first = false;
				board.apply(child);
				score = -negascout(board, -alpha - 1, -alpha, d - 1);
				if (alpha < score) {
					score = -negascout(board, MIN, -score, d - 1);
				}
				board.undo(child);
			} else {
				board.apply(child);
				score = -negascout(board, MIN, -alpha, d - 1);
				board.undo(child);
			}
			if (score > alpha) {
				alpha = score;
				best = child;
			}
		}
		System.out.println("Best score: " + alpha);
		// cache.clear();
		return best;
	}

	public Move getBestMoveMTDF(GameBoard board, int d) {
		int lb = MIN;
		int ub = MAX;
		ScoredMove g = new ScoredMove(null, MIN);
		do {
			int beta = g.score == lb ? g.score + 1 : g.score;
			g = AlphaBetaWithMemory(board, beta - 1, beta, d);
			if (g.score < beta) {
				ub = g.score;
			} else {
				lb = g.score;
			}
		} while (lb < ub);
		return g.move;
//		return getBestMoveMTDFHelper(board, d);
	}

	public Move getBestMoveMTDFHelper(GameBoard board, int d) {
		Move best = null;
		int score = MIN, bestScore = MIN;
		for (Move child : board.getAllPossibleMoves(board.currentColor)) {
			board.apply(child);
			score = AlphaBetaWithMemory(board, MIN, MAX, d - 1).score;
			board.undo(child);
			if (score > bestScore) {
				bestScore = score;
				best = child;
			}
		}
		System.out.println("Best score: " + bestScore);
		return best;
	}

	public Move getBestMove(GameBoard board, int d) {
		/*
		 * Due to unknown reason, negascout search more nodes than negamax(why?)
		 * but MTD-F search less nodes All these three should give same results
		 * - which is the optimal result.
		 */
		System.out.println("My aggressive multiplier is " + aggrMult + " and defensive multiplier is " + defMult);
		System.out.println("New AI Thinking..... d: " + d);
		benchMark = 0;
		long start = System.nanoTime();

		Move ret = getBestMoveMTDF(board, d);
//		Move ret = getBestMoveNegamaxNoThreads(board, d);
		totalNodes += benchMark;
		totalTime += (System.nanoTime() - start);
		double time = (System.nanoTime() - start) / 1.0e9;
		System.out.println(benchMark + " nodes searched in " + time);
		double tpn = benchMark / time;
		System.out.format("Nodes per second: %.3f\n", tpn);
		System.out.println("NEW AI Total Notes: " + totalNodes + " Sec: " + (totalTime / 1e9));
		if (ret == null) {
			System.out.println("No good move found! Picking first possible move.");
			if (board.getAllPossibleMoves(playerColor).size() == 0) {
				System.out.println("~~~~~~~~~I RESIGN~~~~~~~~~~");
				System.out.println(
						"My aggressive multiplier was " + aggrMult + " and defensive multiplier was " + defMult);
				while (true) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return board.getAllPossibleMoves(playerColor).get(0);
		}
		return ret;
	}

	/*
	 * always evaluate from the perspective of the current player
	 */
	public int evaluateBoard(GameBoard board) {
		int pColor = board.currentColor; // pColor is row 6-7
		int eColor = Piece.getOppositeColor(pColor); // eColor is row 0-1

		// System.out.println((pColor == Piece.BLACK ? "Black" : "White"));

		int score = 0, aggressive = 0, defensive = 0;

		short[][] countPieces = new short[Piece.COLORS.length][Piece.NAMES.length];
		short[] castled = new short[Piece.COLORS.length];
		short[] pawnMobility = new short[Piece.COLORS.length];
		short[] pieceMobility = new short[Piece.COLORS.length];
		boolean[][] pawnColumnOccupied = new boolean[Piece.COLORS.length][GameBoard.WIDTH];
		short[] pawnColumnPenalty = new short[Piece.COLORS.length];
		short[] knighNotIsolated = new short[Piece.COLORS.length];
		short[] pawnAdvancedCentered = new short[Piece.COLORS.length];
		short[] squaresControlled = new short[Piece.COLORS.length];
		short[] piecesNotOnFirstRow = new short[Piece.COLORS.length];
		short[] kingHome = new short[Piece.COLORS.length];

		for (short col = 0; col < GameBoard.WIDTH; col++) {
			for (short row = 0; row < GameBoard.HEIGHT; row++) {
				Piece piece = board.getPiece(Position.get(col, row));
				if (piece != null) {
					countPieces[piece.getColor()][piece.getType()]++;

					squaresControlled[piece.getColor()] += MoveHelper.getReachablePosition(board, col, row, true)
							.size();

					if (piece.getType() == Piece.KING) {
						castled[piece.getColor()] = (short) (board.hasCastled(piece.getColor()) ? 1 : 0);

						if (piece.getColor() == pColor) {
							kingHome[piece.getColor()] += (row == 7) ? 1 : 0;
						} else {
							kingHome[piece.getColor()] += (row == 0) ? 1 : 0;
						}
					} else if (piece.getType() == Piece.PAWN) {
						short rowValue;
						if (piece.getColor() == pColor)
							rowValue = (short) (6 - row);
						else
							rowValue = (short) (row - 1);

						short colValue = (short) Math.round(3.5 - Math.abs(3.5 - col));

						// linear mobility bonus per distance out
						pawnMobility[piece.getColor()] += rowValue;
						if (rowValue != 0) {
							// reward advanced pawns near center
							pawnAdvancedCentered[piece.getColor()] += colValue;
						}

						// bonus for getting off start position
						if (rowValue != 0)
							pawnMobility[piece.getColor()] += 1;

						// multiple pawns in the same column
						if (pawnColumnOccupied[piece.getColor()][col])
							pawnColumnPenalty[piece.getColor()]++;
						pawnColumnOccupied[piece.getColor()][col] = true;
					} else {
						short val;
						if (piece.getColor() == pColor)
							val = (short) (7 - row);
						else
							val = (short) (row);

						// linear mobility bonus per distance out
						pieceMobility[piece.getColor()] += val;
						// bonus for getting off start position
						if (val != 0)
							pieceMobility[piece.getColor()] += 2;

						if (piece.getType() == Piece.KNIGHT) {
							if (col != 0 && col != 7)
								knighNotIsolated[piece.getColor()]++;
						}

						if (piece.getColor() == pColor) {
							if (row != 7) {
								piecesNotOnFirstRow[piece.getColor()]++;
							}
						} else {
							if (row != 0) {
								piecesNotOnFirstRow[piece.getColor()]++;
							}
						}
					}
				}
			}
		}

		// KING HOME BROKEN
		// PIECESNOTTONFIRSTROW BROKEN

		// king safety - keep king on starting row, pieces around king
		// spaces protected/attacked by pawns
		// prevent king reward for moving forward
		// why are scores neg? something problematic with the score for the two
		// sides

		score += 1 * (countPieces[pColor][Piece.PAWN] - countPieces[eColor][Piece.PAWN]);
		score += 3 * (countPieces[pColor][Piece.BISHOP] - countPieces[eColor][Piece.BISHOP]);
		score += 3 * (countPieces[pColor][Piece.KNIGHT] - countPieces[eColor][Piece.KNIGHT]);
		score += 5 * (countPieces[pColor][Piece.ROOK] - countPieces[eColor][Piece.ROOK]);
		score += 9 * (countPieces[pColor][Piece.QUEEN] - countPieces[eColor][Piece.QUEEN]);
		score += 100 * ((board.isCheckMate(eColor) ? 1 : 0) - (board.isCheckMate(pColor) ? 1 : 0));
		score *= 64;

		aggressive += 32 * ((board.isCheck(eColor) ? 1 : 0) - (board.isCheck(pColor) ? 1 : 0));
		aggressive += 1 * (pawnMobility[pColor] - pawnMobility[eColor]);
		aggressive += 1 * (pawnAdvancedCentered[pColor] - pawnAdvancedCentered[eColor]);
		aggressive += 1 * (pieceMobility[pColor] - pieceMobility[eColor]);
		aggressive += 16 * aggrMult * (piecesNotOnFirstRow[pColor] - piecesNotOnFirstRow[eColor]);
		aggressive += 16 * (pawnColumnPenalty[pColor] - pawnColumnPenalty[eColor]);
		aggressive += 32 * (knighNotIsolated[pColor] - knighNotIsolated[eColor]);
		aggressive += 1 * (squaresControlled[pColor] - squaresControlled[eColor]);
		// aggressive *= aggrMult;

		defensive += 128 * (kingHome[pColor] - kingHome[eColor]);
		defensive += 64 * (castled[pColor] - castled[eColor]);
		// defensive *= 4 - aggrMult;

		score += aggressive + defensive;

		return score;
	}

	@Override
	public int getColor() {
		return playerColor;
	}
}
