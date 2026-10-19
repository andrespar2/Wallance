# Changelog

Todos los cambios importantes de **Wallance** se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y el proyecto se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

## [1.0.0] — 2026-10-18

Primera versión estable de Wallance. 🎉

### Añadido

- **Cuentas** de efectivo, bancarias, billeteras digitales, tarjetas de crédito,
  inversiones y otras, con saldo inicial y moneda por cuenta.
- **Movimientos** de ingreso, gasto, transferencia y ajuste, con categoría,
  etiquetas, notas, contacto opcional y búsqueda.
- **Categorías** con catálogo inicial en español, listo desde el primer arranque.
- **Presupuestos** por categoría con periodicidad semanal, mensual o anual.
- **Metas de ahorro** con monto objetivo, fecha y seguimiento del progreso.
- **Suscripciones** recurrentes con estados activa, pausada y cancelada.
- **Préstamos y contactos**:
  - Contactos locales con tipo de relación (prestamista, tomador o ambos) y
    valores sugeridos de interés y mora.
  - Varios préstamos por contacto, otorgados y recibidos.
  - Interés simple, compuesto, porcentaje total, plano o sin interés, con tasa
    nominal o efectiva y periodicidad configurable.
  - Sistemas de amortización francés, alemán, intereses periódicos, pago único
    y cronograma personalizado.
  - Registro de abonos parciales, pagos por cuota, extraordinarios y
    liquidación, sin duplicar movimientos.
  - Mora opcional, aplicada únicamente cuando el usuario la habilita.
  - Estados del préstamo: activo, pagado, vencido, cancelado y reestructurado.
  - Vista previa del cronograma antes de guardar.
- **Divisas** con 12 monedas y cotizaciones reales en línea, con caché para
  consulta sin conexión.
- **Estadísticas** con balance por periodo (mes, trimestre, semestre y año) y
  gráficos de barras y de anillo dibujados con Canvas.
- **Configuración** con gestión de cuentas, categorías, presupuestos, metas de
  ahorro, divisas y suscripciones, además de preferencias de privacidad.
- **Seguridad**: bloqueo de la aplicación y opción de ocultar montos.
- Tema oscuro **AMOLED** de alto contraste con acentos en amarillo.
- Identidad visual de Wallance con logotipo propio en forma de **W**.

### Notas técnicas

- Precisión monetaria con `BigDecimal` y redondeo `HALF_UP` a 2 decimales; la
  última cuota absorbe los redondeos.
- Persistencia con Room (el dinero se guarda como texto decimal).
- Inyección de dependencias con Hilt y procesamiento de anotaciones con KSP.
- Interfaz 100 % en español, construida con Jetpack Compose y Material 3.

[1.0.0]: https://github.com/andrespar2/Wallance/releases/tag/v1.0.0
