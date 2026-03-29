package robertocasaban.example.tfg_roberto_casaban.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import robertocasaban.example.tfg_roberto_casaban.R;
import robertocasaban.example.tfg_roberto_casaban.models.FoodEntry;

/**
 * Adaptador para la lista de alimentos añadidos durante el día.
 * Usa el layout food_item_card.xml ya existente en el proyecto.
 */
public class FoodEntryAdapter extends RecyclerView.Adapter<FoodEntryAdapter.ViewHolder> {

    private final List<FoodEntry> entries = new ArrayList<>();

    /** Añade una nueva entrada y notifica al RecyclerView */
    public void addEntry(FoodEntry entry) {
        entries.add(entry);
        notifyItemInserted(entries.size() - 1);
    }

    /** Suma las kcal totales de todos los alimentos del día */
    public double getTotalKcal() {
        double total = 0;
        for (FoodEntry e : entries) total += e.getTotalKcal();
        return total;
    }

    /** Limpia todos los alimentos (para el botón Reset) */
    public void clear() {
        entries.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.food_item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodEntry entry = entries.get(position);
        holder.txtName.setText(entry.getName());
        holder.txtDetails.setText(String.format("%.0fg  •  %.0f kcal/100g", entry.getGrams(), entry.getKcalPer100g()));
        holder.txtKcal.setText(String.format("%.0f", entry.getTotalKcal()));
    }

    @Override
    public int getItemCount() { return entries.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtDetails;
        final TextView txtKcal;

        ViewHolder(@NonNull View v) {
            super(v);
            txtName    = v.findViewById(R.id.lblFoodNameCard);
            txtDetails = v.findViewById(R.id.lblFoodDetailsCard);
            txtKcal    = v.findViewById(R.id.lblKcalValueCard);
        }
    }
}
