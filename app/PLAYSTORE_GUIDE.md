# Guía para Subir Kcal Tracker a Google Play Store

## Archivos Creados

| Archivo | Ruta | Descripción |
|---------|------|-------------|
| Política de Privacidad | `PRIVACY_POLICY.md` | Plantilla de política de privacidad |
| Banner promocional | `app/src/main/res/drawable/banner_promocional.xml` | Banner vectorial 1024x500 |
| Descripción Play Store | `PLAYSTORE_DESCRIPCION.md` | Texto para la listing |
| Nombre de app | `app/src/main/res/values/strings.xml` | Actualizado a "Kcal Tracker" |

---

## 1. Construir el APK/AAB

### Paso 1: Abrir terminal en el proyecto
```bash
cd /Users/robertocasabancliment/Desktop/TFG/TFG_Roberto_Casaban
```

### Paso 2: Build del APK debug
```bash
./gradlew assembleDebug
```
El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

### Paso 3: Build del APK release (para Play Store)
```bash
./gradlew assembleRelease
```

**Nota:** La primera vez que ejecutes Gradle puede tardar varios minutos.

---

## 2. Capturar Screenshots

Necesitas 2-8 capturas de pantalla de la app. Formato requerido: **PNG o JPG, 16:9 o menor**.

### Opción A: Desde un dispositivo real

1. Navega a la pantalla que quieres capturar
2. **Samsung**: Presiona `Volumen abajo + Encendido` simultáneamente
3. **Pixel**: Presiona `Volumen abajo + Encendido` simultáneamente
4. Las capturas se guardan en `Fotos > Álbum de dispositivos > Screenshots`

### Opción B: Desde el Emulador de Android Studio

1. Abre Android Studio
2. Abre el proyecto `TFG_Roberto_Casaban`
3. Ejecuta la app en un emulador
4. Usa la barra de herramientas del emulador: `Camera` icon o `Screen Record`
5. También puedes usar `File > Save Debug...` desde el panel del emulador

### Opción C: Usando ADB

```bash
adb exec-out screencap -p > screenshot.png
```

### Pantallas que deberías capturar

1. **Login/Registro** - LoginActivity
2. **Pantalla principal** - MainActivity (registro de alimentos)
3. **Estadísticas** - EstadisticasActivity (gráficos)
4. **Perfil** - PerfilActivity

---

## 3. Configurar en Google Play Console

### Paso 1: Crear nueva app
1. Ve a [Google Play Console](https://play.google.com/console)
2. Click en **"Crear app"**
3. Selecciona:
   - **Idioma**: Español
   - **Nombre de app**: Kcal Tracker
   - **Tipo de app**: Aplicación

### Paso 2: Completar información de la app

#### Ficha de Play Store
- **Título**: Kcal Tracker - Seguimiento Nutricional
- **Descripción corta**: Controla tus calorías y macronutrientes diaria y fácilmente.
- **Descripción completa**: Usa el texto de `PLAYSTORE_DESCRIPCION.md`

#### assets gráficos
- **Icono de alta resolución**: 512x512 PNG (genera desde el launcher existente)
- **Capturas de pantalla**: Mínimo 2 (teléfono), máximo 8
- **Banner promocional**: 1024x500 (usa `banner_promocional.xml` exportado)

#### Categoría y contacto
- **Categoría**: Salud y fitness
- **Etiquetas**: nutrición, dieta, calorías, fitness
- **Correo electrónico**: Tu email de contacto
- **Web**: Opcional (aquí puedes alojar la política de privacidad)

### Paso 3: Clasificación de contenido
- **Orientada a todos los públicos**: Sí
- **Preguntas adicionais**: Responde según funcionalidad de la app

### Paso 4: Precio y distribución
- **Precio**: Gratis o de pago según decisión
- **Países**: Selecciona los países donde estará disponible

### Paso 5: Política de privacidad
- **URL de la política de privacidad**: Debes hostear `PRIVACY_POLICY.md` en una URL pública
- Opciones para hostear:
  - GitHub Pages (gratis)
  - Tu propia web
  - Google Drive (con enlace público)

---

## 4. Subir el AAB

### Generar AAB (Android App Bundle)
```bash
./gradlew bundleRelease
```
El archivo estará en: `app/build/outputs/bundle/release/app-release.aab`

### Subir a Play Console
1. En Play Console, ve a **"Producción"**
2. Click en **"Crear release"**
3. Sube el archivo `.aab`
4. Si es la primera vez, necesitas **crear una clave de firma**
5. Google puede pedir que subas tu keystore existente o que_generes uno nuevo

---

## 5. Keystore (IMPORTANTE)

Si esta es la primera vez que publicas, necesitarás crear un keystore:

```bash
keytool -genkey -v -keystore ~/Documents/keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kcal-tracker
```

**GUARDA ESTE ARCHIVO EN UN LUGAR SEGURO** - Lo necesitarás para:
- Firmar actualizaciones futuras
- Si lo pierdes, NO podrás actualizar la app

Si ya tienes cuenta de Play Store y ya publicaste otra app, busca el keystore existente que usaste antes.

---

## Checklist Final

- [ ] APK/AAB construido
- [ ] 2-8 screenshots capturados
- [ ] Icono 512x512
- [ ] Banner promocional 1024x500
- [ ] Política de privacidad en URL pública
- [ ] Descripción completa escrita
- [ ] Clasificación de contenido completada
- [ ] Keystore guardado en lugar seguro
- [ ] Cuenta de desarrollador verificada ($25 pagados)

---

## Problemas Comunes

### Error: "La app no está firmada"
Necesitas firmar con tu keystore. Agrega esto en `app/build.gradle`:

```groovy
signingConfigs {
    release {
        storeFile file("path/to/keystore.jks")
        storePassword "tu_password"
        keyAlias "tu_alias"
        keyPassword "tu_password"
    }
}
buildTypes {
    release {
        signingConfig signingConfigs.release
    }
}
```

### Error: "Dispositivo no compatible"
Verifica que `minSdk` sea 30 o menor, o que el dispositivo/emulador cumple los requisitos.

---

*Documento creado: Mayo 2026*