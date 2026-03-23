package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.btnHomeNavBar) {
                return true;
            } else if (id == R.id.btnTargetNavBar) {
                showTargetDialog();
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show();
                return true;
            }
            
            return false;
        });
    }

    private void showTargetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_kg_edit, null);
        
        builder.setView(dialogView);
        
        // Aquí podrías añadir botones de "Guardar" o "Cerrar" si el layout no los tiene
        builder.setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}