import java.io.Serializable;
import java.util.ArrayList;

/**
 * Evidence.java - Tracks physical, digital, and documentary evidence.
 *
 * UI/UX Improvements:
 * - Added setType(), setDescription(), and setCaseId() to enable editing evidence records.
 * - Chain of custody records any automated modifications made during edits.
 */
public class Evidence implements Serializable {
    private String evidenceId;
    private String caseId;
    private String type; // Weapon, Digital, Document, Biological
    private String description;
    private String status; // In Storage, At Lab/Forensics, Released/Destroyed
    private ArrayList<String> custodyLog;

    public Evidence(String evidenceId, String caseId, String type, String description, String status) {
        this.evidenceId = evidenceId;
        this.caseId = caseId;
        this.type = type;
        this.description = description;
        this.status = status;
        this.custodyLog = new ArrayList<>();
    }

    public void logCustody(String logEntry) { this.custodyLog.add(logEntry); }

    // Getters
    public String getEvidenceId() { return evidenceId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public ArrayList<String> getCustodyLog() { return new ArrayList<>(custodyLog); }

    // Setters for editing
    public void setStatus(String status) { this.status = status; }
    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}