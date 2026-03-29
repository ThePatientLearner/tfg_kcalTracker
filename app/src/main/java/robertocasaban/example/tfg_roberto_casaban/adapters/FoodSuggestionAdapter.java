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
import robertocasaban.example.tfg_roberto_casaban.models.FoodProduct;

/**
 * Adaptador para el dropdown de sugerencias de búsqueda.
 * Muestra hasta 3 resultados de la API con nombre y kcal/100g.
 * Al hacer clic en una sugerencia, notifica a MainActivity
 * a través de OnSuggestionClickListener.
 */
public class FoodSuggestionAdapter extends RecyclerView.Adapter<FoodSuggestionAdapter.ViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(FoodProduct product);
    }

    private final List<FoodProduct> suggestions = new ArrayList<>();
    private OnSuggestionClickListener listener;

    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    /**
     * Reemplaza la lista de sugerencias, filtrando solo productos con datos válidos
     * y limitando a 3 resultados máximo.
     */
    public void setSuggestions(List<FoodProduct> products) {
        suggestions.clear();
        if (products != null) {
            for (FoodProduct p : products) {
                if (p.hasValidData()) {
                    suggestions.add(p);
                    if (suggestions.size() >= 5) break;
                }
            }
        }
        notifyDataSetChanged();
    }

    public void clearSuggestions() {
        suggestions.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return suggestions.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_suggestion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodProduct product = suggestions.get(position);
        holder.txtName.setText(product.getProductName());
        holder.txtKcal.setText(String.format("%.0f kcal / 100g", product.getKcalPer100g()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionClick(product);
        });
    }

    @Override
    public int getItemCount() { return suggestions.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtKcal;

        ViewHolder(@NonNull View v) {
            super(v);
            txtName = v.findViewById(R.id.txtSuggestionName);
            txtKcal = v.findViewById(R.id.txtSuggestionKcal);
        }
    }
}
