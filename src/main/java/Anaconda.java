import java.util.Scanner;

public class Anaconda {
    public static void main(String[] args) {
        Task[] list = new Task[100];
        int listNum = 0;
        String line = "____________________________________________________________";

        String exit = line + "\nAlright, until next time.\n" + line;

        String banner =
                "    _    _   _    _    ____ ___  _   _ ____    _    \n"
                        + "   / \\  | \\ | |  / \\  / ___/ _ \\| \\ | |  _ \\  / \\   \n"
                        + "  / _ \\ |  \\| | / _ \\| |  | | | |  \\| | | | |/ _ \\  \n"
                        + " / ___ \\| |\\  |/ ___ \\ |__| |_| | |\\  | |_| / ___ \\ \n"
                        + "/_/   \\_\\_| \\_/_/   \\_\\____\\___/|_| \\_|____/_/   \\_\\\n";
        System.out.println(line);
        System.out.println(banner);
        System.out.println("Yo, it's Anaconda.");
        System.out.println("What do you want?");

        Scanner scanner = new Scanner(System.in);
        String s;
        while(true) {
            s = scanner.next();
            if(s.equalsIgnoreCase("bye")) break;

            System.out.println(line);
            if(s.equalsIgnoreCase("list")) {
                System.out.println("Your list:");

                System.out.print(Task.displayList(list));
            }
            else if(s.equalsIgnoreCase("mark")) {
                System.out.println("Marked it down for you:");
                int x = scanner.nextInt();
                list[x - 1].markAsDone();
                System.out.println(list[x - 1]);
            }
            else if(s.equalsIgnoreCase("unmark")) {
                System.out.println("Unmarked?? Sure . . . done:");
                int x = scanner.nextInt();
                list[x - 1].markAsUndone();
                System.out.println(list[x - 1]);
            }
            else {
                System.out.println("Added: " + s + ". what else?");
                list[listNum++] = new Task(s);
            }
            System.out.println(line);
        }
        System.out.println(exit);
    }
}

class Task {
    String task;
    boolean done;

    public Task(String t) {
        task = t;
        done = false;
    }

    public void markAsDone() {
        done = true;
    }

    public void markAsUndone() {
        done = false;
    }

    public static String displayList(Task[] list) {
        String s = "";
        int i = 1;
        for(Task t : list) {
            if(t == null) break;
            s += i++ + "." + t + "\n";
        }

        return s;
    }

    public String toString() {
        String x = done ? "X" : " ";
        return "[" + x + "] " + task;
    }
        }