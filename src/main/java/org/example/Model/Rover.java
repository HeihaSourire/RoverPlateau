package org.example.Model;

import org.example.log.Event;

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

    /**
     * Execute commands in fail-fast-per-rover mode.
     * @param commands command string
     * @param plateau bounds
     */
    public ExecutionResult executeWithFailFastPerRoverMode(String commands, Plateau plateau, Event logger) {
        logger.info("ROVER_START","start rover","roverId",roverId,"pos",position,"dir",direction);

        //Initial position is an occupied cell => Stop immediately
        if (plateau.isOccupied(position)) {
            var res = new ExecutionResult.Stopped(roverId, position, direction,
                    ExecutionResult.Fault.OCCUPIED, 0, '-', position);
            logger.warn("FAULT","start on occupied","roverId",roverId,"fault",res.fault(),"step",0,"attempted",position);
//            logger.info("ROVER_STOPPED","stopped","roverId",id,"pos",position,"dir",direction);
            return res;
        }

        int step = 0;
        for (char c : commands.toCharArray()) {
            step++;
            Command cmd = Command.fromChar(c);
            switch (cmd) {
                case L -> {
                    direction = direction.left();
                    logger.info("TURN","turn left","roverId",roverId,"step",step,"dir",direction);
                }
                case R -> {
                    direction = direction.right();
                    logger.info("TURN","turn right","roverId",roverId,"step",step,"dir",direction);
                }
                case M -> {
                    Position next = position.translate(direction);
                    logger.info("MOVE_ATTEMPT","move","roverId",roverId,"step",step,"from",position,"to",next,"dir",direction);

                    if (!plateau.isBounds(next)) {
                        var res = new ExecutionResult.Stopped(roverId, position, direction,
                                ExecutionResult.Fault.OUT_OF_BOUNDS, step, 'M', next);
                        logger.warn("FAULT","out of bounds","roverId",roverId,"fault",res.fault(),"step",step,"attempted",next);
                        plateau.occupy(position);
                        return res;
                    }
                    if (plateau.isOccupied(next)) {
                        var res = new ExecutionResult.Stopped(roverId, position, direction,
                                ExecutionResult.Fault.OCCUPIED, step, 'M', next);
                        logger.warn("FAULT","cell occupied","roverId",roverId,"fault",res.fault(),"step",step,"attempted",next);
                        plateau.occupy(position);
                        return res;
                    }
                    position = next;
                    logger.info("MOVE_OK","moved","roverId",roverId,"step",step,"pos",position,"dir",direction);
                }
            }
        }

        var res = new ExecutionResult.Completed(roverId, position, direction);
        logger.info("ROVER_COMPLETED","completed","roverId",roverId,"pos",position,"dir",direction);
        plateau.occupy(position);
        return res;
    }
}
