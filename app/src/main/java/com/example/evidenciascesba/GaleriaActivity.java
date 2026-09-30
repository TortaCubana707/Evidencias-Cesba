package com.example.evidenciascesba;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GaleriaActivity extends AppCompatActivity implements EvidenciaAdapter.OnEvidenciaClickListener {

    private TextView txtGaleriaContador;
    private View layoutEmpty;
    private RecyclerView recyclerEvidencias;

    private EvidenciaAdapter adapter;
    private List<Evidencia> listaEvidencias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galeria);

        txtGaleriaContador = findViewById(R.id.txtGaleriaContador);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        recyclerEvidencias = findViewById(R.id.recyclerEvidencias);
        Button btnGaleriaVolver = findViewById(R.id.btnGaleriaVolver);

        btnGaleriaVolver.setOnClickListener(v -> finish());

        recyclerEvidencias.setLayoutManager(new LinearLayoutManager(this));

        cargarDatos();
    }

    private void cargarDatos() {
        listaEvidencias = EvidenciaStorageManager.cargarEvidencias(this);
        actualizarUI();
    }

    private void actualizarUI() {
        int total = listaEvidencias.size();
        if (total == 1) {
            txtGaleriaContador.setText(R.string.galeria_total_singular);
        } else {
            txtGaleriaContador.setText(getString(R.string.galeria_total_plural, total));
        }

        if (total == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerEvidencias.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerEvidencias.setVisibility(View.VISIBLE);
            adapter = new EvidenciaAdapter(listaEvidencias, this);
            recyclerEvidencias.setAdapter(adapter);
        }
    }

    @Override
    public void onFichaClick(Evidencia evidencia) {
        mostrarDialogFicha(evidencia);
    }

    @Override
    public void onCompartirClick(Evidencia evidencia) {
        compartirEvidencia(evidencia);
    }

    @Override
    public void onEliminarClick(Evidencia evidencia, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar evidencia")
                .setMessage("¿Estás seguro de que deseas eliminar esta evidencia permanentemente?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    EvidenciaStorageManager.eliminarEvidencia(this, evidencia.getId());
                    listaEvidencias.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, listaEvidencias.size());
                    actualizarUI();
                    Toast.makeText(this, "Evidencia eliminada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogFicha(Evidencia evidencia) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ficha, null);

        ImageView imgFoto = view.findViewById(R.id.imgFichaFoto);
        TextView txtTitulo = view.findViewById(R.id.txtFichaTitulo);
        TextView txtDescripcion = view.findViewById(R.id.txtFichaDescripcion);
        TextView txtFecha = view.findViewById(R.id.txtFichaFecha);
        TextView txtHora = view.findViewById(R.id.txtFichaHora);
        TextView txtEstado = view.findViewById(R.id.txtFichaEstado);
        Button btnCerrar = view.findViewById(R.id.btnFichaCerrar);
        Button btnCompartir = view.findViewById(R.id.btnFichaCompartir);

        txtTitulo.setText(evidencia.getTitulo() != null && !evidencia.getTitulo().isEmpty()
                ? evidencia.getTitulo() : "Sin título");
        txtDescripcion.setText(evidencia.getDescripcion() != null && !evidencia.getDescripcion().isEmpty()
                ? evidencia.getDescripcion() : "Sin descripción");
        txtFecha.setText(evidencia.getFecha() != null ? evidencia.getFecha() : "--/--/----");
        txtHora.setText(evidencia.getHora() != null ? evidencia.getHora() : "--:--:--");
        txtEstado.setText("GUARDADA / COMPLETADA");

        if (evidencia.getImagePath() != null) {
            File imgFile = new File(evidencia.getImagePath());
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    imgFoto.setImageBitmap(bitmap);
                }
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        btnCompartir.setOnClickListener(v -> {
            dialog.dismiss();
            compartirEvidencia(evidencia);
        });

        dialog.show();
    }

    private void compartirEvidencia(Evidencia evidencia) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");

        String texto = "--- EVIDENCIA CESBA ---\n" +
                "Título: " + evidencia.getTitulo() + "\n" +
                "Descripción: " + evidencia.getDescripcion() + "\n" +
                "Fecha: " + evidencia.getFecha() + " - Hora: " + evidencia.getHora();

        shareIntent.putExtra(Intent.EXTRA_TEXT, texto);

        if (evidencia.getImagePath() != null) {
            File file = new File(evidencia.getImagePath());
            if (file.exists()) {
                Uri imageUri = EvidenciaStorageManager.getUriParaArchivo(this, file);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        startActivity(Intent.createChooser(shareIntent, "Compartir evidencia CESBA"));
    }
}
