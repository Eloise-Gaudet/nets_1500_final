package graph_algorithms;

import graph_structures.IMDBGraph;

import java.util.*;

/**
 * Identifies triadic closure opportunities in an IMDBGraph.
 *
 * <p>A <em>weak triad</em> exists when person A is connected to both B and C, but B and C
 * have never collaborated directly. Completing the triad would mean B and C work together.
 *
 * <p>A <em>strong triad</em> (strong-triadic-closure) additionally requires that at least
 * two of the three edges carry a weight ≥ {@code STRONG_THRESHOLD} (i.e. A has collaborated
 * with both B and C on multiple titles), making it especially likely that B–C would benefit
 * from being introduced.
 */
public class TriadicClosure {

    /**
     * Minimum edge weight (number of shared titles) to count as a "strong" connection.
     * Adjust as needed for your dataset.
     */
    public static final int STRONG_THRESHOLD = 2;

    private TriadicClosure() {}

    // -----------------------------------------------------------------------
    // Public result type
    // -----------------------------------------------------------------------

    /**
     * Represents one open triad: a pivot node A connected to both B and C,
     * where B–C is the missing edge.
     */
    public static class OpenTriad {
        public final String pivot; // A
        public final String nodeB; // B
        public final String nodeC; // C
        public final int weightAB;
        public final int weightAC;
        public final boolean isStrong; // true if both A–B and A–C are "strong"

        public OpenTriad(String pivot, String b, String c,
                         int weightAB, int weightAC, boolean isStrong) {
            this.pivot   = pivot;
            this.nodeB   = b;
            this.nodeC   = c;
            this.weightAB = weightAB;
            this.weightAC = weightAC;
            this.isStrong = isStrong;
        }

        @Override
        public String toString() {
            return String.format(
                "OpenTriad{pivot='%s', B='%s'(w=%d), C='%s'(w=%d), strong=%b}",
                pivot, nodeB, weightAB, nodeC, weightAC, isStrong);
        }
    }

    // -----------------------------------------------------------------------
    // Core algorithm
    // -----------------------------------------------------------------------

    /**
     * Finds all open triads in the graph (weak triadic closure candidates).
     *
     * <p>For every node A, iterates over every pair (B, C) of A's neighbours.
     * If B and C are not themselves connected, the triad A–B–C is open.
     *
     * <p>Each unordered pair {B, C} is reported only once per pivot A.
     *
     * @param g the IMDB collaboration graph
     * @return list of all open triads, in no particular order
     */
    public static List<OpenTriad> findOpenTriads(IMDBGraph g) {
        List<OpenTriad> result = new ArrayList<>();

        for (String pivot : g.getNodes()) {
            Set<String> neighbors = g.outNeighbors(pivot);
            List<String> neighborList = new ArrayList<>(neighbors);

            for (int i = 0; i < neighborList.size(); i++) {
                for (int j = i + 1; j < neighborList.size(); j++) {
                    String b = neighborList.get(i);
                    String c = neighborList.get(j);

                    // Check whether B and C are already directly connected
                    if (!g.outNeighbors(b).contains(c)) {
                        int wAB = g.getWeight(pivot, b);
                        int wAC = g.getWeight(pivot, c);
                        boolean strong = (wAB >= STRONG_THRESHOLD) && (wAC >= STRONG_THRESHOLD);
                        result.add(new OpenTriad(pivot, b, c, wAB, wAC, strong));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Convenience method: returns only the open triads that satisfy the
     * strong-triadic-closure condition (both edges from the pivot have
     * weight ≥ {@code STRONG_THRESHOLD}).
     *
     * @param g the IMDB collaboration graph
     * @return list of strong open triads
     */
    public static List<OpenTriad> findStrongOpenTriads(IMDBGraph g) {
        List<OpenTriad> all = findOpenTriads(g);
        List<OpenTriad> strong = new ArrayList<>();
        for (OpenTriad t : all) {
            if (t.isStrong) strong.add(t);
        }
        return strong;
    }

    /**
     * Returns open triads sorted by the total pivot-edge weight descending
     * (highest-weight triads first — i.e. the most likely collaborations to
     * recommend).
     *
     * @param g the IMDB collaboration graph
     * @return sorted list of open triads
     */
    public static List<OpenTriad> findOpenTriadsByPriority(IMDBGraph g) {
        List<OpenTriad> triads = findOpenTriads(g);
        triads.sort((a, b) -> (b.weightAB + b.weightAC) - (a.weightAB + a.weightAC));
        return triads;
    }

    /**
     * Returns all open triads where the pivot is the specified node.
     * Useful for a "who should [person] introduce?" query.
     *
     * @param g     the IMDB collaboration graph
     * @param pivot the node ID to focus on
     * @return list of open triads centred on {@code pivot}
     */
    public static List<OpenTriad> findOpenTriadsForNode(IMDBGraph g, String pivot) {
        List<OpenTriad> result = new ArrayList<>();
        if (!g.getNodes().contains(pivot)) return result;

        Set<String> neighbors = g.outNeighbors(pivot);
        List<String> neighborList = new ArrayList<>(neighbors);

        for (int i = 0; i < neighborList.size(); i++) {
            for (int j = i + 1; j < neighborList.size(); j++) {
                String b = neighborList.get(i);
                String c = neighborList.get(j);
                if (!g.outNeighbors(b).contains(c)) {
                    int wAB = g.getWeight(pivot, b);
                    int wAC = g.getWeight(pivot, c);
                    boolean strong = (wAB >= STRONG_THRESHOLD) && (wAC >= STRONG_THRESHOLD);
                    result.add(new OpenTriad(pivot, b, c, wAB, wAC, strong));
                }
            }
        }
        return result;
    }
}
