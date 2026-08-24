/**
 * User.java - Represents an agent or administrator account.
 *
 * UI/UX Improvements:
 * - Added setters to allow administrators to edit agent names, roles, and credentials.
 */
public class User extends Person {
    private String username;
    private String password;
    private String role; // "Admin" or "Field Agent"

    public User(String id, String firstName, String lastName, String dob, String username, String password, String role) {
        super(id, firstName, lastName, dob);
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @Override
    public String getRoleDescription() {
        return "Authorized Personnel [" + getRole() + "]";
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    // Setters for editing agent profiles
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
}