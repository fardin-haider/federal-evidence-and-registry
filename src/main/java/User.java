public class User extends Person {
    private String username;
    private String password;
    private String role; // e.g., "Admin" or "Field Agent"

    public User(String id, String firstName, String lastName, String dob, String username, String password, String role) {
        super(id, firstName, lastName, dob);
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}