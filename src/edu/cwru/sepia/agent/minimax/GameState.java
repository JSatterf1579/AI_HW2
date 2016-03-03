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

    boolean myTurn = true;
    Double utility;
    List<StateUnit> footmen = new ArrayList<StateUnit>();
    List<StateUnit> archers = new ArrayList<StateUnit>();;
    List<Position> resources = new ArrayList<Position>();
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
        List<Unit.UnitView> origFootmen = state.getUnits(0);
        for (Unit.UnitView footman: origFootmen) {
            footmen.add(new StateUnit(footman));
        }
        List<Unit.UnitView> origArchers = state.getUnits(1);
        for (Unit.UnitView archer: origArchers) {
            footmen.add(new StateUnit(archer));
        }
        List<ResourceNode.ResourceView> origNodes = state.getAllResourceNodes();
        for (ResourceNode.ResourceView resource: origNodes) {
            resources.add(new Position(resource));
        }
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
    public List<GameStateChild> getChildren() {
        ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
        if (myTurn) {
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
        for (Position resource: resources) {
            if (x == resource.x && y == resource.y) {
                return false;
            }
        }
        return true;
    }

    private boolean notTheSameMove(int x1, int x2, int y1, int y2) {
        return (x1 != x2 && y1 != y2);
    }

    private StateUnit nextToArcher(StateUnit footman) {
        for (StateUnit archer: archers) {
            if (footman.nextTo(archer)) {
                return archer;
            }
        }
        return null;
    }

    private void updateChildState(GameState newState) {
        StateUnit footman1 = footmen.get(0);
        StateUnit footman2 = footmen.get(1);
        StateUnit archer1 = archers.get(0);
        StateUnit archer2 = archers.get(1);

    }

    private void generateFootmenChildren(List<GameStateChild> children){
        StateUnit footman1 = footmen.get(0);
        StateUnit footman2 = footmen.get(1);
        for (Direction direction1: Direction.values()) {
            for (Direction direction2: Direction.values()) {
                int x1 = footman1.getXPosition() + direction1.xComponent();
                int y1 = footman1.getYPosition() + direction1.yComponent();
                int x2 = footman2.getXPosition() + direction2.xComponent();
                int y2 = footman2.getYPosition() + direction2.yComponent();
                if (isInMap(x1, y1) && isInMap(x2, y2) && notOnResourceNode(x1, y1) && notOnResourceNode(x2, y2) && notTheSameMove(x1, x2, y1, y2)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    StateUnit footman1Target = nextToArcher(footman1);
                    StateUnit footman2Target = nextToArcher(footman2);
                    Action action1;
                    Action action2;
                    if (footman1Target != null) {
                        action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, footman1Target.ID);
                    } else {
                        action1 = new DirectedAction(footman1.ID, ActionType.PRIMITIVEMOVE, direction1);
                    }
                    if (footman2Target != null) {
                        action2 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, footman2Target.ID);
                    } else {
                        action2 = new DirectedAction(footman2.ID, ActionType.PRIMITIVEMOVE, direction2);
                    }
                    actionSet.put(0, action1);
                    actionSet.put(1, action2);

                }
            }
        }
    }

    private void generateArcherChildren(List<GameStateChild> children) {

    }

    private class Position {
        public int x;
        public int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Position(ResourceNode.ResourceView view) {
            this.x = view.getXPosition();
            this.y = view.getYPosition();
        }

    }

    private class StateUnit {
        public int ID;
        public Position position;
        public int health;
        public int range;
        public int damage;

        public StateUnit(edu.cwru.sepia.environment.model.state.Unit.UnitView realUnit) {
            this.ID = realUnit.getID();
            this.position = new Position(realUnit.getXPosition(), realUnit.getYPosition());
            this.health = realUnit.getTemplateView().getBaseHealth();
            this.range = realUnit.getTemplateView().getRange();
            this.damage = realUnit.getTemplateView().getBasicAttack();
        }

        public int getXPosition() {
            return position.x;
        }

        public int getYPosition() {
            return position.y;
        }

        public boolean nextTo(StateUnit otherUnit) {
            return (1 == Math.max(Math.abs(this.getXPosition() - otherUnit.getXPosition()), Math.abs(this.getYPosition() - otherUnit.getYPosition())));
        }

        public boolean alive() {
            return (this.health > 0);
        }
    }
}
