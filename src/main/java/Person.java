import java.io.Serializable;

/**
 * Person.java - Base class for people tracked in the registry.
 *
 * WHY SETTERS WERE ADDED:
 * To support full record editing in the UI, mutable fields (firstName, lastName, dateOfBirth)
 * now have clean setters so the Edit Dialogs can update existing instances directly.
 */
public abstract class Person implements Serializable {
    protected String id;
    protected String firstName;
    protected String lastName;
    protected String dateOfBirth;

    public Person(String id, String firstName, String lastName, String dateOfBirth) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }

    // Getters
    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }

    // Setters for UI edit operations
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}