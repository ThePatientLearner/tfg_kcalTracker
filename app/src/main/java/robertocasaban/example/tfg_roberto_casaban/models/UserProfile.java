package robertocasaban.example.tfg_roberto_casaban.models;

public class UserProfile {

    private String name;
    private String email;
    private double weight;
    private double height;
    private int age;
    private double goalWeight;

    // Constructor vacío requerido por Firebase Realtime Database
    public UserProfile() {}

    public UserProfile(String name, String email, double weight, double height, int age, double goalWeight) {
        this.name = name;
        this.email = email;
        this.weight = weight;
        this.height = height;
        this.age = age;
        this.goalWeight = goalWeight;
    }

    public String getName()          { return name; }
    public String getEmail()         { return email; }
    public double getWeight()        { return weight; }
    public double getHeight()        { return height; }
    public int    getAge()           { return age; }
    public double getGoalWeight()    { return goalWeight; }

    public void setName(String name)             { this.name = name; }
    public void setEmail(String email)           { this.email = email; }
    public void setWeight(double weight)         { this.weight = weight; }
    public void setHeight(double height)         { this.height = height; }
    public void setAge(int age)                  { this.age = age; }
    public void setGoalWeight(double goalWeight) { this.goalWeight = goalWeight; }
}
