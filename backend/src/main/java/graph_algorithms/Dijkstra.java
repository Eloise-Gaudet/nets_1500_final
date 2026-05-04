package graph_algorithms;



import graph_structures.IMDBGraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Provides access to Dijkstra's algorithm for a weighted graph.
 */
final public class Dijkstra {
    private Dijkstra() {
    }

    /**
     * Computes the shortest path between two nodes in a weighted graph.
     * Input graph is guaranteed to be valid and have no negative-weighted edges.
     *
     * @param g   the weighted graph to compute the shortest path on
     * @param src the source node
     * @param tgt the target node
     * @return an empty list if there is no path from {@param src} to {@param tgt}, otherwise an
     * ordered list of vertices in the shortest path from {@param src} to {@param tgt},
     * with the first element being {@param src} and the last element being {@param tgt}.
     */
    public static List<Integer> getShortestPath(IMDBGraph g, int src, int tgt) {
        if (src == tgt) {
            LinkedList<Integer> out = new LinkedList<>();
            out.add(src);
            return out;
        }

        int[] distance = new int[g.getSize()];
        ArrayList<String> spots = new ArrayList<>();
        Set<String> nodes = g.getNodes();
        int[] parent = new int[g.getSize()];
        int count = 0;
        for (String n: nodes) {
            spots.add(n);
            distance[count] = Integer.MAX_VALUE;
            parent[count] = -1;
            count++;
        }

        distance[src] = 0;

        BinaryMinHeapImpl<Integer, Integer> heap = new BinaryMinHeapImpl<>();

        for (int i = 0; i < g.getSize(); i++) {
            heap.add(distance[i], i);
        }

        while (!heap.isEmpty()) {
            int u = heap.extractMin().value;
            String curr = spots.get(u);
            Set<String> list = g.outNeighbors(curr);
            for (String v: list) {
                int i1 = spots.indexOf(curr);
                int i2 = spots.indexOf(v);
                if (heap.containsValue(i2) && distance[i2] > distance[i1] + g.getWeight(curr, v)) {
                    distance[i2] = distance[i1] + g.getWeight(curr,v);
                    heap.decreaseKey(i2,distance[i2]);
                    parent[i2] = i1;
                }
            }
        }

        if (parent[tgt] == -1) {
            return new LinkedList<>();
        }

        LinkedList<Integer> out = new LinkedList<>();
        int current = tgt;
        boolean done = false;
        while (!done) {
            out.addFirst(current);
            if (current == src) {
                done = true;
            } else if (parent[current] == -1) {
                return new LinkedList<>();
            }
            current = parent[current];
        }

        return out;
    }
}

