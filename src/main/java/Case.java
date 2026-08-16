import java.io.Serializable;
import java.util.ArrayList;

public class Case implements Serializable {
    private String caseId;
    private String title;
    private String dateOpened;
    private String status; // Open, Closed, Cold
    private ArrayList<String> suspectIds;
    private ArrayList<String> evidenceIds;

    public Case(String caseId, String title, String dateOpened, String status) {
        this.caseId = caseId;
        this.title = title;
        this.dateOpened = dateOpened;
        this.status = status;
        this.suspectIds = new ArrayList<>();
        this.evidenceIds = new ArrayList<>();
    }

    public void addSuspect(String suspectId) { this.suspectIds.add(suspectId); }
    public void addEvidence(String evidenceId) { this.evidenceIds.add(evidenceId); }

    public String getCaseId() { return caseId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getDateOpened() { return dateOpened; }
    public void setStatus(String status) { this.status = status; }
}