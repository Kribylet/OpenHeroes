package gamemodel;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract class for maps that provides pathfinding functionality.
 *
 * Implements A* using an admissible heuristic. Modified in that it starts from
 * the goal, this prevents almost all full-edge searches in OpenHeroes.
 */
public abstract class PathMap
{
    private static final Logger LOGGER = Logger.getLogger(PathMap.class.getName());

    abstract boolean isPassable(Position pos);

    abstract int passCost(Position pos);

    abstract Set<Position> getNeighbors(Position pos);

    abstract int heuristicCostEstimate(Position start, Position goal);


    public LinkedList<Position> findPath(final Position goal, final Position start)
    {

        /*
         * The pathfinding actually starts at the goal. This prevents large full-edge searches
         * when a player clicks a tile that they have yet to discover the path to.
         */

        if (!isPassable(start)) {
            // We can never go to an impassable tile.
            return new LinkedList<>();
        }
        // We set our own position as the goal.

        Set<Node> closedSet = new HashSet<>();
        Set<Node> openSet = new HashSet<>();
        // The first node we check is the final destination node.
        Node startNode = new Node(start);
        // The g-score represents the cost of the shortest path we've found to this node.
        // In this case, the cost of stepping onto the final node of our path.
        startNode.setgScore(passCost(start));
        // The f-score represents an estimate of the total cost to move through this node towards the goal.
        // Roughly: KnownShortestCostToReachThisNode + HeuristicFunctionGuessAtTheRemainder
        startNode.setfScore(heuristicCostEstimate(start, goal));
        // Add the start node to our candidate nodes.
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            // Retrieve the most promising node we know of.
            Node currentNode = lowestFScoreNode(openSet);

            if (currentNode.getPosition().is(goal)) {
                /* If the node we're working on is the goal, we're done!
                 * Since each node remembers the node it is reached most cheaply through, we simple
                 * recurse through the chain of nodes to retrieve the full path.
                 */
                return reconstructPath(currentNode);
            }

            // If we're not at the goal, mark this node as checked
            openSet.remove(currentNode);
            closedSet.add(currentNode);

            // Find all nodes next to our current node and add them unless we've checked them already.
            // There's no point re-checking nodes since we're working from the most promising nodes first.
            for (Node neighbor : neighbors(currentNode, goal)) {
                if (hasNode(closedSet, neighbor)) {
                    continue;
                }

                int nextTilePassCost = passCost(neighbor.getPosition());

                // If the cost of reaching a neighbor node through this node is
                // lower than any cost to reach it we've recorded previously,
                // update its path chain with our node instead.
                int tentativeGScore = currentNode.getgScore() + nextTilePassCost;
                if (tentativeGScore < neighbor.getgScore()) {
                    neighbor.setCameFrom(currentNode);
                    neighbor.setgScore(tentativeGScore);
                    neighbor.setfScore(tentativeGScore + heuristicCostEstimate(neighbor.getPosition(), goal));
                }
                openSet.add(neighbor);
            }
        }
        LOGGER.log(Level.FINE, "Failed to find a path.");
        return new LinkedList<>();
    }

    private static boolean hasNode(Set<Node> set, Node node) {

        // Check if a node is in a set
        for (Node setMember : set) {
            if (setMember.is(node)) {
                return true;
            }
        }
        return false;
    }

    private static LinkedList<Position> reconstructPath(final Node currentNode) {

        // Rebuild the entire path from the node chain
        LinkedList<Position> fullPath = new LinkedList<>();
        for (Node current = currentNode; current != null; current = current.getCameFrom()) {
            fullPath.add(current.position);
        }
        fullPath.removeFirst();
        return fullPath;
    }

    private Set<Node> neighbors(Node node, Position goal) {
        //Ask the map
        Set<Node> neighbors = new HashSet<>();
        for (Position neighborPos : getNeighbors(node.position)) {
            // As a consequence if starting from the end destination, we must
            // allow the mover as a neighbor even though they're impassable.
            // The check is relatively cheap since it only is made if we can't
            // pass through something.
            if (isPassable(neighborPos) || neighborPos.is(goal)) {
                neighbors.add(new Node(neighborPos));
            }
        }
        return neighbors;
    }

    private static Node lowestFScoreNode(Set<Node> openSet) {
        int lowestFScore = Integer.MAX_VALUE;
        Node cheapestNode = null;
        for (Node node : openSet) {
            if (node.getfScore() < lowestFScore) {
                cheapestNode = node;
                lowestFScore = node.getfScore();
            }
        }
        return cheapestNode;
    }

    private static final class Node
    {
        private Position position;
        private Node cameFrom = null;

        // A node has a terrible initial cost, this means we will always assign a new value upon discovery.
        private int gScore = Integer.MAX_VALUE;
        private int fScore = Integer.MAX_VALUE;

        private Node(final Position position) {
            this.position = position;
        }

        private void setCameFrom(final Node cameFrom) {
            this.cameFrom = cameFrom;
        }

        public void setgScore(final int gScore) {
            this.gScore = gScore;
        }

        public void setfScore(final int fScore) {
            this.fScore = fScore;
        }

        public Position getPosition() {
            return position;
        }

        public Node getCameFrom() {
            return cameFrom;
        }

        public int getgScore() {
            return gScore;
        }

        public int getfScore() {
            return fScore;
        }

        // Nodes are the same if they have the same position.
        public boolean is(Node node) {
            return this.position.is(node.position);
        }
    }
}
