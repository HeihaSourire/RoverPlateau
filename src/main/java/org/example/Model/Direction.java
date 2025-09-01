package org.example.Model;

import org.example.Exception.InputFormatException;

public enum Direction {
    N(0, 1), E(1, 0), S(0, -1), W(-1, 0);
    private final int dx, dy;
    Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }


    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public Direction left(){
        return switch (this) {
            case N -> W;
            case E -> N;
            case S -> E;
            case W -> S;
            //default throw new InputFormatException
        };
    }

    public Direction right(){
        return switch (this) {
            case N -> E;
            case E -> S;
            case S -> W;
            case W -> N;
            //default throw new InputFormatException
        };
    }

    public static Direction fromChar(char c) throws InputFormatException {
        return switch (Character.toUpperCase(c)) {
            case 'N' -> N;
            case 'E' -> E;
            case 'S' -> S;
            case 'W' -> W;
            default -> throw new InputFormatException("Invalid direction" + c);
        };
    }
}
