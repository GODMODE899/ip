package anaconda;

import java.util.List;

/**
 * Represents a task that can be marked as done or not done.
 */
public class Task {
    private final String description;
    private boolean isDone;

    /**
     * Creates an incomplete task with the given description.
     *
     * @param description Description of the task.
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /** Marks this task as completed. */
    public void markAsDone() {
        isDone = true;
    }

    /** Marks this task as incomplete. */
    public void markAsUndone() {
        isDone = false;
    }

    /**
     * Returns the description used to identify this task.
     *
     * @return Task description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Reports whether this task has been completed.
     *
     * @return {@code true} if the task is done.
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Formats all stored tasks as a numbered list.
     *
     * @param tasks Tasks to format.
     * @return Numbered task list.
     */
    public static String displayList(List<Task> tasks) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < tasks.size(); i++) {
            output.append(i + 1).append('.').append(tasks.get(i)).append('\n');
        }
        return output.toString();
    }

    @Override
    public String toString() {
        String statusIcon = isDone ? "X" : " ";
        return "[" + statusIcon + "] " + description;
    }
}
