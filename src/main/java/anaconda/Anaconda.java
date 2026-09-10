package anaconda;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import anaconda.exception.AnacondaException;
import anaconda.parser.Command;
import anaconda.parser.Parser;
import anaconda.storage.Storage;
import anaconda.task.Task;
import anaconda.task.TaskList;
import anaconda.ui.Ui;

/**
 * Coordinates user interaction, command parsing, task operations, and storage.
 */
public class Anaconda {
    private static final Path DATA_FILE = Path.of("data", "anaconda.txt");

    private final Storage storage;
    private final TaskList tasks;
    private final Ui ui;
    private final Parser parser;
    private boolean isAwaitingGuiClearConfirmation;

    /**
     * Creates the chatbot using the default relative data path.
     */
    public Anaconda() {
        this(DATA_FILE);
    }

    /**
     * Creates the chatbot and loads its existing tasks.
     *
     * @param filePath Path to the task data file.
     */
    public Anaconda(Path filePath) {
        ui = new Ui();
        parser = new Parser();
        storage = new Storage(filePath);
        tasks = loadTasks();
    }

    /**
     * Starts the chatbot using the default relative data path.
     *
     * @param args Command-line arguments, which are not used.
     */
    public static void main(String[] args) {
        new Anaconda(DATA_FILE).run();
    }

    /**
     * Accepts commands and clear confirmations until the user enters bye.
     */
    public void run() {
        ui.showWelcome();
        boolean isAwaitingClearConfirmation = false;
        while (true) {
            String input = ui.readCommand();
            if (!isAwaitingClearConfirmation && parser.isExitCommand(input)) {
                break;
            }

            ui.showLine();
            try {
                if (isAwaitingClearConfirmation) {
                    isAwaitingClearConfirmation = false;
                    clearTasksIfConfirmed(input);
                } else {
                    isAwaitingClearConfirmation = handleCommand(input);
                }
            } catch (AnacondaException exception) {
                ui.showError(exception.getMessage());
            }
            ui.showLine();
        }

        ui.close();
        ui.showGoodbye();
    }

    /**
     * Processes one GUI command and returns the same response text used by the console interface.
     *
     * @param input Complete user input.
     * @return Response to display in the GUI.
     */
    public String getResponse(String input) {
        if (!isAwaitingGuiClearConfirmation && parser.isExitCommand(input)) {
            return "Alright, until next time.";
        }

        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (PrintStream responseOutput = new PrintStream(responseBuffer, true, StandardCharsets.UTF_8)) {
            System.setOut(responseOutput);
            processGuiInput(input);
        } finally {
            System.setOut(originalOutput);
        }
        return responseBuffer.toString(StandardCharsets.UTF_8).stripTrailing();
    }

    /**
     * Processes a GUI command or a pending clear confirmation using the existing command handlers.
     */
    private void processGuiInput(String input) {
        try {
            if (isAwaitingGuiClearConfirmation) {
                isAwaitingGuiClearConfirmation = false;
                clearTasksIfConfirmed(input);
            } else {
                isAwaitingGuiClearConfirmation = handleCommand(input);
            }
        } catch (AnacondaException exception) {
            ui.showError(exception.getMessage());
        }
    }

    /**
     * Dispatches a parsed command to the task list, storage, and user interface.
     *
     * @param input Complete user input.
     * @return Whether the next input must confirm a clear operation.
     * @throws AnacondaException If the command is invalid or saving fails.
     */
    private boolean handleCommand(String input) throws AnacondaException {
        Parser.ParsedCommand parsedCommand = parser.parse(input);
        Command command = parsedCommand.command();
        String arguments = parsedCommand.arguments();

        switch (command) {
            case LIST -> ui.showTasks(tasks.asList(), false);
            case MARK, UNMARK -> changeTaskStatus(arguments, command == Command.MARK);
            case DELETE -> deleteTask(arguments);
            case CLEAR -> {
                ui.showClearQuestion();
                return true;
            }
            case FIND -> findTasks(arguments);
            case TODO, DEADLINE, EVENT -> addTask(command, arguments);
            case BY, FROM -> filterTasksByDate(command, arguments);
            case BYE -> {
                // Console and GUI entry points handle standalone bye commands before dispatch.
            }
            default -> throw new IllegalStateException("Unsupported command: " + command);
        }
        return false;
    }

    /**
     * Updates a task's completion state and reports success only after saving.
     */
    private void changeTaskStatus(String arguments, boolean isDone) throws AnacondaException {
        Task task = tasks.mark(parser.parseTaskNumber(arguments), isDone);
        saveTasks();
        ui.showMarked(task, isDone);
    }

    /**
     * Removes the selected task and reports success only after saving.
     */
    private void deleteTask(String arguments) throws AnacondaException {
        Task removedTask = tasks.delete(parser.parseTaskNumber(arguments));
        saveTasks();
        ui.showTaskRemoved(removedTask, tasks.size());
    }

    /**
     * Creates a task and reports success only after saving.
     */
    private void addTask(Command command, String arguments) throws AnacondaException {
        Task task = parser.parseTask(command, arguments);
        tasks.add(task);
        saveTasks();
        ui.showTaskAdded(task, tasks.size());
    }

    /**
     * Displays tasks matching a validated description keyword.
     */
    private void findTasks(String arguments) throws AnacondaException {
        String keyword = parser.parseKeyword(arguments);
        ui.showFindResults(tasks.find(keyword));
    }

    /**
     * Displays tasks matching a validated date filter.
     */
    private void filterTasksByDate(Command command, String arguments) throws AnacondaException {
        Parser.DateFilter filter = parser.parseDateFilter(arguments, command);
        ui.showTasks(tasks.filterByDate(filter.date(), command, filter.isSharp()), true);
    }

    /**
     * Clears and saves tasks only after explicit approval.
     */
    private void clearTasksIfConfirmed(String confirmation) throws AnacondaException {
        if (!parser.isClearConfirmed(confirmation)) {
            ui.showClearCancelled();
            return;
        }
        tasks.clear();
        saveTasks();
        ui.showCleared();
    }

    /**
     * Loads saved tasks, reporting file-reading errors before starting with an empty list.
     */
    private TaskList loadTasks() {
        try {
            return new TaskList(storage.loadTasks());
        } catch (IOException exception) {
            ui.showLoadingError();
            return new TaskList();
        }
    }

    /**
     * Saves the current list and translates file errors into user-facing exceptions.
     */
    private void saveTasks() throws AnacondaException {
        try {
            storage.saveTasks(tasks.asList());
        } catch (IOException exception) {
            throw new AnacondaException("I couldn't save your task list.");
        }
    }
}
