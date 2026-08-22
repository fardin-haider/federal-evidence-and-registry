import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Evidence implements Serializable {
    private static final long serialVersionUID = 1L;

    private String evidenceId;
    private String caseId;
    private String type;
    private String description;
    private String status;
    private List<String> custodyLog = new ArrayList<>();

    public Evidence(String evidenceId, String caseId, String type, String description, String status) {
        this.evidenceId = evidenceId;
        this.caseId = caseId;
        this.type = type;
        this.description = description;
        this.status = status;
    }

    public String getEvidenceId() { return evidenceId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getCustodyLog() { return custodyLog; }
    public void logCustody(String entry) { custodyLog.add(entry); }
}