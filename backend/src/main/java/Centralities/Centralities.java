package Centralities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import graph_structures.IMDBGraph;
import graph_structures.Person;

public class Centralities {

    private final IMDBGraph graph;

    // cached after first call --> reused by filters without recomputing
    private Map<String, Integer> degreeScores;
    private Map<String, Double> eigenvectorScores;
    private Map<String, Double> katzScores;

    public Centralities(IMDBGraph graph) {
        this.graph = graph;
    }

    //needed for the number of collaborations when finding degree of seperation
    public static class PersonRank {
        public final String personId;
        public final String personName;
        public final double score;

        public PersonRank(String personId, String personName, double score) {
            this.personId = personId;
            this.personName = personName;
            this.score = score;
        }

        @Override
        public String toString() {
            return personName + " (" + personId + "): " + score;
        }
    }

    public static class PersonStep {
        public final String personId;
        public final String personName;
        public final int collaborationCount; //edge weight to previous node in path

        public PersonStep(String personId, String personName, int collaborationCount) {
            this.personId = personId;
            this.personName = personName;
            this.collaborationCount = collaborationCount;
        }

        @Override
        public String toString() {
            if (collaborationCount > 0) {
                return personName + " [" + collaborationCount + " shared titles]";
            } else {
                return personName;
            }
        }
    }

    //get degree centrality of the graph
    private void computeAndCacheDegrees() {
        //if already computed then all good
        if (degreeScores != null) {
            return;
        }

        degreeScores = new HashMap<>();
        //get degrees of all people from their ids
        for (String id : graph.get_all_person_ids()) {
            degreeScores.put(id, graph.get_degree(id));
        }
    }

    /**
     * Ranks people by degree centrality (number of distinct collaborators).
     * @param topN          max results to return
     * @param actorsOnly    if true, only include actors
     * @param directorsOnly if true, only include directors
     */
    public List<PersonRank> rankByDegree(int topN, boolean actorsOnly, boolean directorsOnly) {
        //get or compute degrees
        computeAndCacheDegrees();

        List<PersonRank> results = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : degreeScores.entrySet()) {
            String id = entry.getKey();
            Person p = graph.get_person(id);
            if (p == null) {
                continue;
            }

            //check if we want actors or directors 
            if (actorsOnly && !p.isActor()) {
                continue;
            }
            if (directorsOnly && !p.isDirector()) {
                continue;
            }

            results.add(new PersonRank(id, p.getName(), entry.getValue()));
        }

        //sort then return the top N of them
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topN, results.size()));
    }

    //eigenvector centralities

    private void computeAndCacheEigenvector(int maxIterations) {
        if (eigenvectorScores != null) {
            return;
        }
        Set<String> ids = graph.get_all_person_ids();

        int N = ids.size();

        Map<String, Double> scores = new HashMap<>(N);

        for (String id : ids) {
            scores.put(id, 1.0 / N);
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            Map<String, Double> newScores = new HashMap<>(N);

            for (String id : ids) {
                double sum = 0.0;
                for (Map.Entry<String, Integer> neighbor : graph.get_neighbors(id).entrySet()) {
                    sum += scores.getOrDefault(neighbor.getKey(), 0.0) * neighbor.getValue();
                }

                newScores.put(id, sum);
            }

            //get norm of scores to then normalize 
            double norm = 0.0;
            for (double v : newScores.values()) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            if (norm == 0.0) {
                break;
            }

            double maxDelta = 0.0; //make sure nothing breaks
            for (String id : ids) {
                //normalize values
                double newVal = newScores.get(id) / norm;
                maxDelta = Math.max(maxDelta, Math.abs(newVal - scores.get(id)));
                newScores.put(id, newVal);
            }

            scores = newScores;
            if (maxDelta < 1e-6) {
                break;
            }
        }

        eigenvectorScores = scores; //update scores
    }

    /**
     * Ranks people by eigenvector centrality — rewards having well connected collaborators
     * @param topN          max results to return
     * @param actorsOnly    if true, only include actors
     * @param directorsOnly if true, only include directors
     * @param maxIterations power iteration cap --> to efficiently compute 
     */

    public List<PersonRank> rankByEigenvector(int topN, boolean actorsOnly, boolean directorsOnly, int maxIterations) {
        //compute or get cached values
        computeAndCacheEigenvector(maxIterations);

        List<PersonRank> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : eigenvectorScores.entrySet()) {
            String id = entry.getKey();
            Person p = graph.get_person(id);
            if (p == null) {
                continue;
            }
            //filter
            if (actorsOnly && !p.isActor()) {
                continue;
            }
            if (directorsOnly && !p.isDirector()) {
                continue;
            }

            results.add(new PersonRank(id, p.getName(), entry.getValue()));
        }
        //sort then return
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topN, results.size()));
    }

    // -------------------------------------------------------------------------
    // Feature 1c: Katz Centrality
    // Similar to eigenvector centrality but the constant baseline added is beta that handles
    // when values that have no value
    // -------------------------------------------------------------------------

    private void computeAndCacheKatz(double alpha, double beta, int maxIterations) {
        if (katzScores != null) {
            return;
        }

        //get all people to place 
        Set<String> ids = graph.get_all_person_ids();
        Map<String, Double> scores = new HashMap<>(ids.size());
        
        for (String id : ids) {
            scores.put(id, beta);
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            Map<String, Double> newScores = new HashMap<>(ids.size());
            double maxDelta = 0.0;

            for (String id : ids) {
                double sum = 0.0;
                for (Map.Entry<String, Integer> neighbor : graph.get_neighbors(id).entrySet()) {
                    sum += scores.getOrDefault(neighbor.getKey(), 0.0) * neighbor.getValue();
                }

                double newVal = alpha * sum + beta;
                maxDelta = Math.max(maxDelta, Math.abs(newVal - scores.getOrDefault(id, 0.0)));
                newScores.put(id, newVal);
            }

            scores = newScores;
            if (maxDelta < 1e-6) {
                break;
            }
        }
        katzScores = scores;
    }

    /**
     *
     * @param alpha         use 0.005 for safety on large graphs
     * @param beta          baseline added to every node each iteration (typically 1.0)
     * @param maxIterations iteration cap (50 is a good default)
     */

    public List<PersonRank> rankByKatz(int topN, boolean actorsOnly, boolean directorsOnly,
                                       double alpha, double beta, int maxIterations) {
        computeAndCacheKatz(alpha, beta, maxIterations);
        List<PersonRank> results = new ArrayList<>();

        for (Map.Entry<String, Double> entry : katzScores.entrySet()) {
            String id = entry.getKey();
            Person p = graph.get_person(id);

            //filter out
            if (p == null) {
                continue;
            }
            if (actorsOnly && !p.isActor()) {
                continue;
            }
            if (directorsOnly && !p.isDirector()) {
                continue;
            }
            results.add(new PersonRank(id, p.getName(), entry.getValue()));
        }
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topN, results.size()));
    }

    // -------------------------------------------------------------------------
    // degrees of Separation (BFS)
    // Returns the shortest collaboration path between two people.
    // -------------------------------------------------------------------------

    /**
     * Finds the shortest path between two people with BFS.
     * Returns Optional.empty() if no path exists (disconnected components).
     * Each PersonStep includes the number of shared titles with the previous node.
     */

    public Optional<List<PersonStep>> findDegreesOfSeparation(String sourceId, String targetId) {
        //check for non existent values
        if (graph.get_person(sourceId) == null || graph.get_person(targetId) == null) {
            return Optional.empty();
        }

        if (sourceId.equals(targetId)) {
            Person p = graph.get_person(sourceId);
            return Optional.of(Collections.singletonList(new PersonStep(sourceId, p.getName(), 0)));
        }

        Queue<String> queue = new ArrayDeque<>();
        Map<String, String> predecessor = new HashMap<>();
        queue.add(sourceId);
        predecessor.put(sourceId, null);

        boolean found = false;
        while (!queue.isEmpty() && !found) {
            String current = queue.poll();
            for (String neighbor : graph.get_neighbors(current).keySet()) {
                if (!predecessor.containsKey(neighbor)) {
                    predecessor.put(neighbor, current);
                    if (neighbor.equals(targetId)) {
                        found = true;
                        break;
                    }
                    queue.add(neighbor);
                }
            }
        }

        if (!found) {
            return Optional.empty();
        }

        // reconstruct path by walking predecessor map backwards
        List<String> pathIds = new ArrayList<>();
        String cur = targetId;
        while (cur != null) {
            pathIds.add(cur);
            cur = predecessor.get(cur);
        }
        Collections.reverse(pathIds);

        List<PersonStep> path = new ArrayList<>();
        for (int i = 0; i < pathIds.size(); i++) {
            String id = pathIds.get(i);
            Person p = graph.get_person(id);
            int collabCount = 0;
            if (i > 0) {
                Map<String, Integer> neighbors = graph.get_neighbors(pathIds.get(i - 1));
                collabCount += neighbors.getOrDefault(id, 0);
            }
            String name = id;
            if (p != null) {
                name = p.getName();
            }
            path.add(new PersonStep(id, name, collabCount));
        }
        return Optional.of(path);
    }

    public static void main(String[] args) {
        System.out.println("Use Centralities(IMDBGraph graph) to initialize.");
    }
}
