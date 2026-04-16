package robertocasaban.example.tfg_roberto_casaban.models;

public class UserProfile {

    private String name;
    private String email;
    private double weight;
    private double height;
    private int    age;
    private double goalWeight;
    private String sex; // "Hombre" o "Mujer"
    private boolean isPro;

    // Constructor vacío requerido por Firebase Realtime Database
    public UserProfile() {}

    public UserProfile(String name, String email, double weight, double height, int age, double goalWeight, String sex) {
        this.name       = name;
        this.email      = email;
        this.weight     = weight;
        this.height     = height;
        this.age        = age;
        this.goalWeight = goalWeight;
        this.sex        = sex;
        this.isPro      = false;
    }

    public String  getName()       { return name; }
    public String  getEmail()      { return email; }
    public double  getWeight()     { return weight; }
    public double  getHeight()     { return height; }
    public int     getAge()        { return age; }
    public double  getGoalWeight() { return goalWeight; }
    public String  getSex()        { return sex != null ? sex : "Hombre"; }
    public boolean getIsPro()      { return isPro; }

    public void setName(String name)             { this.name = name; }
    public void setEmail(String email)           { this.email = email; }
    public void setWeight(double weight)         { this.weight = weight; }
    public void setHeight(double height)         { this.height = height; }
    public void setAge(int age)                  { this.age = age; }
    public void setGoalWeight(double goalWeight) { this.goalWeight = goalWeight; }
    public void setSex(String sex)               { this.sex = sex; }
    public void setIsPro(boolean isPro)          { this.isPro = isPro; }
}
