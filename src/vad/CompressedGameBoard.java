package vad;

public class CompressedGameBoard
{
	long fst2c;
	long snd2c;
	long trd2c;
	long lst2c;
	byte flags;

	public static int CURRENT_PLAYER_FLAG = 0;
	public static int BLACK_FLAG_LOW = 1;
	public static int WHITE_FLAG_LOW = 4;

	public CompressedGameBoard(GameBoard b)
	{
		for (int c = 0; c < 2; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int offset = getOffset(c, r);
				if (!b.isEmpty(Position.get(c, r)))
					fst2c |= getPieceNum(b, c, r) << offset;
				else
					fst2c |= 0b1111l << offset;
			}
		}
		for (int c = 2; c < 4; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int offset = getOffset(c - 2, r);
				if (!b.isEmpty(Position.get(c, r)))
					snd2c |= getPieceNum(b, c, r) << offset;
				else
					snd2c |= 0b1111l << offset;
			}
		}
		for (int c = 4; c < 6; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int offset = getOffset(c - 4, r);
				if (!b.isEmpty(Position.get(c, r)))
					trd2c |= getPieceNum(b, c, r) << offset;
				else
					trd2c |= 0b1111l << offset;
			}
		}
		for (int c = 6; c < 8; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int offset = getOffset(c - 6, r);
				if (!b.isEmpty(Position.get(c, r)))
					lst2c |= getPieceNum(b, c, r) << offset;
				else
					lst2c |= 0b1111l << offset;
			}
		}
		flags |= b.currentColor << CURRENT_PLAYER_FLAG;
		flags |= b.blackFlags << BLACK_FLAG_LOW;
		flags |= b.whiteFlags << WHITE_FLAG_LOW;
	}

	public static int getOffset(int dc, int r)
	{
		return (dc * 8 + r) * 4;
	}

	public static long getPieceNum(GameBoard b, int c, int r)
	{
		Piece p = b.getPiece(Position.get(c, r));
		return (p.getColor() << 3) | p.getType();
	}

	public GameBoard getGameBoard()
	{
		GameBoard board = new GameBoard(false);
		for (int c = 0; c < 2; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int extract = (int) ((fst2c >>> getOffset(c, r)) & 0b1111);
				if (extract == 0b1111)
					continue;
				board.board[c][r] = Piece.allPieces[extract >> 3][extract & 0b111];
			}
		}
		for (int c = 2; c < 4; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int extract = (int) ((snd2c >>> getOffset(c - 2, r)) & 0b1111);
				if (extract == 0b1111)
					continue;
				board.board[c][r] = Piece.allPieces[extract >> 3][extract & 0b111];
			}
		}
		for (int c = 4; c < 6; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int extract = (int) ((trd2c >>> getOffset(c - 4, r)) & 0b1111);
				if (extract == 0b1111)
					continue;
				board.board[c][r] = Piece.allPieces[extract >> 3][extract & 0b111];
			}
		}
		for (int c = 6; c < 8; c++)
		{
			for (int r = 0; r < 8; r++)
			{
				int extract = (int) ((lst2c >>> getOffset(c - 6, r)) & 0b1111);
				if (extract == 0b1111)
					continue;
				board.board[c][r] = Piece.allPieces[extract >> 3][extract & 0b111];
			}
		}
		board.currentColor = (flags >> CURRENT_PLAYER_FLAG) & 0b1;
		board.blackFlags = (byte) ((flags >> BLACK_FLAG_LOW) & 0b111);
		board.whiteFlags = (byte) ((flags >> BLACK_FLAG_LOW) & 0b111);
		return board;
	}

	public int hashCode()
	{
		return (int) (fst2c + snd2c + trd2c + lst2c + flags);
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof CompressedGameBoard))
			return false;
		CompressedGameBoard b = (CompressedGameBoard) o;
		return b.fst2c == fst2c && b.snd2c == snd2c && b.trd2c == trd2c && b.lst2c == lst2c && b.flags == flags;
	}

}
