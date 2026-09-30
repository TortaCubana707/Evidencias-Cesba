package com.example.evidenciascesba;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EvidenciaAdapter extends RecyclerView.Adapter<EvidenciaAdapter.ViewHolder> {

    public interface OnEvidenciaClickListener {
        void onFichaClick(Evidencia evidencia);
        void onCompartirClick(Evidencia evidencia);
        void onEliminarClick(Evidencia evidencia, int position);
    }

    private final List<Evidencia> listaEvidencias;
    private final OnEvidenciaClickListener listener;

    public EvidenciaAdapter(List<Evidencia> listaEvidencias, OnEvidenciaClickListener listener) {
        this.listaEvidencias = listaEvidencias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_evidencia, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Evidencia evidencia = listaEvidencias.get(position);

        holder.txtTitulo.setText(evidencia.getTitulo() != null && !evidencia.getTitulo().isEmpty()
                ? evidencia.getTitulo() : "Sin título");

        String fechaHora = "Fecha: " + (evidencia.getFecha() != null ? evidencia.getFecha() : "--")
                + " | Hora: " + (evidencia.getHora() != null ? evidencia.getHora() : "--");
        holder.txtFechaHora.setText(fechaHora);

        holder.txtDescripcion.setText(evidencia.getDescripcion() != null && !evidencia.getDescripcion().isEmpty()
                ? evidencia.getDescripcion() : "Sin descripción");

        if (evidencia.getImagePath() != null) {
            Bitmap bitmap = EvidenciaStorageManager.decodeSampledBitmapFromFile(evidencia.getImagePath(), 200, 200);
            if (bitmap != null) {
                holder.imgThumb.setImageBitmap(bitmap);
            } else {
                holder.imgThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.imgThumb.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnFicha.setOnClickListener(v -> {
            if (listener != null) listener.onFichaClick(evidencia);
        });

        holder.btnCompartir.setOnClickListener(v -> {
            if (listener != null) listener.onCompartirClick(evidencia);
        });

        holder.btnEliminar.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) {
                listener.onEliminarClick(evidencia, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaEvidencias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView txtTitulo;
        TextView txtFechaHora;
        TextView txtDescripcion;
        Button btnFicha;
        Button btnCompartir;
        Button btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgItemThumb);
            txtTitulo = itemView.findViewById(R.id.txtItemTitulo);
            txtFechaHora = itemView.findViewById(R.id.txtItemFechaHora);
            txtDescripcion = itemView.findViewById(R.id.txtItemDescripcion);
            btnFicha = itemView.findViewById(R.id.btnItemFicha);
            btnCompartir = itemView.findViewById(R.id.btnItemCompartir);
            btnEliminar = itemView.findViewById(R.id.btnItemEliminar);
        }
    }
}
