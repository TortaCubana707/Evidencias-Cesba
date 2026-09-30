package com.example.evidenciascesba;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EvidenciasCESBA_Main";

    // COMPONENTES DE LA INTERFAZ
    private ImageView imgFoto;
    private TextView txtFecha;
    private TextView txtHora;
    private TextView txtEstado;
    private TextView txtContador;

    private EditText edtTitulo;
    private EditText edtDescripcion;

    private Button btnTomarFoto;

    // VARIABLES DE DATOS Y CAPTURA DE CÁMARA
    private Bitmap fotografiaBitmap = null;
    private String fechaCaptura = null;
    private String horaCaptura = null;

    private File tempCamaraFile = null;

    // LAUNCHER PARA PERMISOS DE CÁMARA
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d(TAG, "Resultado de permiso de cámara: " + isGranted);
                if (isGranted) {
                    lanzarIntentCamara();
                } else {
                    Toast.makeText(this, "Se requiere permiso de cámara para tomar fotografías", Toast.LENGTH_LONG).show();
                }
            });

    // LAUNCHER PARA LA CÁMARA
    @SuppressWarnings("deprecation")
    private final ActivityResultLauncher<Intent> launcherCamara =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        Log.d(TAG, "Resultado de cámara recibido. ResultCode: " + resultado.getResultCode());
                        if (resultado.getResultCode() == RESULT_OK) {
                            Bitmap loadedBitmap = null;

                            // 1. Intentar cargar desde archivo temporal
                            if (tempCamaraFile != null && tempCamaraFile.exists() && tempCamaraFile.length() > 0) {
                                loadedBitmap = EvidenciaStorageManager.decodeSampledBitmapFromFile(
                                        tempCamaraFile.getAbsolutePath(), 1024, 1024
                                );
                                Log.d(TAG, "Fotografía cargada desde archivo temporal");
                            }

                            // 2. Fallback: Intentar obtener desde extras data thumbnail
                            if (loadedBitmap == null && resultado.getData() != null && resultado.getData().getExtras() != null) {
                                Object dataObj = resultado.getData().getExtras().get("data");
                                if (dataObj instanceof Bitmap) {
                                    loadedBitmap = (Bitmap) dataObj;
                                    Log.d(TAG, "Fotografía cargada desde extras thumbnail");
                                }
                            }

                            if (loadedBitmap != null) {
                                fotografiaBitmap = loadedBitmap;

                                Date fechaActual = new Date();
                                SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                                fechaCaptura = sdfFecha.format(fechaActual);
                                horaCaptura = sdfHora.format(fechaActual);

                                imgFoto.setImageBitmap(fotografiaBitmap);
                                txtFecha.setText(getString(R.string.fecha_captura, fechaCaptura));
                                txtHora.setText(getString(R.string.hora_captura, horaCaptura));

                                txtEstado.setText(R.string.estado_capturada);
                                txtEstado.setTextColor(ContextCompat.getColor(this, R.color.status_captured));

                                btnTomarFoto.setText(R.string.tomar_otra_foto);

                                Toast.makeText(
                                        MainActivity.this,
                                        "Fotografía recibida con éxito",
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                Log.e(TAG, "No se pudo recuperar la imagen tomada");
                                Toast.makeText(this, "No se pudo procesar la fotografía", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "Operación de cámara cancelada por el usuario");
                            Toast.makeText(
                                    MainActivity.this,
                                    "Operación cancelada",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate MainActivity iniciado");

        // INICIALIZAR VISTAS
        imgFoto = findViewById(R.id.imgFoto);
        txtFecha = findViewById(R.id.txtFecha);
        txtHora = findViewById(R.id.txtHora);
        txtEstado = findViewById(R.id.txtEstado);
        txtContador = findViewById(R.id.txtContador);

        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);

        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        Button btnEliminarFoto = findViewById(R.id.btnEliminarFoto);
        Button btnGuardar = findViewById(R.id.btnGuardar);
        Button btnCompartir = findViewById(R.id.btnCompartir);
        Button btnGenerarFicha = findViewById(R.id.btnGenerarFicha);
        Button btnVerGaleria = findViewById(R.id.btnVerGaleria);

        // ASIGNAR LISTENERS DE BOTONES
        btnTomarFoto.setOnClickListener(v -> solicitarPermisoYTomarFoto());
        btnEliminarFoto.setOnClickListener(v -> eliminarFotoActual());
        btnGuardar.setOnClickListener(v -> guardarEvidencia());
        btnCompartir.setOnClickListener(v -> compartirEvidenciaActual());
        btnGenerarFicha.setOnClickListener(v -> generarFichaTecnica());
        btnVerGaleria.setOnClickListener(v -> abrirGaleria());

        actualizarContadorEvidencias();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarContadorEvidencias();
    }

    private void solicitarPermisoYTomarFoto() {
        Log.d(TAG, "solicitarPermisoYTomarFoto invocado");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permiso de cámara no otorgado. Solicitando...");
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Log.d(TAG, "Permiso de cámara previamente otorgado. Lanzando cámara...");
            lanzarIntentCamara();
        }
    }

    private void lanzarIntentCamara() {
        try {
            Intent intentCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File cacheDir = new File(getCacheDir(), "camera_temp");
            if (!cacheDir.exists()) {
                boolean created = cacheDir.mkdirs();
                Log.d(TAG, "Directorio de caché de cámara creado: " + created);
            }
            tempCamaraFile = new File(cacheDir, "temp_photo_" + System.currentTimeMillis() + ".jpg");

            Uri tempCamaraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    tempCamaraFile
            );

            intentCamara.putExtra(MediaStore.EXTRA_OUTPUT, tempCamaraUri);
            intentCamara.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            Log.d(TAG, "Iniciando Activity para cámara con URI: " + tempCamaraUri);
            launcherCamara.launch(intentCamara);
        } catch (Exception e) {
            Log.e(TAG, "Error crítico al intentar lanzar la cámara: " + e.getMessage(), e);
            Toast.makeText(this, "Error al abrir la cámara: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // NIVEL 1: ELIMINAR FOTO
    private void eliminarFotoActual() {
        if (fotografiaBitmap == null) {
            Toast.makeText(this, "No hay fotografía para eliminar", Toast.LENGTH_SHORT).show();
            return;
        }

        fotografiaBitmap = null;
        fechaCaptura = null;
        horaCaptura = null;

        if (tempCamaraFile != null && tempCamaraFile.exists()) {
            boolean deleted = tempCamaraFile.delete();
            Log.d(TAG, "Archivo temporal eliminado: " + deleted);
        }

        imgFoto.setImageResource(android.R.drawable.ic_menu_gallery);
        txtFecha.setText(R.string.fecha_vacia);
        txtHora.setText(R.string.hora_vacia);

        txtEstado.setText(R.string.estado_inicial);
        txtEstado.setTextColor(ContextCompat.getColor(this, R.color.status_pending));

        btnTomarFoto.setText(R.string.tomar_foto);

        Toast.makeText(this, "Fotografía eliminada", Toast.LENGTH_SHORT).show();
    }

    // NIVEL 2: CONTADOR DE FOTOGRAFÍAS / EVIDENCIAS
    private void actualizarContadorEvidencias() {
        int total = EvidenciaStorageManager.getCantidadEvidencias(this);
        txtContador.setText(getString(R.string.contador_fotografias, total));
    }

    // NIVEL 3: GUARDAR FOTOGRAFÍA / EVIDENCIA
    private void guardarEvidencia() {
        if (fotografiaBitmap == null) {
            Toast.makeText(this, "Por favor toma una fotografía primero", Toast.LENGTH_SHORT).show();
            return;
        }

        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();

        if (titulo.isEmpty()) {
            edtTitulo.setError("Ingresa un título para la evidencia");
            edtTitulo.requestFocus();
            return;
        }

        String id = String.valueOf(System.currentTimeMillis());
        String imagePath = EvidenciaStorageManager.guardarImagen(this, fotografiaBitmap, id);

        if (imagePath == null) {
            Toast.makeText(this, "Error al guardar la imagen en el dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        Evidencia evidencia = new Evidencia(
                id,
                titulo,
                descripcion,
                fechaCaptura,
                horaCaptura,
                imagePath
        );

        EvidenciaStorageManager.guardarEvidencia(this, evidencia);

        actualizarContadorEvidencias();
        txtEstado.setText(R.string.estado_guardada);
        txtEstado.setTextColor(ContextCompat.getColor(this, R.color.status_saved));

        Toast.makeText(this, "¡Evidencia guardada con éxito!", Toast.LENGTH_LONG).show();

        limpiarFormulario();
    }

    private void limpiarFormulario() {
        fotografiaBitmap = null;
        fechaCaptura = null;
        horaCaptura = null;

        imgFoto.setImageResource(android.R.drawable.ic_menu_gallery);
        txtFecha.setText(R.string.fecha_vacia);
        txtHora.setText(R.string.hora_vacia);
        edtTitulo.setText("");
        edtDescripcion.setText("");
        btnTomarFoto.setText(R.string.tomar_foto);
    }

    // NIVEL 3: COMPARTIR EVIDENCIA
    private void compartirEvidenciaActual() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();

        if (fotografiaBitmap == null && titulo.isEmpty()) {
            Toast.makeText(this, "Toma una foto o escribe información para compartir", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");

        String texto = "--- EVIDENCIA CESBA ---\n" +
                "Título: " + (titulo.isEmpty() ? "Sin título" : titulo) + "\n" +
                "Descripción: " + (descripcion.isEmpty() ? "Sin descripción" : descripcion) + "\n" +
                "Fecha: " + (fechaCaptura != null ? fechaCaptura : "--/--/----") +
                " - Hora: " + (horaCaptura != null ? horaCaptura : "--:--:--");

        shareIntent.putExtra(Intent.EXTRA_TEXT, texto);

        if (fotografiaBitmap != null) {
            Uri imageUri = EvidenciaStorageManager.getCacheUriParaCompartir(this, fotografiaBitmap);
            if (imageUri != null) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        startActivity(Intent.createChooser(shareIntent, "Compartir Evidencia CESBA"));
    }

    // NIVEL 3: GENERAR FICHA DE EVIDENCIAS
    private void generarFichaTecnica() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ficha, null);

        ImageView imgFichaFoto = view.findViewById(R.id.imgFichaFoto);
        TextView txtFichaTitulo = view.findViewById(R.id.txtFichaTitulo);
        TextView txtFichaDescripcion = view.findViewById(R.id.txtFichaDescripcion);
        TextView txtFichaFecha = view.findViewById(R.id.txtFichaFecha);
        TextView txtFichaHora = view.findViewById(R.id.txtFichaHora);
        TextView txtFichaEstado = view.findViewById(R.id.txtFichaEstado);
        Button btnCerrar = view.findViewById(R.id.btnFichaCerrar);
        Button btnCompartir = view.findViewById(R.id.btnFichaCompartir);

        txtFichaTitulo.setText(titulo.isEmpty() ? "Sin título asignado" : titulo);
        txtFichaDescripcion.setText(descripcion.isEmpty() ? "Sin descripción agregada" : descripcion);
        txtFichaFecha.setText(fechaCaptura != null ? fechaCaptura : "--/--/----");
        txtFichaHora.setText(horaCaptura != null ? horaCaptura : "--:--:--");

        if (fotografiaBitmap != null) {
            imgFichaFoto.setImageBitmap(fotografiaBitmap);
            txtFichaEstado.setText("CAPTURADA / LISTA PARA GUARDAR");
            txtFichaEstado.setTextColor(ContextCompat.getColor(this, R.color.status_captured));
        } else {
            imgFichaFoto.setImageResource(android.R.drawable.ic_menu_gallery);
            txtFichaEstado.setText("PENDIENTE DE FOTOGRAFÍA");
            txtFichaEstado.setTextColor(ContextCompat.getColor(this, R.color.status_pending));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        btnCompartir.setOnClickListener(v -> {
            dialog.dismiss();
            compartirEvidenciaActual();
        });

        dialog.show();
    }

    // NIVEL 3: GALERÍA DE EVIDENCIAS
    private void abrirGaleria() {
        Intent intent = new Intent(this, GaleriaActivity.class);
        startActivity(intent);
    }
}
