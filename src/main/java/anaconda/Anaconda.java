package anaconda;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Runs the Anaconda chatbot and processes commands entered by the user.
 */
public class Anaconda {
    private static final String LINE = "____________________________________________________________";
    private static final Path DATA_FILE = Path.of("data", "anaconda.txt");

    /**
     * Starts the chatbot and keeps accepting commands until the user enters {@code bye}.
     *
     * @param args Command-line arguments, which are not used.
     */
    public static void main(String[] args) {
        Storage storage = new Storage(DATA_FILE);
        ArrayList<Task> tasks = loadTasks(storage);

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

        Scanner scanner = new Scanner(System.in);
        boolean isAwaitingClearConfirmation = false;
        while (true) {
            String input = scanner.nextLine().trim();

            if (isAwaitingClearConfirmation) {
                System.out.println(LINE);
                try {
                    clearTasksIfConfirmed(input, tasks, storage);
                } catch (AnacondaException exception) {
                    System.out.println("Oops! " + exception.getMessage());
                }
                System.out.println(LINE);
                isAwaitingClearConfirmation = false;
                continue;
            }

            if (input.equalsIgnoreCase("bye")) {
                break;
            }

            System.out.println(LINE);
            try {
                isAwaitingClearConfirmation = handleCommand(input, tasks, storage);
            } catch (AnacondaException exception) {
                System.out.println("Oops! " + exception.getMessage());
            }
            System.out.println(LINE);
        }

        scanner.close();
        System.out.println(LINE);
        System.out.println("Alright, until next time.");
        System.out.println(LINE);
    }

    /**
     * Executes one command and updates the task list when necessary.
     *
     * @param input Complete input entered by the user.
     * @param tasks List containing the current tasks.
     * @param storage Storage manager used to save task changes.
     * @return {@code true} if the next input should confirm a clear command.
     * @throws AnacondaException If the command or its arguments are invalid, or a change cannot be saved.
     */
    private static boolean handleCommand(String input, ArrayList<Task> tasks, Storage storage)
            throws AnacondaException {
        if (input.isEmpty()) {
            throw new AnacondaException("Please enter a command.");
        }

        String[] inputParts = input.split("\\s+", 2);
        Command command = parseCommand(inputParts[0]);
        String arguments = inputParts.length == 2 ? inputParts[1].trim() : "";

        switch (command) {
        case LIST:
            requireNoArguments(arguments, "list");
            System.out.println("Your list:");
            System.out.print(Task.displayList(tasks));
            break;
        case MARK:
            int markIndex = parseTaskIndex(arguments, tasks.size());
            tasks.get(markIndex).markAsDone();
            saveTasks(storage, tasks);
            System.out.println("Marked it done for you:");
            System.out.println("  " + tasks.get(markIndex));
            break;
        case UNMARK:
            int unmarkIndex = parseTaskIndex(arguments, tasks.size());
            tasks.get(unmarkIndex).markAsUndone();
            saveTasks(storage, tasks);
            System.out.println("Really? Unmarked? Alright . . .");
            System.out.println("  " + tasks.get(unmarkIndex));
            break;
        case DELETE:
            int deleteIndex = parseTaskIndex(arguments, tasks.size());
            Task removedTask = tasks.remove(deleteIndex);
            saveTasks(storage, tasks);
            System.out.println("Noted. I've removed this task:");
            System.out.println("  " + removedTask);
            System.out.println("Now you have " + tasks.size() + " tasks in the list.");
            break;
        case CLEAR:
            requireNoArguments(arguments, "clear");
            System.out.println("You sure? (yes/no)");
            return true;
        case TODO:
            requireDescription(arguments, "todo");
            addTask(tasks, new ToDo(arguments), storage);
            break;
        case DEADLINE:
            addDeadline(tasks, arguments, storage);
            break;
        case EVENT:
            addEvent(tasks, arguments, storage);
            break;
        case BYE:
            throw new AnacondaException("The bye command cannot have extra text.");
        }
        return false;
    }

    /**
     * Clears and saves the task list only when the user explicitly confirms.
     *
     * @param confirmation Confirmation entered by the user.
     * @param tasks List to clear.
     * @param storage Storage manager used to save the empty list.
     * @throws AnacondaException If the cleared task list cannot be saved.
     */
    private static void clearTasksIfConfirmed(String confirmation, ArrayList<Task> tasks, Storage storage)
            throws AnacondaException {
        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("That's not a yes. Kept your tasks.");
            return;
        }

        tasks.clear();
        saveTasks(storage, tasks);
        System.out.println("Fine. Everything's gone.");
    }

    /**
     * Loads saved tasks before the command loop starts. If the file cannot be
     * read, Anaconda starts with an empty list and explains the problem.
     *
     * @param storage Storage manager used to load tasks.
     * @return Tasks loaded from storage, or an empty list if loading fails.
     */
    private static ArrayList<Task> loadTasks(Storage storage) {
        try {
            return storage.loadTasks();
        } catch (IOException exception) {
            System.out.println("Oops! I couldn't load your saved tasks.");
            return new ArrayList<>();
        }
    }

    /**
     * Saves the current task list and converts file errors into user-facing errors.
     *
     * @param storage Storage manager used to save tasks.
     * @param tasks Tasks to save.
     * @throws AnacondaException If the task list cannot be saved.
     */
    private static void saveTasks(Storage storage, ArrayList<Task> tasks) throws AnacondaException {
        try {
            storage.saveTasks(tasks);
        } catch (IOException exception) {
            throw new AnacondaException("I couldn't save your task list.");
        }
    }

    /**
     * Converts a command word into its corresponding enum value.
     *
     * @param commandWord Command word entered by the user.
     * @return Matching command.
     * @throws AnacondaException If the command word is not supported.
     */
    private static Command parseCommand(String commandWord) throws AnacondaException {
        try {
            return Command.valueOf(commandWord.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AnacondaException("I don't recognize that command.");
        }
    }

    /**
     * Parses and adds a deadline command's arguments.
     *
     * @param tasks List to which the deadline is added.
     * @param arguments Deadline description and date entered by the user.
     * @param storage Storage manager used to save the updated list.
     * @throws AnacondaException If the arguments are invalid or the task cannot be saved.
     */
    private static void addDeadline(ArrayList<Task> tasks, String arguments, Storage storage)
            throws AnacondaException {
        int byPosition = arguments.indexOf("/by");
        if (byPosition < 0) {
            throw new AnacondaException("A deadline needs '/by' followed by a date or time.");
        }

        String description = arguments.substring(0, byPosition).trim();
        String by = arguments.substring(byPosition + "/by".length()).trim();
        requireDescription(description, "deadline");
        if (by.isEmpty()) {
            throw new AnacondaException("A deadline needs a date or time after '/by'.");
        }
        addTask(tasks, new Deadline(description, by), storage);
    }

    /**
     * Parses and adds an event command's arguments.
     *
     * @param tasks List to which the event is added.
     * @param arguments Event description and times entered by the user.
     * @param storage Storage manager used to save the updated list.
     * @throws AnacondaException If the arguments are invalid or the task cannot be saved.
     */
    private static void addEvent(ArrayList<Task> tasks, String arguments, Storage storage)
            throws AnacondaException {
        int fromPosition = arguments.indexOf("/from");
        int toPosition = fromPosition < 0
                ? -1
                : arguments.indexOf("/to", fromPosition + "/from".length());
        if (fromPosition < 0 || toPosition < 0) {
            throw new AnacondaException("An event needs both '/from' and '/to' times.");
        }

        String description = arguments.substring(0, fromPosition).trim();
        String from = arguments.substring(fromPosition + "/from".length(), toPosition).trim();
        String to = arguments.substring(toPosition + "/to".length()).trim();
        requireDescription(description, "event");
        if (from.isEmpty() || to.isEmpty()) {
            throw new AnacondaException("An event needs times after both '/from' and '/to'.");
        }
        addTask(tasks, new Event(description, from, to), storage);
    }

    /**
     * Converts a one-based task number to a valid list index.
     *
     * @param arguments Task number entered by the user.
     * @param taskCount Number of tasks in the list.
     * @return Zero-based list index.
     * @throws AnacondaException If the task number is missing or outside the list.
     */
    private static int parseTaskIndex(String arguments, int taskCount) throws AnacondaException {
        int taskNumber;
        try {
            taskNumber = Integer.parseInt(arguments);
        } catch (NumberFormatException exception) {
            throw new AnacondaException("Please provide one task number.");
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            throw new AnacondaException("Task " + taskNumber + " does not exist.");
        }
        return taskNumber - 1;
    }

    /**
     * Stores a task and prints the updated list size.
     *
     * @param tasks List to which the task is added.
     * @param task Task to add.
     * @param storage Storage manager used to save the updated list.
     * @throws AnacondaException If the updated task list cannot be saved.
     */
    private static void addTask(ArrayList<Task> tasks, Task task, Storage storage) throws AnacondaException {
        tasks.add(task);
        saveTasks(storage, tasks);
        printTaskAdded(task, tasks.size());
    }

    /**
     * Ensures a task description is present.
     *
     * @param description Task description to validate.
     * @param taskType Task type used in the error message.
     * @throws AnacondaException If the description is empty.
     */
    private static void requireDescription(String description, String taskType) throws AnacondaException {
        if (description.isEmpty()) {
            throw new AnacondaException("The description of a " + taskType + " cannot be empty.");
        }
    }

    /**
     * Ensures a command that takes no arguments has none.
     *
     * @param arguments Command arguments to validate.
     * @param command Command name used in the error message.
     * @throws AnacondaException If extra arguments are present.
     */
    private static void requireNoArguments(String arguments, String command) throws AnacondaException {
        if (!arguments.isEmpty()) {
            throw new AnacondaException("The " + command + " command does not take extra text.");
        }
    }

    /**
     * Prints confirmation after a task is added.
     *
     * @param task Task that was added.
     * @param taskCount Number of tasks now stored.
     */
    private static void printTaskAdded(Task task, int taskCount) {
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + task);
        System.out.println("Now you have " + taskCount + " tasks in the list.");
    }
}
