package vad;

public class Position
{
	private static final Position[][]	positions	= new Position[8][8];
	static
	{
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				positions[c][r] = new Position(c, r);
			}
		}
	}

	int									col, row;

	private Position(int col, int row)
	{
		this.col = col;
		this.row = row;
	}

	public static Position get(int col, int row)
	{
		return positions[col][row];
	}

	public int getColumn()
	{
		return col;
	}

	public int getRow()
	{
		return row;
	}

	public String toString()
	{
		return "[" + col + ", " + row + "]";
	}

	public Position getRight()
	{
		if (col == 7)
			return null;
		return get(col + 1, row);
	}

	public Position getLeft() {
		if (col == 0)
			return null;
		return get(col - 1, row);
	}
}