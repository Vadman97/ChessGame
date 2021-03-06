package vad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

public class AIPlayer implements Player {
	public static final int CACHE_INITIAL_SIZE = 2000003;
	public static final float CACHE_LOAD_FACTOR = 0.8f;
	public static final int CACHE_NUM_SHARDS = 4;

	public static final int MAX = Integer.MAX_VALUE;
	public static final int MIN = -MAX;
	public static final int SEARCH_PRINT_DELAY = 2000; // ms

	public static final int REPEATED_MOVE_PENALTY = 10000;

	private static final boolean UI_ENABLED = true;

	public long SEARCH_LIMIT_NS = (long) (7 * 1e9); // nanoseconds
	public static final int MOVE_MAX_REPETITIONS = 3;

	long searchStart;
	private int playerColor;
	int depth = 100;
	Map<CompressedGameBoard, TranspositionTableEntry> cache = new HashMap<>(CACHE_INITIAL_SIZE, CACHE_LOAD_FACTOR);
	Map<Move, Integer> visitedMoves = new HashMap<>(CACHE_INITIAL_SIZE, CACHE_LOAD_FACTOR);

	ChessGUI gui;

	int benchMark;

	Random r = new Random();

	public volatile boolean thinking = false;

	public long totalTime = 0;
	public long totalNodes = 0;
	public int increased = 0;

	public int myRow = -1, enemyRow = -1;

	Queue<Move> lastMoves = new LinkedList<>();

	public AIPlayer(int playerColor, double thinkTimeSec) {
		this.playerColor = playerColor;
		this.SEARCH_LIMIT_NS = (long) (thinkTimeSec * 1e9);

		if (UI_ENABLED)
			gui = new ChessGUI(null, playerColor);
	}

	@Override
	public Move makeMove(GameBoard board) {
		
		thinking = true;
		if (myRow == -1 && board.getPiece(Position.get(0, 0)) != null) {
			if (board.getPiece(Position.get(0, 0)).getColor() == playerColor) {
				myRow = 0;
				enemyRow = 7;
			} else {
				myRow = 7;
				enemyRow = 0;
			}
		}

		System.out.println(board);
		System.out.println("Current enemy board value: " + evaluateBoard(board, null));
		
		if (board.getNumAllPieces() <= 24 && increased == 0) {
			SEARCH_LIMIT_NS *= 2;
			increased++;
		} else if (board.getNumAllPieces() <= 12 && increased == 1) {
			SEARCH_LIMIT_NS *= 2;
			increased++;
		} else if (board.getNumAllPieces() <= 8 && increased == 2) {
			SEARCH_LIMIT_NS *= 2;
			increased++;
		}

		Move move = getBestMove(board, depth);
		thinking = false;
		return move;
	}

	public void update(GameBoard board) {
		if (UI_ENABLED)
			gui.updateBoard(board);
	}

	public ScoredMove AlphaBetaWithMemory(GameBoard board, int alpha, int beta, int d, Move m) {
		if (System.nanoTime() - searchStart > SEARCH_LIMIT_NS)
			return null;
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
			score = evaluateBoard(board, m);
			benchMark++;
		} else if (board.currentColor == playerColor) {
			// This is a max node
			score = MIN;
			int a = alpha;
			for (Move child : board.getAllPossibleMoves(board.currentColor)) {
				if (score >= beta)
					break;

				if (visitedMoves.containsKey(child) && visitedMoves.get(child) >= MOVE_MAX_REPETITIONS) {
					System.out.println("!!!SKIPPING REPEATED!!!");
					continue;
				} else if (visitedMoves.containsKey(child)) {
					visitedMoves.put(child, visitedMoves.get(child) + 1);
				} else {
					visitedMoves.put(child, 1);
				}

				board.apply(child);
				ScoredMove val = AlphaBetaWithMemory(board, a, beta, d - 1, child);
				board.undo(child);
				
				if (visitedMoves.get(child) > 1) {
					visitedMoves.put(child, visitedMoves.get(child) - 1);
				} else {
					visitedMoves.remove(child);
				}
				
				if (val == null)
					return null;
				if (val.score > score) {
					score = val.score;
					best = child;
				}
				a = Math.max(a, score);
			}
		} else {
			// This is a min node
			score = MAX;
			int b = beta;
			for (Move child : board.getAllPossibleMoves(board.currentColor)) {
				if (score <= alpha)
					break;

				if (visitedMoves.containsKey(child) && visitedMoves.get(child) >= MOVE_MAX_REPETITIONS) {
					System.out.println("!!!SKIPPING REPEATED!!!");
					continue;
				} else if (visitedMoves.containsKey(child)) {
					visitedMoves.put(child, visitedMoves.get(child) + 1);
				} else {
					visitedMoves.put(child, 1);
				}
				
				board.apply(child);
				ScoredMove val = AlphaBetaWithMemory(board, alpha, b, d - 1, child);
				board.undo(child);
				
				if (visitedMoves.get(child) > 1) {
					visitedMoves.put(child, visitedMoves.get(child) - 1);
				} else {
					visitedMoves.remove(child);
				}
				
				if (val == null)
					return null;
				if (val.score < score) {
					score = val.score;
					best = child;
				}
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
			if (System.nanoTime() - searchStart > SEARCH_LIMIT_NS)
				break;
			int beta = g.score == lb ? g.score + 1 : g.score;
			g = AlphaBetaWithMemory(board, beta - 1, beta, d, null);
			if (g == null)
				break;
			if (g.score < beta) {
				ub = g.score;
			} else {
				lb = g.score;
			}
		} while (lb < ub);
		return g;
	}

	public ScoredMove getBestMoveIterativeMTDF(GameBoard board, int max_depth) {
		searchStart = System.nanoTime();
		ScoredMove firstGuess = new ScoredMove(null, 0);
		int d = 1;
		for (d = 1; d <= max_depth; d++) {
			if (System.nanoTime() - searchStart > SEARCH_LIMIT_NS) {
				break;
			}
			ScoredMove temp = getBestMoveMTDF(board, firstGuess.score, d);
			if (temp == null) {
				System.out.println("Ran out of time! Aborting");
				d--;
				break;
			} else
				firstGuess = temp;
			// System.out.println("Searched to depth " + d + " and found move
			// score " + firstGuess.score);

			// clear cache if we only have 256 MB left
			// this should be ok because most of the stuff in the cache will be old nodes
			// that we won't look at again, i assume this program is run with ~8GB ram
			if (Runtime.getRuntime().freeMemory() < 256 * 1000000) {
				System.out.println("Clearing cache!");
				cache.clear();
				System.gc();
			}
		}
		System.out.println("Finished search to depth " + (d - 1) + " with score " + firstGuess.score);
		return firstGuess;
	}

	public Move getBestMove(GameBoard board, int d) {
		System.out.println("AI Thinking..........");
		benchMark = 0;
		long start = System.nanoTime();

		ScoredMove best = getBestMoveIterativeMTDF(board, d);

		// keep last 3 moves
		if (lastMoves.size() > 3)
			lastMoves.remove();
		lastMoves.add(best.move);

		totalNodes += benchMark;
		totalTime += (System.nanoTime() - start);
		double time = (System.nanoTime() - start) / 1.0e9;
		double tpn = benchMark / time;
		System.out.format(benchMark + " nodes searched in " + time + ". Nodes per second: %.3f\n", tpn);
		System.out.format("AI Total Nodes: %d Nodes cached: %d Sec: %.3f pieces: %d\n", 
						  totalNodes, cache.size(), (totalTime / 1e9), board.getNumAllPieces());
		if (best.move == null) {
			System.out.println("No good move found! Picking random move.");
			if (board.getAllPossibleMoves(playerColor).size() == 0) {
				return null;
			}
			ArrayList<Move> moves = board.getAllPossibleMoves(playerColor);
			return moves.get(r.nextInt(moves.size()));
		}
		return best.move;
	}

	public int countKingSurrounding(GameBoard board, Position kingPos) {
		Piece king = board.getPiece(kingPos);
		int count = 0;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				int c = kingPos.col + i, r = kingPos.row + j;
				if (c < 0 || c > 7 || r < 0 || r > 7)
					continue;
				Position pos = Position.get(c, r);
				Piece p = board.getPiece(pos);

				// count our pieces surrounding the king
				if (p != null && p.getColor() == king.getColor())
					count++;
			}
		}
		return count;
	}

	/*
	 * always evaluate from the perspective of the current player
	 */
	public int evaluateBoard(GameBoard board, Move lastMove) {
		int pColor = playerColor; // board.currentColor; // pColor is row 6-7
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
		short[] piecesSurroundingKing = new short[Piece.COLORS.length];
		short[] rookOpenCol = new short[Piece.COLORS.length];
		boolean[] check = new boolean[Piece.COLORS.length];
		boolean[] checkMate = new boolean[Piece.COLORS.length];

		for (short col = 0; col < GameBoard.WIDTH; col++) {
			for (short row = 0; row < GameBoard.HEIGHT; row++) {
				Piece piece = board.getPiece(Position.get(col, row));
				if (piece != null) {
					int playerStartRow = (piece.getColor() == pColor) ? myRow : enemyRow;
					countPieces[piece.getColor()][piece.getType()]++;

					squaresControlled[piece.getColor()] += MoveHelper.getReachablePosition(board, col, row, true)
							.size();

					if (piece.getType() == Piece.KING) {
						castled[piece.getColor()] = (short) (board.hasCastled(piece.getColor()) ? 1 : 0);
						kingHome[piece.getColor()] += (row == playerStartRow) ? 1 : 0;
						piecesSurroundingKing[piece.getColor()] += countKingSurrounding(board, Position.get(col, row));
					} else if (piece.getType() == Piece.PAWN) {
						short rowValue = (short) (Math.abs(playerStartRow - row) - 1);
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
						short rowValue = (short) Math.abs(playerStartRow - row);

						// linear mobility bonus per distance out
						pieceMobility[piece.getColor()] += rowValue;

						if (piece.getType() == Piece.KNIGHT) {
							if (col != 0 && col != 7)
								knighNotIsolated[piece.getColor()]++;
						} else if (piece.getType() == Piece.ROOK) {
							if (playerStartRow == 0) {
								boolean foundPiece = false;
								for (int c = col; c <= 7; c++) {
									if (board.getPiece(Position.get(c, row)) != null) {
										if (board.getPiece(Position.get(c, row)).getColor() == piece.getColor()) {
											foundPiece = true;
											break;
										}
									}
								}
								rookOpenCol[piece.getColor()] += foundPiece ? 0 : 1;
							} else if (playerStartRow == 7) {
								boolean foundPiece = false;
								for (int c = col; c >= 0; c--) {
									if (board.getPiece(Position.get(c, row)) != null) {
										if (board.getPiece(Position.get(c, row)).getColor() == piece.getColor()) {
											foundPiece = true;
											break;
										}
									}
								}
								rookOpenCol[piece.getColor()] += foundPiece ? 0 : 1;
							}
						}

						if (row != playerStartRow && piece.getType() != Piece.ROOK)
							piecesNotOnFirstRow[piece.getColor()]++;
					}
				}
			}
		}
		
		for (int i: Piece.COLORS) {
			check[i] = board.isCheck(i);
			checkMate[i] = squaresControlled[i] == 0 && check[i];
		}
		
		// spaces protected/attacked by pawns
		// play with pawn heuristic weights

		score += 1 * (countPieces[pColor][Piece.PAWN] - countPieces[eColor][Piece.PAWN]);
		score += 3 * (countPieces[pColor][Piece.BISHOP] - countPieces[eColor][Piece.BISHOP]);
		score += 3 * (countPieces[pColor][Piece.KNIGHT] - countPieces[eColor][Piece.KNIGHT]);
		score += 5 * (countPieces[pColor][Piece.ROOK] - countPieces[eColor][Piece.ROOK]);
		score += 9 * (countPieces[pColor][Piece.QUEEN] - countPieces[eColor][Piece.QUEEN]);
		score += 100 * (countPieces[pColor][Piece.KING] - countPieces[eColor][Piece.KING]);
		score += 100 * ((checkMate[eColor] ? 1 : 0) - (checkMate[pColor] ? 1 : 0));
		score *= 64;

		aggressive += 16 * ((check[eColor] ? 1 : 0) - (check[pColor] ? 1 : 0));
		aggressive += 2 * (pawnMobility[pColor] - pawnMobility[eColor]);
		aggressive += 1 * (pawnAdvancedCentered[pColor] - pawnAdvancedCentered[eColor]);
		aggressive += 1 * (pieceMobility[pColor] - pieceMobility[eColor]);
		aggressive += 32 * (piecesNotOnFirstRow[pColor] - piecesNotOnFirstRow[eColor]);
		aggressive += 16 * (pawnColumnPenalty[pColor] - pawnColumnPenalty[eColor]);
		aggressive += 32 * (knighNotIsolated[pColor] - knighNotIsolated[eColor]);
		aggressive += 2 * (squaresControlled[pColor] - squaresControlled[eColor]);
		aggressive += 32 * (rookOpenCol[pColor] - rookOpenCol[eColor]);
		// aggressive *= aggrMult;

		defensive += 64 * (kingHome[pColor] - kingHome[eColor]);
		defensive += 8 * (piecesSurroundingKing[pColor] - piecesSurroundingKing[eColor]);
		defensive += 64 * (castled[pColor] - castled[eColor]);
		// defensive *= 4 - aggrMult;

		for (Move move : lastMoves) {
			if (lastMove != null && lastMove.equals(move)) {
				defensive -= 64;
			}
		}

		score += aggressive + defensive;

		return score;
	}

	@Override
	public int getColor() {
		return playerColor;
	}
}
