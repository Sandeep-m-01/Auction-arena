package com.sandeep.auctionarena;

public class Player {

    private int id;
    private String name;
    private String position;
    private String type;
    private String image;

    // Required empty constructor
    public Player() {
    }

    public Player(
            int id,
            String name,
            String position,
            String type,
            String image
    ) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.type = type;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}