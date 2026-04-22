package robertocasaban.example.tfg_roberto_casaban.fasting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import robertocasaban.example.tfg_roberto_casaban.models.FastingOption;

/**
 * Gestor del ayuno intermitente. Almacena el estado en SharedPreferences
 * y programa alarmas para notificar el cambio de fase.
 *
 * Modelo mental:
 *   - El ciclo dura (fastHours + eatHours) horas y se repite indefinidamente.
 *   - startMs marca el inicio del ciclo actual.
 *   - startingPhase indica si el ciclo empieza en AYUNO o COMIDA.
 *
 * Solo se programa UNA alarma por ciclo (la próxima transición). Al dispararse,
 * el BroadcastReceiver re-programa la siguiente.
 */
public final class FastingManager {

    private static final String PREFS = "fasting_prefs";

    private static final String KEY_ACTIVE         = "active";
    private static final String KEY_PROTOCOL       = "protocol";        // "16:8"
    private static final String KEY_FAST_HOURS     = "fast_hours";
    private static final String KEY_EAT_HOURS      = "eat_hours";
    private static final String KEY_START_MS       = "start_ms";
    private static final String KEY_STARTING_PHASE = "starting_phase";  // "FAST" o "EAT"

    public static final int ALARM_REQUEST_CODE = 7420;

    public enum Phase { FASTING, EATING, INACTIVE }

    public static final class State {
        public final boolean active;
        public final Phase   phase;
        public final long    remainingMs;   // tiempo restante hasta la siguiente transición
        public final long    totalPhaseMs;  // duración total de la fase actual
        public final String  protocolName;
        public final int     fastHours;
        public final int     eatHours;

        State(boolean active, Phase phase, long remainingMs, long totalPhaseMs,
              String protocolName, int fastHours, int eatHours) {
            this.active        = active;
            this.phase         = phase;
            this.remainingMs   = remainingMs;
            this.totalPhaseMs  = totalPhaseMs;
            this.protocolName  = protocolName;
            this.fastHours     = fastHours;
            this.eatHours      = eatHours;
        }

        public static State inactive() {
            return new State(false, Phase.INACTIVE, 0, 0, null, 0, 0);
        }
    }

    private FastingManager() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ─── Iniciar / parar ─────────────────────────────────────────────────────────

    public static void start(Context ctx, FastingOption option, Phase startingPhase) {
        long now = System.currentTimeMillis();
        prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putString(KEY_PROTOCOL, option.name)
                .putInt(KEY_FAST_HOURS, option.fastHours)
                .putInt(KEY_EAT_HOURS, option.eatHours)
                .putLong(KEY_START_MS, now)
                .putString(KEY_STARTING_PHASE, startingPhase.name())
                .apply();
        scheduleNextAlarm(ctx);
    }

    public static void stop(Context ctx) {
        cancelAlarm(ctx);
        prefs(ctx).edit().clear().apply();
    }

    // ─── Estado actual ───────────────────────────────────────────────────────────

    public static State getState(Context ctx) {
        SharedPreferences sp = prefs(ctx);
        if (!sp.getBoolean(KEY_ACTIVE, false)) return State.inactive();

        int    fastH    = sp.getInt(KEY_FAST_HOURS, 16);
        int    eatH     = sp.getInt(KEY_EAT_HOURS, 8);
        long   startMs  = sp.getLong(KEY_START_MS, System.currentTimeMillis());
        String protocol = sp.getString(KEY_PROTOCOL, "16:8");
        Phase  startPh  = Phase.valueOf(sp.getString(KEY_STARTING_PHASE, Phase.FASTING.name()));

        long fastMs  = fastH * 3600_000L;
        long eatMs   = eatH  * 3600_000L;
        long cycleMs = fastMs + eatMs;

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed < 0) elapsed = 0;
        long inCycle = elapsed % cycleMs;

        Phase phase;
        long  remaining;
        long  total;

        if (startPh == Phase.FASTING) {
            if (inCycle < fastMs) {
                phase     = Phase.FASTING;
                remaining = fastMs - inCycle;
                total     = fastMs;
            } else {
                phase     = Phase.EATING;
                remaining = cycleMs - inCycle;
                total     = eatMs;
            }
        } else {
            if (inCycle < eatMs) {
                phase     = Phase.EATING;
                remaining = eatMs - inCycle;
                total     = eatMs;
            } else {
                phase     = Phase.FASTING;
                remaining = cycleMs - inCycle;
                total     = fastMs;
            }
        }

        return new State(true, phase, remaining, total, protocol, fastH, eatH);
    }

    // ─── Alarmas ─────────────────────────────────────────────────────────────────

    public static void scheduleNextAlarm(Context ctx) {
        State s = getState(ctx);
        if (!s.active) return;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, FastingAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx.getApplicationContext(),
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | pendingIntentMutabilityFlag());

        long triggerAt = System.currentTimeMillis() + s.remainingMs;
        // setAndAllowWhileIdle no requiere permiso SCHEDULE_EXACT_ALARM y
        // basta con precisión de minutos para un cambio de fase en horas.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
    }

    public static void cancelAlarm(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(ctx, FastingAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx.getApplicationContext(),
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | pendingIntentMutabilityFlag());
        am.cancel(pi);
    }

    private static int pendingIntentMutabilityFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
    }
}
