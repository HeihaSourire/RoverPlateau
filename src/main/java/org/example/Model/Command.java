package org.example.Model;

import org.example.Exception.InputFormatException;

public enum Command {
    L, R, M;
    public static Command fromChar(char c) throws InputFormatException {
        return switch (Character.toUpperCase(c)) {
            case 'L' -> L; case 'R' -> R; case 'M' -> M;
            default -> throw new InputFormatException("Invalid command: " + c);
        };
    }
}
