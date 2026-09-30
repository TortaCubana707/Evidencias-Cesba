package com.example.evidenciascesba;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EvidenciaStorageManager {
    private static final String TAG = "EvidenciasCESBA_Storage";
    private static final String PREF_NAME = "EvidenciasCESBA_Prefs";
    private static final String KEY_EVIDENCIAS = "key_evidencias_list";

    public static String guardarImagen(Context context, Bitmap bitmap, String id) {
        if (context == null || bitmap == null || id == null) {
            Log.e(TAG, "guardarImagen: Parámetros nulos");
            return null;
        }
        File imagesDir = new File(context.getFilesDir(), "images");
        if (!imagesDir.exists()) {
            boolean created = imagesDir.mkdirs();
            Log.d(TAG, "Directorio de imágenes creado: " + created);
        }
        File imageFile = new File(imagesDir, "evidencia_" + id + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            Log.d(TAG, "Imagen guardada exitosamente en: " + imageFile.getAbsolutePath());
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error guardando imagen: " + e.getMessage(), e);
            return null;
        }
    }

    public static Uri getCacheUriParaCompartir(Context context, Bitmap bitmap) {
        if (context == null || bitmap == null) return null;
        File cacheImagesDir = new File(context.getCacheDir(), "images");
        if (!cacheImagesDir.exists()) {
            cacheImagesDir.mkdirs();
        }
        File shareFile = new File(cacheImagesDir, "compartir_evidencia.jpg");
        try (FileOutputStream fos = new FileOutputStream(shareFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    shareFile
            );
        } catch (Exception e) {
            Log.e(TAG, "Error preparando URI para compartir: " + e.getMessage(), e);
            return null;
        }
    }

    public static Uri getUriParaArchivo(Context context, File file) {
        if (context == null || file == null || !file.exists()) return null;
        try {
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo URI de archivo: " + e.getMessage(), e);
            return null;
        }
    }

    public static void guardarEvidencia(Context context, Evidencia evidencia) {
        if (context == null || evidencia == null) return;
        List<Evidencia> lista = cargarEvidencias(context);
        lista.add(0, evidencia);
        guardarLista(context, lista);
        Log.d(TAG, "Evidencia guardada. Total ahora: " + lista.size());
    }

    public static List<Evidencia> cargarEvidencias(Context context) {
        List<Evidencia> lista = new ArrayList<>();
        if (context == null) return lista;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_EVIDENCIAS, null);
        if (jsonString != null && !jsonString.isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonString);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Evidencia item = new Evidencia(
                            obj.optString("id"),
                            obj.optString("titulo"),
                            obj.optString("descripcion"),
                            obj.optString("fecha"),
                            obj.optString("hora"),
                            obj.optString("imagePath")
                    );
                    lista.add(item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cargando evidencias de SharedPreferences: " + e.getMessage(), e);
            }
        }
        return lista;
    }

    public static int getCantidadEvidencias(Context context) {
        return cargarEvidencias(context).size();
    }

    public static void eliminarEvidencia(Context context, String id) {
        if (context == null || id == null) return;
        List<Evidencia> lista = cargarEvidencias(context);
        Evidencia paraEliminar = null;
        for (Evidencia e : lista) {
            if (Objects.equals(e.getId(), id)) {
                paraEliminar = e;
                break;
            }
        }
        if (paraEliminar != null) {
            if (paraEliminar.getImagePath() != null) {
                File file = new File(paraEliminar.getImagePath());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "Archivo de imagen eliminado: " + deleted);
                }
            }
            lista.remove(paraEliminar);
            guardarLista(context, lista);
            Log.d(TAG, "Evidencia eliminada ID: " + id);
        }
    }

    public static Bitmap decodeSampledBitmapFromFile(String imagePath, int reqWidth, int reqHeight) {
        if (imagePath == null) return null;
        File file = new File(imagePath);
        if (!file.exists()) return null;

        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(imagePath, options);
        } catch (Exception e) {
            Log.e(TAG, "Error decodificando bitmap de " + imagePath + ": " + e.getMessage(), e);
            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static void guardarLista(Context context, List<Evidencia> lista) {
        try {
            JSONArray array = new JSONArray();
            for (Evidencia e : lista) {
                JSONObject obj = new JSONObject();
                obj.put("id", e.getId());
                obj.put("titulo", e.getTitulo());
                obj.put("descripcion", e.getDescripcion());
                obj.put("fecha", e.getFecha());
                obj.put("hora", e.getHora());
                obj.put("imagePath", e.getImagePath());
                array.put(obj);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_EVIDENCIAS, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error guardando lista a SharedPreferences: " + e.getMessage(), e);
        }
    }
}
