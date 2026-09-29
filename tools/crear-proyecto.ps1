<#
    Crea la estructura de memoria y del loop de desarrollo en una carpeta.

    Sirve tanto para un repositorio de proyecto como para un vault de Obsidian
    (son la misma cosa si el vault es el repositorio).

    No sobrescribe nada salvo que se pida con -Sobrescribir.
    No consume cuota de Claude.

    USO:
        powershell -ExecutionPolicy Bypass -File crear-proyecto.ps1 -Destino "C:\ruta\al\proyecto"

    OPCIONES:
        -Origen <ruta>    usar una copia local del repositorio en vez de descargar
        -Sobrescribir     reemplazar archivos que ya existan
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Destino,
    [string]$Origen = "",
    [switch]$Sobrescribir
)

$ErrorActionPreference = "Stop"

$Base = "https://raw.githubusercontent.com/rikycunha-coder/el-profe/claude/metatrader-gold-signals-realtime-y46qrf/plantilla-proyecto"

$Archivos = @(
    "CLAUDE.md",
    ".gitignore",
    "memoria/ESTADO.md",
    "memoria/DECISIONES.md",
    "memoria/ARQUITECTURA.md",
    "memoria/PENDIENTE.md",
    "memoria/diario/2026-09-29.md",
    ".claude/commands/siguiente.md",
    ".claude/commands/cerrar-sesion.md",
    "docs/LOOP.md"
)

function L($t, $c = "Gray") { Write-Host $t -ForegroundColor $c }

if (-not (Test-Path $Destino)) {
    New-Item -ItemType Directory -Path $Destino -Force | Out-Null
    L "Carpeta creada: $Destino" "Cyan"
}
$Destino = (Resolve-Path $Destino).Path

L ""
L "Montando la estructura en: $Destino" "White"
L ""

try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch { }

$puestos = 0
$saltados = 0

foreach ($rel in $Archivos) {
    $destino = Join-Path $Destino ($rel -replace "/", "\")
    $carpeta = Split-Path $destino -Parent
    if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta -Force | Out-Null }

    if ((Test-Path $destino) -and (-not $Sobrescribir)) {
        L "  ya existe, se deja como esta: $rel" "Yellow"
        $saltados++
        continue
    }

    if ($Origen -ne "") {
        $src = Join-Path $Origen ("plantilla-proyecto\" + ($rel -replace "/", "\"))
        if (-not (Test-Path $src)) {
            L "  no esta en el origen: $rel" "Yellow"
            continue
        }
        Copy-Item $src $destino -Force
    } else {
        try {
            Invoke-WebRequest -Uri "$Base/$rel" -OutFile $destino -UseBasicParsing
        } catch {
            L "  no se pudo descargar: $rel" "Red"
            continue
        }
    }
    L "  puesto: $rel" "Green"
    $puestos++
}

L ""
L "$puestos archivos puestos, $saltados respetados." "White"
L ""
L "Siguiente paso:" "Cyan"
L "  1. Abre esa carpeta como vault en Obsidian para leer y editar la memoria."
L "  2. Si aun no es repositorio git:  cd `"$Destino`"  y luego  git init"
L "  3. Cuando vuelva tu cuota:  claude   y escribe  /siguiente"
L ""
