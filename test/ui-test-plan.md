# Level 4 Console UI Test Plan

- Working directory: repository root
- Java version: Microsoft OpenJDK 25.0.4.1
- Build command: `C:\Users\moons\.jdks\ms-25.0.4.1\bin\javac.exe -d out\test-classes src\main\java\Anaconda.java src\main\java\Task.java src\main\java\ToDo.java src\main\java\Deadline.java src\main\java\Event.java`
- Launch command: `C:\Users\moons\.jdks\ms-25.0.4.1\bin\java.exe -cp out\test-classes Anaconda`
- Process timeout: 10 seconds per test case
- Comparison: normalize CRLF/LF line endings and one final newline only
- Process isolation: launch a fresh program process for every test case

## Test case: TC01 - Add and list all Level 4 task types

### Aim

Verify that multi-word ToDo, Deadline, and Event commands are parsed into the correct task types, that their date/time portions remain strings, and that `list` preserves their insertion order.

### Inputs

```text
todo borrow book
deadline return book /by Sunday
event project meeting /from Mon 2pm /to 4pm
list
bye
```

### Expected output

```text
____________________________________________________________
    _    _   _    _    ____ ___  _   _ ____    _
   / \  | \ | |  / \  / ___/ _ \| \ | |  _ \  / \
  / _ \ |  \| | / _ \| |  | | | |  \| | | | |/ _ \
 / ___ \| |\  |/ ___ \ |__| |_| | |\  | |_| / ___ \
/_/   \_\_| \_/_/   \_\____\___/|_| \_|____/_/   \_\

Yo, it's Anaconda.
What do you want?
____________________________________________________________
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
____________________________________________________________
____________________________________________________________
Got it. I've added this task:
  [D][ ] return book (by: Sunday)
Now you have 2 tasks in the list.
____________________________________________________________
____________________________________________________________
Got it. I've added this task:
  [E][ ] project meeting (from: Mon 2pm to: 4pm)
Now you have 3 tasks in the list.
____________________________________________________________
____________________________________________________________
Your list:
1.[T][ ] borrow book
2.[D][ ] return book (by: Sunday)
3.[E][ ] project meeting (from: Mon 2pm to: 4pm)
____________________________________________________________
____________________________________________________________
Alright, until next time.
____________________________________________________________
```

## Test case: TC02 - Mark and unmark a typed task

### Aim

Verify that a ToDo remains typed as `[T]` when its completion status changes and that the final list displays its unmarked state.

### Inputs

```text
todo read book
mark 1
unmark 1
list
bye
```

### Expected output

```text
____________________________________________________________
    _    _   _    _    ____ ___  _   _ ____    _
   / \  | \ | |  / \  / ___/ _ \| \ | |  _ \  / \
  / _ \ |  \| | / _ \| |  | | | |  \| | | | |/ _ \
 / ___ \| |\  |/ ___ \ |__| |_| | |\  | |_| / ___ \
/_/   \_\_| \_/_/   \_\____\___/|_| \_|____/_/   \_\

Yo, it's Anaconda.
What do you want?
____________________________________________________________
Got it. I've added this task:
  [T][ ] read book
Now you have 1 tasks in the list.
____________________________________________________________
____________________________________________________________
Marked it done for you:
  [T][X] read book
____________________________________________________________
____________________________________________________________
Really? Unmarked? Alright . . .
  [T][ ] read book
____________________________________________________________
____________________________________________________________
Your list:
1.[T][ ] read book
____________________________________________________________
____________________________________________________________
Alright, until next time.
____________________________________________________________
```
