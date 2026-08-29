# Level 5 Console UI Test Plan

- Working directory: repository root
- Java version: Microsoft OpenJDK 25.0.4.1
- Build command: `C:\Users\moons\.jdks\ms-25.0.4.1\bin\javac.exe -d out\test-classes src\main\java\Anaconda.java src\main\java\AnacondaException.java src\main\java\Task.java src\main\java\ToDo.java src\main\java\Deadline.java src\main\java\Event.java`
- Launch command: `C:\Users\moons\.jdks\ms-25.0.4.1\bin\java.exe -cp out\test-classes Anaconda`
- Process timeout: 10 seconds per test case
- Comparison: normalize CRLF/LF line endings and one final newline only
- Process isolation: launch a fresh program process for every test case

## Test case: TC01 - Add and list all task types

### Aim

Verify that valid ToDo, Deadline, and Event commands still work after exception handling is introduced.

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

## Test case: TC02 - Recover from general and task-number errors

### Aim

Verify that empty and unknown commands, invalid task numbers, and extra arguments produce friendly errors without changing valid task state.

### Inputs

```text

todo
blah
todo read book
mark two
mark 2
mark 1
unmark 0
unmark 1
list extra
list
bye now
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
Oops! Please enter a command.
____________________________________________________________
____________________________________________________________
Oops! The description of a todo cannot be empty.
____________________________________________________________
____________________________________________________________
Oops! I don't recognize that command.
____________________________________________________________
____________________________________________________________
Got it. I've added this task:
  [T][ ] read book
Now you have 1 tasks in the list.
____________________________________________________________
____________________________________________________________
Oops! Please provide one task number.
____________________________________________________________
____________________________________________________________
Oops! Task 2 does not exist.
____________________________________________________________
____________________________________________________________
Marked it done for you:
  [T][X] read book
____________________________________________________________
____________________________________________________________
Oops! Task 0 does not exist.
____________________________________________________________
____________________________________________________________
Really? Unmarked? Alright . . .
  [T][ ] read book
____________________________________________________________
____________________________________________________________
Oops! The list command does not take extra text.
____________________________________________________________
____________________________________________________________
Your list:
1.[T][ ] read book
____________________________________________________________
____________________________________________________________
Oops! The bye command cannot have extra text.
____________________________________________________________
____________________________________________________________
Alright, until next time.
____________________________________________________________
```

## Test case: TC03 - Recover from malformed deadlines and events

### Aim

Verify that missing deadline and event fields are rejected and that later valid typed tasks are stored with the correct numbering and values.

### Inputs

```text
deadline return book
deadline return book /by
deadline return book /by Sunday
event project meeting /from Mon
event project meeting /from /to 4pm
event project meeting /from Mon /to 4pm
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
Oops! A deadline needs '/by' followed by a date or time.
____________________________________________________________
____________________________________________________________
Oops! A deadline needs a date or time after '/by'.
____________________________________________________________
____________________________________________________________
Got it. I've added this task:
  [D][ ] return book (by: Sunday)
Now you have 1 tasks in the list.
____________________________________________________________
____________________________________________________________
Oops! An event needs both '/from' and '/to' times.
____________________________________________________________
____________________________________________________________
Oops! An event needs times after both '/from' and '/to'.
____________________________________________________________
____________________________________________________________
Got it. I've added this task:
  [E][ ] project meeting (from: Mon to: 4pm)
Now you have 2 tasks in the list.
____________________________________________________________
____________________________________________________________
Your list:
1.[D][ ] return book (by: Sunday)
2.[E][ ] project meeting (from: Mon to: 4pm)
____________________________________________________________
____________________________________________________________
Alright, until next time.
____________________________________________________________
```
