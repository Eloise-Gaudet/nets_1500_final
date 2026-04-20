
import java.io.File;

import graph_structures.DataLoader;
import graph_structures.IMDBGraph;

public class Main {
    public static void main(String[] args) throws Exception {

        //load in the imdb graph
        IMDBGraph imdb_graph;
        
        File cache = new File("../data/graph.ser");
        //either loads from presaved file
        if (cache.exists()) {
            System.out.println("Loading from cache...");
            imdb_graph = DataLoader.load_graph("../data/graph.ser");
        } //or loads from tsvs and then saves to file
        else {
            System.out.println("Building graph from TSVs...");
            imdb_graph = DataLoader.load_graph("../data/name.basics.tsv.gz", "../data/title.basics.tsv.gz", 
                "../data/title.crew.tsv.gz", "../data/title.principals.tsv.gz");
            DataLoader.save_graph(imdb_graph, "../data/graph.ser");
        }

        System.out.println("Success");
    }
}