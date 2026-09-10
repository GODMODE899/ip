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
`undo` takes no arguments. Command names are case-insensitive.

List/search/date-filter commands, invalid commands, and cancelled clears do not consume history.
While a clear confirmation is pending, any response other than `yes` (including `undo`) cancels the
clear; enter `undo` again to undo the previous task change. A successful repeated mark/unmark or an
empty confirmed clear still counts as a command and creates an undo step, even if the state is unchanged.

History belongs to the current running session and is not loaded from disk after restarting.
There is no redo command. If saving a change or undo fails, the in-memory task list and undo history
are retained so the operation can be retried.
