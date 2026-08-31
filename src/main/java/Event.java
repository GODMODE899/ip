/**
 * Represents a task that occurs between specified start and end times.
 */
public class Event extends Task {
    private final String from;
    private final String to;

    /**
     * Creates an Event with its description, start, and end text.
     *
     * @param description description of the task
     * @param from start text supplied by the user
     * @param to end text supplied by the user
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the event's starting-time text.
     *
     * @return starting-time text
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the event's ending-time text.
     *
     * @return ending-time text
     */
    public String getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }
}
