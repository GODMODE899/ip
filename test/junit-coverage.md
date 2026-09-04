# JUnit testing

Tests follow the [course's JUnit tutorial](https://se-education.org/guides/tutorials/junit.html):
`src/test/java` is the test source root, packages mirror the production packages, and test classes use
the `ClassNameTest` suffix. Method names use `methodUnderTest_scenario_expectedBehavior` where practical.

## Target

The baseline guidance prioritizes the highest-value 50% of methods. For this project we use the requested
stretch target instead: JUnit tests for all non-trivial public methods of all application classes.
Add or update tests after each code change to maintain this target. This is a behavioral coverage map,
not a claim of measured 100% line or branch coverage; no coverage instrumentation is configured.

## Coverage map

| Production class | Methods / behavior tested | Important cases |
| --- | --- | --- |
| `Parser` | `parse`, `parseTask`, `parseTaskNumber`, `parseDateFilter`, `isExitCommand`, `isClearConfirmed` | All command types, aliases, whitespace, required fields, invalid markers, both date formats, leap dates, integer boundaries/overflow, sharp modifiers, helpful errors |
| `TaskList` | Constructors, `add`, `delete`, `mark`, `clear`, `size`, `asList`, `filterByDate` | Defensive collection copy, ordering, first/last indices, invalid indices, repeated status changes, empty lists, unmodifiable snapshots, inclusive/exact dates, event end dates, ToDo exclusion |
| `Storage` | `loadTasks`, `saveTasks` | Missing file/folder, empty file, independently specified input/output formats, all task types/statuses, dates, overwrite/truncate, Unicode, independent loads, file I/O failures |
| `Task` | Constructor, getters, `markAsDone`, `markAsUndone`, `toString` | Initial state, repeated transitions, description preservation, status markers |
| `ToDo` | `toString` | Type marker and both completion states |
| `Deadline` | Date getter, `toString` | Date retention, leading zero, both completion states, leap day, English month under a different locale |
| `Event` | Date getters, `toString` | Distinct endpoints, year boundary, same-day event, both completion states, English month under a different locale |
| `Ui` | All public methods, including `readCommand` and `close` | Exact messages, banner, list headers/numbering, empty lists, counts, clear prompts, input trimming, reader closure |
| `Anaconda` | Constructor, `run`, `main` | Command dispatch, saved state after restart, invalid-command recovery, clear confirmation/cancellation, filters, load/save errors, relative default path in a child JVM |

`Command` contains only enum constants, and `AnacondaException` only forwards a message to its superclass;
neither has custom non-trivial methods needing a dedicated test class. Their behavior is exercised by parser
and application tests. The parser's nested records are covered through their returned values.

The tests preserve current behavior; they do not add new date-order validation, storage escaping, or recovery
from corrupted storage files. Corrupted-file recovery remains outside the implemented feature set.

## Running the tests

Use Java 25. From the repository root in PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests anaconda.parser.ParserTest
```

On macOS/Linux use `./gradlew` instead. If Gradle reports the test task as up-to-date and you want to force
another run, use `test --rerun-tasks`. The HTML report is `build/reports/tests/test/index.html`.
The existing Gradle JUnit dependencies are sufficient; no additional framework or mocking dependency is used.

Storage/application tests use JUnit `@TempDir`, never the project's `data/anaconda.txt`.
Console tests restore `System.in`/`System.out` in try-with-resources and share a resource lock;
locale tests restore the original formatting locale in `finally`.

The separate console regression plan in `test/ui-test-plan.md` remains useful alongside these JUnit tests.
Its session transcript presents each input immediately followed by its output.
