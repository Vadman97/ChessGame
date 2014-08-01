package vad;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class PieceButton extends JButton
{
	Piece	piece;
	Position pos;
	Color	blank;

	
	boolean	dirty	= false;

	public PieceButton(Position pos)
	{
		this.pos=pos;
		this.blank = (pos.getColumn() + pos.getRow()) % 2 == 1 ? Color.BLACK : Color.WHITE;
		setBackground(blank);
	}

	public void setPiece(Piece p)
	{
		piece = p;
		dirty = true;
	}

	public Piece getPiece()
	{
		return piece;
	}
	
	public void markReachable(boolean b){
		if(b==true){
			setBackground(Color.GREEN);
		}else{
			setBackground(blank);
		}
	}

	public Dimension getPreferredSize()
	{
		return new Dimension(100, 100);
	}

	public void paintComponent(Graphics g)
	{
		if (dirty)
		{
			dirty = false;
			if (piece == null)
			{
				this.setIcon(null);
			}
			else
			{
				this.setIcon(ChessGUI.CHESS_PIECE_IMAGES[piece.getColor()][piece.getType()]);
			}
		}
		super.paintComponent(g);
	}

	public boolean isReachable()
	{
		return getBackground()==Color.GREEN;
	}

	public Position getPosition()
	{
		return pos;
	}

}
