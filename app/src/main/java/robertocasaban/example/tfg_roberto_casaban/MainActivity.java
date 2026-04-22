package robertocasaban.example.tfg_roberto_casaban;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Log;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Consumer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.android.material.button.MaterialButton;

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
    private DatabaseReference refUserFoods;
    private UserProfile currentProfile;

    // ─── Fecha y UID del usuario ──────────────────────────────────────────────────
    private LocalDate selectedDate;   // día que se está visualizando/editando
    private String    todayDate;      // fecha ISO (yyyy-MM-dd) de selectedDate
    private String    currentUid;

    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM", new Locale("es", "ES"));

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

    // ─── Objetivos de macronutrientes (se recalculan con kcalObjetivo) ───────────
    // Distribución: 50% carbos · 20% proteína · 30% grasa
    private double carbsObjetivo   = (2000 * 0.50) / 4.0;
    private double proteinObjetivo = (2000 * 0.20) / 4.0;
    private double fatObjetivo     = (2000 * 0.30) / 9.0;

    // ─── Launcher para PerfilActivity (recarga perfil si se guardó) ──────────────
    private final ActivityResultLauncher<Intent> perfilLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) loadUserProfile();
            });

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

        selectedDate = LocalDate.now();
        todayDate    = selectedDate.toString();
        FirebaseUser initUser = FirebaseAuth.getInstance().getCurrentUser();
        if (initUser != null) {
            currentUid   = initUser.getUid();
            refUserFoods = refUsers.child(currentUid).child("foods").child(todayDate);
        }

        setupRecyclerViews();
        setupSearch();
        setupDateSelector();
        loadUserProfile();


        // ── Botón +Pro ───────────────────────────────────────────────────────────
        binding.btnResetMainActivity.setOnClickListener(v -> {
            if (isUserPro()) {
                Toast.makeText(this, "Ya eres Pro", Toast.LENGTH_SHORT).show();
            } else {
                showProSubscriptionDialog();
            }
        });

        // ── Logout ───────────────────────────────────────────────────────────────
        binding.btnLogoutMainActivity.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, LogInActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });


        // ── Bottom Navigation ─────────────────────────────────────────────────────
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.btnHomeNavBar) {
                return true;
            } else if (id == R.id.btnTargetNavBar) {
                Intent t = new Intent(this, ObjetivoActivity.class);
                if (currentProfile != null) {
                    t.putExtra("weight", currentProfile.getWeight());
                    t.putExtra("height", currentProfile.getHeight());
                }
                startActivity(t);
                return true;
            } else if (id == R.id.nav_profile) {
                perfilLauncher.launch(new Intent(this, PerfilActivity.class));
                return true;
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, EstadisticasActivity.class));
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
                            showGramsDialog(
                                    entry.getName(),
                                    String.format("%.0f kcal por 100g", entry.getKcalPer100g()),
                                    entry.getGrams(),
                                    newGrams -> {
                                        FoodEntry updated = new FoodEntry(
                                                entry.getName(),
                                                entry.getKcalPer100g(),
                                                entry.getCarbsPer100g(),
                                                entry.getProteinPer100g(),
                                                entry.getFatPer100g(),
                                                newGrams);
                                        updated.setFirebaseKey(entry.getFirebaseKey());
                                        if (refUserFoods != null && entry.getFirebaseKey() != null) {
                                            refUserFoods.child(entry.getFirebaseKey()).child("grams").setValue(newGrams);
                                        }
                                        if (entry.getFirebaseKey() != null) {
                                            localDb.updateFoodEntryGrams(entry.getFirebaseKey(), newGrams);
                                        }
                                        foodEntryAdapter.updateEntry(position, updated);
                                        persistAndUpdateDailyDisplay();
                                    });
                        } else {
                            // Eliminar
                            if (refUserFoods != null && entry.getFirebaseKey() != null) {
                                refUserFoods.child(entry.getFirebaseKey()).removeValue();
                            }
                            if (entry.getFirebaseKey() != null) {
                                localDb.deleteFoodEntry(entry.getFirebaseKey());
                            }
                            foodEntryAdapter.removeEntry(position);
                            persistAndUpdateDailyDisplay();
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
    //  SELECTOR DE FECHA
    // ══════════════════════════════════════════════════════════════════════════════

    private void setupDateSelector() {
        refreshDateLabel();

        binding.btnPrevDayMainActivity.setOnClickListener(v ->
                changeSelectedDate(selectedDate.minusDays(1)));

        binding.btnNextDayMainActivity.setOnClickListener(v ->
                changeSelectedDate(selectedDate.plusDays(1)));

        binding.txtSelectedDateMainActivity.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> changeSelectedDate(LocalDate.of(year, month + 1, day)),
                    selectedDate.getYear(),
                    selectedDate.getMonthValue() - 1,
                    selectedDate.getDayOfMonth()
            );
            picker.show();
        });
    }

    private void changeSelectedDate(LocalDate newDate) {
        selectedDate = newDate;
        todayDate    = newDate.toString();
        if (currentUid != null) {
            refUserFoods = refUsers.child(currentUid).child("foods").child(todayDate);
        }
        refreshDateLabel();
        foodEntryAdapter.clear();
        updateDailyDisplay();
        loadTodayFoodEntriesFromLocal();
        loadTodayFoodEntries();
    }

    private void refreshDateLabel() {
        LocalDate today = LocalDate.now();
        String label;
        if (selectedDate.equals(today))                    label = "Hoy";
        else if (selectedDate.equals(today.minusDays(1)))  label = "Ayer";
        else if (selectedDate.equals(today.plusDays(1)))   label = "Mañana";
        else                                               label = selectedDate.format(SHORT_DATE_FORMATTER);
        binding.txtSelectedDateMainActivity.setText(label);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  BÚSQUEDA EN FIREBASE (/foods, campo nombreLower)
    // ══════════════════════════════════════════════════════════════════════════════

    private void searchFood(String query) {
        // Normalizar igual que nombreLower: minúsculas sin acentos
        String queryNorm = Normalizer.normalize(query.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Firebase no soporta búsqueda por subcadena, así que descargamos todos
        // los alimentos y filtramos en cliente con contains()
        FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("foods")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<FoodProduct> products = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            FoodProduct p = child.getValue(FoodProduct.class);
                            if (p != null && p.hasValidData()
                                    && p.getNombreLower() != null
                                    && p.getNombreLower().contains(queryNorm)) {
                                products.add(p);
                                if (products.size() >= 5) break;
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

        // Gating: si ya hay 1 comida y el usuario no es Pro, mostrar suscripción
        if (foodEntryAdapter.getItemCount() >= 1 && !isUserPro()) {
            showProSubscriptionDialog();
            return;
        }

        showGramsDialog(
                product.getProductName(),
                String.format("%.0f kcal por 100g", product.getKcalPer100g()),
                100.0,
                grams -> addFoodEntry(new FoodEntry(
                        product.getProductName(),
                        product.getKcalPer100g(),
                        product.getCarbohidratos(),
                        product.getProteinas(),
                        product.getGrasas(),
                        grams)));
    }

    /** Diálogo con − y + (de 50 en 50) para fijar los gramos. */
    private void showGramsDialog(String title, String subtitle, double initialGrams,
                                 Consumer<Double> onConfirm) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_grams_edit, null);
        TextView    txtSubtitle = view.findViewById(R.id.txtGramsSubtitle);
        EditText    editGrams   = view.findViewById(R.id.editGramsValue);
        ImageButton btnMinus    = view.findViewById(R.id.btnGramsMinus);
        ImageButton btnPlus     = view.findViewById(R.id.btnGramsPlus);

        txtSubtitle.setText(subtitle);
        editGrams.setText(String.valueOf((int) initialGrams));
        editGrams.setSelection(editGrams.getText().length());

        btnMinus.setOnClickListener(v -> {
            double current = parseGramsOr(editGrams, initialGrams);
            int next = (int) Math.max(50, current - 50);
            editGrams.setText(String.valueOf(next));
            editGrams.setSelection(editGrams.getText().length());
        });
        btnPlus.setOnClickListener(v -> {
            double current = parseGramsOr(editGrams, initialGrams);
            int next = (int) (current + 50);
            editGrams.setText(String.valueOf(next));
            editGrams.setSelection(editGrams.getText().length());
        });

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Aceptar", (d, w) -> {
                    String val = editGrams.getText().toString().trim();
                    if (val.isEmpty()) {
                        Toast.makeText(this, "Introduce los gramos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double grams = Double.parseDouble(val);
                        if (grams <= 0) {
                            Toast.makeText(this, "Los gramos deben ser mayores que 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        onConfirm.accept(grams);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private double parseGramsOr(EditText field, double fallback) {
        try {
            return Double.parseDouble(field.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  AÑADIR ALIMENTO A LA LISTA
    // ══════════════════════════════════════════════════════════════════════════════

    private void addFoodEntry(FoodEntry entry) {
        if (refUserFoods != null) {
            DatabaseReference newRef = refUserFoods.push();
            entry.setFirebaseKey(newRef.getKey());
            java.util.HashMap<String, Object> data = new java.util.HashMap<>();
            data.put("name", entry.getName());
            data.put("kcalPer100g",    entry.getKcalPer100g());
            data.put("carbsPer100g",   entry.getCarbsPer100g());
            data.put("proteinPer100g", entry.getProteinPer100g());
            data.put("fatPer100g",     entry.getFatPer100g());
            data.put("grams",          entry.getGrams());
            newRef.setValue(data);
        }
        if (currentUid != null) localDb.saveFoodEntry(currentUid, todayDate, entry);
        foodEntryAdapter.addEntry(entry);
        persistAndUpdateDailyDisplay();
        Toast.makeText(this,
                String.format("%s añadido (%.0f kcal)", entry.getName(), entry.getTotalKcal()),
                Toast.LENGTH_SHORT).show();
    }

    private void updateDailyDisplay() {
        double totalKcal    = foodEntryAdapter.getTotalKcal();
        double totalCarbs   = foodEntryAdapter.getTotalCarbs();
        double totalProtein = foodEntryAdapter.getTotalProtein();
        double totalFat     = foodEntryAdapter.getTotalFat();

        binding.lblCurrentKcalMainActivity.setText(String.format("%.0f", totalKcal));

        if (kcalObjetivo > 0) {
            int progress = (int) Math.min((totalKcal / kcalObjetivo) * 100, 100);
            binding.barProgressMainActivity.setProgress(progress);
        }

        binding.txtCarbsValueMainActivity.setText(
                String.format("%.0f / %.0f g", totalCarbs, carbsObjetivo));
        binding.txtProteinValueMainActivity.setText(
                String.format("%.0f / %.0f g", totalProtein, proteinObjetivo));
        binding.txtFatValueMainActivity.setText(
                String.format("%.0f / %.0f g", totalFat, fatObjetivo));

        binding.barCarbsMainActivity.setProgress(
                (int) Math.min((totalCarbs   / carbsObjetivo)   * 100, 100));
        binding.barProteinMainActivity.setProgress(
                (int) Math.min((totalProtein / proteinObjetivo) * 100, 100));
        binding.barFatMainActivity.setProgress(
                (int) Math.min((totalFat     / fatObjetivo)     * 100, 100));
    }

    /**
     * Refresca la UI y sincroniza los totales del día a Firebase/SQLite.
     * Solo se llama cuando el usuario ACTÚA (añade, edita o elimina una comida).
     * Nunca desde rutas de carga, para no sobrescribir totales con 0 mientras se
     * está leyendo el contenido del día.
     */
    private void persistAndUpdateDailyDisplay() {
        updateDailyDisplay();
        syncDailyTotals(
                foodEntryAdapter.getTotalKcal(),
                foodEntryAdapter.getTotalCarbs(),
                foodEntryAdapter.getTotalProtein(),
                foodEntryAdapter.getTotalFat());
    }

    private void syncDailyTotals(double totalKcal, double totalCarbs,
                                 double totalProtein, double totalFat) {
        if (refUserFoods != null) {
            refUserFoods.child("totalKcal").setValue(totalKcal);
            refUserFoods.child("totalCarbs").setValue(totalCarbs);
            refUserFoods.child("totalProtein").setValue(totalProtein);
            refUserFoods.child("totalFat").setValue(totalFat);
        }
        if (currentUid != null) {
            localDb.saveDailyTotals(currentUid, todayDate,
                    totalKcal, totalCarbs, totalProtein, totalFat);
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

        // Mostrar datos locales inmediatamente (disponible sin conexión)
        UserProfile localProfile = localDb.getUserProfile(user.getUid());
        if (localProfile != null) {
            currentProfile = localProfile;
            updateProfileUI(localProfile);
            loadTodayFoodEntriesFromLocal();
        }

        // Firebase actualiza por encima con los datos más recientes
        refUsers.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile remoteProfile = snapshot.getValue(UserProfile.class);
                if (remoteProfile != null) {
                    localDb.saveUserProfile(user.getUid(), remoteProfile);
                    currentProfile = remoteProfile;
                    updateProfileUI(remoteProfile);
                    loadTodayFoodEntries();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_PROFILE", "Error al cargar perfil: " + error.getMessage());
                // Ya se mostraron los datos locales arriba; no hace falta hacer nada más
            }
        });
    }

    private void loadTodayFoodEntries() {
        if (refUserFoods == null) return;
        refUserFoods.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Comprobar primero si Firebase tiene al menos una entrada válida.
                // Si no, NO limpiamos local: preservamos los datos que ya se cargaron
                // desde SQLite en lugar de dejar el día vacío por un snapshot fallido.
                boolean hasAnyEntry = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.child("name").getValue(String.class) != null) {
                        hasAnyEntry = true;
                        break;
                    }
                }
                if (!hasAnyEntry) {
                    updateDailyDisplay();
                    return;
                }

                foodEntryAdapter.clear();
                if (currentUid != null) localDb.clearFoodEntriesForDate(currentUid, todayDate);
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name    = child.child("name").getValue(String.class);
                    Double kcal    = child.child("kcalPer100g").getValue(Double.class);
                    Double carbs   = child.child("carbsPer100g").getValue(Double.class);
                    Double protein = child.child("proteinPer100g").getValue(Double.class);
                    Double fat     = child.child("fatPer100g").getValue(Double.class);
                    Double grams   = child.child("grams").getValue(Double.class);
                    if (name != null && kcal != null && grams != null) {
                        FoodEntry entry = new FoodEntry(
                                name, kcal,
                                carbs   != null ? carbs   : 0.0,
                                protein != null ? protein : 0.0,
                                fat     != null ? fat     : 0.0,
                                grams);
                        entry.setFirebaseKey(child.getKey());
                        foodEntryAdapter.addEntry(entry);
                        if (currentUid != null) localDb.saveFoodEntry(currentUid, todayDate, entry);
                    }
                }
                updateDailyDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_FOODS", "Error al cargar comidas: " + error.getMessage());
                // Los datos locales ya se mostraron antes de llamar a este método
            }
        });
    }

    private void loadTodayFoodEntriesFromLocal() {
        if (currentUid == null) return;
        List<FoodEntry> local = localDb.getFoodEntriesForDate(currentUid, todayDate);
        foodEntryAdapter.clear();
        for (FoodEntry e : local) foodEntryAdapter.addEntry(e);
        updateDailyDisplay();
    }

    private void updateProfileUI(UserProfile profile) {
        double heightM   = profile.getHeight() / 100.0;
        double imc       = profile.getWeight() / (heightM * heightM);
        double tmbSexAdj = "Mujer".equals(profile.getSex()) ? -161 : 5;
        double tmb       = (10 * profile.getWeight()) + (6.25 * profile.getHeight())
                           - (5 * profile.getAge()) + tmbSexAdj;
        double tdee      = tmb * 1.375;  // factor actividad ligera

        String status;
        int    textColor;
        int    bgColor;

        if (imc < 16.0) {
            status       = "Delgadez severa"; textColor = 0xFF7F1D1D; bgColor = 0xFFFECACA;
            kcalObjetivo = tdee * 1.20;
        } else if (imc < 18.5) {
            status       = "Bajo peso";       textColor = 0xFF0D47A1; bgColor = 0xFFE3F2FD;
            kcalObjetivo = tdee * 1.15;
        } else if (imc < 25.0) {
            status       = "Normal";          textColor = 0xFF166534; bgColor = 0xFFDCFCE7;
            kcalObjetivo = tdee;
        } else if (imc < 30.0) {
            status       = "Sobrepeso";       textColor = 0xFFB45309; bgColor = 0xFFFEF3C7;
            kcalObjetivo = tdee * 0.85;
        } else if (imc < 35.0) {
            status       = "Obesidad I";      textColor = 0xFFC2410C; bgColor = 0xFFFFEDD5;
            kcalObjetivo = tdee * 0.80;
        } else if (imc < 40.0) {
            status       = "Obesidad II";     textColor = 0xFFB91C1C; bgColor = 0xFFFEE2E2;
            kcalObjetivo = tdee * 0.75;
        } else {
            status       = "Obesidad III";    textColor = 0xFF7F1D1D; bgColor = 0xFFFECACA;
            kcalObjetivo = tdee * 0.75;
        }

        // Suelo de seguridad: no recomendar menos de 1200 kcal (mujer) / 1500 kcal (hombre)
        double minSafe = "Mujer".equals(profile.getSex()) ? 1200 : 1500;
        if (kcalObjetivo < minSafe) kcalObjetivo = minSafe;

        // Distribución estándar: 50% carbos (4 kcal/g) · 20% proteína (4 kcal/g) · 30% grasa (9 kcal/g)
        carbsObjetivo   = (kcalObjetivo * 0.50) / 4.0;
        proteinObjetivo = (kcalObjetivo * 0.20) / 4.0;
        fatObjetivo     = (kcalObjetivo * 0.30) / 9.0;

        binding.txtIMCValueMainActivity.setText(String.format("%.1f", imc));
        binding.txtWeightValueMainActivity.setText(String.format("%.1f kg", profile.getWeight()));
        binding.lblCurrentKcalMainActivity.setText("0");
        binding.lblMaxKcalMainActivity.setText(String.format("/ %.0f kcal", kcalObjetivo));
        binding.barProgressMainActivity.setProgress(0);
        binding.txtIMCStatus.setText(status);
        binding.txtIMCStatus.setTextColor(textColor);

        updateDailyDisplay();

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(24f);
        badge.setColor(bgColor);
        binding.txtIMCStatus.setBackground(badge);

        updateProButton();
    }

    private void updateProButton() {
        boolean pro = isUserPro();
        MaterialButton btn = binding.btnResetMainActivity;

        if (pro) {
            int iconSizePx  = (int) (16 * getResources().getDisplayMetrics().density);
            int iconPadPx   = (int) (4  * getResources().getDisplayMetrics().density);
            btn.setText("Pro");
            btn.setIconResource(R.drawable.ic_check);
            btn.setIconTint(ColorStateList.valueOf(0xFFFFFFFF));
            btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            btn.setIconSize(iconSizePx);
            btn.setIconPadding(iconPadPx);
            btn.setBackgroundTintList(ColorStateList.valueOf(0xFF16A34A));
        } else {
            btn.setText("+Pro");
            btn.setIcon(null);
            btn.setBackgroundTintList(ColorStateList.valueOf(0xFF0284C7));
        }
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
        LocalDate today = LocalDate.now();
        String todayIso = today.toString();
        String lastReset = sp.getString("last_reset_date", "");
        if (!todayIso.equals(lastReset)) {
            // Cambió el día real: saltamos al día actual y limpiamos la vista
            changeSelectedDate(today);
            sp.edit().putString("last_reset_date", todayIso).apply();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SUSCRIPCIÓN PRO
    // ══════════════════════════════════════════════════════════════════════════════

    private boolean isUserPro() {
        return currentProfile != null && currentProfile.getIsPro();
    }

    private void showProSubscriptionDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_pro_subscription, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setCancelable(true)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnSubscribe = content.findViewById(R.id.btnSubscribePro);
        TextView       btnDismiss   = content.findViewById(R.id.btnDismissPro);

        btnSubscribe.setOnClickListener(v -> {
            dialog.dismiss();
            confirmProSubscription();
        });
        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void confirmProSubscription() {
        if (currentProfile == null || currentUid == null) {
            Toast.makeText(this, "Inicia sesión para suscribirte", Toast.LENGTH_SHORT).show();
            return;
        }
        currentProfile.setIsPro(true);
        refUsers.child(currentUid).child("isPro").setValue(true);
        localDb.saveUserProfile(currentUid, currentProfile);
        updateProButton();
        Toast.makeText(this, "¡Bienvenido a Kcal Tracker Pro!", Toast.LENGTH_LONG).show();
    }
}
