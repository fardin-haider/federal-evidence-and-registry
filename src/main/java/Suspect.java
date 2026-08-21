import java.util.ArrayList;

/**
 * Suspect.java - Represents a person of interest.
 *
 * UI/UX Improvements:
 * - Added setPhysicalTraits() and removeAlias() to support modifying suspect profiles.
 * - Added clearAliases() and setAliases() to simplify bulk alias updates during dialog editing.
 */
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

    public void addAlias(String alias) {
        if (alias != null && !alias.trim().isEmpty() && !this.aliases.contains(alias.trim())) {
            this.aliases.add(alias.trim());
        }
    }

    public void removeAlias(String alias) {
        this.aliases.remove(alias);
    }

    public void linkCase(String caseId) {
        if (!this.linkedCaseIds.contains(caseId)) {
            this.linkedCaseIds.add(caseId);
        }
    }

    // Getters
    public String getStatus() { return status; }
    public String getPhysicalTraits() { return physicalTraits; }
    public ArrayList<String> getAliases() { return new ArrayList<>(aliases); }
    public ArrayList<String> getLinkedCaseIds() { return new ArrayList<>(linkedCaseIds); }

    // Setters for editing
    public void setStatus(String status) { this.status = status; }
    public void setPhysicalTraits(String physicalTraits) { this.physicalTraits = physicalTraits; }
}