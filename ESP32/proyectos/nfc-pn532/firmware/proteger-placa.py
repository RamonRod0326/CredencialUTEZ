# Antes de cargar firmware, comprueba que la placa enchufada es la de este
# proyecto (C-011) y no la del Tamagotchi (C-001).
#
# Por qué existe: las dos placas salen en la Mac con el MISMO puerto
# (/dev/cu.usbserial-0001), así que el puerto no dice cuál es. La MAC sí:
# es el número de serie del chip. Si no es la esperada, la carga se cancela
# antes de escribir un solo byte.
import subprocess
Import("env")

MAC_ESPERADA   = "e0:8c:fe:30:c9:1c"   # C-011 · nfc-pn532
MAC_TAMAGOTCHI = "84:1f:e8:2c:3e:c4"   # C-001 · el regalo


def leer_mac(puerto):
    try:
        salida = subprocess.run(
            [env.subst("$PYTHONEXE"), "-m", "esptool", "--port", puerto, "read-mac"],
            capture_output=True, text=True, timeout=30).stdout
    except Exception:
        return None
    for linea in salida.splitlines():
        if linea.startswith("MAC:"):
            return linea.split()[-1].lower()
    return None


def comprobar_placa(source, target, env):
    puerto = env.subst("$UPLOAD_PORT")
    mac = leer_mac(puerto)
    if mac == MAC_ESPERADA:
        print(f"  ✓ Placa correcta: C-011 ({mac})")
        return
    if mac == MAC_TAMAGOTCHI:
        print("  ✗ ¡ALTO! La placa enchufada es la del TAMAGOTCHI (C-001). No se carga nada.")
    elif mac is None:
        print(f"  ✗ No pude leer la MAC en {puerto}. ¿Está enchufada? ¿Otro programa usa "
              "el puerto? ¿Está activado 2-entorno/.venv?")
    else:
        print(f"  ✗ Placa desconocida ({mac}): no es la C-011. No se carga nada.")
    env.Exit(1)


env.AddPreAction("upload", comprobar_placa)
