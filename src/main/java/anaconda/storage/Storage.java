package anaconda.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import anaconda.task.Deadline;
import anaconda.task.Event;
import anaconda.task.Task;
import anaconda.task.ToDo;

/**
 * Loads tasks from and saves tasks to a file on the user's computer.
 */
public class Storage {
    private static final String FIELD_SEPARATOR = " | ";

    private final Path filePath;

    /**
     * Creates a storage manager that uses the given data file.
     *
     * @param filePath Relative or absolute path to the data file.
     */
    public Storage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads all tasks from the data file. An empty list is returned when the file
     * does not exist yet, which is expected when Anaconda is run for the first time.
     *
     * @return Tasks stored in the data file.
     * @throws IOException If the existing data file cannot be read.
     */
    public ArrayList<Task> loadTasks() throws IOException {
        ArrayList<Task> tasks = new ArrayList<>();
        if (!Files.exists(filePath)) {
            return tasks;
        }

        for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
            tasks.add(parseTask(line));
        }
        return tasks;
    }

    /**
     * Replaces the data file contents with the current task list, creating the
     * parent folder first when necessary.
     *
     * @param tasks Current tasks to save.
     * @throws IOException If the tasks cannot be written.
     */
    public void saveTasks(List<Task> tasks) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        ArrayList<String> lines = new ArrayList<>();
        for (Task task : tasks) {
            lines.add(formatTask(task));
        }
        Files.write(filePath, lines, StandardCharsets.UTF_8);
    }

    /**
     * Converts one task into its storage-file representation.
     *
     * @param task Task to convert.
     * @return Storage-file representation of the task.
     */
    private String formatTask(Task task) {
        String status = task.isDone() ? "1" : "0";
        if (task instanceof Deadline deadline) {
            return "D" + FIELD_SEPARATOR + status + FIELD_SEPARATOR
                    + task.getDescription() + FIELD_SEPARATOR + deadline.getBy();
        }
        if (task instanceof Event event) {
            return "E" + FIELD_SEPARATOR + status + FIELD_SEPARATOR
                    + task.getDescription() + FIELD_SEPARATOR + event.getFrom()
                    + FIELD_SEPARATOR + event.getTo();
        }
        return "T" + FIELD_SEPARATOR + status + FIELD_SEPARATOR + task.getDescription();
    }

    /**
     * Converts one storage-file line back into a task.
     *
     * @param line Storage-file line to convert.
     * @return Task represented by the line.
     */
    private Task parseTask(String line) {
        String[] fields = line.split(" \\| ", -1);
        Task task = switch (fields[0]) {
            case "T" -> new ToDo(fields[2]);
            case "D" -> new Deadline(fields[2], LocalDate.parse(fields[3]));
            case "E" -> new Event(fields[2], LocalDate.parse(fields[3]), LocalDate.parse(fields[4]));
            default -> throw new IllegalArgumentException("Unknown task type: " + fields[0]);
        };

        if (fields[1].equals("1")) {
            task.markAsDone();
        }
        return task;
    }
}
