package graph_structures;
import java.io.Serializable;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author eloisegaudet
 */
public class Person implements Serializable {

    //name and unique identifier for each actor
    private String name;
    private String id;

    //determines if person is actor or director
    private boolean is_actor;
    private boolean is_director; 

    //list of titles 
    //private ArrayList<Title> titles;

    public Person(String name, String id, boolean is_actor, boolean is_director) {
        this.name = name;
        this.id = id;
        this.is_actor = is_actor;
        this.is_director = is_director;

        //titles = new ArrayList<>();
    }

    /*
    public void add_title(Title title) {
        titles.add(title);
    }
    */

    //some helper methods
    public String getName() { 
        return name; 
    }
    public boolean isActor() { 
        return is_actor; 
    }
    public boolean isDirector() { 
        return is_director; 
    }

    public String get_id() {
        return id;
    }

    //be able to find equalities
    public boolean equals(Person other) {
        return id.equals(other.get_id());
    }
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Person)) return false;
        return id.equals(((Person) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
