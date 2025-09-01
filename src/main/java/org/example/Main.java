package org.example;

import org.example.Exception.InputFormatException;
import org.example.Model.*;
import org.example.Parser.InputParser;
import org.example.log.Event;
import org.example.log.EventLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Entry point.
 * Usage: java -jar rover.jar input.txt
 * TODO: [--mode=fail-fast|fail-fast-per-rover|skip-step]
 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java -jar rover.jar <input.txt>");
            System.exit(1);
        }

        Path inputFile = Path.of(args[0]);
        InputParser parser = new InputParser();
        InputParser.Mission mission = null;
        try {
            mission = parser.parser(inputFile);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(1);
        } catch (InputFormatException e) {
            System.err.println("Input error: " + e.getMessage());
            System.exit(2);
        }

        //Rovers execution
        try (Event logger = new EventLogger()){
            logger.info("RUN_START","begin","maxX",mission.plateau().maxX(),"maxY",mission.plateau().maxY());
            int roverId = 0;
            for (InputParser.RoverPlan rsPlan : mission.roverPlans()) {
                roverId++;
                Rover rover = new Rover(roverId, rsPlan.position(), rsPlan.direction());
                ExecutionResult res = rover.executeWithFailFastPerRoverMode(rsPlan.commands(), mission.plateau(), logger);

                if (res instanceof ExecutionResult.Completed c) {
//                    mission.plateau().occupy(c.position()); // final cell becomes occupied
                    logger.info("ROVER_COMPLETED","completed","roverId",c.roverId(),"pos",c.position(),"dir",c.direction());
                    System.out.println(c.position().x() + " " + c.position().y() + " " + c.direction().name());
                } else if (res instanceof ExecutionResult.Stopped s) {
//                    mission.plateau().occupy(s.position()); // stopped rover still occupies its last valid position
                    logger.info("ROVER_STOPPED","stopped","roverId",s.roverId(),"pos",s.position(),"dir",s.direction());
                    System.out.println(s.position().x() + " " + s.position().y() + " " + s.direction().name());
                }
            }
            logger.info("RUN_END","end");
        }
        System.exit(0);
    }

}