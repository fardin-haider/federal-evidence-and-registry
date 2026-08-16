import java.io.*;
import java.util.HashMap;

public class DataManager {
    public HashMap<String, User> users = new HashMap<>();
    public HashMap<String, Suspect> suspects = new HashMap<>();
    public HashMap<String, Evidence> evidence = new HashMap<>();
    public HashMap<String, Case> cases = new HashMap<>();

    private final String DATA_FILE = "registry_data.dat";

    // File I/O: Save Data to persistent storage
    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
            oos.writeObject(suspects);
            oos.writeObject(evidence);
            oos.writeObject(cases);
            System.out.println("System data successfully persisted.");
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    // File I/O: Load Data into memory on startup
    @SuppressWarnings("unchecked")
    public void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            users = (HashMap<String, User>) ois.readObject();
            suspects = (HashMap<String, Suspect>) ois.readObject();
            evidence = (HashMap<String, Evidence>) ois.readObject();
            cases = (HashMap<String, Case>) ois.readObject();
            System.out.println("System data loaded successfully.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    // Report Generation: Export Dossier to .txt file
    public void exportCaseDossier(String caseId) {
        Case c = cases.get(caseId);
        if (c == null) {
            System.out.println("Case not found.");
            return;
        }

        String filename = "Dossier_" + caseId + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("========================================\n");
            writer.write("       CASE DOSSIER: " + c.getCaseId() + "\n");
            writer.write("========================================\n");
            writer.write("Title: " + c.getTitle() + "\n");
            writer.write("Date Opened: " + c.getDateOpened() + "\n");
            writer.write("Status: " + c.getStatus() + "\n\n");
            writer.write("End of Report.\n");
            System.out.println("Dossier exported to " + filename);
        } catch (IOException e) {
            System.out.println("Error generating dossier: " + e.getMessage());
        }
    }
}