package ru.job4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class FilesFinder {
    private static final Logger LOG = LoggerFactory.getLogger(FilesFinder.class);

    public static void main(String[] args) {
        Map<String, String> arguments = checkArguments(args);
        Predicate<Path> condition = getCondition(arguments);
        List<Path> paths = getPaths(Path.of(arguments.get("d")), condition);
        savePaths(Path.of(arguments.get("o")), paths);
    }

    private static void savePaths(Path file, List<Path> paths) {
        try (PrintStream stream = new PrintStream(new FileOutputStream(String.valueOf(file)))) {
            for (Path path : paths) {
                stream.println(path);
            }
        } catch (IOException e) {
            LOG.error("Error with file to write", e);
        }
    }

    private static List<Path> getPaths(Path root, Predicate<Path> condition) {
        SearchFiles searcher = new SearchFiles(condition);
        try {
            Files.walkFileTree(root, searcher);
        } catch (IOException e) {
            LOG.error("File visitor error", e);
            throw new IllegalArgumentException(e);
        }
        return searcher.getPaths();
    }

    private static Predicate<Path> getCondition(Map<String, String> arguments) {
        return switch (arguments.get("t")) {
            case "mask" -> {
                PathMatcher maskMatcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + arguments.get("n"));
                yield path -> maskMatcher.matches(path.getFileName());
            }
            case "name" -> path -> path.toFile().getName().equals(arguments.get("n"));
            case "regex" -> {
                try {
                    Pattern regexPattern = Pattern.compile(arguments.get("n"));
                    yield path -> regexPattern.matcher(path.getFileName().toString()).matches();
                } catch (PatternSyntaxException e) {
                    LOG.error("Regex is wrong");
                    throw new IllegalArgumentException("Regex is wrong");
                }
            }
            default -> {
                LOG.error("Invalid search type");
                throw new IllegalArgumentException("Invalid search type");
            }
        };
    }

    private static Map<String, String> checkArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        if (args.length != 4) {
            LOG.error("Four arguments required");
            throw new IllegalArgumentException("Four arguments required");
        }

        String[] pair = checkArgument(args[0]);
        if (!"-d".equals(pair[0])) {
            LOG.error("Wrong first argument");
            throw new IllegalArgumentException("Wrong first argument");
        }
        Path directory = Paths.get(pair[1]);
        if (!Files.exists(directory)) {
            LOG.error("Not exist %s - {}", directory.toAbsolutePath());
            throw new IllegalArgumentException(String.format("Not exist %s",
                    directory.toAbsolutePath()));
        }
        if (!Files.isDirectory(directory)) {
            LOG.error("{} - is not directory", directory.toAbsolutePath());
            throw new IllegalArgumentException(String.format("%s - is not directory",
                    directory.toAbsolutePath()));
        }
        arguments.put(pair[0].substring(1), pair[1]);

        pair = checkArgument(args[1]);
        if (!"-n".equals(pair[0])) {
            LOG.error("Wrong second argument");
            throw new IllegalArgumentException("Wrong second argument");
        }
        arguments.put(pair[0].substring(1), pair[1]);

        pair = checkArgument(args[2]);
        if (!"-t".equals(pair[0])) {
            LOG.error("Wrong third argument");
            throw new IllegalArgumentException("Wrong third argument");
        }
        Set<String> types = Set.of("mask", "name", "regex");
        if (!types.contains(pair[1])) {
            LOG.error("{} - wrong type of search", pair[1]);
            throw new IllegalArgumentException(String.format("%s - wrong type of search",
                    pair[1]));
        }
        arguments.put(pair[0].substring(1), pair[1]);

        pair = checkArgument(args[3]);
        if (!"-o".equals(pair[0])) {
            LOG.error("Wrong fourth argument");
            throw new IllegalArgumentException("Wrong fourth argument");
        }
        Path file = Paths.get(pair[1]);
        if (!Files.exists(file)) {
            LOG.error("Not exist - {}", file.toAbsolutePath());
            throw new IllegalArgumentException(String.format("Not exist %s",
                    file.toAbsolutePath()));
        }
        if (!Files.isRegularFile(file)) {
            LOG.error("{} - is not file", file.toAbsolutePath());
            throw new IllegalArgumentException(String.format("%s - is not file",
                    file.toAbsolutePath()));
        }
        arguments.put(pair[0].substring(1), pair[1]);

        return arguments;
    }

    private static String[] checkArgument(String arg) {
        if (!arg.startsWith("-")) {
            LOG.error("{} - argument should starts with -", arg);
            throw new IllegalArgumentException(String.format("%s - argument should starts with -",
                    arg));
        }
        if (!arg.contains("=")) {
            LOG.error("{} - argument should contains =", arg);
            throw new IllegalArgumentException(String.format("%s - argument should contains =",
                    arg));
        }
        String[] pair = arg.split("=", 2);
        if (pair[0] == null || pair[0].isEmpty()) {
            LOG.error("Absent key in argument - {}", arg);
            throw new IllegalArgumentException(String.format("Absent key in argument - %s",
                    arg));
        }
        if (pair[0].length() < 2) {
            LOG.error("Short key in pair - {}", arg);
            throw new IllegalArgumentException(String.format("Short key in pair - %s", arg));
        }
        if (pair[1] == null || pair[1].isEmpty()) {
            LOG.error("Absent value in argument - {}", arg);
            throw new IllegalArgumentException(String.format("Absent value in argument - %s",
                    arg));
        }
        return pair;
    }
}
