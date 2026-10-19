package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.data.TextData.Line;
import eu.amadeos.gameengine.motion.FlightPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stages of Swarm, read from {@code stages.txt}: the flight paths, and for each stage the groups of
 * insects that fly in along them. After the last stage in the file, the first comes again, faster.
 */
public final class Stages {
    /** One insect of a group: its kind, and its slot in the formation, or {@code null} to fly through. */
    public record Member(Kind kind, Slot slot) {
    }

    /** Insects that fly in one after the other along the same path, starting {@code time} seconds into the stage. */
    public record Group(double time, FlightPath path, List<Member> members) {
    }

    /** A stage: its groups, and whether it is a challenge, with nothing to fear and a bonus to earn. */
    public record Plan(boolean challenge, List<Group> groups) {
        public int insects() {
            return groups.stream().mapToInt(group -> group.members().size()).sum();
        }
    }

    /** The stages that come with the game. */
    public static final Stages STANDARD = new Stages(TextData.load(Stages.class, "/eu/amadeos/games/swarm/stages.txt"));

    private final Map<String, FlightPath> paths = new HashMap<>();
    private final List<Plan> plans = new ArrayList<>();

    // --8<-- [start:parse]
    /** Reads the lines of a stage file; a mistake in it is an error naming its line. */
    public Stages(List<Line> lines) {
        String pathName = null;
        FlightPath.Builder path = null;
        Line pathLine = null;
        List<Group> groups = null;
        boolean challenge = false;
        Set<Slot> taken = new HashSet<>();
        for (Line line : lines) {
            switch (line.keyword()) {
                case "path" -> {
                    finishPath(pathName, path, pathLine);
                    pathName = line.word(1);
                    path = null;
                    pathLine = line;
                }
                case "from" -> path = FlightPath.from(line.number(1), line.number(2));
                case "curve" -> pathBuilder(path, line).curveTo(line.number(1), line.number(2), line.number(3),
                        line.number(4), line.number(5), line.number(6));
                case "line" -> pathBuilder(path, line).lineTo(line.number(1), line.number(2));
                case "mirror" -> {
                    finishPath(pathName, path, pathLine);
                    pathName = null;
                    paths.put(line.word(1), path(line, line.word(2)).mirrored(Field.WIDTH));
                }
                case "stage" -> {
                    finishPath(pathName, path, pathLine);
                    pathName = null;
                    if (groups != null) plans.add(new Plan(challenge, List.copyOf(groups)));
                    groups = new ArrayList<>();
                    challenge = line.size() > 1 && line.word(1).equals("challenge");
                    taken.clear();
                }
                case "group" -> {
                    if (groups == null) throw line.error("a group belongs to a stage");
                    List<Member> members = new ArrayList<>();
                    for (int i = 3; i < line.size(); i++) members.add(member(line, line.word(i), taken));
                    if (members.isEmpty()) throw line.error("a group without insects");
                    groups.add(new Group(line.number(1), path(line, line.word(2)), List.copyOf(members)));
                }
                default -> throw line.error("unknown: \"" + line.keyword() + '"');
            }
        }
        finishPath(pathName, path, pathLine);
        if (groups != null) plans.add(new Plan(challenge, List.copyOf(groups)));
        if (plans.isEmpty()) throw new TextData.DataException("no stages");
    }

    /** One insect: {@code bee} flies through, {@code bee@4,3} takes the slot in column 4, row 3. */
    private static Member member(Line line, String word, Set<Slot> taken) {
        String[] parts = word.split("@");
        Kind kind = Kind.named(parts[0]);
        if (kind == null) throw line.error('"' + parts[0] + "\" is not an insect");
        if (parts.length == 1) return new Member(kind, null);
        String[] place = parts[1].split(",");
        try {
            Slot slot = new Slot(Integer.parseInt(place[0]), Integer.parseInt(place[1]));
            if (slot.column() < 0 || slot.column() >= Field.COLUMNS || slot.row() < 0 || slot.row() >= Field.ROWS) {
                throw line.error("no slot " + parts[1] + " in the formation");
            }
            if (!taken.add(slot)) throw line.error("slot " + parts[1] + " is taken twice");
            return new Member(kind, slot);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw line.error('"' + word + "\" is not kind@column,row");
        }
    }
    // --8<-- [end:parse]

    private FlightPath path(Line line, String name) {
        FlightPath path = paths.get(name);
        if (path == null) throw line.error('"' + name + "\" is not a path");
        return path;
    }

    private static FlightPath.Builder pathBuilder(FlightPath.Builder path, Line line) {
        if (path == null) throw line.error("a path starts with \"from\"");
        return path;
    }

    private void finishPath(String name, FlightPath.Builder path, Line line) {
        if (name == null) return;
        if (path == null) throw line.error("the path \"" + name + "\" has no points");
        try {
            paths.put(name, path.build());
        } catch (IllegalArgumentException e) {
            throw line.error("the path \"" + name + "\" has no curves or lines");
        }
    }

    /** The plan of stage {@code number}, from 1; after the last, the first again. */
    public Plan plan(int number) {
        return plans.get((number - 1) % plans.size());
    }

    public int count() {
        return plans.size();
    }

    public FlightPath path(String name) {
        return paths.get(name);
    }
}
