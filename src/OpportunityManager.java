import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

//manages the heavy work
public class OpportunityManager {
    HashTableEntry hashTable = new HashTableEntry(11);

    private final ArrayList<Opportunity> internship = new ArrayList<>();
    private final ArrayList<Opportunity> scholarship = new ArrayList<>();

    private Queue<Opportunity> upcomingDeadline = new Queue<>();
    private Queue<Opportunity> expiredQueue = new Queue<>();

    // CSV schema: ID,Name,Type,Link,Status,DueDate,Comments,Flagged,FlagNote,SuggestedReopenDate,Description
    private static final String HEADER = "ID,Name,Type,Link,Status,DueDate,Comments,Flagged,FlagNote,SuggestedReopenDate,Description";

    //---------- loading ----------
    // reads the shared data file and populates this manager. Safe to call once at startup.
    public void loadFile(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            System.out.println("No data file found at " + filePath + " — starting empty.");
            return;
        }
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return; // empty file
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = splitCsvLine(line);
                if (cols.length < 9) continue; // skip malformed rows

                String name = cols[1].trim();
                String type = cols[2].trim();
                String link = cols[3].trim();
                String status = cols[4].trim();
                LocalDate deadline = LocalDate.parse(cols[5].trim());
                String comments = cols[6].trim();
                boolean flagged = Boolean.parseBoolean(cols[7].trim());
                String flagNote = cols[8].trim();
                String suggestedReopenDate = cols.length > 9 ? cols[9].trim() : "";
                String description = cols.length > 10 ? cols[10].trim() : "";

                Opportunity opp = createOpp(name, type, link, deadline, status, comments);
                opp.setFlag(flagged, flagNote);
                opp.setSuggestedReopenDate(suggestedReopenDate);
                opp.setDescription(description);
                addNew(opp);
            }
        }
    }

    // very small CSV splitter that tolerates commas inside quoted fields, e.g. "Foo, Inc."
    private String[] splitCsvLine(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    //---------- saving ----------
    //method to save changes in the database, sorted by deadline (soonest first)
    public void saveFile(String filePath) {
        sortAll();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            pw.println(HEADER);

            ArrayList<Opportunity> all = new ArrayList<>();
            all.addAll(internship);
            all.addAll(scholarship);
            mergeSort(all);

            int id = 1;
            for (Opportunity opp : all) {
                pw.println(String.join(",",
                        String.valueOf(id++),
                        csvSafe(opp.getName()),
                        csvSafe(opp.getType()),
                        csvSafe(opp.getLink()),
                        csvSafe(opp.getStatus()),
                        opp.getDeadline().toString(),
                        csvSafe(opp.getComments()),
                        String.valueOf(opp.isFlagged()),
                        csvSafe(opp.getFlagNote()),
                        csvSafe(opp.getSuggestedReopenDate()),
                        csvSafe(opp.getDescription())
                ));
            }
            System.out.println("Saved " + all.size() + " opportunities to " + filePath);
        } catch (IOException e) {
            System.out.println("Error saving to file: " + e.getMessage());
        }
    }

    private String csvSafe(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    //created opportunity object
    public Opportunity createOpp(String name, String type, String link, LocalDate deadline, String status, String comments) {
        if (type.equalsIgnoreCase("Internship")) {
            return new Internship(name, type, link, deadline, status, comments);
        } else {
            return new Scholarship(name, type, link, deadline, status, comments);
        }
    }

    //add an opportunity — checks the type of opp to add to their respective list
    public void addNew(Opportunity opp) {
        hashTable.put(opp.getName(), opp);

        if (opp instanceof Internship) {
            internship.add(opp);
        } else if (opp instanceof Scholarship) {
            scholarship.add(opp);
        }
    }

    public ArrayList<Opportunity> getAll() {
        ArrayList<Opportunity> all = new ArrayList<>();
        all.addAll(internship);
        all.addAll(scholarship);
        return all;
    }

    //combines all opp and sort them by deadline, adds only the upcoming deadlines to the queue
    public void mainQueue() {
        ArrayList<Opportunity> combine = getAll();
        mergeSort(combine);
        upcomingDeadline = new Queue<>();

        for (Opportunity opp : combine) {
            if (opp.getDeadline().isAfter(LocalDate.now())) {
                upcomingDeadline.enqueue(opp);
            }
        }
    }

    //same process as mainQueue but only add the expired deadlines to another queue
    public void listExpiredQueue() {
        expiredQueue = new Queue<>();
        ArrayList<Opportunity> combine = getAll();

        for (Opportunity opp : combine) {
            if (opp.getDeadline().isBefore(LocalDate.now())) {
                expiredQueue.enqueue(opp);
            }
        }
    }

    public Opportunity nextUpcoming() {
        return upcomingDeadline.peek();
    }

    public Opportunity searchByName(String name) {
        return hashTable.get(name);
    }

    //remove opportunity from the database
    public void removeExpired(String name) {
        Opportunity opp = hashTable.get(name);
        if (opp == null) {
            System.out.println("Not found");
            return;
        }
        hashTable.remove(name);
        if (opp instanceof Internship) {
            internship.remove(opp);
        } else if (opp instanceof Scholarship) {
            scholarship.remove(opp);
        }
        System.out.println("Removed Successfully");

        sortAll();
        mainQueue();
        listExpiredQueue();
    }

    //sorts all opportunities
    public void sortAll() {
        mergeSort(internship);
        mergeSort(scholarship);
    }

    //------merge sort------
    private void mergeSort(ArrayList<Opportunity> list) {
        if (list.size() <= 1) return;
        int middle = list.size() / 2;
        ArrayList<Opportunity> left = new ArrayList<>(list.subList(0, middle));
        ArrayList<Opportunity> right = new ArrayList<>(list.subList(middle, list.size()));

        mergeSort(left);
        mergeSort(right);
        merge(list, left, right);
    }

    private void merge(ArrayList<Opportunity> list, ArrayList<Opportunity> left, ArrayList<Opportunity> right) {
        int i = 0, j = 0, k = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getDeadline().compareTo(right.get(j).getDeadline()) <= 0) {
                list.set(k, left.get(i));
                i++;
            } else {
                list.set(k, right.get(j));
                j++;
            }
            k++;
        }
        while (i < left.size()) {
            list.set(k, left.get(i));
            i++; k++;
        }
        while (j < right.size()) {
            list.set(k, right.get(j));
            j++; k++;
        }
    }
    //---------------------------------------

    //displays all - menu option 1
    public void displayAll() {
        for (Opportunity o : internship) o.displayInfo();
        for (Opportunity o : scholarship) o.displayInfo();
    }

    //displays scholarships - menu option 3
    public void displayScholarship() {
        if (scholarship.isEmpty()) {
            System.out.println("No scholarships on list");
            return;
        }
        for (Opportunity opp : scholarship) opp.displayInfo();
    }

    //displays internships - menu option 2
    public void displayInternship() {
        if (internship.isEmpty()) {
            System.out.println("No internships on list");
            return;
        }
        for (Opportunity opp : internship) opp.displayInfo();
    }

    //displays expired opp - menu option 7
    public void displayExpired(java.util.Scanner scanner, String savePath) {
        if (expiredQueue.isEmpty()) {
            System.out.println("No expired opportunities.");
            return;
        }
        System.out.println("Expired list: ");
        Queue<Opportunity> temp = new Queue<>();
        while (!expiredQueue.isEmpty()) {
            Opportunity opp = expiredQueue.dequeue();
            opp.displayInfo();

            System.out.println("1. Update");
            System.out.println("2. Remove");
            System.out.println("3. Skip");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {
                System.out.print("Enter new deadline (YYYY-MM-DD): ");
                String newDate = scanner.nextLine();
                try {
                    LocalDate newDeadline = LocalDate.parse(newDate);
                    opp.setDeadline(newDeadline);
                    sortAll();
                    mainQueue();
                    saveFile(savePath);
                    System.out.println("Deadline updated");
                } catch (Exception e) {
                    System.out.println("Invalid date.");
                }
                temp.enqueue(opp);
            } else if (choice == 2) {
                removeExpired(opp.getName());
                saveFile(savePath);
            } else {
                temp.enqueue(opp);
            }
        }
        while (!temp.isEmpty()) {
            expiredQueue.enqueue(temp.dequeue());
        }
        listExpiredQueue();
    }
}
