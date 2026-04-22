package robertocasaban.example.tfg_roberto_casaban.models;

import java.util.Arrays;
import java.util.List;

/** Protocolo de ayuno intermitente (horas de ayuno / horas de ventana de comer). */
public class FastingOption {

    public final String name;        // "16:8"
    public final String subtitle;    // "Estándar"
    public final String description; // Explicación corta
    public final int    fastHours;
    public final int    eatHours;

    public FastingOption(String name, String subtitle, String description,
                         int fastHours, int eatHours) {
        this.name        = name;
        this.subtitle    = subtitle;
        this.description = description;
        this.fastHours   = fastHours;
        this.eatHours    = eatHours;
    }

    /** Lista fija de protocolos comunes, ordenados de más suave a más exigente. */
    public static List<FastingOption> defaults() {
        return Arrays.asList(
                new FastingOption("14:10", "Inicio suave",
                        "14 h de ayuno, 10 h para comer. Ideal si empiezas.",
                        14, 10),
                new FastingOption("16:8", "Estándar",
                        "16 h de ayuno, 8 h para comer. El protocolo más popular.",
                        16, 8),
                new FastingOption("18:6", "Intermedio",
                        "18 h de ayuno, 6 h para comer. Pequeño extra de disciplina.",
                        18, 6),
                new FastingOption("20:4", "Warrior",
                        "20 h de ayuno, 4 h para comer. Exigente, para avanzados.",
                        20, 4),
                new FastingOption("23:1", "OMAD",
                        "Una comida al día. Muy restrictivo, requiere planificación.",
                        23, 1)
        );
    }

    /** Recrea la opción a partir de su nombre (ej. al recuperar de SharedPreferences). */
    public static FastingOption fromName(String name) {
        if (name == null) return null;
        for (FastingOption o : defaults()) {
            if (o.name.equals(name)) return o;
        }
        return null;
    }
}
