package org.example.Model;

import java.util.Set;

public class Rover {
    private final int roverId;
    private Position position;
    private Direction direction;


    public Rover(int roverId, Position position, Direction direction) {
        this.roverId = roverId;
        this.position = position;
        this.direction = direction;
    }

    public int roverId() {
        return roverId;
    }

    public Position getPosition() {
        return position;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return position.x() + " " + position.y() + " " + direction.name();
    }

    /**
     * Execute commands in fail-fast-per-rover mode.
     * @param commands command string
     * @param plateau bounds
     * @param occupied final positions of completed/stopped rovers (no entry allowed)
     */
    public ExecutionResult executeWithFailFastPerRoverMode(String commands, Plateau plateau, Set<Position> occupied) {
        //Initial position is an occupied cell => Stop immediately
        if (occupied.contains(position)) {
            return new ExecutionResult.Stopped(roverId, position, direction,
                    ExecutionResult.Fault.OCCUPIED, 0, '-', position);
        }
        int step = 0;
        for (char c : commands.toCharArray()) {
            step++;
            Command cmd = Command.fromChar(c);
            switch (cmd) {
                case L -> direction = direction.left();
                case R -> direction = direction.right();
                case M -> {
                    Position next = position.translate(direction);
                    if (!plateau.isBounds(next)) {
                        return new ExecutionResult.Stopped(roverId, position, direction,
                                ExecutionResult.Fault.OUT_OF_BOUNDS, step, 'M', next);
                    }
                    if (occupied.contains(next)) {
                        return new ExecutionResult.Stopped(roverId, position, direction,
                                ExecutionResult.Fault.OCCUPIED, step, 'M', next);
                    }
                    position = next;
                }
            }
        }
        return new ExecutionResult.Completed(roverId, position, direction);
    }
}
