package edu.cwru.sepia.agent.minimax;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath;
import com.sun.org.apache.xml.internal.serializer.utils.SystemIDResolver;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
import org.omg.PortableInterceptor.INACTIVE;

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
    List<Integer> bestDistance = new ArrayList<>();
    List<StateUnit> archers = new ArrayList<StateUnit>();
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
        for (StateUnit footman: footmen) {
            footman.damage = 10;
        }

        List<Unit.UnitView> origArchers = state.getUnits(1);
        for (Unit.UnitView archer: origArchers) {
            archers.add(new StateUnit(archer));
        }
        for (StateUnit archer: archers) {
            archer.damage = 6;
        }

        List<ResourceNode.ResourceView> origNodes = state.getAllResourceNodes();
        for (ResourceNode.ResourceView resource: origNodes) {
            resources.add(new Position(resource));
        }

        xExtent = state.getXExtent();
        yExtent = state.getYExtent();
        oldState = state;




    }

    public static void removeDeadUnits(GameState state) {
        ArrayList<StateUnit> deadGuys = new ArrayList<>();
        for (StateUnit archer: state.archers) {
            if (archer.isDead()) {
                deadGuys.add(archer);
            }
        }
        for (StateUnit unit: deadGuys) {
            state.archers.remove(unit);
        }

        deadGuys = new ArrayList<>();
        for (StateUnit footman: state.footmen) {
            if (footman.isDead()) {
                deadGuys.remove(footman);
            }
        }
        for (StateUnit unit: deadGuys) {
            state.footmen.remove(unit);
        }
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
        if (bestDistance.size() == 0) {
            for (int i = 0; i < footmen.size(); i++) {
                int distance = Integer.MAX_VALUE;
                for (StateUnit archer : archers) {
                    int testDist = getAStarPathLength(footmen.get(i).position, archer.position, xExtent, yExtent);
                    if (testDist < distance) {
                        distance = testDist;
                    }
                }
                bestDistance.add(distance);
                //System.out.println("Distance = " + distance);
            }
        }
        double utility = 0.0;
        for(int i = 0; i < footmen.size(); i++) {
            StateUnit unit = footmen.get(i);
            if(unit.attacking) {
                utility += 500;
            }
            //Distance
            utility -= bestDistance.get(i);
            //health
            //utility+=unit.health;
        }

        for(StateUnit unit: archers) {
            if (unit.attacking) {
               utility  -= 1;
            }

            //health
           // utility -= unit.health;
        }

        //utility += 600*(2 - archers.size());

        return utility;
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

    private boolean onTopOfArcher(int x, int y) {
        for (StateUnit archer: archers) {
            if (x == archer.getXPosition() && y == archer.getYPosition()) {
                return true;
            }
        }
        return false;
    }

    private void updateChildState(GameState newState) {
        newState.myTurn = !myTurn;

        StateUnit footman1 = footmen.get(0);
        StateUnit otherFootman1 = newState.footmen.get(0);
        otherFootman1.health = footman1.health;
        otherFootman1.position = footman1.position.copy();

        if (footmen.size() == 2) {
            StateUnit footman2 = footmen.get(1);
            StateUnit otherFootman2 = newState.footmen.get(1);
            otherFootman2.health = footman2.health;
            otherFootman2.position = footman2.position.copy();
        } else if (newState.footmen.size() == 2) {
            newState.footmen.remove(1);
        }

        StateUnit archer1 = archers.get(0);
        StateUnit otherArcher1 = newState.archers.get(0);
        otherArcher1.health = archer1.health;
        otherArcher1.position = archer1.position.copy();

        if (archers.size() == 2) {
            StateUnit archer2 = archers.get(1);
            StateUnit otherArcher2 = newState.archers.get(1);
            otherArcher2.health = archer2.health;
            otherArcher2.position = archer2.position.copy();
        }
        else if (newState.archers.size() == 2) {
            newState.archers.remove(1);
        }



    }

    private void updateChildDistance(GameState newState) {

        newState.bestDistance.clear();

        for(int i = 0; i < newState.footmen.size(); i++) {
            int distance = Integer.MAX_VALUE;
            for(StateUnit archer : newState.archers) {
                int testDist = getAStarPathLength(newState.footmen.get(i).position, archer.position, xExtent, yExtent);
                if (testDist < distance) {
                    distance = testDist;
                }
            }
            newState.bestDistance.add(distance);
            //System.out.println("Distance = " + distance);
        }
    }

    private void generateFootmenChildren(List<GameStateChild> children){
        StateUnit footman1 = footmen.get(0);
        StateUnit archer1 = archers.get(0);
        if (footmen.size() == 2) {
            StateUnit footman2 = footmen.get(1);
            for (Direction direction1 : Direction.values()) {
                if (direction1.equals(Direction.NORTHEAST) || direction1.equals(Direction.SOUTHEAST) || direction1.equals(Direction.SOUTHWEST) || direction1.equals(Direction.SOUTHEAST)) {
                    continue;
                }
                for (Direction direction2 : Direction.values()) {
                    if (direction2.equals(Direction.NORTHEAST) || direction2.equals(Direction.SOUTHEAST) || direction2.equals(Direction.SOUTHWEST) || direction2.equals(Direction.SOUTHEAST)) {
                        continue;
                    }
                    int x1 = footman1.getXPosition() + direction1.xComponent();
                    int y1 = footman1.getYPosition() + direction1.yComponent();
                    int x2 = footman2.getXPosition() + direction2.xComponent();
                    int y2 = footman2.getYPosition() + direction2.yComponent();
                    // TODO: check for move on top of archer
                    if (isInMap(x1, y1) && isInMap(x2, y2) && notOnResourceNode(x1, y1) && notOnResourceNode(x2, y2) && !onTopOfArcher(x1, y1) &&!onTopOfArcher(x2, y2) && notTheSameMove(x1, x2, y1, y2)) {
                        GameStateChild child = new GameStateChild(oldState);
                        updateChildState(child.state);
                        Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                        Action action1 = new DirectedAction(footman1.ID, ActionType.PRIMITIVEMOVE, direction1);
                        child.state.footmen.get(0).position.x = x1;
                        child.state.footmen.get(0).position.y = y1;
                        Action action2 = new DirectedAction(footman2.ID, ActionType.PRIMITIVEMOVE, direction2);
                        child.state.footmen.get(1).position.x = x2;
                        child.state.footmen.get(1).position.y = y2;
                        updateChildDistance(child.state);
                        actionSet.put(footman1.ID, action1);
                        actionSet.put(footman2.ID, action2);
                        child.action = actionSet;
                        children.add(child);
                    }
                }
            }
            if (footman1.nextTo(archer1)) {
                for (Direction direction: Direction.values()) {
                    if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                        continue;
                    }
                    int x = footman2.getXPosition() + direction.xComponent();
                    int y = footman2.getYPosition() + direction.yComponent();
                    if (isInMap(x, y) && notOnResourceNode(x, y) && !onTopOfArcher(x, y) && notTheSameMove(x, y, footman1.getXPosition(), footman1.getYPosition())) {
                        GameStateChild child = new GameStateChild(oldState);
                        updateChildState(child.state);
                        Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                        Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                        child.state.footmen.get(0).attacking = true;
                        child.state.archers.get(0).health -= footman1.damage;
                        Action action2 = new DirectedAction(footman2.ID, ActionType.PRIMITIVEMOVE, direction);
                        child.state.footmen.get(1).position.x = x;
                        child.state.footmen.get(1).position.y = y;
                        removeDeadUnits(child.state);
                        updateChildDistance(child.state);
                        actionSet.put(footman1.ID, action1);
                        actionSet.put(footman2.ID, action2);
                        child.action = actionSet;
                        children.add(child);
                    }
                }
            }
            if (footman2.nextTo(archer1)) {
                for (Direction direction: Direction.values()) {
                    if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                        continue;
                    }
                    int x = footman1.getXPosition() + direction.xComponent();
                    int y = footman1.getYPosition() + direction.yComponent();
                    if (isInMap(x, y) && notOnResourceNode(x, y) && !onTopOfArcher(x, y) && notTheSameMove(x, y, footman2.getXPosition(), footman2.getYPosition())) {
                        GameStateChild child = new GameStateChild(oldState);
                        updateChildState(child.state);
                        Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                        Action action1 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                        child.state.footmen.get(1).attacking = true;
                        child.state.archers.get(0).health -= footman2.damage;
                        Action action2 = new DirectedAction(footman1.ID, ActionType.PRIMITIVEMOVE, direction);
                        child.state.footmen.get(0).position.x = x;
                        child.state.footmen.get(0).position.y = y;
                        removeDeadUnits(child.state);
                        updateChildDistance(child.state);
                        actionSet.put(footman2.ID, action1);
                        actionSet.put(footman1.ID, action2);
                        child.action = actionSet;
                        children.add(child);
                    }
                }
            }
            if (footman1.nextTo(archer1) && footman2.nextTo(archer1)) {
                GameStateChild child = new GameStateChild(oldState);
                updateChildState(child.state);
                Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                child.state.footmen.get(0).attacking = true;
                child.state.archers.get(0).health -= footman1.damage;
                Action action2 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                child.state.footmen.get(1).attacking = true;
                child.state.archers.get(0).health -= footman2.damage;
                removeDeadUnits(child.state);
                updateChildDistance(child.state);
                actionSet.put(footman1.ID, action1);
                actionSet.put(footman2.ID, action2);
                child.action = actionSet;
                children.add(child);
            }

            if (archers.size() == 2) {
                StateUnit archer2 = archers.get(1);
                if (footman1.nextTo(archer2)) {
                    for (Direction direction: Direction.values()) {
                        if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                            continue;
                        }
                        int x = footman2.getXPosition() + direction.xComponent();
                        int y = footman2.getYPosition() + direction.yComponent();
                        if (isInMap(x, y) && notOnResourceNode(x, y) && !onTopOfArcher(x, y) && notTheSameMove(x, y, footman1.getXPosition(), footman1.getYPosition())) {
                            GameStateChild child = new GameStateChild(oldState);
                            updateChildState(child.state);
                            Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                            Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                            child.state.footmen.get(0).attacking = true;
                            child.state.archers.get(1).health -= footman1.damage;
                            Action action2 = new DirectedAction(footman2.ID, ActionType.PRIMITIVEMOVE, direction);
                            child.state.footmen.get(1).position.x = x;
                            child.state.footmen.get(1).position.y = y;
                            removeDeadUnits(child.state);
                            updateChildDistance(child.state);
                            actionSet.put(footman1.ID, action1);
                            actionSet.put(footman2.ID, action2);
                            child.action = actionSet;
                            children.add(child);
                        }
                    }
                }
                if (footman2.nextTo(archer2)) {
                    for (Direction direction: Direction.values()) {
                        if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                            continue;
                        }
                        int x = footman1.getXPosition() + direction.xComponent();
                        int y = footman1.getYPosition() + direction.yComponent();
                        if (isInMap(x, y) && notOnResourceNode(x, y) && !onTopOfArcher(x, y) && notTheSameMove(x, footman2.getXPosition(), y, footman2.getYPosition())) {
                            GameStateChild child = new GameStateChild(oldState);
                            updateChildState(child.state);
                            Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                            Action action1 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                            child.state.footmen.get(1).attacking = true;
                            child.state.archers.get(1).health -= footman2.damage;
                            Action action2 = new DirectedAction(footman1.ID, ActionType.PRIMITIVEMOVE, direction);
                            child.state.footmen.get(0).position.x = x;
                            child.state.footmen.get(0).position.y = y;
                            removeDeadUnits(child.state);
                            updateChildDistance(child.state);
                            actionSet.put(footman2.ID, action1);
                            actionSet.put(footman1.ID, action2);
                            child.action = actionSet;
                            children.add(child);
                        }
                    }
                }
                if (footman1.nextTo(archer1) && footman2.nextTo(archer2)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                    child.state.footmen.get(0).attacking = true;
                    child.state.archers.get(0).health -= footman1.damage;
                    Action action2 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                    child.state.footmen.get(1).attacking = true;
                    child.state.archers.get(1).health -= footman2.health;
                    removeDeadUnits(child.state);
                    updateChildDistance(child.state);
                    actionSet.put(footman1.ID, action1);
                    actionSet.put(footman2.ID, action2);
                    child.action = actionSet;
                    children.add(child);
                }
                if (footman1.nextTo(archer2) && footman2.nextTo(archer2)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                    child.state.footmen.get(0).attacking = true;
                    child.state.archers.get(1).health -= footman1.damage;
                    Action action2 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                    child.state.footmen.get(1).attacking = true;
                    child.state.archers.get(1).health -= footman2.health;
                    removeDeadUnits(child.state);
                    updateChildDistance(child.state);
                    actionSet.put(footman1.ID, action1);
                    actionSet.put(footman2.ID, action2);
                    child.action = actionSet;
                    children.add(child);
                }
                if (footman1.nextTo(archer2) && footman2.nextTo(archer1)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                    child.state.footmen.get(0).attacking = true;
                    child.state.archers.get(1).health -= footman1.damage;
                    Action action2 = new TargetedAction(footman2.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                    child.state.footmen.get(1).attacking = true;
                    child.state.archers.get(0).health -= footman2.health;
                    removeDeadUnits(child.state);
                    updateChildDistance(child.state);
                    actionSet.put(footman1.ID, action1);
                    actionSet.put(footman2.ID, action2);
                    child.action = actionSet;
                    children.add(child);
                }
            }
        }else {
            for (Direction direction : Direction.values()) {
                if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                    continue;
                }
                int x = footman1.getXPosition() + direction.xComponent();
                int y = footman1.getYPosition() + direction.yComponent();
                // TODO: check for move on top of archer
                if (isInMap(x, y) && notOnResourceNode(x, y)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    Action action1 = new DirectedAction(footman1.ID, ActionType.PRIMITIVEMOVE, direction);
                    child.state.footmen.get(0).position.x = x;
                    child.state.footmen.get(0).position.y = y;
                    updateChildDistance(child.state);
                    actionSet.put(footman1.ID, action1);
                    child.action = actionSet;
                    children.add(child);
                }
            }
            if (footman1.nextTo(archer1)) {
                GameStateChild child = new GameStateChild(oldState);
                updateChildState(child.state);
                Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer1.ID);
                child.state.footmen.get(0).attacking = true;
                child.state.archers.get(0).health -= footman1.damage;
                updateChildDistance(child.state);
                removeDeadUnits(child.state);
                actionSet.put(footman1.ID, action1);
                child.action = actionSet;
                children.add(child);
            }
            if (archers.size() == 2) {
                StateUnit archer2 = archers.get(1);
                if (footman1.nextTo(archer2)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                    Action action1 = new TargetedAction(footman1.ID, ActionType.PRIMITIVEATTACK, archer2.ID);
                    child.state.footmen.get(0).attacking = true;
                    child.state.archers.get(1).health -= footman1.damage;
                    updateChildDistance(child.state);
                    removeDeadUnits(child.state);
                    actionSet.put(footman1.ID, action1);
                    child.action = actionSet;
                    children.add(child);
                }
            }
        }
    }

    private void generateArcherChildren(List<GameStateChild> children) {
        if(archers.size() == 1) {
            StateUnit archer = archers.get(0);
            for (Direction direction : Direction.values()) {
                if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                    continue;
                }
                int x = archer.getXPosition() + direction.xComponent();
                int y = archer.getYPosition() + direction.yComponent();
                if(isInMap(x, y) && notOnResourceNode(x,y)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<>();
                    Action action = new DirectedAction(archer.ID, ActionType.PRIMITIVEMOVE, direction);
                    actionSet.put(1, action);
                    child.state.archers.get(0).position.x = x;
                    child.state.archers.get(0).position.y = y;
                    updateChildDistance(child.state);
                    child.action = actionSet;
                    children.add(child);
                }
            }
            for (int i = 0; i < footmen.size(); i++) {
                StateUnit footman = footmen.get(i);
                if (isInRange(archer, footman)) {
                    GameStateChild child = new GameStateChild(oldState);
                    updateChildState(child.state);
                    Map<Integer, Action> actionSet = new HashMap<>();
                    Action action = new TargetedAction(archer.ID, ActionType.PRIMITIVEATTACK, footman.ID);
                    actionSet.put(1, action);
                    child.state.footmen.get(i).health -= 6; //hardcoded expected value lol
                    child.state.archers.get(0).attacking = true;
                    updateChildDistance(child.state);
                    child.action = actionSet;
                    children.add(child);
                }
            }
        } else if (archers.size() ==2) {
            StateUnit archer1 = archers.get(0);
            StateUnit archer2 = archers.get(1);
            for (Direction direction : Direction.values()) {
                if (direction.equals(Direction.NORTHEAST) || direction.equals(Direction.SOUTHEAST) || direction.equals(Direction.SOUTHWEST) || direction.equals(Direction.SOUTHEAST)) {
                    continue;
                }



                for (Direction direction1: Direction.values()) {
                    if (direction1.equals(Direction.NORTHEAST) || direction1.equals(Direction.SOUTHEAST) || direction1.equals(Direction.SOUTHWEST) || direction1.equals(Direction.SOUTHEAST)) {
                        continue;
                    }
                    int x1 = archer1.getXPosition() + direction.xComponent();
                    int x2 = archer2.getXPosition() + direction1.yComponent();
                    int y1 = archer1.getYPosition() + direction.yComponent();
                    int y2 = archer2.getYPosition() + direction1.yComponent();

                    if(isInMap(x1, y1) && isInMap(x2,y2) && notOnResourceNode(x1,y1) && notOnResourceNode(x2, y2) && notTheSameMove(x1, x2, y1, y2)) {
                        GameStateChild child = new GameStateChild(oldState);
                        updateChildState(child.state);
                        Map<Integer, Action> actionSet = new HashMap<>();
                        Action action  = new DirectedAction(archer1.ID, ActionType.PRIMITIVEMOVE, direction);
                        Action action2 = new DirectedAction(archer2.ID, ActionType.PRIMITIVEMOVE, direction1);
                        actionSet.put(1, action);
                        actionSet.put(2, action2);
                        child.state.archers.get(0).position.x = x1;
                        child.state.archers.get(0).position.y = y1;
                        child.state.archers.get(1).position.x = x2;
                        child.state.archers.get(1).position.y = y2;
                        updateChildDistance(child.state);
                        child.action = actionSet;
                        children.add(child);
                    }
                }
            }

            //Archer 1 attacking
            for (int i = 0; i < footmen.size(); i++) {
                StateUnit footman = footmen.get(i);
                if(isInRange(archer1, footman)) {
                    for (Direction direction1: Direction.values()) {
                        if (direction1.equals(Direction.NORTHEAST) || direction1.equals(Direction.SOUTHEAST) || direction1.equals(Direction.SOUTHWEST) || direction1.equals(Direction.SOUTHEAST)) {
                            continue;
                        }
                        int x2 = archer2.getXPosition() + direction1.yComponent();
                        int y2 = archer2.getYPosition() + direction1.yComponent();

                        if(isInMap(x2,y2) && notOnResourceNode(x2, y2)) {
                            GameStateChild child = new GameStateChild(oldState);
                            updateChildState(child.state);
                            Map<Integer, Action> actionSet = new HashMap<>();
                            Action action  = new TargetedAction(archer1.ID, ActionType.PRIMITIVEATTACK, footman.ID);
                            Action action2 = new DirectedAction(archer2.ID, ActionType.PRIMITIVEMOVE, direction1);
                            actionSet.put(1, action);
                            actionSet.put(2, action2);
                            child.state.archers.get(1).position.x = x2;
                            child.state.archers.get(1).position.y = y2;
                            child.state.footmen.get(i).health -= 6;
                            child.state.archers.get(0).attacking = true;
                            updateChildDistance(child.state);
                            child.action = actionSet;
                            children.add(child);
                        }
                    }
                }
            }

            //Archer 2 attacking
            for (int i = 0; i < footmen.size(); i++) {
                StateUnit footman = footmen.get(i);
                if(isInRange(archer2, footman)) {
                    for (Direction direction1: Direction.values()) {
                        if (direction1.equals(Direction.NORTHEAST) || direction1.equals(Direction.SOUTHEAST) || direction1.equals(Direction.SOUTHWEST) || direction1.equals(Direction.SOUTHEAST)) {
                            continue;
                        }
                        int x2 = archer1.getXPosition() + direction1.yComponent();
                        int y2 = archer1.getYPosition() + direction1.yComponent();

                        if(isInMap(x2,y2) && notOnResourceNode(x2, y2)) {
                            GameStateChild child = new GameStateChild(oldState);
                            updateChildState(child.state);
                            Map<Integer, Action> actionSet = new HashMap<>();
                            Action action  = new TargetedAction(archer2.ID, ActionType.PRIMITIVEATTACK, footman.ID);
                            Action action2 = new DirectedAction(archer1.ID, ActionType.PRIMITIVEMOVE, direction1);
                            actionSet.put(1, action);
                            actionSet.put(2, action2);
                            child.state.archers.get(0).position.x = x2;
                            child.state.archers.get(0).position.y = y2;
                            child.state.footmen.get(i).health -= 6;
                            child.state.archers.get(1).attacking = true;
                            updateChildDistance(child.state);
                            child.action = actionSet;
                            children.add(child);
                        }
                    }
                }
            }

            //Both archers attacking
            for (int i = 0; i < footmen.size(); i++) {
                StateUnit footman = footmen.get(i);
                if(isInRange(archer1, footman)) {
                    for (int j = 0; j < footmen.size(); j++) {
                        StateUnit footman1 = footmen.get(i);
                        if(isInRange(archer2, footman1)) {
                            GameStateChild child = new GameStateChild(oldState);
                            updateChildState(child.state);
                            Map<Integer, Action> actionSet = new HashMap<>();
                            Action action = new TargetedAction(archer1.ID, ActionType.PRIMITIVEATTACK, footman.ID);
                            Action action1 = new TargetedAction(archer2.ID, ActionType.PRIMITIVEATTACK, footman1.ID);
                            actionSet.put(1, action);
                            actionSet.put(2, action1);
                            child.state.archers.get(0).attacking = true;
                            child.state.archers.get(1).attacking = true;
                            child.state.footmen.get(i).health -= 6;
                            child.state.footmen.get(j).health -= 6;
                            updateChildDistance(child.state);
                            child.action = actionSet;
                            children.add(child);
                        }
                    }
                }
            }
        }
    }

    private boolean isInRange(StateUnit archer, StateUnit footman) {
        return  Math.abs(archer.getXPosition() - footman.getXPosition()) + Math.abs(archer.getYPosition() - footman.getYPosition()) <= archer.range;
    }

    private int getAStarPathLength(Position start, Position end, int xExtent, int yExtent) {

        boolean done = false;
        MapLocation current = null;
        PriorityQueue<MapLocation> nextLocs = new PriorityQueue<>();
        Set<MapLocation> closedList = new HashSet<>();

        //Transform Positions to map locations
        MapLocation startLocation = new MapLocation(start.x, start.y);
        MapLocation endLocation = new MapLocation(end.x, end.y);

        startLocation.cost = 0;
        startLocation.heuristic = chebyshev(startLocation, endLocation);
        nextLocs.add(startLocation);

        while(nextLocs.peek() != null) {
            if(done){
                int pathLength = 0;
                current = current.cameFrom;
                while (current != null && current.cameFrom != null) {
                    pathLength++;
                    current = current.cameFrom;
                }
                return pathLength;
            }

            current = nextLocs.poll();
            closedList.add(current);
            List<MapLocation> neighbors = getValidNeighbors(current, xExtent, yExtent);
            for(MapLocation neighbor : neighbors){
                neighbor.heuristic = chebyshev(neighbor, endLocation);
                neighbor.cost = current.cost + 1;

                if(closedList.contains(neighbor)) {
                    continue;
                }
                if(!nextLocs.contains(neighbor)) {
                    nextLocs.add(neighbor);
                } else if(current.cost + 1 >= neighbor.cost) {
                    continue;
                }

                neighbor.cameFrom = current;
            }
            if (current.equals(endLocation)) {
                done = true;
            }

        }

        return Integer.MAX_VALUE;

    }

    private List<MapLocation> getValidNeighbors(MapLocation current, int xExtent, int yExtent) {
        List<MapLocation> neighborList = new ArrayList<>();

        for (int x = -1; x < 2; x=x+2) {
            if (current.x + x >= 0 && current.x + x < xExtent && current.y>= 0 && current.y< yExtent &&(x != 0)) { // check if it's on the map
                MapLocation test = new MapLocation(current.x + x, current.y);
                if (!isLocationOccupied(test.x, test.y)) { // check if the location is occupied
                    neighborList.add(test);
                }
            }
        }

        for (int y = -1; y < 2; y=y+2) { // iterate over all nodes around current
            if (current.x >= 0 && current.x < xExtent && current.y + y >= 0 && current.y + y < yExtent &&(y != 0)) { // check if it's on the map
                MapLocation test = new MapLocation(current.x , current.y + y);
                if (!isLocationOccupied(test.x, test.y)) { // check if the location is occupied
                    neighborList.add(test);
                }
            }
        }

        return neighborList;
    }

    private boolean isLocationOccupied(int x, int y)
    {
        for(StateUnit unit: footmen) {
            if(unit.getXPosition() == x && unit.getYPosition() ==y) {
                return true;
            }
        }

//        for(StateUnit unit: archers) {
//            if(unit.getXPosition() == x && unit.getYPosition() ==y) {
//                return true;
//            }
//        }

        for(Position resource : resources) {
            if(resource.x == x && resource.y == y) {
                return true;
            }
        }

        return false;
    }

    private float chebyshev(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    private class MapLocation implements Comparable<MapLocation>{
        public int x, y;

        public MapLocation cameFrom; // previous location on map from search

        public float cost; // Cost of reaching this location on the map from the start node

        public float heuristic; // the chebyshev distance from this node to the goal

        /**
         * Constructor used to initialize a MapLocation in search.
         * @param x the column that the cell is in
         * @param y the row the cell is in
         * @param cameFrom the previous cell in the A* search
         * @param cost the cost to reach this node in A* search
         * @param heuristic the chebyshev distance from this cell to the goal
         */
        public MapLocation(int x, int y, MapLocation cameFrom, float cost, float heuristic)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
            this.heuristic = heuristic;
        }

        /**
         * Constructor used to initialize a MapLocation when all info is not known.
         * @param x the column that the cell is in
         * @param y the row the cell is in
         */
        public MapLocation(int x, int y) {
            this.x = x;
            this.y = y;
            this.cost = Integer.MAX_VALUE;
            this.heuristic = Integer.MAX_VALUE;
            this.cameFrom = null;
        }

        /**
         * Returns true if the given object is a MapLocation with the same coordinates.
         * @param other The other cell in question
         * @return
         */
        public boolean equals(Object other) {

            if (other != null && other instanceof  MapLocation) {
                MapLocation oMap = (MapLocation)other;
                if (this.x == oMap.x && this.y == oMap.y) {
                    return true;
                } else {
                    return false;
                }
            }
            return  false;
        }

        /**
         * Used in Priority queues to order MapLocations.  A MapLocation is "less" when
         * it has a lower total cost than another MapLocation
         * @param other
         * @return -1 if less, 0 if same, 1 if more
         */
        public int compareTo(MapLocation other) {
            return Float.compare(this.heuristic + this.cost, other.heuristic + other.cost);
        }

        /**
         * Used for hashcoding
         * @return the string version of the coordinates of the cell
         */
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        /**
         * Implemented to allow for ArrayList.contains to work
         * @return the hash of the toString of this object. Equivalent cells will hash to the same value
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
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

        public Position copy() {
            return new Position(x, y);
        }

    }

    private class StateUnit {
        public int ID;
        public Position position;
        public int health;
        public int range;
        public int damage;
        public boolean attacking = false;

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

        public boolean isDead() {
            return (this.health <= 0);
        }
    }
}
