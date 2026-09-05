package anaconda.ui;

import java.util.List;
import java.util.Scanner;

import anaconda.task.Task;

/**
 * Handles console input and all messages displayed by the chatbot.
 */
public class Ui implements AutoCloseable {
    private static final String LINE = "____________________________________________________________";

    private final Scanner scanner = new Scanner(System.in);

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

        System.out.println(LINE);
        System.out.println(banner);
        System.out.println("Yo, it's Anaconda.");
        System.out.println("What do you want?");
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
        System.out.println(LINE);
    }

    /**
     * Displays a user-facing error.
     *
     * @param message Explanation of the error.
     */
    public void showError(String message) {
        System.out.println("Oops! " + message);
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
        System.out.println(isFiltered ? "Matching tasks:" : "Your list:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + "." + tasks.get(i));
        }
    }

    /**
     * Displays tasks whose descriptions match a find command.
     *
     * @param tasks Matching tasks in their original order.
     */
    public void showFindResults(List<Task> tasks) {
        System.out.println("Here are the matching tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + "." + tasks.get(i));
        }
    }

    /**
     * Displays an added task and the updated task count.
     *
     * @param task Task that was added.
     * @param taskCount Number of tasks now stored.
     */
    public void showTaskAdded(Task task, int taskCount) {
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + task);
        showTaskCount(taskCount);
    }

    /**
     * Displays a removed task and the updated task count.
     *
     * @param task Task that was removed.
     * @param taskCount Number of tasks remaining.
     */
    public void showTaskRemoved(Task task, int taskCount) {
        System.out.println("Noted. I've removed this task:");
        System.out.println("  " + task);
        showTaskCount(taskCount);
    }

    /**
     * Displays a task after changing its completion status.
     *
     * @param task Task that was updated.
     * @param isDone Whether the task was marked as completed.
     */
    public void showMarked(Task task, boolean isDone) {
        System.out.println(isDone ? "Marked it done for you:" : "Really? Unmarked? Alright . . .");
        System.out.println("  " + task);
    }

    /**
     * Asks the user to confirm clearing the list.
     */
    public void showClearQuestion() {
        System.out.println("You sure? (yes/no)");
    }

    /**
     * Reports that clearing the list was cancelled.
     */
    public void showClearCancelled() {
        System.out.println("That's not a yes. Kept your tasks.");
    }

    /**
     * Reports that the task list was cleared.
     */
    public void showCleared() {
        System.out.println("Fine. Everything's gone.");
    }

    /**
     * Displays the farewell message.
     */
    public void showGoodbye() {
        showLine();
        System.out.println("Alright, until next time.");
        showLine();
    }

    /**
     * Displays the current task count.
     */
    private void showTaskCount(int taskCount) {
        System.out.println("Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Closes the console input reader when the chatbot finishes.
     */
    @Override
    public void close() {
        scanner.close();
    }
}
