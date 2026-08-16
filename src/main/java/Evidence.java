import java.io.Serializable;
import java.util.ArrayList;

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
    public String getEvidenceId() { return evidenceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getCaseId() {
        return caseId;
    }
}