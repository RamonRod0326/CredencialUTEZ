# Mete el commit de Git dentro del firmware, para que la placa pueda decir
# qué versión trae.
#
# Sin esto, saber qué hay cargado en la placa es adivinar: el último commit
# de Git puede ser posterior (si compilaste sin cargar) o anterior (si
# cargaste algo sin commitear). Y la placa es lo único que no se puede
# consultar desde fuera.
import subprocess
Import("env")

def git(*args):
    try:
        return subprocess.check_output(["git", *args],
                                       stderr=subprocess.DEVNULL).decode().strip()
    except Exception:
        return "desconocido"

commit = git("rev-parse", "--short", "HEAD")
sucio  = "+cambios" if git("status", "--porcelain") else ""
env.Append(CPPDEFINES=[("FW_VERSION", env.StringifyMacro(commit + sucio))])
print(f"  Firmware marcado como: {commit}{sucio}")
