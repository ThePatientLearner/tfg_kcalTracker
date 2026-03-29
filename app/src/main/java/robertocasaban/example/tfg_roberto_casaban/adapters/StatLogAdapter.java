package robertocasaban.example.tfg_roberto_casaban.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import robertocasaban.example.tfg_roberto_casaban.R;

public class StatLogAdapter extends RecyclerView.Adapter<StatLogAdapter.ViewHolder> {

    // Cada entrada: [0]=fecha, [1]=kcal
    private final List<String[]> items = new ArrayList<>();

    public void setData(Map<String, Double> totals) {
        items.clear();
        // Mostrar del más reciente al más antiguo
        List<String> dates = new ArrayList<>(totals.keySet());
        for (int i = dates.size() - 1; i >= 0; i--) {
            String date = dates.get(i);
            items.add(new String[]{date, String.format("%.0f kcal", totals.get(date))});
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
        String[] item = items.get(position);
        holder.txtDate.setText(item[0]);
        holder.txtKcal.setText(item[1]);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDate;
        final TextView txtKcal;

        ViewHolder(@NonNull View v) {
            super(v);
            txtDate = v.findViewById(R.id.txtStatDate);
            txtKcal = v.findViewById(R.id.txtStatKcal);
        }
    }
}
