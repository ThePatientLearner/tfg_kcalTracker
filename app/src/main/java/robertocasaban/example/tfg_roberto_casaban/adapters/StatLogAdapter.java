package robertocasaban.example.tfg_roberto_casaban.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import robertocasaban.example.tfg_roberto_casaban.R;
import robertocasaban.example.tfg_roberto_casaban.models.DailyMacros;

public class StatLogAdapter extends RecyclerView.Adapter<StatLogAdapter.ViewHolder> {

    private final List<String>      dates = new ArrayList<>();
    private final List<DailyMacros> items = new ArrayList<>();

    public void setData(Map<String, DailyMacros> totals) {
        dates.clear();
        items.clear();
        // Mostrar del más reciente al más antiguo
        List<String> keys = new ArrayList<>(totals.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) {
            String date = keys.get(i);
            dates.add(date);
            items.add(totals.get(date));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stat_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String      date = dates.get(position);
        DailyMacros dm   = items.get(position);

        holder.txtDate.setText(date);
        holder.txtKcal.setText(String.format("%.0f kcal", dm.getKcal()));
        holder.txtMacros.setText(String.format("%.0fg C  ·  %.0fg P  ·  %.0fg G",
                dm.getCarbs(), dm.getProtein(), dm.getFat()));

        // Proporción por kcal aportadas: carbs*4, protein*4, fat*9
        double kcalCarbs   = dm.getCarbs()   * 4.0;
        double kcalProtein = dm.getProtein() * 4.0;
        double kcalFat     = dm.getFat()     * 9.0;
        double totalMacroKcal = kcalCarbs + kcalProtein + kcalFat;

        if (totalMacroKcal <= 0) {
            // Sin datos de macros: barra gris uniforme
            setWeight(holder.segCarbs,   0f);
            setWeight(holder.segProtein, 0f);
            setWeight(holder.segFat,     0f);
        } else {
            setWeight(holder.segCarbs,   (float) (kcalCarbs   / totalMacroKcal));
            setWeight(holder.segProtein, (float) (kcalProtein / totalMacroKcal));
            setWeight(holder.segFat,     (float) (kcalFat     / totalMacroKcal));
        }
    }

    private void setWeight(View v, float weight) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.weight = weight;
        v.setLayoutParams(lp);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDate;
        final TextView txtKcal;
        final TextView txtMacros;
        final View     segCarbs;
        final View     segProtein;
        final View     segFat;

        ViewHolder(@NonNull View v) {
            super(v);
            txtDate    = v.findViewById(R.id.txtStatDate);
            txtKcal    = v.findViewById(R.id.txtStatKcal);
            txtMacros  = v.findViewById(R.id.txtStatMacros);
            segCarbs   = v.findViewById(R.id.segCarbs);
            segProtein = v.findViewById(R.id.segProtein);
            segFat     = v.findViewById(R.id.segFat);
        }
    }
}
