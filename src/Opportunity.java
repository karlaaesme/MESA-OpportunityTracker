import java.time.LocalDate;

//parent class
public abstract class Opportunity {
    String name;
    String type;
    String link;
    LocalDate deadline;
    String status;      // "Open", "On Hold", or "Close"
    String comments;
    boolean flagged;
    String flagNote;
    String suggestedReopenDate;
    String description;

    public Opportunity(String name, String type, String link, LocalDate deadline, String status, String comments) {
        this.name = name;
        this.type = type;
        this.link = link;
        this.deadline = deadline;
        this.status = (status == null || status.isBlank()) ? "Open" : status;
        this.comments = (comments == null) ? "" : comments;
        this.flagged = false;
        this.flagNote = "";
        this.suggestedReopenDate = "";
        this.description = "";
    }

    public abstract void displayInfo();

    public String getName() {
        return name;
    }
    public String getLink() {
        return link;
    }
    public LocalDate getDeadline() {
        return deadline;
    }
    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
    public String getType() {
        return type;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getComments() {
        return comments;
    }
    public void setComments(String comments) {
        this.comments = comments;
    }
    public boolean isFlagged() {
        return flagged;
    }
    public String getFlagNote() {
        return flagNote;
    }
    public void setFlag(boolean flagged, String note) {
        this.flagged = flagged;
        this.flagNote = (note == null) ? "" : note;
    }
    public String getSuggestedReopenDate() {
        return suggestedReopenDate;
    }
    public void setSuggestedReopenDate(String date) {
        this.suggestedReopenDate = (date == null) ? "" : date;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = (description == null) ? "" : description;
    }
}

//child
class Internship extends Opportunity {

    public Internship(String name, String type, String link, LocalDate deadline, String status, String comments) {
        super(name, type, link, deadline, status, comments);
    }

    @Override
    public void displayInfo() {
        System.out.println(name + " [" + status + "] Deadline: " + deadline);
        System.out.println("Link: " + link);
        if (flagged) {
            System.out.println("FLAGGED: " + flagNote);
        }
    }
}

//child
class Scholarship extends Opportunity {

    public Scholarship(String name, String type, String link, LocalDate deadline, String status, String comments) {
        super(name, type, link, deadline, status, comments);
    }

    @Override
    public void displayInfo() {
        System.out.println(name + " [" + status + "] Deadline: " + deadline);
        System.out.println("Link: " + link);
        if (flagged) {
            System.out.println("FLAGGED: " + flagNote);
        }
    }
}
