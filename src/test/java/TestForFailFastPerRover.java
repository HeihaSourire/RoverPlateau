import org.example.Exception.InputFormatException;
import org.example.Model.*;
import org.example.Parser.InputParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestForFailFastPerRover {

    @Test
    void sampleInputProducesExpectedOutput() throws IOException {
        InputParser parser = new InputParser();
        var mission = parser.parser(Path.of("src/test/resources/sample.txt"));

        Set<Position> occupied = new HashSet<>();
        StringBuilder out = new StringBuilder();
        int id = 0;
        for (var rs : mission.roverPlans()) {
            id++;
            Rover rover = new Rover(id, rs.position(), rs.direction());
            var res = rover.executeWithFailFastPerRoverMode(rs.commands(), mission.plateau(), occupied);
            if (res instanceof ExecutionResult.Completed c) {
                occupied.add(c.position());
                out.append(c.position().x()).append(" ").append(c.position().y()).append(" ").append(c.direction().name()).append("\n");
            } else if (res instanceof ExecutionResult.Stopped s) {
                occupied.add(s.position());
                out.append(s.position().x()).append(" ").append(s.position().y()).append(" ").append(s.direction().name()).append("\n");
            }
        }
        assertEquals("1 3 N\n5 1 E\n", out.toString());
    }

    @Test
    void oobStopsCurrentRoverButNextContinues() {
        Plateau plateau = new Plateau(1, 1); // (0,0),(1,0),(0,1),(1,1)
        Set<Position> occupied = new HashSet<>();

        Rover r1 = new Rover(1, new Position(0, 0), Direction.N);
        var res1 = r1.executeWithFailFastPerRoverMode("MM", plateau, occupied); // 第二步将 OOB (0,2)
        assertInstanceOf(ExecutionResult.Stopped.class, res1);
        var s1 = (ExecutionResult.Stopped) res1;
        assertEquals(new Position(0,1), s1.position());
        assertEquals(Direction.N, s1.direction());
        assertEquals(2, s1.stepIndex());
        assertEquals(ExecutionResult.Fault.OUT_OF_BOUNDS, s1.fault());
        occupied.add(s1.position());

        // 后车继续
        Rover r2 = new Rover(2, new Position(1, 0), Direction.N);
        var res2 = r2.executeWithFailFastPerRoverMode("M", plateau, occupied);
        assertInstanceOf(ExecutionResult.Completed.class, res2);
        var c2 = (ExecutionResult.Completed) res2;
        assertEquals(new Position(1,1), c2.position());
        assertEquals(Direction.N, c2.direction());
    }

    @Test
    void movingIntoOccupiedStops() {
        Plateau plateau = new Plateau(2, 2);
        Set<Position> occupied = new HashSet<>();

        Rover r1 = new Rover(1, new Position(0,0), Direction.N);
        var c1 = (ExecutionResult.Completed) r1.executeWithFailFastPerRoverMode("M", plateau, occupied);
        occupied.add(c1.position()); // (0,1)

        Rover r2 = new Rover(2, new Position(0,0), Direction.N);
        var res2 = r2.executeWithFailFastPerRoverMode("M", plateau, occupied); // 目标 (0,1) 已占用
        assertInstanceOf(ExecutionResult.Stopped.class, res2);
        var s2 = (ExecutionResult.Stopped) res2;
        assertEquals(new Position(0,0), s2.position());
        assertEquals(Direction.N, s2.direction());
        assertEquals(ExecutionResult.Fault.OCCUPIED, s2.fault());
        assertEquals(new Position(0,1), s2.posAttempted());
    }

    @Test
    void startingOnOccupiedStopsImmediately() {
        Plateau plateau = new Plateau(2, 2);
        Set<Position> occupied = new HashSet<>();
        Rover r1 = new Rover(1, new Position(1,1), Direction.E);
        var c1 = (ExecutionResult.Completed) r1.executeWithFailFastPerRoverMode("", plateau, occupied);
        occupied.add(c1.position()); // (1,1)

        Rover r2 = new Rover(2, new Position(1,1), Direction.N);
        var res2 = r2.executeWithFailFastPerRoverMode("MMM", plateau, occupied);
        assertInstanceOf(ExecutionResult.Stopped.class, res2);
        var s2 = (ExecutionResult.Stopped) res2;
        assertEquals(0, s2.stepIndex()); // 起点即冲突
        assertEquals(ExecutionResult.Fault.OCCUPIED, s2.fault());
    }

    @Test
    void plateauZeroZeroOnlyAllowsStaying() {
        Plateau plateau = new Plateau(0, 0);
        Set<Position> occ = new HashSet<>();
        Rover r = new Rover(1, new Position(0,0), Direction.N);
        var res = r.executeWithFailFastPerRoverMode("MMMM", plateau, occ);
        assertInstanceOf(ExecutionResult.Stopped.class, res);
        var s = (ExecutionResult.Stopped) res;
        assertEquals(new Position(0,0), s.position());
        assertEquals(1, s.stepIndex());
        assertEquals(ExecutionResult.Fault.OUT_OF_BOUNDS, s.fault());
    }

    @Test
    void parserRejectsInvalidCommand() {
        InputParser parser = new InputParser();
        String content = """
                5 5
                0 0 N
                MAB
                """; // 'A','B' 非法
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }

    @Test
    void parserAcceptsLowercase() throws IOException {
        InputParser parser = new InputParser();
        String content = """
                5 5
                1 2 n
                lmlmlmlmm
                3 3 e
                mmrmmrmrrm
                """;
        var path = TestUtils.toFile(content);
        var spec = parser.parser(path);
        assertEquals(2, spec.roverPlans().size());
        assertEquals(Direction.N, spec.roverPlans().get(0).direction());
        assertEquals(Direction.E, spec.roverPlans().get(1).direction());
    }

    @Test
    void parserSkipsBlankLinesAndExtraSpaces() throws IOException {
        InputParser parser = new InputParser();
        String content = """
                5   5

                1  2    N
                LMLMLMLMM

                3 3   E
                MMRMMRMRRM
                """;
        var path = TestUtils.toFile(content);
        var spec = parser.parser(path);
        assertEquals(2, spec.roverPlans().size());
        assertEquals(5, spec.plateau().maxX());
        assertEquals(5, spec.plateau().maxY());
    }

    @Test
    void parserErrorsWhenRoverCommandsMissing() throws IOException {
        InputParser parser = new InputParser();
        String content = """
                5 5
                1 2 N
                3 3 E
                MMR
                """;
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }
}
