package vad;

import java.util.ArrayList;

import vad.ChessGUI.ClickListener;

public class UserPlayer implements Player, ClickListener
{
	ChessGUI	gui;
	GameBoard	board;
	Move		move;
	int			playerColor;

	public UserPlayer(int playerColor)
	{
		this.playerColor = playerColor;
		gui = new ChessGUI(this, playerColor);
	}
	
	@Override
	public void update(CompressedGameBoard board)
	{
		update(board.getGameBoard());
	}

	public void update(GameBoard board)
	{
		gui.updateBoard(board);
	}

	public Move makeMove(CompressedGameBoard cgb)
	{
		GameBoard b=cgb.getGameBoard();
		update(b);
		board = b;
		synchronized (this)
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		board = null;
		return move;
	}

	Position		selectedPosition;
	ArrayList<Move>	possibleMoves;

	@Override
	public void onClick(Position position)
	{
		if (board == null)
		{
			return;
		}

		if (!board.isEmpty(position) && board.getPiece(position).getColor() == playerColor)
		{
			if (selectedPosition == position)
			{
				selectedPosition = null;
				gui.clearReachablity();
				return;
			}
			possibleMoves = MoveHelper.getAllMoves4Piece(board, position, false);
			if (!possibleMoves.isEmpty())
			{
				gui.clearReachablity();
				selectedPosition = position;
				gui.setReachablity(possibleMoves);
			}
		}
		else if (selectedPosition == null)
		{
			return;
		}
		else
		{
			for (Move m : possibleMoves)
			{
				if (m.getDestPosition() == position)
				{
					gui.clearReachablity();
					move = m;
					synchronized (this)
					{
						notifyAll();
					}
					return;
				}
			}

		}

	}
	
	@Override
	public int getColor() {
		return playerColor;
	}

}
