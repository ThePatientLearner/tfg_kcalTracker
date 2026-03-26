package robertocasaban.example.tfg_roberto_casaban.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import robertocasaban.example.tfg_roberto_casaban.models.UserProfile;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "tfg_local.db";
    private static final int    DB_VERSION = 1;

    // Tabla de perfil de usuario
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
            COL_UID        + " TEXT PRIMARY KEY, " +
            COL_NAME       + " TEXT, " +
            COL_EMAIL      + " TEXT, " +
            COL_WEIGHT     + " REAL, " +
            COL_HEIGHT     + " REAL, " +
            COL_AGE        + " INTEGER, " +
            COL_GOAL_WEIGHT + " REAL" +
            ");";

    public LocalDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER_PROFILE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_PROFILE);
        onCreate(db);
    }

    // Inserta o actualiza el perfil del usuario (INSERT OR REPLACE)
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

        // CONFLICT_REPLACE: si el uid ya existe, actualiza el registro
        db.insertWithOnConflict(TABLE_USER_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Recupera el perfil del usuario por su uid
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

        db.close();
        return profile;
    }
}
