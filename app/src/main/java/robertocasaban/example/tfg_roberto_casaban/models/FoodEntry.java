package robertocasaban.example.tfg_roberto_casaban.models;

/**
 * Representa una comida que el usuario ha añadido al registro del día.
 * Guarda el nombre, los kcal por 100g y los gramos consumidos.
 */
public class FoodEntry {

    private final String name;
    private final double kcalPer100g;
    private final double grams;
    private String firebaseKey;

    public FoodEntry(String name, double kcalPer100g, double grams) {
        this.name        = name;
        this.kcalPer100g = kcalPer100g;
        this.grams       = grams;
    }

    public String getName()        { return name; }
    public double getKcalPer100g() { return kcalPer100g; }
    public double getGrams()       { return grams; }
    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String key) { this.firebaseKey = key; }

    /** Calcula las kcal totales según los gramos consumidos */
    public double getTotalKcal() {
        return (kcalPer100g / 100.0) * grams;
    }
}
