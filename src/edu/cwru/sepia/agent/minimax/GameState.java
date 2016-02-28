package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    Double utility;
    List<Unit.UnitView> footmen;
    List<Unit.UnitView> archers;
    List<ResourceNode.ResourceView> resources;
    int xExtent;
    int yExtent;
    State.StateView oldState;

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        footmen =  state.getUnits(0);
        archers =  state.getUnits(1);
        resources = state.getAllResourceNodes();
        xExtent = state.getXExtent();
        yExtent = state.getYExtent();
        oldState = state;
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
        return 0.0;
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren(MinimaxAlphaBeta.MinimaxState turn) {
        ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
        if (turn == MinimaxAlphaBeta.MinimaxState.MAX) {
            generateFootmenChildren(children);
        } else {
            generateArcherChildren(children);
        }
        return children;
    }

    private boolean isInMap(int x, int y) {
        if (x >= 0 && x < xExtent){
            if (y >= 0 && y < yExtent) {
                return true;
            }
        }
        return false;
    }

    private boolean notOnResourceNode(int x, int y) {
        for (ResourceNode.ResourceView resource: resources) {
            if (x == resource.getXPosition() && y == resource.getYPosition()) {
                return false;
            }
        }
        return true;
    }

    private void generateFootmenChildren(List<GameStateChild> children){
        Unit.UnitView footman1 = footmen.get(0);
        Unit.UnitView footman2 = footmen.get(1);
        for (Direction direction1: Direction.values()) {
            for (Direction direction2: Direction.values()) {
                int x1 = footman1.getXPosition() + direction1.xComponent();
                int y1 = footman1.getYPosition() + direction1.yComponent();
                int x2 = footman2.getXPosition() + direction2.xComponent();
                int y2 = footman2.getYPosition() + direction2.yComponent();
                if (isInMap(x1, y1) && isInMap(x2, y2) && notOnResourceNode(x1, y1) && notOnResourceNode((x2, y2))) {
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    Action action1 = Action.createPrimitiveMove(footman1.getID(), direction1);
                    Action action2 = Action.createPrimitiveMove(footman2.getID(), direction2);
                    actionSet.put(0, action1);
                    actionSet.put(1, action2);
                    State.StateView newState = createNewState();
                }
            }
        }
    }

    private void generateArcherChildren(List<GameStateChild> children) {

    }
}
