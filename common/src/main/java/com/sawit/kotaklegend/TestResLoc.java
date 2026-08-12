import net.minecraft.resources.Identifier;

public class TestResLoc {
    public static void main(String[] args) {
        try {
            Identifier loc = Identifier.fromNamespaceAndPath("entity/signs/sawitmod", "sawit");
            System.out.println("Result: " + loc);
            System.out.println("Namespace: " + loc.getNamespace());
            System.out.println("Path: " + loc.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
