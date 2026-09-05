package anaconda.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import anaconda.task.Deadline;
import anaconda.task.Event;
import anaconda.task.Task;
import anaconda.task.ToDo;

/**
 * Tests storage using temporary files, never the user's real saved tasks.
 */
public class StorageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    public void loadTasks_missingFileAndFolder_returnsEmptyListWithoutCreatingFile() throws IOException {
        Path file = temporaryDirectory.resolve("missing/data/tasks.txt");
        assertTrue(new Storage(file).loadTasks().isEmpty());
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(file.getParent()));
    }

    @Test
    public void loadTasks_emptyFile_returnsEmptyList() throws IOException {
        Path file = Files.createFile(temporaryDirectory.resolve("tasks.txt"));
        assertTrue(new Storage(file).loadTasks().isEmpty());
    }

    @Test
    public void loadTasks_handwrittenData_restoresTypesOrderStatusesAndDates() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Files.writeString(file, "T | 1 | read book\nD | 0 | return book | 2024-02-29\n"
                + "E | 1 | meeting | 2026-08-18 | 2026-08-19\n");
        List<Task> tasks = new Storage(file).loadTasks();

        assertEquals(3, tasks.size());
        assertInstanceOf(ToDo.class, tasks.get(0));
        assertEquals("read book", tasks.get(0).getDescription());
        assertTrue(tasks.get(0).isDone());
        Deadline deadline = assertInstanceOf(Deadline.class, tasks.get(1));
        assertEquals("return book", deadline.getDescription());
        assertFalse(deadline.isDone());
        assertEquals(LocalDate.of(2024, 2, 29), deadline.getBy());
        Event event = assertInstanceOf(Event.class, tasks.get(2));
        assertEquals("meeting", event.getDescription());
        assertTrue(event.isDone());
        assertEquals(LocalDate.of(2026, 8, 18), event.getFrom());
        assertEquals(LocalDate.of(2026, 8, 19), event.getTo());
    }

    @Test
    public void saveTasks_allTypesAndStatuses_writesExactIsoFormatAndCreatesParents() throws IOException {
        Path file = temporaryDirectory.resolve("new/data/tasks.txt");
        LocalDate date = LocalDate.of(2026, 8, 19);
        Task todo = new ToDo("read book");
        Task deadline = new Deadline("return book", date);
        Task event = new Event("meeting", date.minusDays(1), date);
        Storage storage = new Storage(file);
        storage.saveTasks(List.of(todo, deadline, event));
        assertEquals(List.of("T | 0 | read book", "D | 0 | return book | 2026-08-19",
                "E | 0 | meeting | 2026-08-18 | 2026-08-19"), Files.readAllLines(file));

        todo.markAsDone();
        deadline.markAsDone();
        event.markAsDone();
        storage.saveTasks(List.of(todo, deadline, event));
        assertEquals(List.of("T | 1 | read book", "D | 1 | return book | 2026-08-19",
                "E | 1 | meeting | 2026-08-18 | 2026-08-19"), Files.readAllLines(file));
    }

    @Test
    public void saveTasks_shorterOrEmptyList_replacesRatherThanAppends() throws IOException {
        Path file = temporaryDirectory.resolve("tasks.txt");
        Storage storage = new Storage(file);
        storage.saveTasks(List.of(new ToDo("first"), new ToDo("second")));
        storage.saveTasks(List.of(new ToDo("replacement")));
        assertEquals(List.of("T | 0 | replacement"), Files.readAllLines(file));
        storage.saveTasks(List.of());
        assertEquals(0L, Files.size(file));
        assertTrue(storage.loadTasks().isEmpty());
    }

    @Test
    public void saveAndLoad_unicodeAndRepeatedLoads_preserveTextWithoutSharingTaskInstances()
            throws IOException {
        Storage storage = new Storage(temporaryDirectory.resolve("tasks.txt"));
        Task original = new ToDo("读书 — café  break");
        storage.saveTasks(List.of(original));
        Task firstLoad = storage.loadTasks().getFirst();
        assertEquals(original.getDescription(), firstLoad.getDescription());
        assertNotSame(original, firstLoad);
        firstLoad.markAsDone();
        assertFalse(storage.loadTasks().getFirst().isDone());
    }

    @Test
    public void loadTasks_directoryInsteadOfFile_throwsIOException() {
        assertThrows(IOException.class, () -> new Storage(temporaryDirectory).loadTasks());
    }

    @Test
    public void saveTasks_directoryInsteadOfFile_throwsIOException() {
        assertThrows(IOException.class, () -> new Storage(temporaryDirectory).saveTasks(List.of()));
    }

    @Test
    public void saveTasks_parentIsAFile_throwsIOException() throws IOException {
        Path parentFile = Files.createFile(temporaryDirectory.resolve("not-a-folder"));
        Storage storage = new Storage(parentFile.resolve("tasks.txt"));
        assertThrows(IOException.class, () -> storage.saveTasks(List.of(new ToDo("book"))));
    }
}
