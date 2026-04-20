/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package graph_structures;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;


/**
 *
 * @author eloisegaudet
 */
public class DataLoader {

    /**
     * Creates and returns the IMDB graph from the tsv files
     * @param name_basics = the path to the name.basics.tsv.gz file
     * @param title_basics = the path to the title.basics.tsv.gz file
     * @param title_crew = the path to the title.crew.tsv.gz file
     * @param title_principals = the path to the title.principals.tsv.gz file
     * @return the IMDBGraph 
     */

    public static IMDBGraph load_graph(String name_basics, String title_basics, String title_crew, String title_principals) throws IOException {

        IMDBGraph imdb_graph = new IMDBGraph();
        HashMap<String, Title> titles = new HashMap<>();

        // load in people from name_basics
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(name_basics))))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                String nconst = fields[0];
                String name = fields[1];
                String professions = fields[4];

                boolean is_actor = professions.contains("actor") || professions.contains("actress");
                boolean is_director = professions.contains("director");

                if (is_actor || is_director) {
                    imdb_graph.add_person(new Person(name, nconst, is_actor, is_director));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading people data: " + e);
        }

        //then load in title data
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(title_basics))))) {
            br.readLine(); 
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                String tconst = fields[0];
                int title_type;
                if (fields[1].contains("movie")) {
                    title_type = 0;
                } else if (fields[1].contains("tvshow")) {
                    title_type = 1;
                } else {
                    title_type = 2;
                }

                String title_name = fields[2];
                int start_year;
                if (!fields[5].contains("N")) { 
                    start_year = Integer.parseInt(fields[5]);
                } else {
                    start_year = -1;
                }
                int end_year;
                if (!fields[6].contains("N")){
                    end_year = Integer.parseInt(fields[6]);
                } else {
                    end_year = -1;
                }
                String genres = fields[7];

                if (title_type < 2) {
                    Title new_title = new Title(title_name, tconst, title_type, start_year, end_year, genres);
                    titles.put(tconst, new_title);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading title data: " + e);
        }

        //then load in crew data
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(title_crew))))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                String tconst = fields[0];
                String[] directors = fields[1].split(",");

                //check if title already in titles

                //add all directors to the title
                if (titles.containsKey(tconst)) {
                    Title title = titles.get(tconst);
                    for (String director : directors) {
                        Person dir = imdb_graph.get_person(director);
                        if (dir != null) {
                            title.add_director(dir);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error loading crew data: " + e);
        }

        //finally load in actor data
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(title_principals))))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                String tconst = fields[0];
                String nconst = fields[2];
                String category = fields[3];

                Person person = imdb_graph.get_person(nconst);

                //add actor to title
                if ((category.contains("actor") || category.contains("actress")) && titles.containsKey(tconst) && person != null) {
                    String character = fields[5];
                    Title title = titles.get(tconst);
                    title.add_actor(person, character);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading actor data: " + e);
        }

        //finally load all the titles into the graph
        for (Title title : titles.values()) {
            imdb_graph.add_title(title);
        }

        return imdb_graph;

    }

    /**
     * saves the imdb graph to the input file 
     * @param graph = the IMDBGraph we want to save
     * @param path = the path to the file we want to save the graph to
     */


    public static void save_graph(IMDBGraph graph, String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(graph);
        }
        System.out.println("Graph saved to " + path);
    }

    /**
     * loads the IMDBGraph from the input path
     * @param path = the path to the file where the IMDB graph was stored
     */

    public static IMDBGraph load_graph(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (IMDBGraph) ois.readObject();
        }
    }



}
