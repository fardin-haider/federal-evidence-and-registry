/**
 * Custom Exception for federal registry validation errors.
 */
public class RegistryValidationException extends Exception {
    public RegistryValidationException(String message) {
        super(message);
    }
}
