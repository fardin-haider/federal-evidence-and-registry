import java.io.Serializable;
import java.util.ArrayList;

/**
 * Case.java - Represents an investigation case file.
 *
 * UI/UX Improvements:
 * - Added setTitle() so investigators can rename or correct case titles via the new Edit Dialog.
 * - Encapsulated suspect and evidence ID lists with defensive copying to prevent unwanted external mutations.
 */
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

    public void addSuspect(String suspectId) {
        if (!this.suspectIds.contains(suspectId)) {
            this.suspectIds.add(suspectId);
        }
    }

    public void addEvidence(String evidenceId) {
        if (!this.evidenceIds.contains(evidenceId)) {
            this.evidenceIds.add(evidenceId);
        }
    }

    // Getters
    public String getCaseId() { return caseId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getDateOpened() { return dateOpened; }
    public ArrayList<String> getSuspectIds() { return new ArrayList<>(suspectIds); }
    public ArrayList<String> getEvidenceIds() { return new ArrayList<>(evidenceIds); }

    // Setters for editing records
    public void setTitle(String title) { this.title = title; }
    public void setStatus(String status) { this.status = status; }
}