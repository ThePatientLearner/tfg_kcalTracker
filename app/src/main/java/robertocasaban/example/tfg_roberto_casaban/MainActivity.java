package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.util.Log;
import java.time.LocalDate;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import robertocasaban.example.tfg_roberto_casaban.adapters.FoodEntryAdapter;
import robertocasaban.example.tfg_roberto_casaban.adapters.FoodSuggestionAdapter;
import robertocasaban.example.tfg_roberto_casaban.database.LocalDatabaseHelper;
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityMainBinding;
import robertocasaban.example.tfg_roberto_casaban.models.FoodEntry;
import robertocasaban.example.tfg_roberto_casaban.models.FoodProduct;
import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class MainActivity extends AppCompatActivity {

    // ─── ViewBinding ─────────────────────────────────────────────────────────────
    // El layout es activity_main_.xml → clase generada: ActivityMain_Binding
    private ActivityMainBinding binding;

    // ─── Firebase ────────────────────────────────────────────────────────────────
    private DatabaseReference refUsers;
    private UserProfile currentProfile;

    // ─── Local DB ────────────────────────────────────────────────────────────────
    private LocalDatabaseHelper localDb;

    // ─── Shared Preferences ──────────────────────────────────────────────────────
    private SharedPreferences sp;

    // ─── Adaptadores ─────────────────────────────────────────────────────────────
    private FoodSuggestionAdapter suggestionAdapter;
    private FoodEntryAdapter      foodEntryAdapter;

    // ─── Búsqueda en tiempo real ─────────────────────────────────────────────────
    private final Handler  searchHandler  = new Handler(Looper.getMainLooper());
    private       Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 400;

    // ─── Kcal objetivo del día ───────────────────────────────────────────────────
    private double kcalObjetivo = 2000;

    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sp      = getSharedPreferences("tfg_prefs", MODE_PRIVATE);
        localDb = new LocalDatabaseHelper(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/"
        );
        refUsers = database.getReference("users");

        setupRecyclerViews();
        setupSearch();
        loadUserProfile();

        // ── Logout ───────────────────────────────────────────────────────────────
        binding.btnLogoutMainActivity.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sp.edit().clear().apply();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, LogInActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // ── Reset ────────────────────────────────────────────────────────────────
        binding.btnResetMainActivity.setOnClickListener(v -> {
            foodEntryAdapter.clear();
            updateKcalDisplay();
        });

        // ── Bottom Navigation ─────────────────────────────────────────────────────
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.btnHomeNavBar) {
                return true;
            } else if (id == R.id.btnTargetNavBar) {
                showTargetDialog();
                return true;
            } else if (id == R.id.nav_profile) {
                showProfileDialog();
                return true;
            }
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════════════════════

    private void setupRecyclerViews() {
        // Dropdown de sugerencias de búsqueda
        suggestionAdapter = new FoodSuggestionAdapter();
        suggestionAdapter.setOnSuggestionClickListener(this::onSuggestionSelected);
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(suggestionAdapter);

        // Lista de alimentos añadidos al día
        foodEntryAdapter = new FoodEntryAdapter();
        foodEntryAdapter.setOnEntryClickListener((position, entry) -> {
            new AlertDialog.Builder(this)
                    .setTitle(entry.getName())
                    .setItems(new String[]{"Cambiar gramos", "Eliminar"}, (dialog, which) -> {
                        if (which == 0) {
                            // Cambiar gramos
                            EditText input = new EditText(this);
                            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            input.setHint("Gramos (ej: 150)");
                            input.setText(String.valueOf((int) entry.getGrams()));
                            int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
                            input.setPadding(dp16, dp16, dp16, dp16);

                            new AlertDialog.Builder(this)
                                    .setTitle("Cambiar gramos")
                                    .setView(input)
                                    .setPositiveButton("Actualizar", (d, w) -> {
                                        String val = input.getText().toString().trim();
                                        if (val.isEmpty()) return;
                                        double newGrams = Double.parseDouble(val);
                                        if (newGrams <= 0) return;
                                        foodEntryAdapter.updateEntry(position,
                                                new FoodEntry(entry.getName(), entry.getKcalPer100g(), newGrams));
                                        updateKcalDisplay();
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        } else {
                            // Eliminar
                            foodEntryAdapter.removeEntry(position);
                            updateKcalDisplay();
                        }
                    })
                    .show();
        });
        binding.recyclerViewMainActivity.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewMainActivity.setAdapter(foodEntryAdapter);
    }

    private void setupSearch() {
        binding.txtSearchMainActivity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();

                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                if (query.length() < 3) {
                    hideSuggestions();
                    return;
                }

                // Debounce: espera 400ms tras la última tecla antes de llamar a la API
                searchRunnable = () -> searchFood(query);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  BÚSQUEDA EN FIREBASE (/foods, campo nombreLower)
    // ══════════════════════════════════════════════════════════════════════════════

    private void searchFood(String query) {
        // Normalizar el query igual que nombreLower: minúsculas sin acentos
        String queryNorm = Normalizer.normalize(query.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Firebase: busca nodos cuyo nombreLower empieza por el query normalizado
        // \uf8ff es el carácter Unicode más alto, sirve de límite superior del rango
        FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("foods")
                .orderByChild("nombreLower")
                .startAt(queryNorm)
                .endAt(queryNorm + "\uf8ff")
                .limitToFirst(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<FoodProduct> products = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            FoodProduct p = child.getValue(FoodProduct.class);
                            if (p != null && p.hasValidData()) {
                                products.add(p);
                            }
                        }

                        if (products.isEmpty()) {
                            hideSuggestions();
                            return;
                        }

                        suggestionAdapter.setSuggestions(products);
                        if (!suggestionAdapter.isEmpty()) showSuggestions();
                        else hideSuggestions();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideSuggestions();
                        Log.e("FIREBASE_SEARCH", "Error: " + error.getMessage());
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SELECCIÓN DE SUGERENCIA → DIÁLOGO DE GRAMOS
    // ══════════════════════════════════════════════════════════════════════════════

    private void onSuggestionSelected(FoodProduct product) {
        hideSuggestions();
        binding.txtSearchMainActivity.setText("");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(product.getProductName());
        builder.setMessage(String.format("%.0f kcal por 100g\n\n¿Cuántos gramos has consumido?",
                product.getKcalPer100g()));

        final EditText inputGrams = new EditText(this);
        inputGrams.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputGrams.setHint("Gramos (ej: 150)");
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        inputGrams.setPadding(dp16, dp16, dp16, dp16);
        builder.setView(inputGrams);

        builder.setPositiveButton("Añadir", (dialog, which) -> {
            String gramsStr = inputGrams.getText().toString().trim();
            if (gramsStr.isEmpty()) {
                Toast.makeText(this, "Introduce los gramos", Toast.LENGTH_SHORT).show();
                return;
            }
            double grams = Double.parseDouble(gramsStr);
            if (grams <= 0) {
                Toast.makeText(this, "Los gramos deben ser mayores que 0", Toast.LENGTH_SHORT).show();
                return;
            }
            addFoodEntry(new FoodEntry(product.getProductName(), product.getKcalPer100g(), grams));
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  AÑADIR ALIMENTO A LA LISTA
    // ══════════════════════════════════════════════════════════════════════════════

    private void addFoodEntry(FoodEntry entry) {
        foodEntryAdapter.addEntry(entry);
        updateKcalDisplay();
        Toast.makeText(this,
                String.format("%s añadido (%.0f kcal)", entry.getName(), entry.getTotalKcal()),
                Toast.LENGTH_SHORT).show();
    }

    private void updateKcalDisplay() {
        double totalKcal = foodEntryAdapter.getTotalKcal();
        binding.lblCurrentKcalMainActivity.setText(String.format("%.0f", totalKcal));

        if (kcalObjetivo > 0) {
            int progress = (int) Math.min((totalKcal / kcalObjetivo) * 100, 100);
            binding.barProgressMainActivity.setProgress(progress);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  MOSTRAR / OCULTAR SUGERENCIAS
    // ══════════════════════════════════════════════════════════════════════════════

    private void showSuggestions() {
        binding.cardSuggestions.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        binding.cardSuggestions.setVisibility(View.GONE);
        suggestionAdapter.clearSuggestions();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  PERFIL DE USUARIO (Firebase)
    // ══════════════════════════════════════════════════════════════════════════════

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        refUsers.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentProfile = snapshot.getValue(UserProfile.class);
                if (currentProfile != null) updateProfileUI(currentProfile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Error al cargar perfil: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfileUI(UserProfile profile) {
        double heightM   = profile.getHeight() / 100.0;
        double imc       = profile.getWeight() / (heightM * heightM);
        double tmb       = (10 * profile.getWeight()) + (6.25 * profile.getHeight())
                           - (5 * profile.getAge()) + 5;
        double kcalBase  = tmb + 400;

        String status;
        int    textColor;
        int    bgColor;

        if (imc < 18.5) {
            status       = "Bajo peso";  textColor = 0xFF0D47A1; bgColor = 0xFFE3F2FD;
            kcalObjetivo = kcalBase + 500;
        } else if (imc < 25.0) {
            status       = "Normal";     textColor = 0xFF166534; bgColor = 0xFFDCFCE7;
            kcalObjetivo = kcalBase;
        } else if (imc < 30.0) {
            status       = "Sobrepeso";  textColor = 0xFFE65100; bgColor = 0xFFFFF3E0;
            kcalObjetivo = kcalBase - 500;
        } else if (imc < 35.0) {
            status       = "Obesidad I"; textColor = 0xFFB71C1C; bgColor = 0xFFFFEBEE;
            kcalObjetivo = kcalBase - 600;
        } else if (imc < 40.0) {
            status       = "Obesidad II"; textColor = 0xFFB71C1C; bgColor = 0xFFFFCDD2;
            kcalObjetivo = kcalBase - 700;
        } else {
            status       = "Obesidad III"; textColor = 0xFF7F0000; bgColor = 0xFFEF9A9A;
            kcalObjetivo = kcalBase - 800;
        }

        binding.txtIMCValueMainActivity.setText(String.format("%.1f", imc));
        binding.txtWeightValueMainActivity.setText(String.format("%.1f kg", profile.getWeight()));
        binding.lblCurrentKcalMainActivity.setText("0");
        binding.lblMaxKcalMainActivity.setText(String.format("/ %.0f kcal", kcalObjetivo));
        binding.barProgressMainActivity.setProgress(0);
        binding.txtIMCStatus.setText(status);
        binding.txtIMCStatus.setTextColor(textColor);

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(24f);
        badge.setColor(bgColor);
        binding.txtIMCStatus.setBackground(badge);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  DIÁLOGOS
    // ══════════════════════════════════════════════════════════════════════════════

    @Override
    protected void onResume() {
        super.onResume();
        checkDailyReset();
    }

    private void checkDailyReset() {
        String today = LocalDate.now().toString();
        String lastReset = sp.getString("last_reset_date", "");
        if (!today.equals(lastReset)) {
            foodEntryAdapter.clear();
            updateKcalDisplay();
            sp.edit().putString("last_reset_date", today).apply();
        }
    }

    private void showTargetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.alert_kg_edit, null);
        builder.setView(dialogView);
        builder.setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.popup_profile_edit, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText editName       = dialogView.findViewById(R.id.editName);
        EditText editWeight     = dialogView.findViewById(R.id.editWeight);
        EditText editHeight     = dialogView.findViewById(R.id.editHeight);
        EditText editAge        = dialogView.findViewById(R.id.editAge);
        EditText editGoalWeight = dialogView.findViewById(R.id.editGoalWeight);
        Button   btnSave        = dialogView.findViewById(R.id.button);
        Button   btnDelete      = dialogView.findViewById(R.id.btnDeleteAccount);

        if (currentProfile != null) {
            editName.setText(currentProfile.getName());
            editWeight.setText(String.valueOf(currentProfile.getWeight()));
            editHeight.setText(String.valueOf(currentProfile.getHeight()));
            editAge.setText(String.valueOf(currentProfile.getAge()));
            editGoalWeight.setText(String.valueOf(currentProfile.getGoalWeight()));
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        btnSave.setOnClickListener(v -> {
            String name      = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            String heightStr = editHeight.getText().toString().trim();
            String ageStr    = editAge.getText().toString().trim();
            String goalWStr  = editGoalWeight.getText().toString().trim();

            if (name.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty()
                    || ageStr.isEmpty() || goalWStr.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            UserProfile updatedProfile = new UserProfile(
                    name, user.getEmail(),
                    Double.parseDouble(weightStr),
                    Double.parseDouble(heightStr),
                    Integer.parseInt(ageStr),
                    Double.parseDouble(goalWStr)
            );

            refUsers.child(user.getUid()).setValue(updatedProfile)
                    .addOnSuccessListener(aVoid -> {
                        localDb.saveUserProfile(user.getUid(), updatedProfile);
                        currentProfile = updatedProfile;
                        updateProfileUI(updatedProfile);
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar cuenta")
                    .setMessage("¿Estás seguro? Esta acción es irreversible y borrará todos tus datos.")
                    .setPositiveButton("Sí, eliminar", (confirmDialog, which) -> {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser == null) return;

                        String uid = currentUser.getUid();
                        refUsers.child(uid).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    localDb.deleteUserProfile(uid);
                                    currentUser.delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                                Intent i = new Intent(this, LogInActivity.class);
                                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(i);
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Error al eliminar cuenta: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Error al borrar datos: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        dialog.show();
    }
}
