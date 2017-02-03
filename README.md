# Java Chess Game and AI (ELO ~1500 @ 15s)

## The GUI
The graphical interface is made in Java Swing and features simple and easy to use functionality for interacting with the AI agent.

## The AI Engine
### Overview
The computer opponent uses a memory-optimized iterative MTD-f algorithm with Alpha-Beta pruning. The algorithm uses a form of MiniMax with a heuristic function to evaluate the quality of the board at every position. Alpha-Beta pruning is used to optimize the search of the tree and prune nodes that are known to lead to worse positions than ones already analyzed. The algorithm also uses a HashMap to index positions that have been analyzed to improve performance allowing a tremendous speedup as most positions do not have be re-analyzed and lower/upper bounds can be stored for analyzed nodes.
### Performance
The iterative algorithm allows time constraints on the moves and starts analyzing the game tree from depth 1 as deep as allowed by the time contraint. The algorithm aborts if it runs out of time and returns the best move according to the previous depth search. When allowed 15s per move, the AI will search to depth 6 in the early game to about depth 14 by the end of the game on a single core of a modern Intel i7 CPU. Allowing more RAM to the program will also improve performance as more nodes can be cached by the memory optimizations of the search algorithm. 

## Future work
While the search algorithm works well, the heuristic function needs to be improved to lead to better board configurations. 
Quiscence-search can be added to reduce the horizon problem where the search algorithm will "push-back" an impending attack outside of the search depth. 
The killer-move heuristic should be added to improve move ordering and lead to better alpha-beta pruning.
Multithreaded search should be implemented though this would pose significant challenges with MTD-f.
