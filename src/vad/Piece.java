package vad;


public class Piece
{
	public static Piece[][] allPieces=new Piece[2][6];
	public static final int	QUEEN = 1, KING = 0, ROOK = 2, KNIGHT = 3, BISHOP = 4, PAWN = 5;
	public static final int	BLACK=0, WHITE=1;
	public static final String[] NAMES={"King", "Queen", "Rook", "Knight", "Bishop", "Pawn"};
	
	public static int getOppositeColor(int color){
		return 1-color;
	}
	
	static{
		for(int color=0;color<2;color++){
			for(int type=0;type<6;type++){
				allPieces[color][type]=new Piece(color, type);
			}
		}
	}
	
	private int type;
	private int	color;
	
	private Piece(int color, int type){
		this.color=color;
		this.type=type;
	}
	
	public int getType(){
		return type;
	}
	
	public int getColor(){
		return color;
	}
	
	public String toString(){
		return (color==BLACK?"Black ":"White ")+NAMES[type];
	}
	
	
	

}
