package robertocasaban.example.tfg_roberto_casaban.models;

/**
 * Modelo de alimento que mapea directamente con los nodos de Firebase
 * en la ruta /foods/{id}.
 *
 * Estructura en Firebase:
 * {
 *   "nombre":        "Arroz Blanco",
 *   "nombreLower":   "arroz blanco",   <- para búsqueda insensible a mayúsculas
 *   "calorias":      345.0,            <- kcal por 100g
 *   "proteinas":     8.1,
 *   "grasas":        0.7,
 *   "carbohidratos": 76.5,
 *   "fibra":         1.2
 * }
 */
public class FoodProduct {

    private String nombre;
    private String nombreLower;
    private double calorias;
    private double proteinas;
    private double grasas;
    private double carbohidratos;
    private double fibra;

    /** Constructor vacío requerido por Firebase */
    public FoodProduct() {}

    public FoodProduct(String nombre, double calorias) {
        this.nombre   = nombre;
        this.calorias = calorias;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────

    /** Nombre mostrado al usuario */
    public String getProductName() {
        return (nombre != null && !nombre.isEmpty()) ? nombre : "Sin nombre";
    }

    /** Kcal por 100g — directo de la tabla */
    public double getKcalPer100g() { return calorias; }

    public String getNombre()        { return nombre; }
    public String getNombreLower()   { return nombreLower; }
    public double getCalorias()      { return calorias; }
    public double getProteinas()     { return proteinas; }
    public double getGrasas()        { return grasas; }
    public double getCarbohidratos() { return carbohidratos; }
    public double getFibra()         { return fibra; }

    // ── Setters (requeridos por Firebase deserializer) ────────────────────────────

    public void setNombre(String nombre)               { this.nombre = nombre; }
    public void setNombreLower(String nombreLower)     { this.nombreLower = nombreLower; }
    public void setCalorias(double calorias)           { this.calorias = calorias; }
    public void setProteinas(double proteinas)         { this.proteinas = proteinas; }
    public void setGrasas(double grasas)               { this.grasas = grasas; }
    public void setCarbohidratos(double carbohidratos) { this.carbohidratos = carbohidratos; }
    public void setFibra(double fibra)                 { this.fibra = fibra; }

    /** Solo mostramos alimentos con nombre y calorías válidas */
    public boolean hasValidData() {
        return nombre != null && !nombre.trim().isEmpty() && calorias > 0;
    }
}
