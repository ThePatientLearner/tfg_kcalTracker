package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import robertocasaban.example.tfg_roberto_casaban.database.LocalDatabaseHelper;
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityLogBinding;
import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class LogInActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private ActivityLogBinding binding;
    private FirebaseUser user;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private DatabaseReference refUsers;
    private LocalDatabaseHelper localDb;
    private Handler animHandler;
    private Runnable animRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        localDb = new LocalDatabaseHelper(this);
        refUsers = FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users");

        // Comprobamos si ya hay un usuario autenticado
        user = auth.getCurrentUser();
        if (user != null) {
            updateUi(user);
        }

        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLoginLogInActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.txtEmailLogInActivity.getText().toString();
                String password = binding.txtPasswordLogInActivity.getText().toString();
                if(!email.isEmpty() && !password.isEmpty()){
                    doLogin(email, password);
                } else {
                    Toast.makeText(LogInActivity.this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnGoogleLogInActivity.setOnClickListener(v -> launchGoogleSignIn());

        binding.txtRegisterLogInActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LogInActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        // Iniciar animación de la mascota en bucle
        startMascotAnimation();
    }

    private void startMascotAnimation() {
        animHandler = new Handler(Looper.getMainLooper());
        animRunnable = new Runnable() {
            @Override
            public void run() {
                Drawable drawable = binding.imgLogoLogInActivity.getDrawable();
                if (drawable instanceof Animatable) {
                    ((Animatable) drawable).start();
                }
                animHandler.postDelayed(this, 1200);
            }
        };
        animHandler.post(animRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animHandler != null && animRunnable != null) {
            animHandler.removeCallbacks(animRunnable);
        }
    }

    private void updateUi(FirebaseUser user){
        if(user != null){
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void doLogin(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LogInActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            user = auth.getCurrentUser();
                            updateUi(user);
                            Toast.makeText(LogInActivity.this,
                                    "Inicio de sesión correcto",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LogInActivity.this,
                                    "Error al iniciar sesión: " + (task.getException() != null ? task.getException().getLocalizedMessage() : "error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(LogInActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LogInActivity.this,
                                e.getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchGoogleSignIn() {
        // signOut primero para forzar el selector de cuentas cada vez
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this,
                        "Google Sign-In cancelado o error (" + e.getStatusCode() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkProfileAndContinue(firebaseUser);
                        }
                    } else {
                        Toast.makeText(this,
                                "Error al autenticar con Firebase: " +
                                        (task.getException() != null ? task.getException().getLocalizedMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkProfileAndContinue(FirebaseUser firebaseUser) {
        refUsers.child(firebaseUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        // Ya tiene perfil → Main
                        Toast.makeText(this, "Bienvenido de nuevo", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        // Nuevo usuario → mostrar diálogo de perfil
                        Toast.makeText(this, "Completa tu perfil", Toast.LENGTH_SHORT).show();
                        showProfileDialog(firebaseUser);
                    }
                });
    }

    private void showProfileDialog(FirebaseUser firebaseUser) {
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
                refreshBunny(imageBunny, txtStatus,
                        editWeight.getText().toString(),
                        editHeight.getText().toString());
            }
        };
        editWeight.addTextChangedListener(bunnyWatcher);
        editHeight.addTextChangedListener(bunnyWatcher);

        // Prerrellenar nombre con el de Google si existe
        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
            editName.setText(firebaseUser.getDisplayName());
        }

        final String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";

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

            refUsers.child(firebaseUser.getUid()).setValue(profile)
                    .addOnSuccessListener(aVoid -> {
                        localDb.saveUserProfile(firebaseUser.getUid(), profile);
                        Toast.makeText(this, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Error al guardar el perfil: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show());
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

        if (imc < 18.5) {
            bunnyDrawable = R.drawable.bunny_underweight;
            statusText = "Bajo peso";   textColor = 0xFF0D47A1; bgColor = 0xFFE3F2FD;
        } else if (imc < 25.0) {
            bunnyDrawable = R.drawable.bunny_fit;
            statusText = "Normal";      textColor = 0xFF166534; bgColor = 0xFFDCFCE7;
        } else if (imc < 30.0) {
            bunnyDrawable = R.drawable.bunny_overweight;
            statusText = "Sobrepeso";   textColor = 0xFFE65100; bgColor = 0xFFFFF3E0;
        } else if (imc < 35.0) {
            bunnyDrawable = R.drawable.bunny_obese1;
            statusText = "Obesidad I";  textColor = 0xFFB71C1C; bgColor = 0xFFFFEBEE;
        } else if (imc < 40.0) {
            bunnyDrawable = R.drawable.bunny_obese2;
            statusText = "Obesidad II"; textColor = 0xFFB71C1C; bgColor = 0xFFFFCDD2;
        } else {
            bunnyDrawable = R.drawable.bunny_obese3;
            statusText = "Obesidad III"; textColor = 0xFF7F0000; bgColor = 0xFFEF9A9A;
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
