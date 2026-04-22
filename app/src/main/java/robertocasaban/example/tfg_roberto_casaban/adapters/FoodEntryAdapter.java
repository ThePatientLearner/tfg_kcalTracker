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

    public interface OnEntryClickListener {
        void onEntryClick(int position, FoodEntry entry);
    }

    private final List<FoodEntry> entries = new ArrayList<>();
    private OnEntryClickListener listener;

    public void setOnEntryClickListener(OnEntryClickListener listener) {
        this.listener = listener;
    }

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

    public double getTotalCarbs() {
        double total = 0;
        for (FoodEntry e : entries) total += e.getTotalCarbs();
        return total;
    }

    public double getTotalProtein() {
        double total = 0;
        for (FoodEntry e : entries) total += e.getTotalProtein();
        return total;
    }

    public double getTotalFat() {
        double total = 0;
        for (FoodEntry e : entries) total += e.getTotalFat();
        return total;
    }

    /** Elimina una entrada por posición */
    public void removeEntry(int position) {
        entries.remove(position);
        notifyItemRemoved(position);
    }

    /** Reemplaza una entrada (para actualizar gramos) */
    public void updateEntry(int position, FoodEntry entry) {
        entries.set(position, entry);
        notifyItemChanged(position);
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
        holder.txtCarbs.setText(String.format("%.0fg C", entry.getTotalCarbs()));
        holder.txtProtein.setText(String.format("%.0fg P", entry.getTotalProtein()));
        holder.txtFat.setText(String.format("%.0fg G", entry.getTotalFat()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEntryClick(holder.getAdapterPosition(), entry);
        });
    }

    @Override
    public int getItemCount() { return entries.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtDetails;
        final TextView txtKcal;
        final TextView txtCarbs;
        final TextView txtProtein;
        final TextView txtFat;

        ViewHolder(@NonNull View v) {
            super(v);
            txtName    = v.findViewById(R.id.lblFoodNameCard);
            txtDetails = v.findViewById(R.id.lblFoodDetailsCard);
            txtKcal    = v.findViewById(R.id.lblKcalValueCard);
            txtCarbs   = v.findViewById(R.id.lblCarbsCard);
            txtProtein = v.findViewById(R.id.lblProteinCard);
            txtFat     = v.findViewById(R.id.lblFatCard);
        }
    }
}
