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

	private int playerColor;
	int depth = 6;
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
	/* values from 1-4 */
	private int aggrMult, defMult;

	public AIPlayer(int playerColor) {
		this.playerColor = playerColor;
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

		/*if (board.getNumAllPieces() < 20) {
			// increase search depth if our search space gets smaller
			if (timeSec < 1. && depth < 10) {
				depth += 2;
			} else if (timeSec > 30.) {
				depth -= 2;
			}
		}*/
		if (board.getNumAllPieces() < 25) {
			depth = 12;
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
		if (cache.containsKey(cb)) {
			TranspositionTableEntry entry = cache.get(cb);
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
			cache.put(cb, new TranspositionTableEntry(MIN, score, best));
		}
		if (score > alpha && score < beta) {
			cache.put(cb, new TranspositionTableEntry(score, score, best));
		}
		if (score >= beta) {
			cache.put(cb, new TranspositionTableEntry(score, MAX, best));
		}
		return new ScoredMove(best, score);
	}

	public ScoredMove getBestMoveMTDF(GameBoard board, int startScore, int d) {
		int lb = MIN;
		int ub = MAX;
		ScoredMove g = new ScoredMove(null, startScore);
		do {
			int beta = g.score == lb ? g.score + 1 : g.score;
			g = AlphaBetaWithMemory(board, beta - 1, beta, d);
			if (g.score < beta) {
				ub = g.score;
			} else {
				lb = g.score;
			}
		} while (lb < ub);
		return g;
	}
	
	public ScoredMove getBestMoveIterativeMTDF(GameBoard board, int max_depth) {
		long MAX_MOVE_TIME = (long) 5e9;
		long startTime = System.nanoTime();
		ScoredMove firstGuess = new ScoredMove(null, 0);
		int d = 1;
		for (d = 1; d <= max_depth; d++) {
			if (System.nanoTime() - startTime > MAX_MOVE_TIME) {
				break;
			}
			firstGuess = getBestMoveMTDF(board, firstGuess.score, d);
		}
		System.out.println("Searched to depth " + (d - 1));
		return firstGuess;
	}

	public Move getBestMove(GameBoard board, int d) {
		/*
		 * Due to unknown reason, negascout search more nodes than negamax(why?)
		 * but MTD-F search less nodes All these three should give same results
		 * - which is the optimal result.
		 */
		System.out.println("My aggressive multiplier is " + aggrMult + " and defensive multiplier is " + defMult);
		System.out.println("AI Thinking.....");
		benchMark = 0;
		long start = System.nanoTime();

		ScoredMove best = getBestMoveIterativeMTDF(board, d);
		System.out.println("Best move score: " + best.score);
		totalNodes += benchMark;
		totalTime += (System.nanoTime() - start);
		double time = (System.nanoTime() - start) / 1.0e9;
		System.out.println(benchMark + " nodes searched in " + time);
		double tpn = benchMark / time;
		System.out.format("Nodes per second: %.3f\n", tpn);
		System.out.println("NEW AI Total Notes: " + totalNodes + " Sec: " + (totalTime / 1e9));
		if (best.move == null) {
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
		return best.move;
	}

	/*
	 * always evaluate from the perspective of the current player
	 */
	public int evaluateBoard(GameBoard board) {
		int pColor = playerColor;//board.currentColor; // pColor is row 6-7
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
		aggressive += 32 * (piecesNotOnFirstRow[pColor] - piecesNotOnFirstRow[eColor]);
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
