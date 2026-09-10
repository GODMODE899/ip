# Undo

Type `undo` to reverse the most recent successfully saved task-changing command.
It works for `todo`, `deadline`, `event`, `mark`, `unmark`, `delete`, and confirmed `clear` commands.
Deleted or cleared tasks return in their original order, with their dates and completion statuses.
The restored task list is saved automatically. Repeated `undo` commands walk backward through history.

For example:

```text
todo read book
mark 1
clear
yes
undo
```

The book task returns marked as done. Another `undo` makes it incomplete, and another removes it.

An empty history raises `AnacondaException`, displayed as `Oops! There is nothing to undo.`
Command names are case-insensitive.

Type `undo undo` to reverse the most recent undo. Repeating it reapplies undone commands in order,
including deletes and confirmed clears, and saves the resulting list. You can alternate `undo` and
`undo undo` to move backward and forward within the same chain. For example:

```text
todo read book
mark 1
undo
undo undo
```

The first undo makes the book incomplete; `undo undo` marks it done again.

This is available only during consecutive `undo` and `undo undo` commands. Any other input ends
the chain, including list/search/date-filter commands, invalid input, a new task change, and a clear
request (even if cancelled). Ordinary undo history remains available; the next successful `undo`
starts a new chain. `undo undo` with nothing available raises `AnacondaException`, displayed as
`Oops! There is no undo to reverse.` An exhausted history or a failed undo/redo save does not end the chain.
Only `undo` and `undo undo` are accepted; extra arguments such as `undo undo undo` are rejected.

List/search/date-filter commands, invalid commands, and cancelled clears do not consume ordinary undo history.
While a clear confirmation is pending, any response other than `yes` (including either undo command) cancels the
clear; enter `undo` again to undo the previous task change. A successful repeated mark/unmark or an
empty confirmed clear still counts as a command and creates an undo step, even if the state is unchanged.

History belongs to the current running session and is not loaded from disk after restarting.
If saving a change, undo, or undo undo fails, the in-memory task list and ordinary undo history
are retained so the operation can be retried. A failed undo or undo undo also retains the chain's
redo history. A different command ends that chain even if it fails to save.
