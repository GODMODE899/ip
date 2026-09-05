package anaconda.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import anaconda.exception.AnacondaException;
import anaconda.parser.Command;

/**
 * Owns the task collection and provides task operations independently of input or storage.
 */
public class TaskList {
    private final ArrayList<Task> tasks;

    /**
     * Creates an empty task list.
     */
    public TaskList() {
        this(List.of());
    }

    /**
     * Creates a task list containing previously loaded tasks in their saved order.
     *
     * @param initialTasks Tasks to copy into the collection.
     */
    public TaskList(List<Task> initialTasks) {
        tasks = new ArrayList<>(initialTasks);
    }

    /**
     * Adds a task at the end of the list.
     *
     * @param task Task to add.
     */
    public void add(Task task) {
        tasks.add(task);
    }

    /**
     * Deletes a task using the one-based number shown to the user.
     *
     * @param taskNumber Task number to remove.
     * @return Removed task.
     * @throws AnacondaException If the task does not exist.
     */
    public Task delete(int taskNumber) throws AnacondaException {
        return tasks.remove(toIndex(taskNumber));
    }

    /**
     * Changes a task's completion status.
     *
     * @param taskNumber One-based task number.
     * @param isDone Whether the task should be completed.
     * @return Updated task.
     * @throws AnacondaException If the task does not exist.
     */
    public Task mark(int taskNumber, boolean isDone) throws AnacondaException {
        Task task = tasks.get(toIndex(taskNumber));
        if (isDone) {
            task.markAsDone();
        } else {
            task.markAsUndone();
        }
        return task;
    }

    /**
     * Removes every task from the list.
     */
    public void clear() {
        tasks.clear();
    }

    /**
     * Returns the number of tasks in the list.
     *
     * @return Current task count.
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns a structurally unmodifiable snapshot for display or saving.
     * The task objects themselves are shared, not copied.
     *
     * @return Tasks in their current order.
     */
    public List<Task> asList() {
        return List.copyOf(tasks);
    }

    /**
     * Finds tasks whose descriptions contain the supplied keyword, ignoring case.
     *
     * @param keyword Text to search for in task descriptions.
     * @return Matching tasks in their original order.
     */
    public List<Task> find(String keyword) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        ArrayList<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getDescription().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                matchingTasks.add(task);
            }
        }
        return List.copyOf(matchingTasks);
    }

    /**
     * Finds dated tasks by their deadline or event ending date without changing the list.
     *
     * @param filterDate Date to compare against.
     * @param direction BY for on-or-before, or FROM for on-or-after.
     * @param isSharp Whether only exact date matches should be returned.
     * @return Matching tasks in their original order.
     */
    public List<Task> filterByDate(LocalDate filterDate, Command direction, boolean isSharp) {
        ArrayList<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks) {
            LocalDate endDate = getEndDate(task);
            if (endDate == null) {
                continue;
            }

            boolean isMatch;
            if (isSharp) {
                isMatch = endDate.equals(filterDate);
            } else if (direction == Command.BY) {
                isMatch = !endDate.isAfter(filterDate);
            } else {
                isMatch = !endDate.isBefore(filterDate);
            }
            if (isMatch) {
                matchingTasks.add(task);
            }
        }
        return List.copyOf(matchingTasks);
    }

    /**
     * Validates a user-facing task number before converting it to a list index.
     */
    private int toIndex(int taskNumber) throws AnacondaException {
        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new AnacondaException("Task " + taskNumber + " does not exist.");
        }
        return taskNumber - 1;
    }

    /**
     * Returns a dated task's ending date, or null for an undated task.
     */
    private LocalDate getEndDate(Task task) {
        if (task instanceof Deadline deadline) {
            return deadline.getBy();
        }
        if (task instanceof Event event) {
            return event.getTo();
        }
        return null;
    }
}
