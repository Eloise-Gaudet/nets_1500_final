package graph_algorithms;

import graph_structures.IMDBGraph;

import java.util.*;


// Finds triadic closure opportunities in the graph
// Strong connection when actors have worked together in at least 2 projects
public class TriadicClosure {

    public static final int STRONG_THRESHOLD = 2;

    private TriadicClosure() {}

    // subclass to make saving OpenTriads easier 
    public static class OpenTriad {
        public final String pivot; // A
        public final String nodeB; // B
        public final String nodeC; // C
        public final int weightAB;
        public final int weightAC;
        public final boolean isStrong; // true if both A–B and A–C are strong

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

    // Main Algorithm
    // Finds all open triads in the graph, no order

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

    // Same as above but only considers open triads with 2 strong connections
    public static List<OpenTriad> findStrongOpenTriads(IMDBGraph g) {
        List<OpenTriad> all = findOpenTriads(g);
        List<OpenTriad> strong = new ArrayList<>();
        for (OpenTriad t : all) {
            if (t.isStrong) strong.add(t);
        }
        return strong;
    }



    // Triadic Closure ordering open triads by those with the strongest connections
    public static List<OpenTriad> findOpenTriadsByPriority(IMDBGraph g) {
        List<OpenTriad> triads = findOpenTriads(g);
        triads.sort((a, b) -> (b.weightAB + b.weightAC) - (a.weightAB + a.weightAC));
        return triads;
    }


    // Triadic Closure but we are only considering a specific node
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
