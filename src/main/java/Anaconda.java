import java.util.Scanner;

public class Anaconda {
    public static void main(String[] args) {
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
        do {
            s = scanner.next();
            System.out.println(line);
            System.out.println(s);
            System.out.println(line);
        }
        while (!s.equalsIgnoreCase("bye"));
        System.out.println(exit);
    }
}