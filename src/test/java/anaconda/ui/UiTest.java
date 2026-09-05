package anaconda.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import anaconda.task.Deadline;
import anaconda.task.Event;
import anaconda.task.Task;
import anaconda.task.ToDo;
import anaconda.testutil.ConsoleSession;

/**
 * Tests console input and exact UI output without using the real terminal.
 */
@ResourceLock("SYSTEM_STREAMS")
public class UiTest {
    private static final String LINE = "____________________________________________________________\n";

    @Test
    public void readCommand_multipleLines_trimsEdgesAndPreservesInternalWhitespace() {
        try (ConsoleSession session = new ConsoleSession("  todo Read  Book \t\n   \nBYE\n");
                Ui ui = new Ui()) {
            assertEquals("todo Read  Book", ui.readCommand());
            assertEquals("", ui.readCommand());
            assertEquals("BYE", ui.readCommand());
            assertEquals("", session.output());
        }
    }

    @Test
    public void showWelcome_startup_printsBannerAndPrompt() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showWelcome();
            String expected = LINE
                    + "    _    _   _    _    ____ ___  _   _ ____    _\n"
                    + "   / \\  | \\ | |  / \\  / ___/ _ \\| \\ | |  _ \\  / \\\n"
                    + "  / _ \\ |  \\| | / _ \\| |  | | | |  \\| | | | |/ _ \\\n"
                    + " / ___ \\| |\\  |/ ___ \\ |__| |_| | |\\  | |_| / ___ \\\n"
                    + "/_/   \\_\\_| \\_/_/   \\_\\____\\___/|_| \\_|____/_/   \\_\\\n"
                    + "\nYo, it's Anaconda.\nWhat do you want?\n";
            assertEquals(expected, session.output());
        }
    }

    @Test
    public void showLineAndGoodbye_printsSeparatorsAndFarewell() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showLine();
            ui.showGoodbye();
            assertEquals(LINE + LINE + "Alright, until next time.\n" + LINE, session.output());
        }
    }

    @Test
    public void showErrorAndLoadingError_printsHelpfulMessages() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showError("Bad command.");
            ui.showLoadingError();
            assertEquals("Oops! Bad command.\nOops! I couldn't load your saved tasks.\n", session.output());
        }
    }

    @Test
    public void showTasks_fullAndFilteredLists_printsCorrectHeadersAndNumbering() {
        Task todo = new ToDo("book");
        todo.markAsDone();
        LocalDate date = LocalDate.of(2026, 8, 19);
        List<Task> tasks = List.of(todo, new Deadline("report", date), new Event("meeting", date, date));
        String rows = "1.[T][X] book\n2.[D][ ] report (by: Aug 19 2026)\n"
                + "3.[E][ ] meeting (from: Aug 19 2026 to: Aug 19 2026)\n";
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showTasks(tasks, false);
            ui.showTasks(tasks, true);
            assertEquals("Your list:\n" + rows + "Matching tasks:\n" + rows, session.output());
            assertEquals(3, tasks.size());
        }
    }

    @Test
    public void showTasks_emptyLists_printsOnlyHeaders() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showTasks(List.of(), false);
            ui.showTasks(List.of(), true);
            assertEquals("Your list:\nMatching tasks:\n", session.output());
        }
    }

    @Test
    public void showFindResults_matchingAndEmptyLists_printsRequiredHeaderAndNumbering() {
        Task todo = new ToDo("read book");
        todo.markAsDone();
        List<Task> tasks = List.of(todo, new Deadline("return book", LocalDate.of(2026, 8, 19)));
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showFindResults(tasks);
            ui.showFindResults(List.of());
            assertEquals("Here are the matching tasks in your list:\n1.[T][X] read book\n"
                    + "2.[D][ ] return book (by: Aug 19 2026)\n"
                    + "Here are the matching tasks in your list:\n", session.output());
        }
    }

    @Test
    public void showTaskAddedAndRemoved_printsTaskAndSuppliedCount() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            Task task = new ToDo("book");
            ui.showTaskAdded(task, 12);
            ui.showTaskRemoved(task, 0);
            assertEquals("Got it. I've added this task:\n  [T][ ] book\n"
                    + "Now you have 12 tasks in the list.\n"
                    + "Noted. I've removed this task:\n  [T][ ] book\n"
                    + "Now you have 0 tasks in the list.\n", session.output());
        }
    }

    @Test
    public void showMarked_bothStatuses_printsMatchingMessage() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            Task task = new ToDo("book");
            task.markAsDone();
            ui.showMarked(task, true);
            task.markAsUndone();
            ui.showMarked(task, false);
            assertEquals("Marked it done for you:\n  [T][X] book\n"
                    + "Really? Unmarked? Alright . . .\n  [T][ ] book\n", session.output());
        }
    }

    @Test
    public void showClearMessages_questionCancellationAndApproval_keepExistingPersonality() {
        try (ConsoleSession session = new ConsoleSession(""); Ui ui = new Ui()) {
            ui.showClearQuestion();
            ui.showClearCancelled();
            ui.showCleared();
            assertEquals("You sure? (yes/no)\nThat's not a yes. Kept your tasks.\n"
                    + "Fine. Everything's gone.\n", session.output());
        }
    }

    @Test
    public void close_readerClosed_furtherReadsThrowIllegalStateException() {
        try (ConsoleSession session = new ConsoleSession("bye\n")) {
            Ui ui = new Ui();
            ui.close();
            assertThrows(IllegalStateException.class, ui::readCommand);
            assertEquals("", session.output());
        }
    }
}
