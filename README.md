<div align="center">

# Wallance

### Tu dinero. Tu equilibrio.

Aplicación Android de finanzas personales para gestionar ingresos, gastos,
presupuestos, ahorros, préstamos con intereses y múltiples monedas, con
estadísticas claras y una interfaz oscura pensada para el ahorro de batería.

<br/>

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.6.1-1976D2?style=for-the-badge&logo=sqlite&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-2.51.1-2196F3?style=for-the-badge&logo=dagger&logoColor=white)

![Estado](https://img.shields.io/badge/estado-estable-0ECB81?style=for-the-badge)
![Versión](https://img.shields.io/badge/versión-1.0.0-FCD535?style=for-the-badge)
![Idioma](https://img.shields.io/badge/idioma-español-E4405F?style=for-the-badge)

</div>

---

## 📖 Acerca de Wallance

**Wallance** es una aplicación de finanzas personales **nativa para Android**,
escrita íntegramente en **Kotlin** y **Jetpack Compose**. Reúne en un solo lugar
el control de tus cuentas, tus movimientos diarios, tus presupuestos y metas de
ahorro, y —lo que la hace especial— un **módulo completo de préstamos personales**
capaz de calcular cronogramas de pago, intereses y mora con precisión financiera.

Todo el dinero se maneja con `BigDecimal` y redondeo `HALF_UP` a dos decimales,
de modo que los saldos, las cuotas y los intereses cuadran al centavo. La
interfaz es **100 % en español**, con un tema oscuro estilo **AMOLED** (negro
puro) y acentos en amarillo.

## ✨ Características

- **Cuentas** — efectivo, bancarias, billeteras digitales, tarjetas de crédito,
  inversiones y más, con saldo inicial y moneda por cuenta.
- **Movimientos** — ingresos, gastos, transferencias y ajustes, con categorías,
  etiquetas, notas, contacto opcional y búsqueda.
- **Presupuestos** — límites por categoría con periodicidad semanal, mensual o
  anual, y seguimiento del avance.
- **Metas de ahorro** — objetivos con monto definido, fecha y progreso.
- **Suscripciones** — pagos recurrentes con estado (activa, pausada, cancelada).
- **Préstamos y contactos** — la joya de la corona:
  - Varios préstamos por contacto, en ambas direcciones (otorgados y recibidos).
  - Interés **simple**, **compuesto**, **porcentaje total**, **plano** o
    **sin interés**, con tasa **nominal o efectiva** y periodicidad configurable.
  - Sistemas de amortización **francés**, **alemán**, **intereses periódicos**,
    **pago único** y **cronograma personalizado**.
  - **Abonos parciales**, pagos por cuota, extraordinarios y liquidación.
  - **Mora** opcional, aplicada solo cuando el usuario la habilita.
  - Estados del préstamo: activo, pagado, vencido, cancelado y reestructurado.
- **Divisas** — 12 monedas con cotizaciones reales en línea y caché para uso
  sin conexión.
- **Estadísticas** — balance por periodo (mes, trimestre, semestre, año) y
  gráficos de barras y de anillo dibujados con **Canvas** propio.
- **Privacidad y seguridad** — bloqueo de la app y opción de ocultar montos.
- **Tema oscuro AMOLED** de alto contraste, cómodo y eficiente.

## 🧠 Precisión financiera

El módulo de préstamos vive en `nucleo/finanzas/MotorPrestamos.kt` y es
completamente independiente de la interfaz. Sus reglas principales:

- Los montos son `BigDecimal`; nunca se usan tipos de punto flotante.
- El redondeo es `HALF_UP` a 2 decimales.
- La **última cuota absorbe los redondeos** para que la suma cuadre exactamente.
- Un pago nunca genera doble movimiento contable.

## 🛠️ Tecnologías

| Área | Herramienta |
|---|---|
| Lenguaje | Kotlin 2.0.21 |
| Interfaz | Jetpack Compose (BOM 2024.12.01) + Material 3 |
| Arquitectura | MVVM + Clean Architecture por capas |
| Persistencia | Room 2.6.1 (dinero como `TEXT` decimal) |
| Inyección | Hilt (Dagger) 2.51.1 |
| Procesamiento | KSP 2.0.21-1.0.28 |
| Red | OkHttp + Kotlinx Serialization |
| Concurrencia | Corrutinas y `Flow` |
| Gráficos | Canvas de Compose (sin librerías externas) |
| Build | Gradle 8.11.1 · AGP 8.7.3 |
| SDK | `minSdk 26`, `targetSdk 35`, `compileSdk 35` |

## 🏗️ Estructura del proyecto

```
app/src/main/java/com/andrespar2/wallance/
├── actividad/            # Actividad principal y contenedor de Compose
├── funcionalidades/      # Una carpeta por pantalla/módulo (MVVM)
│   ├── inicio/  movimientos/  prestamos/  estadisticas/  configuracion/
│   └── cuentas/  categorias/  presupuestos/  ahorros/  divisas/  suscripciones/
├── nucleo/
│   ├── basedatos/        # Room: entidades, DAOs, repositorios
│   ├── comun/            # Utilidades, formatos, periodos, etiquetas
│   ├── diseno/           # Tema, colores AMOLED y componentes reutilizables
│   ├── finanzas/         # MotorPrestamos y MotorDivisas (lógica pura)
│   ├── navegacion/       # Rutas y grafo de navegación
│   ├── preferencias/     # Ajustes del usuario (DataStore)
│   ├── publicidad/       # Integración de anuncios
│   ├── red/              # Cliente de cotizaciones de divisas
│   └── seguridad/        # Bloqueo y protección de la app
└── sesion/               # Estado global de la sesión y catálogo inicial
```

Todos los identificadores, paquetes y textos del proyecto están **en español**.

## 🚀 Compilación

Requisitos:

- **JDK 17**
- **Android SDK** con `compileSdk 35`

```bash
# Clonar
git clone https://github.com/andrespar2/Wallance.git
cd Wallance

# Compilar la versión de depuración
./gradlew :app:assembleDebug

# Compilar la versión de producción (requiere firma, ver abajo)
./gradlew :app:assembleRelease
```

La APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

### 🔐 Firma de la versión de producción

La clave de firma **nunca** se versiona. Crea un archivo `firma.properties` en la
raíz del proyecto con:

```properties
almacen=ruta/a/tu/clave.jks
claveAlmacen=tu_contraseña
alias=tu_alias
claveLlave=tu_contraseña
```

Si el archivo no existe, el proyecto compila igualmente en modo depuración.

## 📥 Descarga

Las versiones publicadas están disponibles en la página de
[**Releases**](https://github.com/andrespar2/Wallance/releases).

## 🗺️ Estado del proyecto

| Etapa | Descripción | Estado |
|:---:|---|:---:|
| 1 | Núcleo de datos, motor financiero y UI base | ✅ |
| 2 | Movimientos, cuentas, presupuestos, ahorros y configuración | ✅ |
| 3 | Préstamos, contactos, cuotas, pagos e intereses | ✅ |
| 4 | Estadísticas y gráficos con Canvas | ✅ |
| 5 | Documentación, build de producción y publicación | ✅ |

## 👤 Autor

**andrespar2**

- GitHub: [@andrespar2](https://github.com/andrespar2)

## 📄 Licencia

Proyecto personal de **andrespar2** — © 2026. Todos los derechos reservados.

<div align="center">

Hecho con 🖤 en Colombia.

**Wallance — Tu dinero. Tu equilibrio.**

</div>
