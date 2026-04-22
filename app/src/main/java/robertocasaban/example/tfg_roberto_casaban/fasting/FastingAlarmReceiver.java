package robertocasaban.example.tfg_roberto_casaban.fasting;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import robertocasaban.example.tfg_roberto_casaban.MainActivity;
import robertocasaban.example.tfg_roberto_casaban.R;

/**
 * Dispara una notificación cuando la ventana de ayuno/comida cambia de fase,
 * y reprograma la alarma para la próxima transición.
 */
public class FastingAlarmReceiver extends BroadcastReceiver {

    public  static final String CHANNEL_ID   = "fasting_channel";
    private static final int    NOTIFICATION_ID = 4201;

    @Override
    public void onReceive(Context context, Intent intent) {
        FastingManager.State s = FastingManager.getState(context);
        if (!s.active) return;

        ensureChannel(context);
        showPhaseNotification(context, s);

        // Al entrar en la nueva fase, la siguiente alarma es al terminar esta.
        FastingManager.scheduleNextAlarm(context);
    }

    private void showPhaseNotification(Context context, FastingManager.State s) {
        String title;
        String message;
        int    iconRes;

        if (s.phase == FastingManager.Phase.FASTING) {
            title   = "Empieza tu ayuno";
            message = "Ventana de comida cerrada · " + s.protocolName
                    + " · Vuelve a comer en " + formatHours(s.remainingMs);
            iconRes = R.drawable.ic_moon;
        } else {
            title   = "¡Puedes comer!";
            message = "Ventana abierta · " + s.protocolName
                    + " · Tienes " + formatHours(s.remainingMs) + " para tus comidas";
            iconRes = R.drawable.ic_meal;
        }

        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentPi = PendingIntent.getActivity(context, 0, open, piFlags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPi);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, b.build());
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "Ayuno intermitente",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Avisos de inicio/fin de ventana de comida y ayuno");
        nm.createNotificationChannel(ch);
    }

    private static String formatHours(long ms) {
        long totalMin = Math.max(0, ms / 60_000);
        long h = totalMin / 60;
        long m = totalMin % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + " h";
        return h + " h " + m + " min";
    }
}
