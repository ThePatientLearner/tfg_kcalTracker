package robertocasaban.example.tfg_roberto_casaban.models;

/** Totales diarios de kcal y macronutrientes (en gramos). */
public class DailyMacros {

    private final double kcal;
    private final double carbs;
    private final double protein;
    private final double fat;

    public DailyMacros(double kcal, double carbs, double protein, double fat) {
        this.kcal    = kcal;
        this.carbs   = carbs;
        this.protein = protein;
        this.fat     = fat;
    }

    public double getKcal()    { return kcal; }
    public double getCarbs()   { return carbs; }
    public double getProtein() { return protein; }
    public double getFat()     { return fat; }
}
