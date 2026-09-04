package anaconda;

import java.io.IOException;
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
        case LIST:
            ui.showTasks(tasks.asList(), false);
            break;
        case MARK:
            Task markedTask = tasks.mark(parser.parseTaskNumber(arguments), true);
            saveTasks();
            ui.showMarked(markedTask, true);
            break;
        case UNMARK:
            Task unmarkedTask = tasks.mark(parser.parseTaskNumber(arguments), false);
            saveTasks();
            ui.showMarked(unmarkedTask, false);
            break;
        case DELETE:
            Task removedTask = tasks.delete(parser.parseTaskNumber(arguments));
            saveTasks();
            ui.showTaskRemoved(removedTask, tasks.size());
            break;
        case CLEAR:
            ui.showClearQuestion();
            return true;
        case TODO:
        case DEADLINE:
        case EVENT:
            Task task = parser.parseTask(command, arguments);
            tasks.add(task);
            saveTasks();
            ui.showTaskAdded(task, tasks.size());
            break;
        case BY:
        case FROM:
            Parser.DateFilter filter = parser.parseDateFilter(arguments, command);
            ui.showTasks(tasks.filterByDate(filter.date(), command, filter.isSharp()), true);
            break;
        case BYE:
            // Standalone bye commands are handled by the run loop.
            break;
        }
        return false;
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
