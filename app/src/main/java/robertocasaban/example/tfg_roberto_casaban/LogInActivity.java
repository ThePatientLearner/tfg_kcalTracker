package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityLogBinding;

public class LogInActivity extends AppCompatActivity {

    private ActivityLogBinding binding;
    private FirebaseUser user;
    private FirebaseAuth auth;
    private Handler animHandler;
    private Runnable animRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Comprobamos si ya hay un usuario autenticado
        user = auth.getCurrentUser();
        if (user != null) {
            updateUi(user);
        }

        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                // Repetir cada 1.2 segundos (duración total de la animación)
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
}