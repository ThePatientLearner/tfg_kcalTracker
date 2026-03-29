package robertocasaban.example.tfg_roberto_casaban.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import robertocasaban.example.tfg_roberto_casaban.models.FoodEntry;
import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "tfg_local.db";
    private static final int    DB_VERSION = 3;

    // ─── Tabla: perfil de usuario ─────────────────────────────────────────────
    public static final String TABLE_USER_PROFILE = "user_profile";
    public static final String COL_UID        = "uid";
    public static final String COL_NAME       = "name";
    public static final String COL_EMAIL      = "email";
    public static final String COL_WEIGHT     = "weight";
    public static final String COL_HEIGHT     = "height";
    public static final String COL_AGE        = "age";
    public static final String COL_GOAL_WEIGHT = "goal_weight";

    private static final String CREATE_TABLE_USER_PROFILE =
            "CREATE TABLE " + TABLE_USER_PROFILE + " (" +
            COL_UID         + " TEXT PRIMARY KEY, " +
            COL_NAME        + " TEXT, " +
            COL_EMAIL       + " TEXT, " +
            COL_WEIGHT      + " REAL, " +
            COL_HEIGHT      + " REAL, " +
            COL_AGE         + " INTEGER, " +
            COL_GOAL_WEIGHT + " REAL" +
            ");";

    // ─── Tabla: registro diario de comidas ────────────────────────────────────
    public static final String TABLE_FOOD_ENTRIES   = "food_entries";
    public static final String COL_FIREBASE_KEY     = "firebase_key";
    public static final String COL_ENTRY_UID        = "uid";
    public static final String COL_DATE             = "date";
    public static final String COL_ENTRY_NAME       = "name";
    public static final String COL_KCAL_PER_100G    = "kcal_per_100g";
    public static final String COL_GRAMS            = "grams";

    private static final String CREATE_TABLE_FOOD_ENTRIES =
            "CREATE TABLE " + TABLE_FOOD_ENTRIES + " (" +
            COL_FIREBASE_KEY  + " TEXT PRIMARY KEY, " +
            COL_ENTRY_UID     + " TEXT NOT NULL, " +
            COL_DATE          + " TEXT NOT NULL, " +
            COL_ENTRY_NAME    + " TEXT, " +
            COL_KCAL_PER_100G + " REAL, " +
            COL_GRAMS         + " REAL" +
            ");";

    // ─── Tabla: totales diarios de kcal ──────────────────────────────────────
    public static final String TABLE_DAILY_TOTALS  = "daily_totals";
    public static final String COL_TOTAL_UID       = "uid";
    public static final String COL_TOTAL_DATE      = "date";
    public static final String COL_TOTAL_KCAL      = "total_kcal";

    private static final String CREATE_TABLE_DAILY_TOTALS =
            "CREATE TABLE " + TABLE_DAILY_TOTALS + " (" +
            COL_TOTAL_UID  + " TEXT NOT NULL, " +
            COL_TOTAL_DATE + " TEXT NOT NULL, " +
            COL_TOTAL_KCAL + " REAL, " +
            "PRIMARY KEY (" + COL_TOTAL_UID + ", " + COL_TOTAL_DATE + ")" +
            ");";

    public LocalDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER_PROFILE);
        db.execSQL(CREATE_TABLE_FOOD_ENTRIES);
        db.execSQL(CREATE_TABLE_DAILY_TOTALS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_FOOD_ENTRIES);
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_TABLE_DAILY_TOTALS);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PERFIL DE USUARIO
    // ══════════════════════════════════════════════════════════════════════════

    public void saveUserProfile(String uid, UserProfile profile) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_UID,         uid);
        values.put(COL_NAME,        profile.getName());
        values.put(COL_EMAIL,       profile.getEmail());
        values.put(COL_WEIGHT,      profile.getWeight());
        values.put(COL_HEIGHT,      profile.getHeight());
        values.put(COL_AGE,         profile.getAge());
        values.put(COL_GOAL_WEIGHT, profile.getGoalWeight());

        db.insertWithOnConflict(TABLE_USER_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteUserProfile(String uid) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_USER_PROFILE, COL_UID + " = ?", new String[]{uid});
    }

    public UserProfile getUserProfile(String uid) {
        SQLiteDatabase db = getReadableDatabase();
        UserProfile profile = null;

        Cursor cursor = db.query(
                TABLE_USER_PROFILE,
                null,
                COL_UID + " = ?",
                new String[]{uid},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            profile = new UserProfile(
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HEIGHT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_AGE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_GOAL_WEIGHT))
            );
            cursor.close();
        }

        return profile;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTRO DIARIO DE COMIDAS
    // ══════════════════════════════════════════════════════════════════════════

    /** Inserta o actualiza una entrada de comida (INSERT OR REPLACE por firebase_key). */
    public void saveFoodEntry(String uid, String date, FoodEntry entry) {
        if (entry.getFirebaseKey() == null) return;
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_FIREBASE_KEY,  entry.getFirebaseKey());
        values.put(COL_ENTRY_UID,     uid);
        values.put(COL_DATE,          date);
        values.put(COL_ENTRY_NAME,    entry.getName());
        values.put(COL_KCAL_PER_100G, entry.getKcalPer100g());
        values.put(COL_GRAMS,         entry.getGrams());

        db.insertWithOnConflict(TABLE_FOOD_ENTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Actualiza solo los gramos de una entrada existente. */
    public void updateFoodEntryGrams(String firebaseKey, double grams) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_GRAMS, grams);

        db.update(TABLE_FOOD_ENTRIES, values,
                COL_FIREBASE_KEY + " = ?", new String[]{firebaseKey});
    }

    /** Elimina una entrada por su clave de Firebase. */
    public void deleteFoodEntry(String firebaseKey) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FOOD_ENTRIES,
                COL_FIREBASE_KEY + " = ?", new String[]{firebaseKey});
    }

    /** Elimina todas las entradas de un usuario para una fecha concreta (reset del día). */
    public void clearFoodEntriesForDate(String uid, String date) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FOOD_ENTRIES,
                COL_ENTRY_UID + " = ? AND " + COL_DATE + " = ?",
                new String[]{uid, date});
    }

    /** Devuelve todas las entradas de comida de un usuario para una fecha concreta. */
    public List<FoodEntry> getFoodEntriesForDate(String uid, String date) {
        SQLiteDatabase db = getReadableDatabase();
        List<FoodEntry> entries = new ArrayList<>();

        Cursor cursor = db.query(
                TABLE_FOOD_ENTRIES,
                null,
                COL_ENTRY_UID + " = ? AND " + COL_DATE + " = ?",
                new String[]{uid, date},
                null, null, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name  = cursor.getString(cursor.getColumnIndexOrThrow(COL_ENTRY_NAME));
                double kcal  = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_KCAL_PER_100G));
                double grams = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_GRAMS));
                String key   = cursor.getString(cursor.getColumnIndexOrThrow(COL_FIREBASE_KEY));

                FoodEntry entry = new FoodEntry(name, kcal, grams);
                entry.setFirebaseKey(key);
                entries.add(entry);
            }
            cursor.close();
        }

        return entries;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TOTALES DIARIOS DE KCAL
    // ══════════════════════════════════════════════════════════════════════════

    /** Inserta o actualiza el total de kcal de un día concreto. */
    public void saveDailyTotal(String uid, String date, double totalKcal) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_TOTAL_UID,  uid);
        values.put(COL_TOTAL_DATE, date);
        values.put(COL_TOTAL_KCAL, totalKcal);

        db.insertWithOnConflict(TABLE_DAILY_TOTALS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Devuelve el total de kcal guardado para un día. Retorna 0.0 si no existe. */
    public double getDailyTotal(String uid, String date) {
        SQLiteDatabase db = getReadableDatabase();
        double total = 0.0;

        Cursor cursor = db.query(
                TABLE_DAILY_TOTALS,
                new String[]{COL_TOTAL_KCAL},
                COL_TOTAL_UID + " = ? AND " + COL_TOTAL_DATE + " = ?",
                new String[]{uid, date},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TOTAL_KCAL));
            cursor.close();
        }

        return total;
    }

    /** Devuelve todos los totales diarios de un usuario, ordenados por fecha ascendente. */
    public Map<String, Double> getAllDailyTotals(String uid) {
        SQLiteDatabase db = getReadableDatabase();
        Map<String, Double> totals = new LinkedHashMap<>();

        Cursor cursor = db.query(
                TABLE_DAILY_TOTALS,
                new String[]{COL_TOTAL_DATE, COL_TOTAL_KCAL},
                COL_TOTAL_UID + " = ?",
                new String[]{uid},
                null, null,
                COL_TOTAL_DATE + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                totals.put(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TOTAL_DATE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TOTAL_KCAL))
                );
            }
            cursor.close();
        }

        return totals;
    }
}