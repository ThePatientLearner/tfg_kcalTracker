package robertocasaban.example.tfg_roberto_casaban.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import robertocasaban.example.tfg_roberto_casaban.R;
import robertocasaban.example.tfg_roberto_casaban.models.FastingOption;

public class FastingOptionsAdapter extends RecyclerView.Adapter<FastingOptionsAdapter.VH> {

    public interface OnOptionClick { void onClick(FastingOption option); }

    private final List<FastingOption> items;
    private final OnOptionClick listener;

    public FastingOptionsAdapter(List<FastingOption> items, OnOptionClick listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fasting_option, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        FastingOption o = items.get(pos);
        h.txtName.setText(o.name);
        h.txtSubtitle.setText(o.subtitle);
        h.txtDescription.setText(o.description);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(o);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtSubtitle;
        final TextView txtDescription;

        VH(@NonNull View v) {
            super(v);
            txtName        = v.findViewById(R.id.txtFastingName);
            txtSubtitle    = v.findViewById(R.id.txtFastingSubtitle);
            txtDescription = v.findViewById(R.id.txtFastingDescription);
        }
    }
}
