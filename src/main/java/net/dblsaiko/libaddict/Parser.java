package net.dblsaiko.libaddict;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dblsaiko.libaddict.ParserException.ErrorType;
import net.dblsaiko.libaddict.ParserException.Trace;

public class Parser {
    public static Map<String, ParameterizedString> include(ResourceManager container, Identifier id) {
        try {
            return include(container, id, new State());
        } catch (IOException e) {
            return Collections.emptyMap();
        } catch (ParserException e) {
            Logger logger = LogManager.getLogger("libaddict");
            List<Trace> trace = e.trace;
            Trace first = trace.get(0);
            logger.log(Level.ERROR, "error: {}", e.error.description);
            int alignSpace = trace.stream().mapToInt(i -> Integer.toString(i.line + 1).length()).max().getAsInt();
            String filler = String.join("", Collections.nCopies(alignSpace, " "));
            for (int i = 0; i < trace.size(); i++) {
                Trace t = trace.get(i);
                if (i == 0) {
                    logger.log(Level.ERROR, "  at {}:{}", t.sourceFile, t.line + 1);
                } else {
                    logger.log(Level.ERROR, "  (included from {}:{})", t.sourceFile, t.line + 1);
                }
            }
            String lineNo = Integer.toString(first.line + 1);
            lineNo = String.join("", Collections.nCopies(alignSpace - lineNo.length(), " ")) + lineNo;
            logger.log(Level.ERROR, "{} |", filler);
            logger.log(Level.ERROR, "{} | {}", lineNo, first.lineSrc);
            logger.log(Level.ERROR, "{} | ^", filler);
            if (!e.note.isEmpty()) {
                logger.log(Level.ERROR, "note: {}", e.note);
            }
            return Collections.emptyMap();
        }
    }

    private static Map<String, ParameterizedString> include(ResourceManager container, Identifier id, State state) throws IOException {
        List<Resource> resources = container.getAllResources(id);

        return resources.stream().flatMap(res -> {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getInputStream()))) {
                String s;
                while ((s = reader.readLine()) != null) lines.add(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return parseInner(lines, container, id, state).entrySet().stream();
        }).collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }

    private static Map<String, ParameterizedString> parseInner(List<String> source, ResourceManager container, Identifier id, State state) {
        state.previousIncludes.add(id);
        Map<String, ParameterizedString> result = new HashMap<>();
        Pair<String, List<String>> multiline = null;
        for (int i = 0; i < source.size(); i++) {
            String lineSrc = source.get(i);
            String line = lineSrc.replaceAll("\\s*$", "");
            if (multiline != null && !line.isEmpty()) {
                char c = line.charAt(0);
                if (!Character.isWhitespace(c)) {
                    result.put(multiline.getLeft(), getParameterizedString(normalizeMultiline(multiline.getRight(), id, i, lineSrc), id, i, lineSrc));
                    multiline = null;
                }
            }

            if (multiline != null) {
                multiline.getRight().add(line);
            } else {
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }

                line = stripTrailing(line);
                if (line.isEmpty()) continue;
                if (Character.isWhitespace(line.charAt(0))) throw ParserException.of(ErrorType.WHITESPACE, "", id, i, lineSrc);
                String nextToken = takeWhile(line, c -> !Character.isWhitespace(c));
                switch (nextToken) {
                    case "include":
                        String includedFile = line.substring(nextToken.length()).trim();
                        Identifier includedPath = resolve(id, includedFile);
                        if (state.previousIncludes.contains(includedPath)) {
                            throw ParserException.of(ErrorType.INFINITE_LOOP, "", id, i, lineSrc);
                        }
                        try {
                            result.putAll(include(container, includedPath, state));
                        } catch (ParserException e) {
                            throw ParserException.propagate(e, id, i, lineSrc);
                        } catch (IOException e) {
                            throw ParserException.of(ErrorType.INCLUDE_NOT_FOUND, String.format("tried looking at %s", includedPath), id, i, lineSrc);
                        }
                        break;
                    // TODO: redesign this
                    // case "prefix":
                    //     state.prefix = line.substring(nextToken.length()).trim();
                    //     break;
                    // case "suffix":
                    //     state.suffix = line.substring(nextToken.length()).trim();
                    //     break;
                    default:
                        String[] arr = line.split(":", 2);
                        if (arr.length != 2) throw ParserException.of(ErrorType.MISSING_COLON, "", id, i, lineSrc);
                        String key = state.prefix + arr[0] + state.suffix;
                        if (arr[1].trim().isEmpty()) {
                            multiline = new Pair<>(key, new ArrayList<>());
                        } else {
                            result.put(key, getParameterizedString(arr[1].trim(), id, i, lineSrc));
                        }
                        break;
                }
            }
        }

        if (multiline != null) {
            int line = source.size() - 1;
            String lineSrc = source.get(line);
            result.put(multiline.getLeft(), getParameterizedString(normalizeMultiline(multiline.getRight(), id, line, lineSrc), id, line, lineSrc));
        }

        state.previousIncludes.remove(id);

        return result;
    }

    private static ParameterizedString getParameterizedString(String s, Identifier id, int lineno, String lineSrc) {
        try {
            return ParameterizedString.from(s);
        } catch (IllegalStateException e) {
            throw ParserException.of(ErrorType.MISSING_RBRACE, "", id, lineno, lineSrc);
        }
    }

    private static String rdropWhile(String s, CharPredicate op) {
        int idx = s.length() - 1;
        while (idx >= 0 && op.test(s.charAt(idx))) idx -= 1;
        return s.substring(0, idx + 1);
    }

    private static String takeWhile(String s, CharPredicate op) {
        int idx = 0;
        while (idx < s.length() && op.test(s.charAt(idx))) idx += 1;
        return s.substring(0, idx);
    }

    private static String stripTrailing(String s) {
        return rdropWhile(s, Character::isWhitespace);
    }

    private static String takeWhitespace(String s) {
        return takeWhile(s, Character::isWhitespace);
    }

    private static String normalizeMultiline(List<String> lines, Identifier id, int lineno, String lineSrc) {
        Optional<String> commonIndent = fold(lines.stream().skip(1)::iterator, Optional.of(takeWhitespace(lines.get(0))), Parser::findCommonIndent);

        if (!commonIndent.isPresent()) throw ParserException.of(ErrorType.MULTILINE, "", id, lineno, lineSrc);
        int len = commonIndent.get().length();
        return lines.stream().map(s -> s.trim().isEmpty() ? "" : s.substring(len)).collect(Collectors.joining("\n"));
    }

    private static Optional<String> findCommonIndent(Optional<String> prevWhitespace, String s) {
        return prevWhitespace.flatMap(s1w -> {
            String s2w = takeWhitespace(s);
            if (s.trim().isEmpty()) return prevWhitespace;

            int ldiff = s1w.length() - s2w.length();
            if (ldiff == 0) {
                return s1w.equals(s2w) ? Optional.of(s1w) : Optional.empty();
            } else if (ldiff < 0) {
                return s2w.startsWith(s1w) ? Optional.of(s1w) : Optional.empty();
            } else {
                return s1w.startsWith(s2w) ? Optional.of(s2w) : Optional.empty();
            }
        });
    }

    private static <R, T> R fold(Iterable<T> iterable, R identity, BiFunction<R, ? super T, R> accumulator) {
        R state = identity;
        for (T t : iterable) {
            state = accumulator.apply(state, t);
        }
        return state;
    }

    public static Identifier resolve(Identifier currentFile, String path) {
        if (path.contains(":")) return new Identifier(path);
        List<String> currentPath = new ArrayList<>(Arrays.asList(currentFile.getPath().split("/+")));
        currentPath.remove(currentPath.size() - 1);

        List<String> newPath = new ArrayList<>(Arrays.asList(path.split("/+")));
        if (newPath.size() == 0) {
            currentPath = Collections.emptyList();
        } else if ("".equals(newPath.get(0)) && newPath.size() > 1) {
            // absolute path: '/foo/bar'
            currentPath = newPath;
            currentPath.remove(0);
        } else {
            // relative path: 'foo/bar'
            currentPath.addAll(newPath);
        }

        for (int i = 0; i < currentPath.size(); i++) {
            String part = currentPath.get(i);
            if (".".equals(part)) {
                currentPath.remove(i);
                i -= 1;
            } else if ("..".equals(part)) {
                currentPath.remove(i);
                if (i > 0) {
                    currentPath.remove(i - 1);
                    i -= 1;
                }
                i -= 1;
            }
        }

        return new Identifier(currentFile.getNamespace(), String.join("/", currentPath));
    }

    static class State {
        String prefix = "";
        String suffix = "";
        Set<Identifier> previousIncludes = new HashSet<>();
    }
}
