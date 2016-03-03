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
        newState.myTurn = !myTurn;
        StateUnit footman1 = footmen.get(0);
        StateUnit otherFootman1 = newState.footmen.get(0);
        otherFootman1.health = footman1.health;
        otherFootman1.position = footman1.position.copy();

        try {
            StateUnit footman2 = footmen.get(1);
            StateUnit otherFootman2 = newState.footmen.get(1);
            otherFootman2.health = footman2.health;
            otherFootman2.position = footman2.position.copy();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            newState.footmen.remove(1);
        }

        StateUnit archer1 = archers.get(0);
        StateUnit otherArcher1 = newState.archers.get(0);
        otherArcher1.health = archer1.health;
        otherArcher1.position = archer1.position.copy();

        try {
            StateUnit archer2 = archers.get(1);
            StateUnit otherArcher2 = newState.archers.get(1);
            otherArcher2.health = archer2.health;
            otherArcher2.position = archer2.position.copy();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            newState.archers.remove(1);
        }

    }

    private void generateFootmenChildren(List<GameStateChild> children){
        StateUnit footman1 = footmen.get(0);
        try {
            StateUnit footman2 = footmen.get(1);
            for (Direction direction1 : Direction.values()) {
                for (Direction direction2 : Direction.values()) {
                    int x1 = footman1.getXPosition() + direction1.xComponent();
                    int y1 = footman1.getYPosition() + direction1.yComponent();
                    int x2 = footman2.getXPosition() + direction2.xComponent();
                    int y2 = footman2.getYPosition() + direction2.yComponent();
                    // TODO: check for diagonal directions(illegal)
                    if (isInMap(x1, y1) && isInMap(x2, y2) && notOnResourceNode(x1, y1) && notOnResourceNode(x2, y2) && notTheSameMove(x1, x2, y1, y2)) {
                        GameStateChild child = new GameStateChild(oldState);
                        updateChildState(child.state);
                        Map<Integer, Action> actionSet = new HashMap<Integer, Action>();
                        Action action1 = new DirectedAction(footman1.ID, ActionType.PRIMITIVEMOVE, direction1);
                        child.state.footmen.get(0).position.x = x1;
                        child.state.footmen.get(0).position.y = y1;
                        Action action2 = new DirectedAction(footman2.ID, ActionType.PRIMITIVEMOVE, direction2);
                        child.state.footmen.get(1).position.x = x2;
                        child.state.footmen.get(1).position.y = y2;
                        actionSet.put(0, action1);
                        actionSet.put(1, action2);
                        child.action = actionSet;
                        children.add(child);
                    }
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            //TODO: case where only one footman is live
        }
    }

    private void generateArcherChildren(List<GameStateChild> children) {

    }

    private int getAStarPathLength(Position start, Position end, List<ResourceNode.ResourceView> resources, int xExtent, int yExtent) {

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
                while (current.cameFrom != null) {
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
            for (int y = -1; y < 2; y=y+2) { // iterate over all nodes around current
                if (current.x + x >= 0 && current.x + x < xExtent && current.y + y >= 0 && current.y + y < yExtent &&(x != 0 || y != 0)) { // check if it's on the map
                    MapLocation test = new MapLocation(current.x + x, current.y + y);
                    if (!isLocationOccupied(current.x, current.y)) { // check if the location is occupied
                        neighborList.add(test);
                    }
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

        for(StateUnit unit: archers) {
            if(unit.getXPosition() == x && unit.getYPosition() ==y) {
                return true;
            }
        }

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

    private class MapLocation {
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

        public boolean alive() {
            return (this.health > 0);
        }
    }
}
