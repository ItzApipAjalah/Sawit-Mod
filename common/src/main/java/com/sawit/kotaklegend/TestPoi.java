package com.sawit.kotaklegend;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.FileWriter;
public class TestPoi {
    public static void main(String[] args) {
        try (FileWriter fw = new FileWriter("poitypes_dump.txt")) {
            for (Method m : PoiTypes.class.getDeclaredMethods()) {
                fw.write("Method: " + m.getName() + " -> " + m.getReturnType().getSimpleName() + "\n");
            }
            for (Field f : PoiTypes.class.getDeclaredFields()) {
                fw.write("Field: " + f.getName() + " -> " + f.getType().getSimpleName() + "\n");
            }
        } catch (Exception e) {}
    }
}
