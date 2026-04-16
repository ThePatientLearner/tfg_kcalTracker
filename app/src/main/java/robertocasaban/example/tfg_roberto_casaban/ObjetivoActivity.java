package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityObjetivoBinding;

public class ObjetivoActivity extends AppCompatActivity {

    private ActivityObjetivoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityObjetivoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        double weight = getIntent().getDoubleExtra("weight", 0);
        double height = getIntent().getDoubleExtra("height", 0);

        if (weight > 0 && height > 0) {
            double heightM   = height / 100.0;
            double imcActual = weight / (heightM * heightM);

            binding.txtPesoActualValue.setText(String.format("%.1f kg", weight));
            binding.textView6.setText(String.format("%.1f", imcActual));

            binding.editTextText4.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String val = s.toString().trim();
                    if (val.isEmpty()) { binding.textView7.setText("..?"); return; }
                    try {
                        double imcDeseado   = Double.parseDouble(val);
                        double pesoObjetivo = imcDeseado * heightM * heightM;
                        binding.textView7.setText(String.format("%.1f kg", pesoObjetivo));
                    } catch (NumberFormatException e) {
                        binding.textView7.setText("..?");
                    }
                }
            });
        }

        setupBottomNav();
    }

    private void setupBottomNav() {
        binding.bottomNavObjetivo.setSelectedItemId(R.id.btnTargetNavBar);
        binding.bottomNavObjetivo.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.btnTargetNavBar) {
                return true;
            } else if (id == R.id.btnHomeNavBar) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, PerfilActivity.class));
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
