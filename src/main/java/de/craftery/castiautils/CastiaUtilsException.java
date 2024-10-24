package de.craftery.castiautils;

public class CastiaUtilsException extends Exception {
    public CastiaUtilsException(String message) {
        super(message);
    }

    @FunctionalInterface
    public interface CastiaExceptionRunnable {
        void run() throws CastiaUtilsException;
    }
}
