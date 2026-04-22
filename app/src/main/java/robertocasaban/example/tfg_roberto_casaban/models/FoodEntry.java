package robertocasaban.example.tfg_roberto_casaban.models;

/**
 * Representa una comida que el usuario ha añadido al registro del día.
 * Guarda el nombre, los kcal por 100g, los macros por 100g y los gramos consumidos.
 */
public class FoodEntry {

    private final String name;
    private final double kcalPer100g;
    private final double carbsPer100g;
    private final double proteinPer100g;
    private final double fatPer100g;
    private final double grams;
    private String firebaseKey;

    public FoodEntry(String name, double kcalPer100g, double grams) {
        this(name, kcalPer100g, 0.0, 0.0, 0.0, grams);
    }

    public FoodEntry(String name,
                     double kcalPer100g,
                     double carbsPer100g,
                     double proteinPer100g,
                     double fatPer100g,
                     double grams) {
        this.name           = name;
        this.kcalPer100g    = kcalPer100g;
        this.carbsPer100g   = carbsPer100g;
        this.proteinPer100g = proteinPer100g;
        this.fatPer100g     = fatPer100g;
        this.grams          = grams;
    }

    public String getName()           { return name; }
    public double getKcalPer100g()    { return kcalPer100g; }
    public double getCarbsPer100g()   { return carbsPer100g; }
    public double getProteinPer100g() { return proteinPer100g; }
    public double getFatPer100g()     { return fatPer100g; }
    public double getGrams()          { return grams; }
    public String getFirebaseKey()    { return firebaseKey; }
    public void setFirebaseKey(String key) { this.firebaseKey = key; }

    public double getTotalKcal()    { return (kcalPer100g    / 100.0) * grams; }
    public double getTotalCarbs()   { return (carbsPer100g   / 100.0) * grams; }
    public double getTotalProtein() { return (proteinPer100g / 100.0) * grams; }
    public double getTotalFat()     { return (fatPer100g     / 100.0) * grams; }
}
