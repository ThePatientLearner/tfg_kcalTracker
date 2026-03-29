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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import robertocasaban.example.tfg_roberto_casaban.adapters.StatLogAdapter;
import robertocasaban.example.tfg_roberto_casaban.database.LocalDatabaseHelper;
import robertocasaban.example.tfg_roberto_casaban.databinding.ActivityEstadisticasBinding;

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

        // Cargar desde SQLite primero (instantáneo)
        if (currentUid != null) {
            Map<String, Double> local = localDb.getAllDailyTotals(currentUid);
            if (!local.isEmpty()) updateUI(local);
        }

        // Firebase actualiza con los datos más recientes
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
                Map<String, Double> totals = new TreeMap<>(); // TreeMap ordena por fecha automáticamente
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    String date = dateSnap.getKey();
                    Double kcal = dateSnap.child("totalKcal").getValue(Double.class);
                    if (date != null && kcal != null) {
                        totals.put(date, kcal);
                        localDb.saveDailyTotal(currentUid, date, kcal);
                    }
                }
                if (!totals.isEmpty()) updateUI(totals);
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

    private void updateUI(Map<String, Double> totals) {
        logAdapter.setData(totals);
        updateChart(totals);
    }

    private void updateChart(Map<String, Double> totals) {
        List<Entry>  entries = new ArrayList<>();
        List<String> labels  = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            entries.add(new Entry(i, e.getValue().floatValue()));
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
}
