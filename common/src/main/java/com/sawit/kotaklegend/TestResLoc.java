import net.minecraft.resources.ResourceLocation;

public class TestResLoc {
    public static void main(String[] args) {
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("entity/signs/sawitmod", "sawit");
            System.out.println("Result: " + loc);
            System.out.println("Namespace: " + loc.getNamespace());
            System.out.println("Path: " + loc.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
