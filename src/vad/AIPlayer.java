package vad;

import java.util.HashMap;
import java.util.Random;

public class AIPlayer implements Player {
	public static final int CACHE_INITIAL_SIZE = 2000003;
	public static final float CACHE_LOAD_FACTOR = 0.9f;
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

	
	public int negascout(GameBoard board, int alpha, int beta, int d) {
		CompressedGameBoard cgb = new CompressedGameBoard(board);
		// check cache in case this board was already evaluated
		if (cache.containsKey(cgb)) {
			TranspositionTableEntry entry = cache.get(cgb);
			if (entry.isLowerBound()) {
				if (entry.getValue() > alpha) {
					System.out.println("Setting alpha from cache");
					alpha = entry.getValue();
				}
			} else if (entry.isUpperBound()) {
				if (entry.getValue() < beta) {
					System.out.println("Setting beta from cache");
					beta = entry.getValue();
				}
			} else {
				System.out.println("Retrieving exact value from cache");
				return entry.getValue();
			}
			if (alpha >= beta) {
				System.out.println("alpha more than beta, returning score from cache");
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
		cache.put(cgb, new TranspositionTableEntry(cacheFlag, alpha, d));

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
//		cache.clear();
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
		return getBestMoveNegamaxNoThreads(board, d);
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
		// Move ret = getBestMoveNegamaxNoThreads(board, d);
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
	 * always evaluate from our perspective
	 */
	public int evaluateBoard(GameBoard board) {
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

						if (piece.getColor() == playerColor) {
							kingHome[piece.getColor()] += (row == 7) ? 1 : 0;
						} else {
							kingHome[piece.getColor()] += (row == 0) ? 1 : 0;
						}
					} else if (piece.getType() == Piece.PAWN) {
						short rowValue;
						if (piece.getColor() == playerColor)
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
						if (piece.getColor() == playerColor)
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

						if (piece.getColor() == playerColor) {
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

		score += 1 * (countPieces[playerColor][Piece.PAWN] - countPieces[enemyColor][Piece.PAWN]);
		score += 3 * (countPieces[playerColor][Piece.BISHOP] - countPieces[enemyColor][Piece.BISHOP]);
		score += 3 * (countPieces[playerColor][Piece.KNIGHT] - countPieces[enemyColor][Piece.KNIGHT]);
		score += 5 * (countPieces[playerColor][Piece.ROOK] - countPieces[enemyColor][Piece.ROOK]);
		score += 9 * (countPieces[playerColor][Piece.QUEEN] - countPieces[enemyColor][Piece.QUEEN]);
		score += 100 * ((board.isCheckMate(enemyColor) ? 1 : 0) - (board.isCheckMate(playerColor) ? 1 : 0));
		score *= 64;

		aggressive += 32 * ((board.isCheck(enemyColor) ? 1 : 0) - (board.isCheck(playerColor) ? 1 : 0));
		aggressive += 1 * (pawnMobility[playerColor] - pawnMobility[enemyColor]);
		aggressive += 1 * (pawnAdvancedCentered[playerColor] - pawnAdvancedCentered[enemyColor]);
		aggressive += 1 * (pieceMobility[playerColor] - pieceMobility[enemyColor]);
		aggressive += 16 * aggrMult * (piecesNotOnFirstRow[playerColor] - piecesNotOnFirstRow[enemyColor]);
		aggressive += 16 * (pawnColumnPenalty[playerColor] - pawnColumnPenalty[enemyColor]);
		aggressive += 32 * (knighNotIsolated[playerColor] - knighNotIsolated[enemyColor]);
		aggressive += 1 * (squaresControlled[playerColor] - squaresControlled[enemyColor]);
		// aggressive *= aggrMult;

		defensive += 128 * (kingHome[playerColor] - kingHome[enemyColor]);
		defensive += 64 * (castled[playerColor] - castled[enemyColor]);
		// defensive *= 4 - aggrMult;

		score += aggressive + defensive;

		return score;
	}

	@Override
	public int getColor() {
		return playerColor;
	}
}
