package vad;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ChessGUI extends JFrame implements ActionListener
{
	public static interface ClickListener
	{
		void onClick(Position position);
	}

	PieceButton[][] buttons = new PieceButton[8][8];

	public static final ImageIcon[][] CHESS_PIECE_IMAGES = new ImageIcon[2][6];
	static
	{
		try
		{
			BufferedImage bi = ImageIO.read(new File("pieces.png"));
			for (int color = 0; color < 2; color++)
				for (int type = 0; type < 6; type++)
					CHESS_PIECE_IMAGES[color][type] = new ImageIcon(bi.getSubimage(type * 64, color * 64, 64, 64));
		} catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	ClickListener onClick;
	JPanel gameBoard;
	private boolean firstUpdate = true;

	public ChessGUI(ClickListener onClick, int color)
	{
		gameBoard = new JPanel();
		gameBoard.setLayout(new GridLayout(8, 8));
		if (color == Piece.WHITE)
		{
			for (Position position : Position.all())
			{
				PieceButton b = new PieceButton(position);
				buttons[position.getColumn()][position.getRow()] = b;
				b.addActionListener(this);
				gameBoard.add(b);
			}
		} else
		{
			Position[] all = Position.all();
			for (int i = all.length - 1; i >= 0; i--)
			{
				Position position = all[i];
				PieceButton b = new PieceButton(position);
				buttons[position.getColumn()][position.getRow()] = b;
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
	
	public void showThinkingModal(long startTime) {
		JOptionPane.showConfirmDialog(null,
                "Thinking",
                "Thinking", 
                JOptionPane.NO_OPTION,
                JOptionPane.QUESTION_MESSAGE); 
	}
	
	public void hideThinkingModal() {
		
	}

	public void updateBoard(GameBoard board)
	{
		for (Position position : Position.all())
		{
			PieceButton button = buttons[position.getColumn()][position.getRow()];
			Piece piece = board.getPiece(position);
			if (!button.getPiece().equals(piece)) {
				button.setPiece(piece, !firstUpdate);
			} else if (piece != null && piece.getType() == Piece.KING && board.isCheck(piece.getColor())) {
				button.setPieceCheck(piece);
			} else {
				button.setPiece(piece);
			}
		}
		if (firstUpdate) {
			firstUpdate = false;
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

	PieceButton selectedbutton;

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
