package anaconda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
                + "event meeting /from 2026-08-18 /to 2026-08-20\nmark 2\nunmark 2\n"
                + "mark 3\ndelete 1\nlist\nbye\n");
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
        assertTrue(output.contains("Your list:\n"
                + "____________________________________________________________"));
        assertEquals("", Files.readString(file));
        assertFalse(runSession(file, "list\nbye\n").contains("[T]"));
    }

    @Test
    public void run_clearNotExplicitlyConfirmed_keepsTasksAndConsumesOnlyOneResponse() throws IOException {
        for (String response : new String[] {"no", "", "yes please", "bye", "todo accidental"}) {
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
    public void getResponse_commandsAndClearConfirmation_returnsExistingMessagesAndUpdatesStorage()
            throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        String lineSeparator = System.lineSeparator();

        assertEquals(String.join(lineSeparator,
                "Got it. I've added this task:",
                "  [T][ ] book",
                "Now you have 1 tasks in the list."), anaconda.getResponse("todo book"));
        assertEquals("You sure? (yes/no)", anaconda.getResponse("clear"));
        assertEquals("That's not a yes. Kept your tasks.", anaconda.getResponse("bye"));
        assertEquals(String.join(lineSeparator,
                "Your list:",
                "1.[T][ ] book"), anaconda.getResponse("list"));
        assertEquals("Oops! I don't recognize that command.", anaconda.getResponse("unknown"));
        assertEquals("You sure? (yes/no)", anaconda.getResponse("clear"));
        assertEquals("Fine. Everything's gone.", anaconda.getResponse("yes"));
        assertEquals("Alright, until next time.", anaconda.getResponse("bye"));
        assertTrue(Files.readAllLines(file).isEmpty());
    }

    @Test
    public void getResponse_successAndError_keepsConsoleStreamsUsable() {
        Path file = temporaryDirectory.resolve("tasks.txt");
        try (ConsoleSession session = new ConsoleSession("list\nbye\n")) {
            InputStream consoleInput = System.in;
            PrintStream consoleOutput = System.out;
            Anaconda anaconda = new Anaconda(file);

            assertTrue(anaconda.getResponse("todo café 读书").contains("[T][ ] café 读书"));
            assertEquals("Oops! I don't recognize that command.", anaconda.getResponse("unknown"));
            assertSame(consoleInput, System.in);
            assertSame(consoleOutput, System.out);
            assertEquals("", session.output());

            anaconda.run();
            System.out.println("Console output still open.");
            assertTrue(session.output().contains("Your list:\n1.[T][ ] café 读书\n"));
            assertTrue(session.output().endsWith("Console output still open.\n"));
            assertFalse(session.output().contains("Got it. I've added this task:"));
            assertFalse(session.output().contains("Oops!"));
        }
    }

    @Test
    public void getResponse_taskUpdates_persistsOnlyTheSelectedTaskChange() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Files.writeString(file, "T | 0 | first\nT | 0 | second\n");
        Anaconda anaconda = new Anaconda(file);

        assertTrue(anaconda.getResponse("mark 2").contains("[T][X] second"));
        assertEquals(List.of("T | 0 | first", "T | 1 | second"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("unmark 2").contains("[T][ ] second"));
        assertEquals(List.of("T | 0 | first", "T | 0 | second"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("delete 1").contains("[T][ ] first"));
        assertEquals(List.of("T | 0 | second"), Files.readAllLines(file));
    }

    @Test
    public void getResponse_failedTaskMutationSave_reportsOnlyError() throws IOException {
        for (String command : new String[] {"todo book", "mark 1", "unmark 1", "delete 1"}) {
            Path file = temporaryDirectory.resolve(command.replace(' ', '-'));
            Files.writeString(file, "T | 1 | existing task\n");
            Anaconda anaconda = new Anaconda(file);
            Files.delete(file);
            Files.createDirectory(file);

            assertEquals("Oops! I couldn't save your task list.", anaconda.getResponse(command), command);
        }
    }

    @Test
    public void getResponse_undoAndRedoEveryMutation_restoresSavedTasksAndExhaustsHistory() throws IOException {
        List<String> original = List.of("T | 0 | book", "D | 1 | report | 2026-09-10",
                "E | 0 | meeting | 2026-09-09 | 2026-09-11");
        String[] commands = {"todo new book", "deadline new report /by 2026-09-12",
            "event new meeting /from 2026-09-12 /to 2026-09-13", "mark 1", "unmark 2", "delete 2", "clear"};
        for (int i = 0; i < commands.length; i++) {
            Path file = temporaryDirectory.resolve("tasks-" + i + ".txt");
            Files.write(file, original);
            Anaconda anaconda = new Anaconda(file);
            assertFalse(anaconda.getResponse(commands[i]).startsWith("Oops!"), commands[i]);
            if (commands[i].equals("clear")) {
                assertEquals("Fine. Everything's gone.", anaconda.getResponse("yes"));
            }
            List<String> changed = Files.readAllLines(file);

            assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                    + System.lineSeparator() + "Your list:"), commands[i]);
            assertEquals(original, Files.readAllLines(file), commands[i]);
            assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
            assertTrue(anaconda.getResponse("undo undo").startsWith("Undid the previous undo."
                    + System.lineSeparator() + "Your list:"), commands[i]);
            assertEquals(changed, Files.readAllLines(file), commands[i]);
            assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
            assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                    + System.lineSeparator() + "Your list:"));
            assertEquals(original, Files.readAllLines(file), commands[i]);
            assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
        }
    }

    @Test
    public void getResponse_repeatedUndoUndo_reappliesChangesInOrder() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("clear");
        anaconda.getResponse("yes");
        anaconda.getResponse("undo");
        anaconda.getResponse("undo");
        anaconda.getResponse("undo");

        assertTrue(anaconda.getResponse("  UnDo\t UnDo  ").startsWith("Undid the previous undo."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo undo").startsWith("Undid the previous undo."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 1 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo undo").startsWith("Undid the previous undo."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
    }

    @Test
    public void getResponse_otherInputAfterUndo_endsRedoChainButPreservesUndoHistory() throws IOException {
        String[] inputs = {"list", "find book", "/by 2026-09-10", "/from 2026-09-10 sharp",
            "unknown", "", "mark 0", "delete 2", "todo", "undo extra", "undo undo undo", "list extra", "bye"};
        for (int i = 0; i < inputs.length; i++) {
            Path file = temporaryDirectory.resolve("tasks-" + i + ".txt");
            Anaconda anaconda = new Anaconda(file);
            anaconda.getResponse("todo book");
            anaconda.getResponse("mark 1");
            anaconda.getResponse("undo");
            anaconda.getResponse(inputs[i]);

            assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"), inputs[i]);
            assertEquals(List.of("T | 0 | book"), Files.readAllLines(file), inputs[i]);
            assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                    + System.lineSeparator() + "Your list:"), inputs[i]);
            assertTrue(Files.readAllLines(file).isEmpty(), inputs[i]);
            assertTrue(anaconda.getResponse("undo undo").startsWith("Undid the previous undo."
                    + System.lineSeparator() + "Your list:"), inputs[i]);
            assertEquals(List.of("T | 0 | book"), Files.readAllLines(file), inputs[i]);
            assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"), inputs[i]);
        }
    }

    @Test
    public void getResponse_newMutationAfterUndo_discardsOldRedoStates() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("undo");
        anaconda.getResponse("todo report");

        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
        assertEquals(List.of("T | 0 | book", "T | 0 | report"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(anaconda.getResponse("undo undo").startsWith("Undid the previous undo."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 0 | book", "T | 0 | report"), Files.readAllLines(file));
        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
    }

    @Test
    public void getResponse_clearCancelledByUndoUndo_endsChainAndKeepsTasks() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("undo");
        assertEquals("You sure? (yes/no)", anaconda.getResponse("clear"));
        assertEquals("That's not a yes. Kept your tasks.", anaconda.getResponse("undo undo"));
        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
    }

    @Test
    public void getResponse_undoUndoWithoutHistory_reportsErrorWithoutSaving() {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
        assertFalse(Files.exists(file));
    }

    @Test
    public void getResponse_undoUndoAfterRestart_hasNoSessionHistory() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("undo");
        Anaconda restarted = new Anaconda(file);
        assertEquals("Oops! There is no undo to reverse.", restarted.getResponse("undo undo"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
    }

    @Test
    public void getResponse_failedRedoSave_keepsCurrentStateAndAllowsRetry() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("undo");
        Files.delete(file);
        Files.createDirectory(file);

        assertEquals("Oops! I couldn't save your task list.", anaconda.getResponse("undo undo"));
        Files.delete(file);
        assertTrue(anaconda.getResponse("undo undo").startsWith("Undid the previous undo."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 1 | book"), Files.readAllLines(file));
        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
    }

    @Test
    public void getResponse_undoAndRedo_showsCurrentListWithoutEndingChain() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Files.write(file, List.of("T | 0 | book", "D | 1 | report | 2026-09-10",
                "E | 0 | meeting | 2026-09-09 | 2026-09-11"));
        Anaconda anaconda = new Anaconda(file);
        String originalRows = "1.[T][ ] book\n2.[D][X] report (by: Sep 10 2026)\n"
                + "3.[E][ ] meeting (from: Sep 09 2026 to: Sep 11 2026)";
        anaconda.getResponse("delete 2");

        assertEquals("Undid the previous command.\nYour list:\n" + originalRows,
                anaconda.getResponse("undo").replace("\r\n", "\n"));
        assertEquals("Undid the previous undo.\nYour list:\n1.[T][ ] book\n"
                + "2.[E][ ] meeting (from: Sep 09 2026 to: Sep 11 2026)",
                anaconda.getResponse("undo undo").replace("\r\n", "\n"));
        assertEquals("Undid the previous command.\nYour list:\n" + originalRows,
                anaconda.getResponse("undo").replace("\r\n", "\n"));

        anaconda.getResponse("clear");
        anaconda.getResponse("yes");
        assertEquals("Undid the previous command.\nYour list:\n" + originalRows,
                anaconda.getResponse("undo").replace("\r\n", "\n"));
        assertEquals("Undid the previous undo.\nYour list:",
                anaconda.getResponse("undo undo").replace("\r\n", "\n"));
        assertEquals("Undid the previous command.\nYour list:\n" + originalRows,
                anaconda.getResponse("undo").replace("\r\n", "\n"));
        Anaconda initiallyEmpty = new Anaconda(temporaryDirectory.resolve("empty.txt"));
        initiallyEmpty.getResponse("todo another");
        assertEquals("Undid the previous command.\nYour list:",
                initiallyEmpty.getResponse("undo").replace("\r\n", "\n"));
    }

    @Test
    public void run_undoUndo_restoresChangesUntilAnotherCommandEndsChain() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String output = runSession(file,
                "todo book\nmark 1\nundo\nundo undo\nundo\nlist\nundo undo\nbye\n");
        assertTrue(output.contains("Undid the previous undo.\nYour list:\n1.[T][X] book\n"));
        assertTrue(output.contains("Undid the previous command.\nYour list:\n1.[T][ ] book\n"));
        assertTrue(output.contains("Oops! There is no undo to reverse."));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
    }

    @Test
    public void getResponse_repeatedUndo_restoresEachEarlierState() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("clear");
        anaconda.getResponse("yes");

        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 1 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
    }

    @Test
    public void getResponse_readOnlyInvalidAndCancelledCommands_preserveUndoHistory() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
        assertFalse(Files.exists(file));
        anaconda.getResponse("todo book");
        for (String command : new String[] {"list", "find book", "/by 2026-09-10", "/from 2026-09-10 sharp",
            "unknown", "mark 0", "delete 2", "todo", "undo extra", "clear", "no"}) {
            anaconda.getResponse(command);
        }
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
    }

    @Test
    public void getResponse_noOpMutations_areSeparateUndoSteps() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("mark 1");
        anaconda.getResponse("undo");
        assertEquals(List.of("T | 1 | book"), Files.readAllLines(file));
        anaconda.getResponse("undo");
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        anaconda.getResponse("undo");
        anaconda.getResponse("clear");
        anaconda.getResponse("yes");
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
    }

    @Test
    public void getResponse_newMutationAfterUndo_keepsRemainingEarlierHistory() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo first");
        anaconda.getResponse("todo second");
        anaconda.getResponse("undo");
        anaconda.getResponse("deadline report /by 2026-09-10");
        anaconda.getResponse("undo");
        assertEquals(List.of("T | 0 | first"), Files.readAllLines(file));
        anaconda.getResponse("undo");
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
    }

    @Test
    public void getResponse_undoDuringClearConfirmation_cancelsClearBeforeUndoing() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("clear");
        assertEquals("That's not a yes. Kept your tasks.", anaconda.getResponse("undo"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
    }

    @Test
    public void getResponse_undoAfterRestart_hasNoSessionHistory() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        new Anaconda(file).getResponse("todo book");
        Anaconda restarted = new Anaconda(file);
        assertEquals("Oops! There is nothing to undo.", restarted.getResponse("undo"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
    }

    @Test
    public void getResponse_failedUndoSave_keepsCurrentStateAndAllowsRetry() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        anaconda.getResponse("mark 1");
        Files.delete(file);
        Files.createDirectory(file);

        assertEquals("Oops! I couldn't save your task list.", anaconda.getResponse("undo"));
        assertEquals("Oops! There is no undo to reverse.", anaconda.getResponse("undo undo"));
        assertTrue(anaconda.getResponse("list").contains("[T][X] book"));
        Files.delete(file);
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
    }

    @Test
    public void getResponse_failedMutationSave_restoresStateAndPreservesUndoHistory() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Anaconda anaconda = new Anaconda(file);
        anaconda.getResponse("todo book");
        Files.delete(file);
        Files.createDirectory(file);
        for (String command : new String[] {"todo another", "mark 1", "delete 1", "clear", "yes"}) {
            anaconda.getResponse(command);
        }
        assertTrue(anaconda.getResponse("list").contains("1.[T][ ] book"));
        assertFalse(anaconda.getResponse("list").contains("another"));
        Files.delete(file);
        assertTrue(anaconda.getResponse("undo").startsWith("Undid the previous command."
                + System.lineSeparator() + "Your list:"));
        assertTrue(Files.readAllLines(file).isEmpty());
        assertEquals("Oops! There is nothing to undo.", anaconda.getResponse("undo"));
    }

    @Test
    public void run_undoClearAndDelete_restoresTasksAndRecoversFromEmptyHistory() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        String output = runSession(file, "undo\ntodo book\ndelete 1\nundo\nclear\nyes\nundo\nlist\nbye\n");
        assertTrue(output.contains("Oops! There is nothing to undo."));
        assertTrue(output.contains("Undid the previous command."));
        assertTrue(output.contains("Your list:\n1.[T][ ] book\n"));
        assertEquals(List.of("T | 0 | book"), Files.readAllLines(file));
    }

    @Test
    public void constructor_unreadableDataFile_reportsErrorAndStartsEmpty() {
        String output = runSession(temporaryDirectory, "list\nbye\n");
        assertTrue(output.startsWith("Oops! I couldn't load your saved tasks.\n"));
        assertTrue(output.contains("Your list:\n"
                + "____________________________________________________________"));
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
        // Use a child JVM to test main's hard-coded relative path without changing
        // this JVM's working directory.
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
