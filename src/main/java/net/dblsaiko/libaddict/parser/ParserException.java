package net.dblsaiko.libaddict.parser;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParserException extends RuntimeException {

    public final List<Trace> trace;

    public final ErrorType error;
    public final String note;

    private ParserException(List<Trace> trace, ErrorType error, String note) {
        this.trace = trace;
        this.error = error;
        this.note = note;
    }

    public static ParserException of(ErrorType error, String note, Identifier sourceFile, int line, String lineSrc) {
        return new ParserException(Collections.singletonList(new Trace(sourceFile, line, lineSrc)), error, note);
    }

    public static ParserException propagate(ParserException parent, Identifier sourceFile, int line, String lineSrc) {
        List<Trace> traces = new ArrayList<>();
        traces.addAll(parent.trace);
        traces.add(new Trace(sourceFile, line, lineSrc));
        return new ParserException(traces, parent.error, parent.note);
    }

    public static class Trace {
        public final Identifier sourceFile;
        public final int line;
        public final String lineSrc;

        public Trace(Identifier sourceFile, int line, String lineSrc) {
            this.sourceFile = sourceFile;
            this.line = line;
            this.lineSrc = lineSrc;
        }

    }

    public enum ErrorType {
        WHITESPACE("invalid whitespace"),
        MISSING_COLON("expected `:'"),
        MULTILINE("malformed multiline string"),
        MISSING_RBRACE("missing `}'"),
        INCLUDE_NOT_FOUND("file to include not found"),
        INFINITE_LOOP("recursive inclusion detected");

        public final String description;

        ErrorType(String description) {
            this.description = description;
        }
    }
}
