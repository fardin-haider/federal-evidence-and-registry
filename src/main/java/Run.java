/**
 * Entry point. Kept separate from FederalRegistryFX so the project has a
 * plain "main class" to point a run configuration/JAR manifest at, without
 * that class needing to itself extend javafx.application.Application.
 */
public class Run {
    public static void main(String[] args) {
        FederalRegistryFX.main(args);
    }
}
