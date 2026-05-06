package com.imdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import Centralities.Centralities;
import graph_structures.DataLoader;
import graph_structures.IMDBGraph;
import graph_structures.Person;
import graph_structures.Title;
import jakarta.annotation.PostConstruct;

@Service
public class GraphService {

    private IMDBGraph graph;
    private Centralities centralities;
    // lowercase name -> list of person IDs (for autocomplete search)
    private final Map<String, List<String>> nameIndex = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            File cache = new File("../data/graph.ser");
            if (cache.exists()) {
                System.out.println("Loading graph from cache...");
                graph = DataLoader.load_graph("../data/graph.ser");
            } else {
                File gzFile = new File("../data/name.basics.tsv.gz");
                if (gzFile.exists()) {
                    System.out.println("Building graph from gzipped TSVs...");
                    graph = DataLoader.load_graph(
                        "../data/name.basics.tsv.gz",
                        "../data/title.basics.tsv.gz",
                        "../data/title.crew.tsv.gz",
                        "../data/title.principals.tsv.gz"
                    );
                    DataLoader.save_graph(graph, "../data/graph.ser");
                } else {
                    System.out.println("No graph data found — starting with empty graph.");
                    graph = new IMDBGraph();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load graph: " + e.getMessage());
            graph = new IMDBGraph();
        }

        centralities = new Centralities(graph);
        buildNameIndex();
        System.out.printf("Graph ready: %,d people%n", graph.size());
    }

    private void buildNameIndex() {
        for (String id : graph.get_all_person_ids()) {
            Person p = graph.get_person(id);
            if (p != null) {
                nameIndex
                    .computeIfAbsent(p.getName().toLowerCase(), k -> new ArrayList<>())
                    .add(id);
            }
        }
    }

    /** Case-insensitive substring search, capped at 20 results. */
    public List<String> findPersonIds(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        String q = query.toLowerCase().trim();
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : nameIndex.entrySet()) {
            if (entry.getKey().contains(q)) {
                results.addAll(entry.getValue());
                if (results.size() >= 20) break;
            }
        }
        return results;
    }

    public IMDBGraph getGraph()           { return graph; }
    public Centralities getCentralities() { return centralities; }

    public IMDBGraph filteredGraph(Integer type, String[] genres, Integer startYear, Integer endYear) {
        if (type == null && (genres == null || genres.length == 0) && startYear == null)
            return graph;

        Set<String> genreSet = (genres != null && genres.length > 0)
            ? new HashSet<>(Arrays.asList(genres)) : null;

        IMDBGraph result = new IMDBGraph();
        for (Title t : graph.get_all_titles()) {
            if (type != null && t.get_type() != type) continue;
            if (genreSet != null && Collections.disjoint(Arrays.asList(t.get_genres()), genreSet)) continue;
            if (startYear != null && endYear != null && (t.get_start_year() < startYear || t.get_end_year() > endYear)) continue;
            for (Person p : t.get_directors()) result.add_person(p);
            for (Person p : t.get_actors()) result.add_person(p);
            result.add_title(t);
        }
        return result;
    }
}
