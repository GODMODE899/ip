package anaconda;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

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
     * States before successfully saved mutations, most recent first; history lasts for this session.
     */
    private final Deque<TaskList.Snapshot> undoHistory = new ArrayDeque<>();
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
                    clearTasksIfConfirmed(input, ui);
                } else {
                    isAwaitingClearConfirmation = handleCommand(input, ui);
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
     * Captures only this response without changing JVM-wide streams.
     *
     * @param input Complete user input.
     * @return Response to display in the GUI.
     */
    public String getResponse(String input) {
        if (!isAwaitingGuiClearConfirmation && parser.isExitCommand(input)) {
            return "Alright, until next time.";
        }

        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
        try (PrintStream responseOutput = new PrintStream(responseBuffer, true, StandardCharsets.UTF_8);
                Ui responseUi = new Ui(InputStream.nullInputStream(), responseOutput)) {
            processGuiInput(input, responseUi);
        }
        return responseBuffer.toString(StandardCharsets.UTF_8).stripTrailing();
    }

    /**
     * Processes a GUI command or a pending clear confirmation using the existing command handlers.
     */
    private void processGuiInput(String input, Ui responseUi) {
        try {
            if (isAwaitingGuiClearConfirmation) {
                isAwaitingGuiClearConfirmation = false;
                clearTasksIfConfirmed(input, responseUi);
            } else {
                isAwaitingGuiClearConfirmation = handleCommand(input, responseUi);
            }
        } catch (AnacondaException exception) {
            responseUi.showError(exception.getMessage());
        }
    }

    /**
     * Dispatches a parsed command to the task list, storage, and user interface.
     *
     * @param input Complete user input.
     * @param responseUi Destination for this command's messages.
     * @return Whether the next input must confirm a clear operation.
     * @throws AnacondaException If the command is invalid or saving fails.
     */
    private boolean handleCommand(String input, Ui responseUi) throws AnacondaException {
        Parser.ParsedCommand parsedCommand = parser.parse(input);
        Command command = parsedCommand.command();
        String arguments = parsedCommand.arguments();

        switch (command) {
            case LIST -> responseUi.showTasks(tasks.asList(), false);
            case MARK, UNMARK -> changeTaskStatus(arguments, command == Command.MARK, responseUi);
            case DELETE -> deleteTask(arguments, responseUi);
            case UNDO -> undoTaskChange(responseUi);
            case CLEAR -> {
                responseUi.showClearQuestion();
                return true;
            }
            case FIND -> findTasks(arguments, responseUi);
            case TODO, DEADLINE, EVENT -> addTask(command, arguments, responseUi);
            case BY, FROM -> filterTasksByDate(command, arguments, responseUi);
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
    private void changeTaskStatus(String arguments, boolean isDone, Ui responseUi) throws AnacondaException {
        TaskList.Snapshot previousState = tasks.snapshot();
        Task task = tasks.mark(parser.parseTaskNumber(arguments), isDone);
        saveChange(previousState);
        responseUi.showMarked(task, isDone);
    }

    /**
     * Removes the selected task and reports success only after saving.
     */
    private void deleteTask(String arguments, Ui responseUi) throws AnacondaException {
        TaskList.Snapshot previousState = tasks.snapshot();
        Task removedTask = tasks.delete(parser.parseTaskNumber(arguments));
        saveChange(previousState);
        responseUi.showTaskRemoved(removedTask, tasks.size());
    }

    /**
     * Creates a task and reports success only after saving.
     */
    private void addTask(Command command, String arguments, Ui responseUi) throws AnacondaException {
        Task task = parser.parseTask(command, arguments);
        TaskList.Snapshot previousState = tasks.snapshot();
        tasks.add(task);
        saveChange(previousState);
        responseUi.showTaskAdded(task, tasks.size());
    }

    /**
     * Displays tasks matching a validated description keyword.
     */
    private void findTasks(String arguments, Ui responseUi) throws AnacondaException {
        String keyword = parser.parseKeyword(arguments);
        responseUi.showFindResults(tasks.find(keyword));
    }

    /**
     * Displays tasks matching a validated date filter.
     */
    private void filterTasksByDate(Command command, String arguments, Ui responseUi) throws AnacondaException {
        Parser.DateFilter filter = parser.parseDateFilter(arguments, command);
        responseUi.showTasks(tasks.filterByDate(filter.date(), command, filter.isSharp()), true);
    }

    /**
     * Clears and saves tasks only after explicit approval.
     */
    private void clearTasksIfConfirmed(String confirmation, Ui responseUi) throws AnacondaException {
        if (!parser.isClearConfirmed(confirmation)) {
            responseUi.showClearCancelled();
            return;
        }
        TaskList.Snapshot previousState = tasks.snapshot();
        tasks.clear();
        saveChange(previousState);
        responseUi.showCleared();
    }

    /**
     * Restores the most recent saved mutation, retaining history if the restored state cannot be saved.
     */
    private void undoTaskChange(Ui responseUi) throws AnacondaException {
        if (undoHistory.isEmpty()) {
            throw new AnacondaException("There is nothing to undo.");
        }
        TaskList.Snapshot currentState = tasks.snapshot();
        tasks.restore(undoHistory.peek());
        saveOrRestore(currentState);
        undoHistory.pop();
        responseUi.showUndo();
    }

    /**
     * Saves a mutation before making its previous state available for undo.
     */
    private void saveChange(TaskList.Snapshot previousState) throws AnacondaException {
        saveOrRestore(previousState);
        undoHistory.push(previousState);
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
     * Saves the current list, restoring its previous in-memory state if writing fails.
     */
    private void saveOrRestore(TaskList.Snapshot previousState) throws AnacondaException {
        try {
            storage.saveTasks(tasks.asList());
        } catch (IOException exception) {
            tasks.restore(previousState);
            throw new AnacondaException("I couldn't save your task list.");
        }
    }
}
