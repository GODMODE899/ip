package anaconda.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import anaconda.task.Task;

/**
 * Handles console input and all messages displayed by the chatbot.
 */
public class Ui implements AutoCloseable {
    private static final String LINE = "____________________________________________________________";

    private final Scanner scanner;
    private final PrintStream output;

    /**
     * Creates a UI using the current console input and output streams.
     */
    public Ui() {
        this(System.in, System.out);
    }

    /**
     * Creates a UI with explicit input and output streams.
     * Closing this UI closes its input reader, but leaves the supplied output open.
     *
     * @param input Input read by this UI.
     * @param output Destination for messages, owned by the caller.
     */
    public Ui(InputStream input, PrintStream output) {
        scanner = new Scanner(input);
        this.output = output;
    }

    /**
     * Displays the welcome banner and initial prompt.
     */
    public void showWelcome() {
        String banner =
                "    _    _   _    _    ____ ___  _   _ ____    _\n"
                        + "   / \\  | \\ | |  / \\  / ___/ _ \\| \\ | |  _ \\  / \\\n"
                        + "  / _ \\ |  \\| | / _ \\| |  | | | |  \\| | | | |/ _ \\\n"
                        + " / ___ \\| |\\  |/ ___ \\ |__| |_| | |\\  | |_| / ___ \\\n"
                        + "/_/   \\_\\_| \\_/_/   \\_\\____\\___/|_| \\_|____/_/   \\_\\\n";

        showToUser(
                LINE,
                banner,
                "Yo, it's Anaconda.",
                "What do you want?");
    }

    /**
     * Reads and trims the next line entered by the user.
     *
     * @return Complete command or confirmation text.
     */
    public String readCommand() {
        return scanner.nextLine().trim();
    }

    /**
     * Displays a separator around a response.
     */
    public void showLine() {
        showToUser(LINE);
    }

    /**
     * Displays a user-facing error.
     *
     * @param message Explanation of the error.
     */
    public void showError(String message) {
        showToUser("Oops! " + message);
    }

    /**
     * Reports that previously saved tasks could not be loaded.
     */
    public void showLoadingError() {
        showError("I couldn't load your saved tasks.");
    }

    /**
     * Displays a numbered full list or filtered search result.
     *
     * @param tasks Tasks to display in their current order.
     * @param isFiltered Whether these tasks are a search result.
     */
    public void showTasks(List<Task> tasks, boolean isFiltered) {
        showToUser(isFiltered ? "Matching tasks:" : "Your list:");
        showNumberedTasks(tasks);
    }

    /**
     * Displays tasks whose descriptions match a find command.
     *
     * @param tasks Matching tasks in their original order.
     */
    public void showFindResults(List<Task> tasks) {
        showToUser("Here are the matching tasks in your list:");
        showNumberedTasks(tasks);
    }

    /**
     * Displays task rows with consecutive one-based numbers for every list view.
     */
    private void showNumberedTasks(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            showToUser((i + 1) + "." + tasks.get(i));
        }
    }

    /**
     * Displays an added task and the updated task count.
     *
     * @param task Task that was added.
     * @param taskCount Number of tasks now stored.
     */
    public void showTaskAdded(Task task, int taskCount) {
        showToUser(
                "Got it. I've added this task:",
                "  " + task,
                "Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Displays a removed task and the updated task count.
     *
     * @param task Task that was removed.
     * @param taskCount Number of tasks remaining.
     */
    public void showTaskRemoved(Task task, int taskCount) {
        showToUser(
                "Noted. I've removed this task:",
                "  " + task,
                "Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Displays a task after changing its completion status.
     *
     * @param task Task that was updated.
     * @param isDone Whether the task was marked as completed.
     */
    public void showMarked(Task task, boolean isDone) {
        showToUser(
                isDone ? "Marked it done for you:" : "Really? Unmarked? Alright . . .",
                "  " + task);
    }

    /**
     * Asks the user to confirm clearing the list.
     */
    public void showClearQuestion() {
        showToUser("You sure? (yes/no)");
    }

    /**
     * Reports that clearing the list was cancelled.
     */
    public void showClearCancelled() {
        showToUser("That's not a yes. Kept your tasks.");
    }

    /**
     * Reports that the task list was cleared.
     */
    public void showCleared() {
        showToUser("Fine. Everything's gone.");
    }

    /**
     * Reports that the previous task-changing command was undone and saved.
     */
    public void showUndo() {
        showToUser("Undid the previous command.");
    }

    /**
     * Displays the farewell message.
     */
    public void showGoodbye() {
        showToUser(LINE, "Alright, until next time.", LINE);
    }

    /**
     * Displays each supplied message on a separate line.
     *
     * @param messages Messages to display in order.
     */
    private void showToUser(String... messages) {
        for (String message : messages) {
            output.println(message);
        }
    }

    /**
     * Closes the console input reader when the chatbot finishes.
     */
    @Override
    public void close() {
        scanner.close();
    }
}
