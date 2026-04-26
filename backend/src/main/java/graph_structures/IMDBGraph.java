package graph_structures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @author eloisegaudet
 */
public class IMDBGraph implements Serializable {

    private final HashMap<String, HashMap<String, Integer>> all_connections; //edges between nodes (identified by string id)
    private final HashMap<String, Title> all_titles; //the actual title objects
    private final HashMap<String, Person> all_people; //the actual person objects


    public IMDBGraph() {
        all_connections = new HashMap<>();
        all_titles = new HashMap<>();
        all_people = new HashMap<>();
    }

    /**
     * adds a new title to graph and updates all corresponding nodes/edges
     * @param title = the input title
     */

    public void add_title(Title title) {

        //check that title hasn't already been added
        if (all_titles.containsKey(title.get_id())) {
            return;
        } 

        all_titles.put(title.get_id(), title);

        //iterate through all people in title
        ArrayList<String> people = new ArrayList<>();
        for (Person p : title.get_directors()) {
            people.add(p.get_id());
        } 
        for (Person p : title.get_actors()) {
            people.add(p.get_id());
        }
       
        for (String person : people) {
            //check if person not already in map
            if (!all_connections.containsKey(person)) {
                continue;
            } 

            HashMap<String, Integer> list = all_connections.get(person);

            //iterate through everyone else and add as connection
            for (String other : people) {
                if (!other.equals(person)) {
                    //check if they already have a connection and add title
                    if (list.containsKey(other)) {
                        list.merge(other, 1, Integer::sum);
                    } else {
                        list.put(other, 1);
                    }
                }
            }
        }

    }

    /**
     * adds a person to graph 
     * @param person = input person to be added
     */

    public void add_person(Person person) {

        //check that person isn't already in graph
        if (!all_connections.containsKey(person.get_id())) {
            all_people.put(person.get_id(), person);
            HashMap<String, Integer> to_add = new HashMap<>();
            all_connections.put(person.get_id(), to_add);
        }
    }

    /**
     * get the title object from the title id
     * @param id = the unique id for the title
     * @return the title object corresponding to the id or null if it doesn't exist
     */

    public Title get_title(String id) {
        if (all_titles.containsKey(id)) {
            return all_titles.get(id);
        } else {
            return null;
        }
    }

    /**
     * get the person object from the id
     * @param id = the unique id for the person
     * @return the person object corresponding to the id or null if one doesn't exist
     */

    public Person get_person(String id) {
        if (all_people.containsKey(id)) {
            return all_people.get(id);
        } else {
            return null;
        }
    }


}
