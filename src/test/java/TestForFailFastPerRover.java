import org.example.Exception.InputFormatException;
import org.example.Model.*;
import org.example.Parser.InputParser;
import org.example.log.Event;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestForFailFastPerRover {
    //Logger for tests
    static final class CaptureLogger implements Event {
        static record Entry(String level, String type) {}
        final List<Entry> entries = new ArrayList<>();
        @Override public void info(String type, String message, Object... kv) { entries.add(new Entry("INFO", type)); }
        @Override public void warn(String type, String message, Object... kv) { entries.add(new Entry("WARNING", type)); }
        @Override public void close() {}
    }

    @Test
    void given_sampleInputValid_when_run_then_matchExpectedPositions() throws IOException {
        InputParser parser = new InputParser();
        var mission = parser.parser(Path.of("src/test/resources/sample.txt"));

        CaptureLogger logger = new CaptureLogger();
        StringBuilder out = new StringBuilder();
        int id = 0;
        for (var rs : mission.roverPlans()) {
            id++;
            Rover rover = new Rover(id, rs.position(), rs.direction());
            var res = rover.executeWithFailFastPerRoverMode(rs.commands(), mission.plateau(), logger);
            if (res instanceof ExecutionResult.Completed c) {
                out.append(c.position().x()).append(" ").append(c.position().y()).append(" ").append(c.direction().name()).append("\n");
            } else if (res instanceof ExecutionResult.Stopped s) {
                out.append(s.position().x()).append(" ").append(s.position().y()).append(" ").append(s.direction().name()).append("\n");
            }
        }
        assertEquals("1 3 N\n5 1 E\n", out.toString());
    }

    @Test
    void given_nextMoveOutOfBounds_when_execute_then_stopThisRoverOnly() {
        Plateau plateau = new Plateau(1, 1); // (0,0),(1,0),(0,1),(1,1)
        CaptureLogger logger = new CaptureLogger();

        Rover r1 = new Rover(1, new Position(0, 0), Direction.N);
        var res1 = r1.executeWithFailFastPerRoverMode("MM", plateau, logger); // 第二步将 OOB (0,2)
        assertInstanceOf(ExecutionResult.Stopped.class, res1);
        var s1 = (ExecutionResult.Stopped) res1;
        assertEquals(new Position(0,1), s1.position());
        assertEquals(Direction.N, s1.direction());
        assertEquals(2, s1.stepIndex());
        assertEquals(ExecutionResult.Fault.OUT_OF_BOUNDS, s1.fault());
        assertTrue(plateau.isOccupied(new Position(0,1)));

        // Following rovers continue
        Rover r2 = new Rover(2, new Position(1, 0), Direction.N);
        var res2 = r2.executeWithFailFastPerRoverMode("M", plateau, logger);
        assertInstanceOf(ExecutionResult.Completed.class, res2);
        var c2 = (ExecutionResult.Completed) res2;
        assertEquals(new Position(1,1), c2.position());
        assertEquals(Direction.N, c2.direction());
    }

    @Test
    void given_nextCellOccupied_when_move_then_stopCurrentRover() {
        Plateau plateau = new Plateau(2, 2);
        CaptureLogger logger = new CaptureLogger();

        Rover r1 = new Rover(1, new Position(0,0), Direction.N);
        var c1 = (ExecutionResult.Completed) r1.executeWithFailFastPerRoverMode("M", plateau, logger);
        assertEquals(new Position(0,1), c1.position());
        assertTrue(plateau.isOccupied(new Position(0,1)));

        Rover r2 = new Rover(2, new Position(0,0), Direction.N);
        var res2 = r2.executeWithFailFastPerRoverMode("M", plateau, logger); // 目标 (0,1) 已占用
        assertInstanceOf(ExecutionResult.Stopped.class, res2);
        var s2 = (ExecutionResult.Stopped) res2;
        assertEquals(new Position(0,0), s2.position());
        assertEquals(Direction.N, s2.direction());
        assertEquals(ExecutionResult.Fault.OCCUPIED, s2.fault());
        assertEquals(new Position(0,1), s2.posAttempted());
    }

    @Test
    void given_startOnOccupied_when_execute_then_stopBeforeFirstStep() {
        Plateau plateau = new Plateau(2, 2);
        CaptureLogger logger = new CaptureLogger();
        Rover r1 = new Rover(1, new Position(1,1), Direction.E);
        var c1 = (ExecutionResult.Completed) r1.executeWithFailFastPerRoverMode("", plateau, logger);
        assertTrue(plateau.isOccupied(new Position(1,1)));

        Rover r2 = new Rover(2, new Position(1,1), Direction.N);
        var res2 = r2.executeWithFailFastPerRoverMode("MMM", plateau, logger);
        assertInstanceOf(ExecutionResult.Stopped.class, res2);
        var s2 = (ExecutionResult.Stopped) res2;
        assertEquals(0, s2.stepIndex());
        assertEquals(ExecutionResult.Fault.OCCUPIED, s2.fault());
    }

    @Test
    void given_plateauZeroZero_when_move_then_outOfBoundsAtStep1() {
        Plateau plateau = new Plateau(0, 0);
        CaptureLogger logger = new CaptureLogger();

        Rover r = new Rover(1, new Position(0,0), Direction.N);
        var res = r.executeWithFailFastPerRoverMode("MMMM", plateau, logger);
        assertInstanceOf(ExecutionResult.Stopped.class, res);
        var s = (ExecutionResult.Stopped) res;
        assertEquals(new Position(0,0), s.position());
        assertEquals(1, s.stepIndex());
        assertEquals(ExecutionResult.Fault.OUT_OF_BOUNDS, s.fault());
        assertTrue(plateau.isOccupied(new Position(0,0)));
    }

    @Test
    void given_invalidCommandChar_when_parse_then_throw() {
        InputParser parser = new InputParser();
        String content = """
                5 5
                0 0 N
                MAB
                """;
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }

    @Test
    void given_lowercaseInput_when_parse_then_normalizedToUppercaseDirections() throws IOException {
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
    void given_blankLinesAndExtraSpaces_when_parse_then_ok() throws IOException {
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
    void given_missingCommandsLine_when_parse_then_throw() throws IOException {
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

    @Test
    void given_positionLineLengthNotThree_when_parse_then_throwInvalidPositionLine() {
        InputParser parser = new InputParser();
        String content = """
                5 5
                1 2
                MM
                """;
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }

    @Test
    void given_positionCoordinatesNotIntegers_when_parse_then_throwInvalidCoordinates() {
        InputParser parser = new InputParser();
        String content = """
                5 5
                a b N
                M
                """;
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }

    @Test
    void given_plateauBothBoundsNegative_when_parse_then_throwIllegalArgument() {
        InputParser parser = new InputParser();
        String content = """
                -1 -1
                0 0 N
                M
                """;
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }

    @Test
    void given_invalidDirectionChar_when_parse_then_throwInvalidHeading() {
        InputParser parser = new InputParser();
        String content = """
                5 5
                1 2 A
                M
                """;
        var path = TestUtils.toFile(content);
        assertThrows(InputFormatException.class, () -> parser.parser(path));
    }
}
