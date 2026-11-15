package com.sntt.appbuild;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {
    private final List<DownloadItem> items;
    private final Context context;

    public DownloadAdapter(Context context, List<DownloadItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadItem item = items.get(position);
        holder.tvTitle.setText(item.getTinyDbName());
        holder.tvUrl.setText(item.getFileLink());

        if (item.getMessage() != null && !item.getMessage().isEmpty()) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(item.getMessage());
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }

        switch (item.getStatus()) {
            case PENDING:
                holder.progressBar.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("Pending");
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                break;
            case DOWNLOADING:
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("Downloading...");
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
                break;
            case COMPLETED:
                holder.progressBar.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("✓ Saved");
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
                break;
            case FAILED:
                holder.progressBar.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("✕ Failed");
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
                break;
            case SKIPPED:
                holder.progressBar.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("Skipped");
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvUrl, tvMessage, tvStatusBadge;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvUrl = itemView.findViewById(R.id.tvItemUrl);
            tvMessage = itemView.findViewById(R.id.tvItemMessage);
            tvStatusBadge = itemView.findViewById(R.id.tvItemStatusBadge);
            progressBar = itemView.findViewById(R.id.itemProgressBar);
        }
    }
}
