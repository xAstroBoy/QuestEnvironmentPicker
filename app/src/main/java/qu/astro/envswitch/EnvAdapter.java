package qu.astro.envswitch;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EnvAdapter extends RecyclerView.Adapter<EnvAdapter.VH> {

    public interface Callbacks {
        void onChooseZip(Models.EnvApk item, TextView sceneView);
        void onApply(Models.EnvApk item, TextView stateView, TextView sceneView);
        void onApplyAsEnv(Models.EnvApk item, TextView stateView, TextView sceneView);
        void onSetDefault(Models.EnvApk item, TextView stateView, TextView sceneView);
        void onUninstall(Models.EnvApk item, TextView stateView);
    }

    private List<Models.EnvApk> items;
    private final PackageManager pm;
    private final Callbacks cb;

    public EnvAdapter(List<Models.EnvApk> items, PackageManager pm, Callbacks cb) {
        this.items = items;
        this.pm = pm;
        this.cb = cb;
    }

    public void update(List<Models.EnvApk> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon; TextView lbl; TextView pkg; TextView scene; TextView state;
        Button btnChoose; Button btnApply; Button btnSetDefault; Button btnApplyEnv; Button btnUninstall;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            lbl = v.findViewById(R.id.lbl);
            pkg = v.findViewById(R.id.pkg);
            scene = v.findViewById(R.id.scene);
            state = v.findViewById(R.id.state);
            btnChoose = v.findViewById(R.id.btnChooseZip);
            btnApply = v.findViewById(R.id.btnApply);
            btnSetDefault = v.findViewById(R.id.btnSetDefault);
            btnApplyEnv = v.findViewById(R.id.btnApplyEnv);
            btnUninstall = v.findViewById(R.id.btnUninstall);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_env, parent, false);
        return new VH(v);
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Models.EnvApk it = items.get(position);

        try {
            ApplicationInfo ai = pm.getApplicationInfo(it.packageName, 0);
            h.icon.setImageDrawable(pm.getApplicationIcon(ai));
        } catch (Exception ignore) {}

        h.lbl.setText(it.label);
        h.pkg.setText(it.packageName);
        h.scene.setText(it.sceneUri);
        h.state.setText("");

        h.lbl.setTypeface(null, Typeface.BOLD);
        switch (it.type) {
            case VISTA:
                h.lbl.setTextColor(0xFF7FC7FF);
                break;
            case FOOTPRINT:
                h.lbl.setTextColor(0xFF7FFF9F);
                break;
            default:
                h.lbl.setTextColor(0xFFD0BCFF);
                break;
        }

        if (it.zipEntries != null && it.zipEntries.size() > 1) {
            h.btnChoose.setVisibility(View.VISIBLE);
            h.btnChoose.setOnClickListener(v -> cb.onChooseZip(it, h.scene));
        } else {
            h.btnChoose.setVisibility(View.GONE);
        }

        switch (it.type) {
            case VISTA:
                h.btnApply.setText("Apply Vista");
                break;
            case FOOTPRINT:
                h.btnApply.setText("Apply Footprint");
                break;
            default:
                h.btnApply.setText("Apply");
                break;
        }

        h.btnApply.setOnClickListener(v -> cb.onApply(it, h.state, h.scene));
        h.btnSetDefault.setOnClickListener(v -> cb.onSetDefault(it, h.state, h.scene));
        h.btnApplyEnv.setOnClickListener(v -> cb.onApplyAsEnv(it, h.state, h.scene));
        h.btnUninstall.setOnClickListener(v -> cb.onUninstall(it, h.state));

        h.btnSetDefault.setVisibility(it.type == Models.Type.ENVIRONMENT ? View.VISIBLE : View.GONE);
        h.btnApplyEnv.setVisibility(it.type == Models.Type.FOOTPRINT ? View.VISIBLE : View.GONE);
        h.btnUninstall.setVisibility(it.isProtected() ? View.GONE : View.VISIBLE);
    }
}
