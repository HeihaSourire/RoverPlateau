package org.example.Model;

public record Position(int x, int y) {
    public Position translate(Direction d) {
        return new Position(x + d.dx(), y + d.dy());
    }

    @Override
    public String toString() {
        return x + " " + y;
    }
}
