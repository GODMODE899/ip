/**
 * Represents a task that can be marked as done or not done.
 */
public class Task {
    private final String description;
    private boolean isDone;

    /**
     * Creates an incomplete task with the given description.
     *
     * @param description description of the task
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
     * Formats all stored tasks as a numbered list.
     *
     * @param tasks task array whose unused entries are {@code null}
     * @return numbered task list
     */
    public static String displayList(Task[] tasks) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < tasks.length && tasks[i] != null; i++) {
            output.append(i + 1).append('.').append(tasks[i]).append('\n');
        }
        return output.toString();
    }

    @Override
    public String toString() {
        String statusIcon = isDone ? "X" : " ";
        return "[" + statusIcon + "] " + description;
    }
}
