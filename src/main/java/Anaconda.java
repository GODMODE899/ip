import java.util.Scanner;

public class Anaconda {
    public static void main(String[] args) {
        String[] list = new String[100];
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
                for(int i = 0; i < listNum; i++)
                    System.out.println(i + 1 + ". " + list[i]);
            }
            else {
                System.out.println("Added: " + s + ". what else?");
                list[listNum++] = s;
            }
            System.out.println(line);
        }
        System.out.println(exit);
    }
}