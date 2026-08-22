import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataManager {
    public Map<String, Case> cases = new LinkedHashMap<>();
    public Map<String, Suspect> suspects = new LinkedHashMap<>();
    public Map<String, Evidence> evidence = new LinkedHashMap<>();
    public Map<String, User> users = new LinkedHashMap<>();

    private static final String FILE_NAME = "registry_data.dat";

    @SuppressWarnings("unchecked")
    public void loadData() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            cases = (Map<String, Case>) ois.readObject();
            suspects = (Map<String, Suspect>) ois.readObject();
            evidence = (Map<String, Evidence>) ois.readObject();
            users = (Map<String, User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(cases);
            oos.writeObject(suspects);
            oos.writeObject(evidence);
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}