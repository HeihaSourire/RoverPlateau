package org.example.Parser;

import org.example.Exception.InputFormatException;
import org.example.Model.Command;
import org.example.Model.Direction;
import org.example.Model.Plateau;
import org.example.Model.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class InputParser {
    public record RoverPlan(Position position, Direction direction, String commands){}
    public record Mission(Plateau plateau, List<RoverPlan> roverPlans){}

    public Mission parser(Path path) throws IOException {
        if (!Files.exists(path)) throw new IOException("file not found: " + path);
        List<String> lines = Files.readAllLines(path).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (lines.isEmpty()) throw new InputFormatException("Empty input");

        //Plateau
        String[] plateauLine = lines.get(0).trim().split("\\s+");
        if (plateauLine.length != 2) throw new InputFormatException("Plateau line must be <maxX> <maxY>");
        int maxX, maxY;
        try { maxX = Integer.parseInt(plateauLine[0]); maxY = Integer.parseInt(plateauLine[1]); }
        catch (NumberFormatException e) { throw new InputFormatException("Plateau coordinates must be integers"); }
        Plateau plateau = new Plateau(maxX, maxY);

        //rovers
        List<RoverPlan> roverPlans = new ArrayList<>();
        Iterator<String> restLines = lines.subList(1, lines.size()).iterator();
        int pairIndex = 0;
        while (restLines.hasNext()) {
            pairIndex++;

            String posLine = restLines.next();
            if (!restLines.hasNext()) throw new InputFormatException("Missing commands line for rover #" + pairIndex);

            String cmdLine = restLines.next();

            String[] parts = posLine.split("\\s+");
            if (parts.length != 3)
                throw new InputFormatException("Invalid position line for rover #" + pairIndex + ": " + posLine +
                        "Rover position line must be: <x> <y> <dir>");

            int x, y;
            try {
                x = Integer.parseInt(parts[0]);
                y = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new InputFormatException("Invalid coordinates for rover #" + pairIndex + ": " + posLine);
            }
            Direction dir = Direction.fromChar(parts[2].charAt(0));

            for (char c : cmdLine.toCharArray()) Command.fromChar(c); // validate L/R/M only

            roverPlans.add(new RoverPlan(new Position(x, y), dir, cmdLine));
        }
        return new Mission(plateau, roverPlans);
    }
}
