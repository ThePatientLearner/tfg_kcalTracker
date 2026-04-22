package robertocasaban.example.tfg_roberto_casaban;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import robertocasaban.example.tfg_roberto_casaban.adapters.StatLogAdapter;
import robertocasaban.example.tfg_roberto_casaban.database.LocalDatabaseHelper;
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityEstadisticasBinding;
import robertocasaban.example.tfg_roberto_casaban.models.DailyMacros;

public class EstadisticasActivity extends AppCompatActivity {

    private ActivityEstadisticasBinding binding;
    private LocalDatabaseHelper localDb;
    private StatLogAdapter logAdapter;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEstadisticasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localDb = new LocalDatabaseHelper(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUid = user.getUid();

        setupRecyclerView();
        setupChart();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescamos cada vez que la pantalla vuelve al frente, para reflejar
        // comidas añadidas/editadas durante la sesión (vuelta desde Home por back,
        // por ejemplo).
        loadStats();
    }

    private void loadStats() {
        if (currentUid == null) return;

        // Cargar desde SQLite primero (instantáneo, refleja escrituras de la sesión)
        Map<String, DailyMacros> local = localDb.getAllDailyMacros(currentUid);
        if (!local.isEmpty()) updateUI(local);

        // Firebase actualiza con los datos más recientes (merge, no sobrescribe)
        loadFromFirebase();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════════════════

    private void setupRecyclerView() {
        logAdapter = new StatLogAdapter();
        binding.rvStatLog.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStatLog.setAdapter(logAdapter);
    }

    private void setupChart() {
        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.getLegend().setEnabled(false);
        binding.lineChart.setTouchEnabled(true);
        binding.lineChart.setDragEnabled(true);
        binding.lineChart.setScaleEnabled(true);
        binding.lineChart.setPinchZoom(true);
        binding.lineChart.setDrawGridBackground(false);

        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(0xFF64748B);
        xAxis.setTextSize(10f);

        binding.lineChart.getAxisLeft().setTextColor(0xFF64748B);
        binding.lineChart.getAxisLeft().setDrawGridLines(true);
        binding.lineChart.getAxisLeft().setGridColor(0xFFF1F5F9);
        binding.lineChart.getAxisRight().setEnabled(false);

        binding.lineChart.setNoDataText("Sin datos todavía");
        binding.lineChart.setNoDataTextColor(0xFF94A3B8);
    }

    private void setupBottomNav() {
        binding.bottomNavStats.setSelectedItemId(R.id.nav_stats);
        binding.bottomNavStats.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.btnHomeNavBar) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_stats) {
                return true;
            } else if (id == R.id.btnTargetNavBar || id == R.id.nav_profile) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARGA DE DATOS
    // ══════════════════════════════════════════════════════════════════════════

    private void loadFromFirebase() {
        if (currentUid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(
                "https://tfg-robertocasaban-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("users").child(currentUid).child("foods");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Autoritativo desde Firebase. Solo entran los días con entradas reales
                // (extractDailyMacros devuelve null si no hay). El caché del SDK ya
                // incluye las comidas añadidas en esta sesión, así que no hace falta
                // mezclar con SQLite (que puede arrastrar fantasmas).
                Map<String, DailyMacros> totals = new TreeMap<>();
                Set<String> realDates = new HashSet<>();
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    String date = dateSnap.getKey();
                    if (date == null) continue;
                    DailyMacros dm = extractDailyMacros(dateSnap);
                    if (dm == null) continue;
                    totals.put(date, dm);
                    realDates.add(date);
                    localDb.saveDailyTotals(currentUid, date,
                            dm.getKcal(), dm.getCarbs(), dm.getProtein(), dm.getFat());
                }

                // Purgar fechas locales que ya no corresponden a días reales
                // (fantasmas heredados de bugs previos).
                Set<String> localDates = new HashSet<>(localDb.getAllDailyMacros(currentUid).keySet());
                for (String date : localDates) {
                    if (!realDates.contains(date)) {
                        localDb.deleteDailyTotal(currentUid, date);
                    }
                }

                updateUI(totals);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("STATS_FIREBASE", "Error: " + error.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ACTUALIZAR UI
    // ══════════════════════════════════════════════════════════════════════════

    private void updateUI(Map<String, DailyMacros> totals) {
        // Filtrar días sin kcal reales: evita mostrar "fantasmas" (días futuros
        // visitados con el selector o registros con todo a 0 que quedaron en la BD).
        Map<String, DailyMacros> filtered = new TreeMap<>();
        for (Map.Entry<String, DailyMacros> e : totals.entrySet()) {
            if (e.getValue().getKcal() > 0) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        logAdapter.setData(filtered);
        updateChart(filtered);
    }

    private void updateChart(Map<String, DailyMacros> totals) {
        List<Entry>  entries = new ArrayList<>();
        List<String> labels  = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, DailyMacros> e : totals.entrySet()) {
            entries.add(new Entry(i, (float) e.getValue().getKcal()));
            // Mostrar solo MM-DD para que no se superponga en el eje
            String date = e.getKey();
            labels.add(date.length() >= 10 ? date.substring(5) : date);
            i++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Kcal");
        dataSet.setColor(0xFF0284C7);
        dataSet.setCircleColor(0xFF0284C7);
        dataSet.setCircleHoleColor(0xFFFFFFFF);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(0xFF0284C7);
        dataSet.setFillAlpha(20);

        binding.lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChart.getXAxis().setLabelCount(Math.min(labels.size(), 6), false);
        binding.lineChart.setData(new LineData(dataSet));
        binding.lineChart.animateX(600);
        binding.lineChart.invalidate();
    }

    /**
     * Extrae los macros diarios de un nodo de fecha en Firebase.
     * Prioriza sumar las entradas individuales (fuente autoritativa: así las comidas recién
     * añadidas durante la sesión siempre quedan reflejadas, sin depender de que `totalKcal`
     * haya sido escrito a tiempo). Si no hay entradas pero sí totales pre-calculados
     * (días históricos), usa esos como fallback.
     */
    private DailyMacros extractDailyMacros(DataSnapshot dateSnap) {
        double sumKcal = 0, sumCarbs = 0, sumProtein = 0, sumFat = 0;
        boolean hasEntries = false;

        for (DataSnapshot child : dateSnap.getChildren()) {
            String name = child.child("name").getValue(String.class);
            if (name == null) continue; // es un campo total u otro, no una entrada

            Double kcalPer100g    = child.child("kcalPer100g").getValue(Double.class);
            Double carbsPer100g   = child.child("carbsPer100g").getValue(Double.class);
            Double proteinPer100g = child.child("proteinPer100g").getValue(Double.class);
            Double fatPer100g     = child.child("fatPer100g").getValue(Double.class);
            Double grams          = child.child("grams").getValue(Double.class);
            if (kcalPer100g == null || grams == null) continue;

            double factor = grams / 100.0;
            sumKcal    += kcalPer100g * factor;
            sumCarbs   += (carbsPer100g   != null ? carbsPer100g   : 0.0) * factor;
            sumProtein += (proteinPer100g != null ? proteinPer100g : 0.0) * factor;
            sumFat     += (fatPer100g     != null ? fatPer100g     : 0.0) * factor;
            hasEntries = true;
        }

        // Solo consideramos un día "real" si hay al menos una entrada individual.
        // NO usamos el fallback de totalKcal porque los registros antiguos pueden
        // tener totales fantasma (basura de bugs previos) sin entradas asociadas.
        if (hasEntries) {
            return new DailyMacros(sumKcal, sumCarbs, sumProtein, sumFat);
        }
        return null;
    }
}
