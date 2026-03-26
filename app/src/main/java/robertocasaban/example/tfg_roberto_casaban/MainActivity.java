package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import robertocasaban.example.tfg_roberto_casaban.database.LocalDatabaseHelper;
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityMainBinding;
import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sp;
    private FirebaseDatabase database;
    private DatabaseReference refUsers;
    private LocalDatabaseHelper localDb;
    private UserProfile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ATENCIÓN: Al usar ViewBinding, si el layout se llama activity_main_.xml
        // la clase generada es ActivityMain_Binding.
        // Pero lo más limpio es que usemos el nombre estándar.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sp      = getSharedPreferences("tfg_prefs", MODE_PRIVATE);
        localDb = new LocalDatabaseHelper(this);

        database = FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/"
        );
        refUsers = database.getReference("users");

        loadUserProfile();

        binding.btnLogoutMainActivity.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sp.edit().clear().apply();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, LogInActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

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

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        refUsers.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentProfile = snapshot.getValue(UserProfile.class);
                if (currentProfile != null) {
                    updateProfileUI(currentProfile);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfileUI(UserProfile profile) {
        double heightM = profile.getHeight() / 100.0;
        double imc     = profile.getWeight() / (heightM * heightM);

        double tmb = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * profile.getAge()) + 5;
        double kcalBase = tmb + 400;
        double kcalObjetivo;

        String status;
        int textColor;
        int bgColor;

        if (imc < 18.5) {
            status    = "Bajo peso";
            textColor = 0xFF0D47A1;
            bgColor   = 0xFFE3F2FD;
            kcalObjetivo = kcalBase + 500;
        } else if (imc < 25.0) {
            status    = "Normal";
            textColor = 0xFF166534;
            bgColor   = 0xFFDCFCE7;
            kcalObjetivo = kcalBase;
        } else if (imc < 30.0) {
            status    = "Sobrepeso";
            textColor = 0xFFE65100;
            bgColor   = 0xFFFFF3E0;
            kcalObjetivo = kcalBase - 500;
        } else if (imc < 35.0) {
            status    = "Obesidad I";
            textColor = 0xFFB71C1C;
            bgColor   = 0xFFFFEBEE;
            kcalObjetivo = kcalBase - 600;
        } else if (imc < 40.0) {
            status    = "Obesidad II";
            textColor = 0xFFB71C1C;
            bgColor   = 0xFFFFCDD2;
            kcalObjetivo = kcalBase - 700;
        } else {
            status    = "Obesidad III";
            textColor = 0xFF7F0000;
            bgColor   = 0xFFEF9A9A;
            kcalObjetivo = kcalBase - 800;
        }

        // CORRECCIÓN DE IDs: Basado en el archivo activity_main_.xml
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

        if (currentProfile != null) {
            editName.setText(currentProfile.getName());
            editWeight.setText(String.valueOf(currentProfile.getWeight()));
            editHeight.setText(String.valueOf(currentProfile.getHeight()));
            editAge.setText(String.valueOf(currentProfile.getAge()));
            editGoalWeight.setText(String.valueOf(currentProfile.getGoalWeight()));
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        btnSave.setOnClickListener(v -> {
            String name      = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            String heightStr = editHeight.getText().toString().trim();
            String ageStr    = editAge.getText().toString().trim();
            String goalWStr  = editGoalWeight.getText().toString().trim();

            if (name.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty() || ageStr.isEmpty() || goalWStr.isEmpty()) {
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

        dialog.show();
    }
}
