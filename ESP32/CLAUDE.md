# Instrucciones para Claude Code en este taller

## Contexto sobre el usuario

Está **aprendiendo** electrónica e IoT. No asumas conocimiento previo de
hardware, protocolos ni conceptos de embebidos.

Esto cambia cómo hay que trabajar aquí:

- **Explica el porqué, no solo el qué.** "Conecta SDA al GPIO21" no sirve solo;
  hay que decir qué es SDA y por qué ese pin.
- **Usa analogías y ejemplos concretos.** Los conceptos abstractos de electrónica
  (pull-up, nivel lógico, corriente de pico) se entienden mucho mejor con una
  comparación que con una definición.
- **Advierte antes de que rompa algo.** Cada vez que haya riesgo de dañar un
  componente —voltaje equivocado, pin incorrecto, polaridad invertida— dilo con
  un ⚠️ **antes** de la instrucción, no después.
- **Traduce la jerga la primera vez** que aparezca en cada documento.
- **Un paso a la vez cuando sea hardware.** Conectar cinco cosas y probar deja
  cinco sospechosos si falla. De una en una, siempre se sabe quién falló.
- **Todo en español**, incluidos los comentarios del código.

## Antes de instalar algo

El usuario dio autorización para instalar por CLI, con una condición: **decirle
qué se instaló y por qué.** Siempre.

---

## Metodología obligatoria

Todo proyecto sigue las 6 fases de [`metodologia/README.md`](metodologia/README.md),
y las carpetas numeradas de cada proyecto las reflejan.

### ADRs

- Van en `<proyecto>/1-decisiones/` con el formato
  [`ADR-plantilla.md`](metodologia/plantillas/ADR-plantilla.md).
- **Nunca se borran ni se reescriben.** Si una decisión cambia: se escribe un ADR
  nuevo, se marca el viejo como `Reemplazado por ADR-XXXX`, y el viejo se queda
  íntegro con su razonamiento original. Eso es lo que permite el backtrack.
- Se escribe un ADR cuando la decisión sería molesta de revertir: elegir un
  componente, una librería, un protocolo, una API, o el esquema de alimentación.
  También cuando se decide **no** hacer algo que parecía obvio.
- Llenar siempre la sección **"Qué sabíamos y qué no"**. Es lo que después
  permite distinguir "me equivoqué al decidir" de "decidí bien y el mundo cambió".
- Actualizar el índice en `1-decisiones/README.md`.

### Inventario

- Es **uno solo y general**: [`inventario/INVENTARIO.md`](inventario/INVENTARIO.md).
  Ningún proyecto tiene el suyo.
- Todo cambio de estado se registra **primero** en
  [`inventario/LOG.md`](inventario/LOG.md) con fecha absoluta, y después se
  actualiza la tabla del inventario. Los dos deben quedar consistentes.
- **Regla dura sobre lo dañado:** un componente en estado `Dañado` **no existe**
  para efectos de planeación. No proponerlo, no contarlo, no incluirlo en ningún
  diseño.
  - Solo pasa a `A reparar` o `A reponer` si **el usuario lo declara** en el LOG.
  - **Nunca cambiar ese estado por iniciativa propia.**
  - `Dañado` no se borra nunca: el histórico de "ya quemé tres de estos" es
    información valiosa para alguien que está aprendiendo.

### Bitácora

Cada proyecto tiene `BITACORA.md`. Al terminar una sesión de trabajo, agregar
entrada con fecha: qué se hizo, qué falló, en qué se quedó.

Diferencia con los ADRs: el ADR captura **una decisión**; la bitácora captura **el
trayecto** — los intentos fallidos, las sospechas sin confirmar, el cable que
resultó estar malo.

---

## Reglas técnicas del taller

- **Diagramas en Mermaid**, nunca imágenes. Al ser texto viven junto al código,
  se versionan, y se puede ver exactamente qué cambió.
- **Fechas absolutas** siempre (`2026-08-29`), nunca relativas.
- **Versiones clavadas** en `platformio.ini` (`@6.21.3`, no "la última"). Es el
  requerimiento RNF-04: recompilar en un año y que funcione igual.
- **Cero secretos en el repositorio.** Credenciales de WiFi y tokens van en
  archivos ignorados por Git. Revisar antes de cada commit.
- **Un entorno virtual por proyecto** en `<proyecto>/2-entorno/.venv/`.

## Avisos de seguridad eléctrica que hay que repetir cuando apliquen

No asumir que ya se dijeron. Repetirlos en contexto:

- ⚠️ **Los pines del ESP32 son 3.3 V y no toleran 5 V.** Es lo que más placas mata.
- ⚠️ **El ESP32 solo se conecta a WiFi de 2.4 GHz**, no a 5 GHz. Clásico de "no
  conecta y no sé por qué", porque muchos routers usan el mismo nombre para ambas.
- ⚠️ **El pico de consumo del ESP32 llega a ~500 mA** al transmitir. Si la fuente
  no lo aguanta, la placa se reinicia sola y parece un bug de software.
- ⚠️ **Desconectar el USB antes de recablear.**

---

## ⚠️ Dos placas idénticas, un solo nombre de puerto

Desde el 2026-11-25 hay dos proyectos con placa propia:

| MAC | Placa | Proyecto |
|---|---|---|
| `84:1F:E8:2C:3E:C4` | C-001 | **tamagotchi — el regalo, no tocar** |
| `E0:8C:FE:30:C9:1C` | C-011 | nfc-pn532 |

Las dos salen como **`/dev/cu.usbserial-0001`** (sus CP2102 llevan el mismo
número de serie). **El puerto no dice cuál está enchufada; la MAC sí.** Antes de
cargar o borrar firmware: `esptool --port /dev/cu.usbserial-0001 read-mac`.
`/abrir-sesion` todavía asume que la placa conectada es la del Tamagotchi.

---

## Skills de este taller

| Skill | Cuándo usarla |
|---|---|
| `/abrir-sesion` | **Al empezar.** Le pregunta a la placa en qué estado está, lo contrasta con lo que dicen los documentos y resume dónde se quedó todo |
| `/cerrar-sesion` | **Antes de limpiar la conversación o dejar el proyecto.** Deja el contexto completo: estado físico, bitácora, hallazgos medidos y verificaciones pendientes |

⚠️ **No uses una skill genérica de handoff aquí.** En este taller **la mitad del
estado no está en Git**: está en una placa enchufada, con un firmware concreto
dentro, unos cables puestos de una manera, y cosas que solo se comprueban
mirando la pantalla. `/cerrar-sesion` captura eso; las genéricas no.

---

## Estado actual

🟢 **El Tamagotchi está terminado: los 16 requerimientos, resueltos.** Muestra
canción, artista, álbum, playlist, progreso y **letras sincronizadas**; el
**joystick** (C-005) pausa, salta, recorre **cuatro pantallas** y elige playlist.
Arranca con **Snoopy bailando** y esconde una **quinta pantalla secreta**.

**Las cuatro pantallas** (arriba/abajo, en bucle, con fundido): sin letra · con
letra · portada del álbum · elegir playlist.

**La quinta, oculta:** se abre con **`↑ ↑ ↓ ↓`** y se cierra con un clic. Muestra
el estado interno y se despide con un `"Te quiero!!"`. RF-10 (volumen) quedó
**descartado** — el joystick no tiene eje libre ([ADR-0007](proyectos/tamagotchi/1-decisiones/ADR-0007-joystick-como-control.md)).

🔋 **Lo único abierto es la batería.** Está diseñada y documentada, pero **falta
comprar el elevador MT3608** (C-009). Hasta entonces vive enchufado por USB.
⚠️ La batería **no se puede conectar directamente**: el `AMS1117` de la placa
necesita 4,4 V y la LiPo llega a 4,2 V como mucho. Ver
[ADR-0014](proyectos/tamagotchi/1-decisiones/ADR-0014-alimentacion-por-bateria.md).

⚠️ **Quedan verificaciones visuales pendientes de la sesión 2**, listadas en el
bloque `🔜 En qué me quedé` de la bitácora.

### 🎁 Va a ser un regalo — cambia las prioridades

Decidido a mitad del proyecto. El aparato va a estar en manos de alguien que no
puede depurarlo ni reiniciarlo a conciencia:

- **La estabilidad pesa más que las funciones nuevas.** Antes de añadir algo,
  medir que lo que hay sigue estable.
- **Todo fallo debe degradar, no romper.** Sin letra se sigue viendo la canción,
  sin red se avisa en rojo, si baja la memoria el hilo de letras se aparta solo.
  Y desde la sesión 3, **un cuelgue ya no es mudo**: el chivato del turno dice
  quién espera, en qué núcleo y en qué línea.
- **Los detalles visuales no son adornos:** son lo único que verá quien lo reciba.

- Placa: **ESP32-D0WD-V3**, MAC `84:1F:E8:2C:3E:C4`, puerto `/dev/cu.usbserial-0001`
- Pantalla: **GMT130** (ST7789, 240×240) cableada según
  [`6-esquemas/conexiones-fisicas.md`](proyectos/tamagotchi/6-esquemas/conexiones-fisicas.md)
- Los **9 supuestos** están resueltos y hay **14 ADRs** decididos

**Arquitectura del firmware:** tres hilos —Spotify (rápido), letras (lento) y
dibujo—, con un mutex de red que garantiza **una sola conexión cifrada a la vez**.
Ver [`6-esquemas/mapa-de-memoria.md`](proyectos/tamagotchi/6-esquemas/mapa-de-memoria.md).

🔑 **La regla que ordena todo el firmware:** el núcleo 0 habla con la red, el
núcleo 1 dibuja, y **nunca al revés**. El joystick se lee en el núcleo 1 y deja
una nota en una cola; el núcleo 0 la recoge. Cada resultado que tarda en llegar
lleva una marca de a quién pertenece — sin ella, lo lento acaba dibujado en el
sitio equivocado.

⚠️ **Y hay un corolario que costó un cuelgue entero:** una función que pide el
turno de datos **no se puede llamar desde dentro de ese turno**. El mutex no es
reentrante y el núcleo se queda esperándose a sí mismo, en silencio y para
siempre. Lo difícil es que la petición anidada puede estar **tres llamadas más
abajo**, invisible a ojo. Ver
[`5-investigacion/cuelgue-por-turno-anidado.md`](proyectos/tamagotchi/5-investigacion/cuelgue-por-turno-anidado.md).

Antes de dar por hecho cualquier estado, compruébalo:

```bash
ls /dev/cu.usbserial*                    # ¿está la placa?
lsof /dev/cu.usbserial-0001              # ¿alguien ocupa el puerto?
git log --oneline -5                     # ¿qué se hizo al final?
```

Y lee la última entrada de
[`proyectos/tamagotchi/BITACORA.md`](proyectos/tamagotchi/BITACORA.md): ahí está
en qué se quedó y qué falta por verificar mirando.
