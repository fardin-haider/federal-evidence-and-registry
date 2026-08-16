import java.util.ArrayList;

public class Suspect extends Person {
    private ArrayList<String> aliases;
    private String physicalTraits;
    private String status; // Wanted, In Custody, Under Surveillance, Cleared
    private ArrayList<String> linkedCaseIds;

    public Suspect(String id, String firstName, String lastName, String dob, String physicalTraits, String status) {
        super(id, firstName, lastName, dob);
        this.physicalTraits = physicalTraits;
        this.status = status;
        this.aliases = new ArrayList<>();
        this.linkedCaseIds = new ArrayList<>();
    }

    public void addAlias(String alias) { this.aliases.add(alias); }
    public void linkCase(String caseId) { this.linkedCaseIds.add(caseId); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}