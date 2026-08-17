import java.time.LocalDate;
import java.util.Scanner;

public class CSVreader {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "docs/data.csv";

        OpportunityManager manager = new OpportunityManager();
        manager.loadFile(path);
        manager.sortAll();
        manager.mainQueue();
        manager.listExpiredQueue();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("-----Main Menu-----");
            System.out.println("1. View table");
            System.out.println("2. View Internship List");
            System.out.println("3. View Scholarship List");
            System.out.println("4. Add Opportunity");
            System.out.println("5. Search by name");
            System.out.println("6. View next upcoming deadline");
            System.out.println("7. View expired opportunities");
            System.out.println("8. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {
                manager.displayAll();
            } else if (choice == 2) {
                System.out.println("----Internships----");
                manager.displayInternship();
            } else if (choice == 3) {
                System.out.println("----Scholarships----");
                manager.displayScholarship();
            } else if (choice == 4) {
                System.out.print("Enter name: ");
                String name = scanner.nextLine();

                System.out.print("Type of opportunity (Internship/Scholarship): ");
                String type = scanner.nextLine();

                System.out.print("Enter link: ");
                String link = scanner.nextLine();

                System.out.print("Status (Open/On Hold/Close): ");
                String status = scanner.nextLine();

                System.out.print("Enter deadline (YYYY-MM-DD): ");
                LocalDate deadline = LocalDate.parse(scanner.nextLine());

                System.out.print("Comments (optional): ");
                String comments = scanner.nextLine();

                Opportunity opportunity = manager.createOpp(name, type, link, deadline, status, comments);
                manager.addNew(opportunity);
                manager.sortAll();
                manager.mainQueue();
                manager.saveFile(path);

                System.out.println("Added to list");
            } else if (choice == 5) {
                System.out.print("Enter name to search: ");
                String searchName = scanner.nextLine();

                Opportunity found = manager.searchByName(searchName);
                if (found != null) {
                    found.displayInfo();
                } else {
                    System.out.println("Not on list");
                }
            } else if (choice == 6) {
                Opportunity next = manager.nextUpcoming();
                if (next != null) {
                    System.out.println("Upcoming deadline:");
                    next.displayInfo();
                } else {
                    System.out.println("No upcoming deadlines.");
                }
            } else if (choice == 7) {
                manager.listExpiredQueue();
                manager.displayExpired(scanner, path);
            } else if (choice == 8) {
                System.out.println("Ending program");
                break;
            } else {
                System.out.println("Invalid choice");
            }
        }
    }
}
