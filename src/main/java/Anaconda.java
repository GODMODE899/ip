import java.util.Scanner;

/**
 * Runs the Anaconda chatbot and processes commands entered by the user.
 */
public class Anaconda {
    private static final int MAX_TASKS = 100;
    private static final String LINE = "____________________________________________________________";

    /**
     * Starts the chatbot and keeps accepting commands until the user enters {@code bye}.
     *
     * @param args command-line arguments, which are not used
     */
    public static void main(String[] args) {
        Task[] tasks = new Task[MAX_TASKS];
        int taskCount = 0;

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
            String[] inputParts = input.split("\\s+", 2);
            String command = inputParts[0].toLowerCase();
            String arguments = inputParts.length == 2 ? inputParts[1].trim() : "";

            if (command.equals("bye")) {
                break;
            }

            System.out.println(LINE);
            switch (command) {
            case "list":
                System.out.println("Your list:");
                System.out.print(Task.displayList(tasks));
                break;
            case "mark":
                int markIndex = Integer.parseInt(arguments) - 1;
                tasks[markIndex].markAsDone();
                System.out.println("Marked it done for you:");
                System.out.println("  " + tasks[markIndex]);
                break;
            case "unmark":
                int unmarkIndex = Integer.parseInt(arguments) - 1;
                tasks[unmarkIndex].markAsUndone();
                System.out.println("Really? Unmarked? Alright . . .");
                System.out.println("  " + tasks[unmarkIndex]);
                break;
            case "todo":
                tasks[taskCount] = new ToDo(arguments);
                printTaskAdded(tasks[taskCount], ++taskCount);
                break;
            case "deadline":
                int byPosition = arguments.indexOf(" /by ");
                String deadlineDescription = arguments.substring(0, byPosition).trim();
                String deadline = arguments.substring(byPosition + " /by ".length()).trim();
                tasks[taskCount] = new Deadline(deadlineDescription, deadline);
                printTaskAdded(tasks[taskCount], ++taskCount);
                break;
            case "event":
                int fromPosition = arguments.indexOf(" /from ");
                int toPosition = arguments.indexOf(" /to ", fromPosition + " /from ".length());
                String eventDescription = arguments.substring(0, fromPosition).trim();
                String start = arguments.substring(fromPosition + " /from ".length(), toPosition).trim();
                String end = arguments.substring(toPosition + " /to ".length()).trim();
                tasks[taskCount] = new Event(eventDescription, start, end);
                printTaskAdded(tasks[taskCount], ++taskCount);
                break;
            default:
                // Invalid commands will be handled in the later exception-handling increment.
                break;
            }
            System.out.println(LINE);
        }

        scanner.close();
        System.out.println(LINE);
        System.out.println("Alright, until next time.");
        System.out.println(LINE);
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
