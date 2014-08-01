package vad;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ChessGUI extends JFrame implements ActionListener
{
	public static interface ClickListener
	{
		void onClick(Position position);
	}

	PieceButton[][]						buttons				= new PieceButton[8][8];

	public static final ImageIcon[][]	CHESS_PIECE_IMAGES	= new ImageIcon[2][6];
	static
	{
		try
		{
			URL url = new URL("http://i.stack.imgur.com/memI0.png");
			BufferedImage bi = ImageIO.read(url);
			for (int color = 0; color < 2; color++)
				for (int type = 0; type < 6; type++)
					CHESS_PIECE_IMAGES[color][type] = new ImageIcon(bi.getSubimage(type * 64, color * 64, 64, 64));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	ClickListener						onClick;

	public ChessGUI(ClickListener onClick)
	{
		JPanel gameBoard = new JPanel();
		gameBoard.setLayout(new GridLayout(8, 8));
		for (int row = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++)
			{
				final PieceButton b = new PieceButton(Position.get(col, row));
				buttons[col][row] = b;
				b.addActionListener(this);
				gameBoard.add(b);
			}
		}

		add(gameBoard);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);

		this.onClick = onClick;
	}

	public void updateBoard(GameBoard board)
	{
		for (int row = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++)
			{
				if (buttons[col][row].getPiece() != board.getPiece(col, row))
				{
					buttons[col][row].setPiece(board.getPiece(col, row));
				}
			}
		}
		repaint();
	}

	public void clearReachablity()
	{
		for (int r = 0; r < 8; r++)
		{
			for (int c = 0; c < 8; c++)
			{
				if (buttons[c][r].isReachable())
				{
					buttons[c][r].markReachable(false);
				}
			}
		}
	}

	public void setReachablity(ArrayList<Move> pm)
	{
		for (Move m : pm)
		{
			buttons[m.getDestPosition().getColumn()][m.getDestPosition().getRow()].markReachable(true);
		}
	}

	PieceButton	selectedbutton;

	@Override
	public void actionPerformed(ActionEvent e)
	{
		PieceButton button = (PieceButton) e.getSource();
		if (onClick != null)
		{
			onClick.onClick(button.getPosition());
		}
	}
}
