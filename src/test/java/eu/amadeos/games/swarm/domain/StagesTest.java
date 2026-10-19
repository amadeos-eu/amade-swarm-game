package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.data.TextData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagesTest {
    private static Stages read(String text) {
        return new Stages(TextData.parse("stages.txt", new StringReader(text)));
    }

    private static String error(String text) {
        return assertThrows(TextData.DataException.class, () -> read(text)).getMessage();
    }

    @Test
    void theStandardStagesFillTheFormation() {
        Stages stages = Stages.STANDARD;
        assertEquals(3, stages.count());
        for (int number = 1; number <= 2; number++) {
            Stages.Plan plan = stages.plan(number);
            assertFalse(plan.challenge());
            Set<Slot> slots = new HashSet<>();
            plan.groups().forEach(group -> group.members().forEach(member -> slots.add(member.slot())));
            assertEquals(40, slots.size(), "stage " + number);
        }
        Stages.Plan challenge = stages.plan(3);
        assertTrue(challenge.challenge());
        assertEquals(40, challenge.insects());
        challenge.groups().forEach(group -> group.members().forEach(member -> assertNull(member.slot())));
        assertEquals(stages.plan(1), stages.plan(4), "then again from the start");
        for (Stages.Group group : challenge.groups()) {
            var end = group.path().end();
            assertTrue(end.x() < 0 || end.x() > Field.WIDTH || end.y() < 0 || end.y() > Field.HEIGHT,
                    "a challenge path leads out of the field");
        }
    }

    // --8<-- [start:test-stages]
    @Test
    void mistakesInTheFileNameTheirLine() {
        String path = """
                path down     # line 1
                from 0 0
                line 10 10
                stage
                """;
        assertEquals("stages.txt:5: \"swoop\" is not a path", error(path + "group 0 swoop bee@1,1"));
        assertEquals("stages.txt:5: \"wasp\" is not an insect", error(path + "group 0 down wasp@1,1"));
        assertEquals("stages.txt:5: no slot 10,0 in the formation", error(path + "group 0 down bee@10,0"));
        assertEquals("stages.txt:6: slot 1,1 is taken twice",
                error(path + "group 0 down bee@1,1\ngroup 1 down bee@1,1"));
        assertEquals("stages.txt:5: \"soon\" is not a number", error(path + "group soon down bee"));
    }
    // --8<-- [end:test-stages]
}
