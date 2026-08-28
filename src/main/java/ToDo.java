/**
 * Represents a task without an attached date or time.
 */
public class ToDo extends Task {
    /**
     * Creates a ToDo with the given description.
     *
     * @param description description of the task
     */
    public ToDo(String description) {
        super(description);
    }

    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
