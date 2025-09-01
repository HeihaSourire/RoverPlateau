package org.example.Model;

public interface ExecutionResult {
    int roverId();
    Position position();
    Direction direction();

    record Completed(int roverId, Position position, Direction direction) implements ExecutionResult {}
    record Stopped(int roverId, Position position, Direction direction,
                   Fault fault, int stepIndex, char commandTried, Position posAttempted) implements ExecutionResult {}
    enum Fault {OUT_OF_BOUNDS, OCCUPIED}
}
