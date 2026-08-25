import java.util.ArrayList;
import java.util.List;

public class Suspect extends Person {

    private String physicalTraits;
    private String status;
    private List<String> aliases = new ArrayList<>();
    private List<String> linkedCaseIds = new ArrayList<>();

    public Suspect(String id, String firstName, String lastName, String dateOfBirth, String physicalTraits, String status) {
        super(id, firstName, lastName, dateOfBirth);
        this.physicalTraits = physicalTraits;
        this.status = status;
    }
    @Override
    public String getRoleDescription() {
        return "Person of Interest / Suspect [Status: " + getStatus() + "]";
    }

    public String getPhysicalTraits() { return physicalTraits; }
    public void setPhysicalTraits(String physicalTraits) { this.physicalTraits = physicalTraits; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getAliases() { return aliases; }
    public void addAlias(String alias) { if (!aliases.contains(alias)) aliases.add(alias); }
    public void removeAlias(String alias) { aliases.remove(alias); }

    public List<String> getLinkedCaseIds() { return linkedCaseIds; }
    public void linkCase(String caseId) { if (!linkedCaseIds.contains(caseId)) linkedCaseIds.add(caseId); }
}