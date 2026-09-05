package anaconda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import anaconda.testutil.ConsoleSession;

/**
 * Tests application orchestration with isolated console streams and temporary saved-task files.
 */
@Timeout(15)
@ResourceLock("SYSTEM_STREAMS")
public class AnacondaTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    public void run_immediateBye_exitsWithoutCreatingDataFile() {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String output = runSession(file, " BYE \n");
        assertTrue(output.contains("Yo, it's Anaconda.\nWhat do you want?\n"));
        assertTrue(output.contains("Alright, until next time."));
        assertFalse(output.contains("Oops!"));
        assertFalse(Files.exists(file));
    }

    @Test
    public void run_taskCommands_persistsChangesAndLoadsThemOnRestart() throws IOException {
        Path file = temporaryDirectory.resolve("data/tasks.txt");
        String output = runSession(file, "todo read book\ndeadline report /by 2026-08-19\n"
                + "event meeting /from 2026-08-18 /to 2026-08-20\nmark 2\nunmark 2\nmark 3\ndelete 1\nlist\nbye\n");
        assertTrue(output.contains("Now you have 3 tasks in the list."));
        assertTrue(output.contains("Marked it done for you:\n  [D][X] report"));
        assertTrue(output.contains("Really? Unmarked? Alright . . .\n  [D][ ] report"));
        assertTrue(output.contains("Noted. I've removed this task:\n  [T][ ] read book"));
        String expectedList = "Your list:\n1.[D][ ] report (by: Aug 19 2026)\n"
                + "2.[E][X] meeting (from: Aug 18 2026 to: Aug 20 2026)\n";
        assertTrue(output.contains(expectedList));
        assertEquals(List.of("D | 0 | report | 2026-08-19", "E | 1 | meeting | 2026-08-18 | 2026-08-20"),
                Files.readAllLines(file));
        assertTrue(runSession(file, "list\nbye\n").contains(expectedList));
    }

    @Test
    public void run_invalidCommand_continuesToProcessLaterCommands() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String output = runSession(file, "nonsense\ntodo\nmark 1\ntodo book\nbye now\nlist\nbye\n");
        assertTrue(output.contains("Oops! I don't recognize that command."));
        assertTrue(output.contains("Oops! The description of a todo cannot be empty."));
        assertTrue(output.contains("Oops! Task 1 does not exist."));
        assertTrue(output.contains("Oops! The bye command cannot have extra text."));
        assertTrue(output.contains("Your list:\n1.[T][ ] book\n"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
    }

    @Test
    public void run_clearConfirmed_clearsSavedTasksAndResumesNormalCommands() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String output = runSession(file, "todo book\nclear\n YES \nlist\nbye\n");
        assertTrue(output.contains("You sure? (yes/no)"));
        assertTrue(output.contains("Fine. Everything's gone."));
        assertTrue(output.contains("Your list:\n____________________________________________________________"));
        assertEquals("", Files.readString(file));
        assertFalse(runSession(file, "list\nbye\n").contains("[T]"));
    }

    @Test
    public void run_clearNotExplicitlyConfirmed_keepsTasksAndConsumesOnlyOneResponse() throws IOException {
        for (String response : new String[]{"no", "", "yes please", "bye", "todo accidental"}) {
            Path file = temporaryDirectory.resolve("tasks.txt");
            Files.writeString(file, "T | 0 | book\n");
            String output = runSession(file, "clear\n" + response + "\nlist\nbye\n");
            assertTrue(output.contains("That's not a yes. Kept your tasks."), response);
            assertTrue(output.contains("Your list:\n1.[T][ ] book\n"), response);
            assertFalse(output.contains("Got it. I've added this task:"), response);
            assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        }
    }

    @Test
    public void run_dateFilters_dispatchesBothDirectionsAndSharpWithoutSaving() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String saved = "D | 0 | before | 2026-08-18\nD | 0 | exact | 2026-08-19\n"
                + "E | 0 | after | 2026-08-18 | 2026-08-20\nT | 0 | undated\n";
        Files.writeString(file, saved);
        String output = runSession(file, "/by 19-08-2026\n/from 19-08-2026\n/by 19-08-2026 sharp\nbye\n");
        String line = "____________________________________________________________\n";
        assertTrue(output.contains("Matching tasks:\n1.[D][ ] before (by: Aug 18 2026)\n"
                + "2.[D][ ] exact (by: Aug 19 2026)\n" + line));
        assertTrue(output.contains("Matching tasks:\n1.[D][ ] exact (by: Aug 19 2026)\n"
                + "2.[E][ ] after (from: Aug 18 2026 to: Aug 20 2026)\n" + line));
        assertTrue(output.contains("Matching tasks:\n1.[D][ ] exact (by: Aug 19 2026)\n" + line));
        assertFalse(output.contains("undated"));
        assertEquals(saved, Files.readString(file));
    }

    @Test
    public void run_findCommand_matchesDescriptionsIgnoringCaseWithoutSaving() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String saved = "T | 1 | Read Book\nD | 0 | return book | 2026-08-19\n"
                + "E | 0 | book launch | 2026-08-18 | 2026-08-20\nD | 0 | report | 2026-08-20\n";
        Files.writeString(file, saved);
        String output = runSession(file, "find BOOK\nfind 2026\nfind\nbye\n");
        String line = "____________________________________________________________\n";
        assertTrue(output.contains("Here are the matching tasks in your list:\n"
                + "1.[T][X] Read Book\n2.[D][ ] return book (by: Aug 19 2026)\n"
                + "3.[E][ ] book launch (from: Aug 18 2026 to: Aug 20 2026)\n" + line));
        assertTrue(output.contains("Here are the matching tasks in your list:\n" + line));
        assertTrue(output.contains("Oops! Please provide a keyword to find."));
        assertEquals(saved, Files.readString(file));
    }

    @Test
    public void constructor_unreadableDataFile_reportsErrorAndStartsEmpty() {
        String output = runSession(temporaryDirectory, "list\nbye\n");
        assertTrue(output.startsWith("Oops! I couldn't load your saved tasks.\n"));
        assertTrue(output.contains("Your list:\n____________________________________________________________"));
        assertTrue(output.contains("Alright, until next time."));
    }

    @Test
    public void run_saveFailure_reportsErrorAndContinuesWithoutSuccessMessage() throws IOException {
        Path parentFile = Files.createFile(temporaryDirectory.resolve("not-a-folder"));
        String output = runSession(parentFile.resolve("tasks.txt"), "todo book\nbye\n");
        assertTrue(output.contains("Oops! I couldn't save your task list."));
        assertFalse(output.contains("Got it. I've added this task:"));
        assertTrue(output.contains("Alright, until next time."));
    }

    @Test
    public void main_newWorkingDirectory_usesRelativeDefaultDataPath() throws Exception {
        // A child JVM safely tests main's hard-coded relative path without changing this JVM's working directory.
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path classes = Path.of(Anaconda.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path input = temporaryDirectory.resolve("input.txt");
        Path output = temporaryDirectory.resolve("output.txt");
        Files.writeString(input, "todo main task\nbye\n");
        Process process = new ProcessBuilder(java.toString(), "-cp", classes.toString(), "anaconda.Anaconda")
                .directory(temporaryDirectory.toFile())
                .redirectInput(input.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Application did not exit after bye.");
            assertEquals(0, process.exitValue(), Files.readString(output));
            assertEquals(List.of("T | 0 | main task"),
                    Files.readAllLines(temporaryDirectory.resolve("data/anaconda.txt")));
            assertTrue(Files.readString(output).contains("Alright, until next time."));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Runs one chatbot session with a supplied storage path and restores console streams afterwards.
     */
    private String runSession(Path file, String input) {
        try (ConsoleSession session = new ConsoleSession(input)) {
            new Anaconda(file).run();
            return session.output();
        }
    }
}
