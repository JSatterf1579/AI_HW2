package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class MinimaxAlphaBeta extends Agent {

    public enum MinimaxState {
        MIN, MAX
    }

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                MinimaxState.MAX);



        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta, MinimaxState maxOrMin)
    {
        List<GameStateChild> children = node.state.getChildren();
        if (children.size() == 0 || depth == 0) {
            return node;
        }

        //Orders the children in heuristic order for pruning
        children = orderChildrenWithHeuristics(children);


        //Pruning when on a max branch
        if (maxOrMin == MinimaxState.MAX) {
            //Keep a reference for what you're actually returning and your comparison
            GameStateChild v = null;
            GameStateChild realV = null;
            //Iterates through all children
            for(int i = 0; i < children.size(); i++) {
                //Backing up values
                GameStateChild vPrime = alphaBetaSearch(children.get(i), depth - 1, alpha, beta, MinimaxState.MIN);
                //Backs up the value if it's bigger
                if(v == null || vPrime.state.getUtility() > v.state.getUtility()) {
                    v = vPrime;
                    realV = children.get(i);
                }
                //Moves alpha if it's necessary
                if (v.state.getUtility() > alpha)
                {
                    alpha = v.state.getUtility();
                }
                //Breaks when there is no possible range of numbers
                if (beta <= alpha)
                {
                    break;
                }
            }
            //Returns the actual current object
            return realV;
        }

        //For the enemy case
        if (maxOrMin == MinimaxState.MIN) {
            GameStateChild v = null;
            GameStateChild realV = null;
            for(int i = 0; i < children.size(); i++) {
                GameStateChild vPrime = alphaBetaSearch(children.get(i), depth - 1, alpha, beta, MinimaxState.MAX);
                //Backs up the smaller value
                if(v == null || vPrime.state.getUtility() < v.state.getUtility()) {
                    v = vPrime;
                    realV = children.get(i);
                }
                //Moves beta up if necessary
                if (v.state.getUtility() < beta)
                {
                    beta = v.state.getUtility();
                }
                //Breaks if overlap
                if (beta <= alpha)
                {
                    break;
                }
            }
            return realV;
        }

        return node;
    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
        Collections.sort(children, new StateComparator());
        return children;
    }
}

//Comparator for sorting GameStateChild
class StateComparator implements Comparator<GameStateChild> {

    @Override
    public int compare(GameStateChild o1, GameStateChild o2) {
        return (int)( o1.state.getUtility() - o2.state.getUtility());
    }
}
