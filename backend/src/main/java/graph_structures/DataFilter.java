/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package graph_structures;

import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author eloisegaudet
 */
public class DataFilter {

    /**
     * filters graph by either movies or tv shows
     * @param graph = IMDBGraph 
     * @param type = 0 for movie or 1 for show
     * @return IMDBGraph filtered_graph
     */

    public static IMDBGraph filter_by_type(IMDBGraph graph, int type) {
        //create new IMDBGraph that's filtered
        IMDBGraph filtered_graph = new IMDBGraph();

        //add all people
        for (Person p : graph.get_all_people()) {
            filtered_graph.add_person(p);
        }

        //add all titles of the type
        for (Title t : graph.get_all_titles()) {
            if (t.get_type() == type) {
                filtered_graph.add_title(t);
            }
        }

        return filtered_graph;
    }

    /**
     * filters graph by genre(s)
     * @param graph = IMDBGraph 
     * @param genres = String[] of the genres
     * @return IMDBGraph filtered_graph
     */

    public static IMDBGraph filter_by_genres(IMDBGraph graph, String[] genres) {
        //create new IMDBGraph that's filtered
        IMDBGraph filtered_graph = new IMDBGraph();

        //add all people
        for (Person p : graph.get_all_people()) {
            filtered_graph.add_person(p);
        }

        //add all titles with genre in list
        for (Title t : graph.get_all_titles()) {
            if (!Collections.disjoint(Arrays.asList(t.get_genres()), Arrays.asList(genres))) {
                filtered_graph.add_title(t);
            }
        }

        return filtered_graph;
    }

    /**
     * filters graph by year range
     * @param graph = IMDBGraph 
     * @param start = int start year
     * @param end = int end year
     * @return IMDBGraph filtered_graph
     */

    public static IMDBGraph filter_by_year_range(IMDBGraph graph, int start, int end) {
        //create new IMDBGraph that's filtered
        IMDBGraph filtered_graph = new IMDBGraph();

        //add all people
        for (Person p : graph.get_all_people()) {
            filtered_graph.add_person(p);
        }

        //add all titles in range
        for (Title t : graph.get_all_titles()) {
            if (t.get_start_year() >= start && t.get_end_year() <= end) {
                filtered_graph.add_title(t);
            }
        }

        return filtered_graph;
    }

}
