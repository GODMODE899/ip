import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Runs the Anaconda chatbot and processes commands entered by the user.
 */
public class Anaconda {
    private static final String LINE = "____________________________________________________________";

    /**
     * Starts the chatbot and keeps accepting commands until the user enters {@code bye}.
     *
     * @param args command-line arguments, which are not used
     */
    public static void main(String[] args) {
        ArrayList<Task> tasks = new ArrayList<>();

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
        while (true) {
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("bye")) {
                break;
            }

            System.out.println(LINE);
            try {
                handleCommand(input, tasks);
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
     * @param input complete input entered by the user
     * @param tasks list containing the current tasks
     * @throws AnacondaException if the command or its arguments are invalid
     */
    private static void handleCommand(String input, ArrayList<Task> tasks) throws AnacondaException {
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
            System.out.println("Marked it done for you:");
            System.out.println("  " + tasks.get(markIndex));
            break;
        case UNMARK:
            int unmarkIndex = parseTaskIndex(arguments, tasks.size());
            tasks.get(unmarkIndex).markAsUndone();
            System.out.println("Really? Unmarked? Alright . . .");
            System.out.println("  " + tasks.get(unmarkIndex));
            break;
        case DELETE:
            int deleteIndex = parseTaskIndex(arguments, tasks.size());
            Task removedTask = tasks.remove(deleteIndex);
            System.out.println("Noted. I've removed this task:");
            System.out.println("  " + removedTask);
            System.out.println("Now you have " + tasks.size() + " tasks in the list.");
            break;
        case TODO:
            requireDescription(arguments, "todo");
            addTask(tasks, new ToDo(arguments));
            break;
        case DEADLINE:
            addDeadline(tasks, arguments);
            break;
        case EVENT:
            addEvent(tasks, arguments);
            break;
        case BYE:
            throw new AnacondaException("The bye command cannot have extra text.");
        }
    }

    /**
     * Converts a command word into its corresponding enum value.
     *
     * @param commandWord command word entered by the user
     * @return matching command
     * @throws AnacondaException if the command word is not supported
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
     */
    private static void addDeadline(ArrayList<Task> tasks, String arguments) throws AnacondaException {
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
        addTask(tasks, new Deadline(description, by));
    }

    /**
     * Parses and adds an event command's arguments.
     */
    private static void addEvent(ArrayList<Task> tasks, String arguments) throws AnacondaException {
        int fromPosition = arguments.indexOf("/from");
        int toPosition = fromPosition < 0 ? -1 : arguments.indexOf("/to", fromPosition + "/from".length());
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
        addTask(tasks, new Event(description, from, to));
    }

    /**
     * Converts a one-based task number to a valid list index.
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
     */
    private static void addTask(ArrayList<Task> tasks, Task task) {
        tasks.add(task);
        printTaskAdded(task, tasks.size());
    }

    /** Ensures a task description is present. */
    private static void requireDescription(String description, String taskType) throws AnacondaException {
        if (description.isEmpty()) {
            throw new AnacondaException("The description of a " + taskType + " cannot be empty.");
        }
    }

    /** Ensures a command that takes no arguments has none. */
    private static void requireNoArguments(String arguments, String command) throws AnacondaException {
        if (!arguments.isEmpty()) {
            throw new AnacondaException("The " + command + " command does not take extra text.");
        }
    }

    /**
     * Prints confirmation after a task is added.
     *
     * @param task task that was added
     * @param taskCount number of tasks now stored
     */
    private static void printTaskAdded(Task task, int taskCount) {
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + task);
        System.out.println("Now you have " + taskCount + " tasks in the list.");
    }
}
