package vad;

import java.io.Serializable;

public class Position implements Serializable
{
	private static final long serialVersionUID = -4029933612261284275L;
	
	private static final Position[][] positions = new Position[8][8];
	private static final Position[] allPositions = new Position[64];

	static
	{
		int i = 0;
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				allPositions[i++] = positions[c][r] = new Position(c, r);
			}
		}
	}

	public static Position get(int col, int row)
	{
		return positions[col][row];
	}

	public static Position[] all()
	{
		return allPositions;
	}

	int col, row;

	private Position(int col, int row)
	{
		this.col = col;
		this.row = row;
	}

	public int getColumn()
	{
		return col;
	}

	public int getRow()
	{
		return row;
	}

	@Override
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

	public Position getLeft()
	{
		if (col == 0)
			return null;
		return get(col - 1, row);
	}

	public Position getUp()
	{
		if (row == 0)
			return null;
		return get(col, row - 1);
	}

	public Position getDown()
	{
		if (row == 7)
			return null;
		return get(col, row + 1);
	}

	public Position getUpLeft()
	{
		if (row == 0 || col == 0)
			return null;
		return get(col - 1, row - 1);
	}

	public Position getUpRight()
	{
		if (row == 0 || col == 7)
			return null;
		return get(col + 1, row - 1);
	}

	public Position getDownLeft()
	{
		if (row == 7 || col == 0)
			return null;
		return get(col - 1, row + 1);
	}

	public Position getDownRight()
	{
		if (row == 7 || col == 7)
			return null;
		return get(col + 1, row + 1);
	}

	public Position getRelative(int dc, int dr)
	{
		int c = col + dc;
		int r = row + dr;
		if (c < 0 || c > 7 || r < 0 || r > 7)
		{
			return null;
		}
		return get(c, r);
	}
	
	private static class PositionReference implements Serializable{
		private static final long serialVersionUID = 1439457913793333450L;
		
		private int col;
		private int row;
		public PositionReference(Position p){
			col=p.col;
			row=p.row;
		}
		
		private Object readResolve(){
			return Position.positions[col][row];
		}
	}

	private Object writeReplace(){
		return new PositionReference(this);
	}
	
}