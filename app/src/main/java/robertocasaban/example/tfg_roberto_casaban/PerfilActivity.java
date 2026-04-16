package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityPerfilBinding;
import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class PerfilActivity extends AppCompatActivity {

    private ActivityPerfilBinding binding;
    private LocalDatabaseHelper localDb;
    private DatabaseReference refUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDb  = new LocalDatabaseHelper(this);
        refUsers = FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("users");

        loadProfile();
        setupSaveButton();
        setupDeleteButton();
        setupBottomNav();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARGA DE PERFIL
    // ══════════════════════════════════════════════════════════════════════════

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // SQLite primero (instantáneo)
        UserProfile local = localDb.getUserProfile(user.getUid());
        if (local != null) fillForm(local);

        // Firebase por encima
        refUsers.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile remote = snapshot.getValue(UserProfile.class);
                if (remote != null) fillForm(remote);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fillForm(UserProfile profile) {
        binding.editName.setText(profile.getName());
        binding.editWeight.setText(String.valueOf(profile.getWeight()));
        binding.editHeight.setText(String.valueOf(profile.getHeight()));
        binding.editAge.setText(String.valueOf(profile.getAge()));
        binding.editGoalWeight.setText(String.valueOf(profile.getGoalWeight()));

        if ("Mujer".equals(profile.getSex())) {
            binding.radioGroupSex.check(R.id.radioMujer);
        } else {
            binding.radioGroupSex.check(R.id.radioHombre);
        }

        updateBunnyImage(profile.getWeight(), profile.getHeight());
    }

    private void updateBunnyImage(double weight, double height) {
        if (weight <= 0 || height <= 0) return;

        double heightM = height / 100.0;
        double imc = weight / (heightM * heightM);

        int    bunnyDrawable;
        String statusText;
        int    textColor;
        int    bgColor;

        if (imc < 18.5) {
            bunnyDrawable = R.drawable.bunny_underweight;
            statusText = "Bajo peso";  textColor = 0xFF0D47A1; bgColor = 0xFFE3F2FD;
        } else if (imc < 25.0) {
            bunnyDrawable = R.drawable.bunny_fit;
            statusText = "Normal";     textColor = 0xFF166534; bgColor = 0xFFDCFCE7;
        } else if (imc < 30.0) {
            bunnyDrawable = R.drawable.bunny_overweight;
            statusText = "Sobrepeso";  textColor = 0xFFE65100; bgColor = 0xFFFFF3E0;
        } else if (imc < 35.0) {
            bunnyDrawable = R.drawable.bunny_obese1;
            statusText = "Obesidad I"; textColor = 0xFFB71C1C; bgColor = 0xFFFFEBEE;
        } else if (imc < 40.0) {
            bunnyDrawable = R.drawable.bunny_obese2;
            statusText = "Obesidad II"; textColor = 0xFFB71C1C; bgColor = 0xFFFFCDD2;
        } else {
            bunnyDrawable = R.drawable.bunny_obese3;
            statusText = "Obesidad III"; textColor = 0xFF7F0000; bgColor = 0xFFEF9A9A;
        }

        binding.imageProfileHeader.setImageResource(bunnyDrawable);

        binding.txtStatusPerfil.setText(statusText);
        binding.txtStatusPerfil.setTextColor(textColor);
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(bgColor);
        binding.txtStatusPerfil.setBackground(badge);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARDAR
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSaveButton() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        binding.button.setOnClickListener(v -> {
            String name      = binding.editName.getText().toString().trim();
            String weightStr = binding.editWeight.getText().toString().trim();
            String heightStr = binding.editHeight.getText().toString().trim();
            String ageStr    = binding.editAge.getText().toString().trim();
            String goalWStr  = binding.editGoalWeight.getText().toString().trim();

            if (name.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty()
                    || ageStr.isEmpty() || goalWStr.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            String sex = (binding.radioGroupSex.getCheckedRadioButtonId() == R.id.radioMujer)
                    ? "Mujer" : "Hombre";

            UserProfile updated = new UserProfile(
                    name, user.getEmail(),
                    Double.parseDouble(weightStr),
                    Double.parseDouble(heightStr),
                    Integer.parseInt(ageStr),
                    Double.parseDouble(goalWStr),
                    sex
            );

            refUsers.child(user.getUid()).setValue(updated)
                    .addOnSuccessListener(aVoid -> {
                        localDb.saveUserProfile(user.getUid(), updated);
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();

                        updateBunnyImage(updated.getWeight(), updated.getHeight());

                        Animation jump = AnimationUtils.loadAnimation(this, R.anim.bunny_jump);
                        binding.imageProfileHeader.startAnimation(jump);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 3000);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ELIMINAR CUENTA
    // ══════════════════════════════════════════════════════════════════════════

    private void setupDeleteButton() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            binding.btnDeleteAccount.setVisibility(View.GONE);
            return;
        }

        binding.btnDeleteAccount.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro? Esta acción es irreversible y borrará todos tus datos.")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    String uid = user.getUid();
                    refUsers.child(uid).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                localDb.deleteUserProfile(uid);
                                user.delete()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show();
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
                .show()
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAVEGACIÓN
    // ══════════════════════════════════════════════════════════════════════════

    private void setupBottomNav() {
        binding.bottomNavPerfil.setSelectedItemId(R.id.nav_profile);
        binding.bottomNavPerfil.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.btnHomeNavBar) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.btnTargetNavBar) {
                startActivity(new Intent(this, ObjetivoActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, EstadisticasActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
