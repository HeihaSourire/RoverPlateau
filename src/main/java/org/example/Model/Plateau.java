package org.example.Model;

import org.example.Exception.InputFormatException;

import java.util.HashSet;
import java.util.Set;

public final class Plateau {
    private final int maxX, maxY;
    private final Set<Position> occupied = new HashSet<>();
    public Plateau(int maxX, int maxY) throws InputFormatException {
        if (maxX < 0 || maxY < 0) {
            throw new InputFormatException("Plateau size must be non-negative");
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean isBounds(Position p){
        return p.x() >= 0 && p.x() <= maxX && p.y() >= 0 && p.y() <=maxY;
    }

    public int maxX(){return maxX;}
    public int maxY(){return maxY;}

    public boolean isOccupied(Position p) {
        return occupied.contains(p);
    }

    public void occupy(Position p) {
        occupied.add(p);
    }

}
