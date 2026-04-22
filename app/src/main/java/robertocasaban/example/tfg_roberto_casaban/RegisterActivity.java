package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import robertocasaban.example.tfg_roberto_casaban.database.LocalDatabaseHelper;
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityRegisterBinding;
import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class RegisterActivity extends AppCompatActivity {

    private static final long BUNNY_DELAY_MS = 1000L;

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference refUsers;
    private LocalDatabaseHelper localDb;

    private final Handler bunnyHandler = new Handler(Looper.getMainLooper());
    private Runnable bunnyRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        localDb = new LocalDatabaseHelper(this);

        database = FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/"
        );
        refUsers = database.getReference("users");

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegisterRegisterActivity.setOnClickListener(v -> {
            String email = binding.txtEmailRegisterActivity.getText().toString().trim();
            String password = binding.txtPsswRegisterActivity.getText().toString();
            String confirmPassword = binding.txtConfPsswRegisterActivity.getText().toString();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            doRegister(email, password);
        });

        binding.btnBackRegisterActivity.setOnClickListener(v -> finish());
    }

    private void doRegister(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                        showProfileDialog(user, email);
                    } else {
                        Toast.makeText(this,
                                "No se pudo registrar: " + (task.getException() != null
                                        ? task.getException().getLocalizedMessage()
                                        : "error desconocido"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showProfileDialog(FirebaseUser user, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.popup_profile_edit, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        EditText   editName       = dialogView.findViewById(R.id.editName);
        EditText   editWeight     = dialogView.findViewById(R.id.editWeight);
        EditText   editHeight     = dialogView.findViewById(R.id.editHeight);
        EditText   editAge        = dialogView.findViewById(R.id.editAge);
        EditText   editGoalWeight = dialogView.findViewById(R.id.editGoalWeight);
        RadioGroup radioGroupSex  = dialogView.findViewById(R.id.radioGroupSex);
        Button     btnSave        = dialogView.findViewById(R.id.button);
        ImageView  imageBunny     = dialogView.findViewById(R.id.imageProfileHeader);
        TextView   txtStatus      = dialogView.findViewById(R.id.txtStatusPopup);
        dialogView.findViewById(R.id.btnDeleteAccount).setVisibility(View.GONE);

        TextWatcher bunnyWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (bunnyRunnable != null) bunnyHandler.removeCallbacks(bunnyRunnable);
                bunnyRunnable = () -> refreshBunny(imageBunny, txtStatus,
                        editWeight.getText().toString(),
                        editHeight.getText().toString());
                bunnyHandler.postDelayed(bunnyRunnable, BUNNY_DELAY_MS);
            }
        };
        editWeight.addTextChangedListener(bunnyWatcher);
        editHeight.addTextChangedListener(bunnyWatcher);

        dialog.setOnDismissListener(d -> {
            if (bunnyRunnable != null) bunnyHandler.removeCallbacks(bunnyRunnable);
        });

        btnSave.setOnClickListener(v -> {
            String name      = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            String heightStr = editHeight.getText().toString().trim();
            String ageStr    = editAge.getText().toString().trim();
            String goalWStr  = editGoalWeight.getText().toString().trim();

            if (name.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty()
                    || ageStr.isEmpty() || goalWStr.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los datos del perfil", Toast.LENGTH_SHORT).show();
                return;
            }

            String sex = (radioGroupSex.getCheckedRadioButtonId() == R.id.radioMujer) ? "Mujer" : "Hombre";

            UserProfile profile = new UserProfile(
                    name,
                    email,
                    Double.parseDouble(weightStr),
                    Double.parseDouble(heightStr),
                    Integer.parseInt(ageStr),
                    Double.parseDouble(goalWStr),
                    sex
            );

            // Guarda el perfil en users/{uid} — solo accesible por ese usuario
            refUsers.child(user.getUid()).setValue(profile)
                    .addOnSuccessListener(aVoid -> {
                        // Guardar también en la base de datos local SQLite
                        localDb.saveUserProfile(user.getUid(), profile);

                        Toast.makeText(this, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        startActivity(new Intent(this, LogInActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Error al guardar el perfil: " + e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void refreshBunny(ImageView imageBunny, TextView txtStatus, String weightStr, String heightStr) {
        double weight, height;
        try {
            weight = Double.parseDouble(weightStr);
            height = Double.parseDouble(heightStr);
        } catch (NumberFormatException e) {
            return;
        }
        if (weight <= 0 || height <= 0) return;

        double heightM = height / 100.0;
        double imc = weight / (heightM * heightM);

        int    bunnyDrawable;
        String statusText;
        int    textColor;
        int    bgColor;

        if (imc < 16.0) {
            bunnyDrawable = R.drawable.bunny_underweight;
            statusText = "Delgadez severa"; textColor = 0xFF7F1D1D; bgColor = 0xFFFECACA;
        } else if (imc < 18.5) {
            bunnyDrawable = R.drawable.bunny_underweight;
            statusText = "Bajo peso";       textColor = 0xFF0D47A1; bgColor = 0xFFE3F2FD;
        } else if (imc < 25.0) {
            bunnyDrawable = R.drawable.bunny_fit;
            statusText = "Normal";          textColor = 0xFF166534; bgColor = 0xFFDCFCE7;
        } else if (imc < 30.0) {
            bunnyDrawable = R.drawable.bunny_overweight;
            statusText = "Sobrepeso";       textColor = 0xFFB45309; bgColor = 0xFFFEF3C7;
        } else if (imc < 35.0) {
            bunnyDrawable = R.drawable.bunny_obese1;
            statusText = "Obesidad I";      textColor = 0xFFC2410C; bgColor = 0xFFFFEDD5;
        } else if (imc < 40.0) {
            bunnyDrawable = R.drawable.bunny_obese2;
            statusText = "Obesidad II";     textColor = 0xFFB91C1C; bgColor = 0xFFFEE2E2;
        } else {
            bunnyDrawable = R.drawable.bunny_obese3;
            statusText = "Obesidad III";    textColor = 0xFF7F1D1D; bgColor = 0xFFFECACA;
        }

        imageBunny.setImageResource(bunnyDrawable);
        txtStatus.setText(statusText);
        txtStatus.setTextColor(textColor);
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(bgColor);
        txtStatus.setBackground(badge);
    }
}
