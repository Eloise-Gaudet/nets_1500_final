package graph_structures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author eloisegaudet
 */
public class Title implements Serializable {

    //name and unique identifier
    private String title;
    private String id;

    //title information
    private int type; //0 = movie, 1 = show 
    private int start_year;
    private int end_year;
    private String[] genres;

    //crew information
    private ArrayList<Person> directors;
    private HashMap<Person, String> actors; //person and character they played

    //ratings information
    private int num_ratings;
    private int avg_rating;

    public Title(String title, String id, int type, int start_year, int end_year, String genres) {
        this.title = title;
        this.id = id;
        this.type = type;
        this.start_year = start_year;
        this.end_year = end_year;
        this.genres = genres.split(",");

        directors = new ArrayList<>();
        actors = new HashMap<>();
    }

    public void add_director(Person director) {
        directors.add(director);
    }

    public void add_actor(Person actor, String character) {
        actors.put(actor, character);
    }

    public void add_ratings(int num_ratings, int avg_rating) {
        this.num_ratings = num_ratings;
        this.avg_rating = avg_rating;
    }

    public ArrayList<Person> get_directors() {
        return directors;
    }

    public ArrayList<Person> get_actors() {
        ArrayList<Person> just_actors = new ArrayList<>(actors.keySet());
        return just_actors;
    }

    public String get_id() {
        return id;
    }

    public boolean equals(Title other) {
        return id.equals(other.get_id());
    }

}
