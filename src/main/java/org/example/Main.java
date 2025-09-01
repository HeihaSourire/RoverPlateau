package org.example;

import org.example.Exception.InputFormatException;
import org.example.Model.*;
import org.example.Parser.InputParser;

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

        //Rover execution
        Set<Position> occupied = new HashSet<>();
        int roverId = 0;
        for (InputParser.RoverPlan rsPlan : mission.roverPlans()) {
            roverId++;
            Rover rover = new Rover(roverId, rsPlan.position(), rsPlan.direction());
            ExecutionResult res = rover.executeWithFailFastPerRoverMode(rsPlan.commands(), mission.plateau(), occupied);

            if (res instanceof ExecutionResult.Completed c) {
                occupied.add(c.position()); // final cell becomes occupied
                System.out.println(c.position().x() + " " + c.position().y() + " " + c.direction().name());
            } else if (res instanceof ExecutionResult.Stopped s) {
                occupied.add(s.position()); // stopped rover still occupies its last valid position
                System.out.println(s.position().x() + " " + s.position().y() + " " + s.direction().name());
            }
        }
    }

}