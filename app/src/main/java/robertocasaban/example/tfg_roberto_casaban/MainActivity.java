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
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private String todayDate;
    private String currentUid;

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

        todayDate = LocalDate.now().toString();
        FirebaseUser initUser = FirebaseAuth.getInstance().getCurrentUser();
        if (initUser != null) {
            currentUid   = initUser.getUid();
            refUserFoods = refUsers.child(currentUid).child("foods").child(todayDate);
        }

        setupRecyclerViews();
        setupSearch();
        loadUserProfile();


        // ── Botón +Pro ───────────────────────────────────────────────────────────
        binding.btnResetMainActivity.setOnClickListener(v -> showProSubscriptionDialog());

        // ── Logout ───────────────────────────────────────────────────────────────
        binding.btnLogoutMainActivity.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sp.edit().clear().apply();
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
                                        FoodEntry updated = new FoodEntry(entry.getName(), entry.getKcalPer100g(), newGrams);
                                        updated.setFirebaseKey(entry.getFirebaseKey());
                                        if (refUserFoods != null && entry.getFirebaseKey() != null) {
                                            refUserFoods.child(entry.getFirebaseKey()).child("grams").setValue(newGrams);
                                        }
                                        if (entry.getFirebaseKey() != null) {
                                            localDb.updateFoodEntryGrams(entry.getFirebaseKey(), newGrams);
                                        }
                                        foodEntryAdapter.updateEntry(position, updated);
                                        updateKcalDisplay();
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        } else {
                            // Eliminar
                            if (refUserFoods != null && entry.getFirebaseKey() != null) {
                                refUserFoods.child(entry.getFirebaseKey()).removeValue();
                            }
                            if (entry.getFirebaseKey() != null) {
                                localDb.deleteFoodEntry(entry.getFirebaseKey());
                            }
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

        builder.setPositiveButton("Añadir", (dialog, mythicalWhich) -> {
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
        if (refUserFoods != null) {
            DatabaseReference newRef = refUserFoods.push();
            entry.setFirebaseKey(newRef.getKey());
            java.util.HashMap<String, Object> data = new java.util.HashMap<>();
            data.put("name", entry.getName());
            data.put("kcalPer100g", entry.getKcalPer100g());
            data.put("grams", entry.getGrams());
            newRef.setValue(data);
        }
        if (currentUid != null) localDb.saveFoodEntry(currentUid, todayDate, entry);
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

        syncTotalKcal(totalKcal);
    }

    private void syncTotalKcal(double totalKcal) {
        if (refUserFoods != null) {
            refUserFoods.child("totalKcal").setValue(totalKcal);
        }
        if (currentUid != null) {
            localDb.saveDailyTotal(currentUid, todayDate, totalKcal);
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
                foodEntryAdapter.clear();
                if (currentUid != null) localDb.clearFoodEntriesForDate(currentUid, todayDate);
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name  = child.child("name").getValue(String.class);
                    Double kcal  = child.child("kcalPer100g").getValue(Double.class);
                    Double grams = child.child("grams").getValue(Double.class);
                    if (name != null && kcal != null && grams != null) {
                        FoodEntry entry = new FoodEntry(name, kcal, grams);
                        entry.setFirebaseKey(child.getKey());
                        foodEntryAdapter.addEntry(entry);
                        if (currentUid != null) localDb.saveFoodEntry(currentUid, todayDate, entry);
                    }
                }
                updateKcalDisplay();
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
        updateKcalDisplay();
    }

    private void updateProfileUI(UserProfile profile) {
        double heightM   = profile.getHeight() / 100.0;
        double imc       = profile.getWeight() / (heightM * heightM);
        double tmbSexAdj = "Mujer".equals(profile.getSex()) ? -161 : 5;
        double tmb       = (10 * profile.getWeight()) + (6.25 * profile.getHeight())
                           - (5 * profile.getAge()) + tmbSexAdj;
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
            todayDate = today;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                currentUid   = user.getUid();
                refUserFoods = refUsers.child(currentUid).child("foods").child(todayDate);
            }
            foodEntryAdapter.clear();
            updateKcalDisplay();
            sp.edit().putString("last_reset_date", today).apply();
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
        Toast.makeText(this, "¡Bienvenido a Kcal Tracker Pro!", Toast.LENGTH_LONG).show();
    }
}
