<#
    Instalador de los indicadores y el robot en MetaTrader 4 / 5.

    Copia los archivos en la carpeta de datos del terminal y los compila
    llamando a MetaEditor por linea de comandos, para no tener que abrir
    el editor ni pulsar F7.

    USO (PowerShell, en Windows):

        powershell -ExecutionPolicy Bypass -File instalar.ps1

    OPCIONES:

        -Origen <ruta>        Carpeta del repositorio ya descargado.
                              Si se omite, descarga los archivos de GitHub.
        -Plataforma mt5|mt4|ambas    Por defecto: ambas (las que encuentre).
        -SinCompilar          Solo copia, no compila.
        -ConRobot             Instalar tambien el robot (EA). Por defecto NO
                              se instala: solo los indicadores, que se limitan
                              a dibujar y avisar, sin tocar la cuenta.

    El script no borra nada: sobrescribe los archivos del sistema si ya
    existen de una instalacion anterior.
#>

param(
    [string]$Origen = "",
    [ValidateSet("mt5","mt4","ambas")]
    [string]$Plataforma = "ambas",
    [switch]$SinCompilar,
    [switch]$ConRobot
)

$ErrorActionPreference = "Stop"

$RepoRaw = "https://raw.githubusercontent.com/rikycunha-coder/el-profe/claude/metatrader-gold-signals-realtime-y46qrf"

$Indicadores = @{
    "mt5" = @(
        "MQL5/Include/GoldSignals/SignalEngine.mqh",
        "MQL5/Include/GoldSignals/Candles.mqh",
        "MQL5/Include/GoldSignals/Structures.mqh",
        "MQL5/Indicators/GoldSignalsRealtime.mq5",
        "MQL5/Indicators/PriceActionPatterns.mq5"
    )
    "mt4" = @(
        "MQL4/Include/GoldSignals/SignalEngine.mqh",
        "MQL4/Include/GoldSignals/Candles.mqh",
        "MQL4/Include/GoldSignals/Structures.mqh",
        "MQL4/Indicators/GoldSignalsRealtime.mq4",
        "MQL4/Indicators/PriceActionPatterns.mq4"
    )
}

$Robot = @{
    "mt5" = @("MQL5/Experts/GoldSignalsEA.mq5")
    "mt4" = @("MQL4/Experts/GoldSignalsEA.mq4")
}

# Lista final segun se pida o no el robot
function Lista {
    param([string]$plataforma)
    $lista = $Indicadores[$plataforma]
    if ($ConRobot) { $lista = $lista + $Robot[$plataforma] }
    return $lista
}

function Escribir($texto, $color = "Gray") { Write-Host $texto -ForegroundColor $color }

#--- 1. Origen de los archivos -------------------------------------------------
function Obtener-Origen {
    param([string]$ruta)

    if ($ruta -ne "") {
        if (-not (Test-Path $ruta)) { throw "No existe la carpeta de origen: $ruta" }
        Escribir "Usando los archivos de: $ruta" "Cyan"
        return (Resolve-Path $ruta).Path
    }

    $carpetaTmp = $env:TEMP
    if ([string]::IsNullOrEmpty($carpetaTmp)) { $carpetaTmp = [IO.Path]::GetTempPath() }
    $tmp = Join-Path $carpetaTmp ("el-profe-" + [Guid]::NewGuid().ToString("N").Substring(0,8))
    New-Item -ItemType Directory -Path $tmp -Force | Out-Null
    Escribir "Descargando los archivos desde GitHub..." "Cyan"

    try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch { }

    $todos = (Lista "mt5") + (Lista "mt4")
    foreach ($rel in $todos) {
        $destino = Join-Path $tmp ($rel -replace "/", "\")
        $carpeta = Split-Path $destino -Parent
        if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta -Force | Out-Null }
        try {
            Invoke-WebRequest -Uri "$RepoRaw/$rel" -OutFile $destino -UseBasicParsing
        } catch {
            throw ("No se pudo descargar $rel.`r`n" +
                   "Si el repositorio es privado, descargalo a mano y ejecuta:`r`n" +
                   "    .\instalar.ps1 -Origen C:\ruta\al\repositorio")
        }
    }
    Escribir "Descarga completada." "Green"
    return $tmp
}

#--- 2. Terminales instalados --------------------------------------------------
function Buscar-Terminales {
    $encontrados = @()

    $base = ""
    if (-not [string]::IsNullOrEmpty($env:APPDATA)) {
        $base = Join-Path $env:APPDATA "MetaQuotes\Terminal"
    }
    if ($base -ne "" -and (Test-Path $base)) {
        foreach ($dir in Get-ChildItem $base -Directory -ErrorAction SilentlyContinue) {
            if ($dir.Name -eq "Common") { continue }
            foreach ($p in @("mt5","mt4")) {
                $sub = if ($p -eq "mt5") { "MQL5" } else { "MQL4" }
                if (Test-Path (Join-Path $dir.FullName $sub)) {
                    $encontrados += [PSCustomObject]@{
                        Plataforma = $p
                        Datos      = $dir.FullName
                        Instalacion = ""
                    }
                }
            }
        }
    }

    # instalaciones "portable": MQL4/MQL5 viven junto a terminal.exe
    foreach ($pf in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if ([string]::IsNullOrEmpty($pf) -or -not (Test-Path $pf)) { continue }
        foreach ($dir in Get-ChildItem $pf -Directory -ErrorAction SilentlyContinue) {
            foreach ($p in @("mt5","mt4")) {
                $sub = if ($p -eq "mt5") { "MQL5" } else { "MQL4" }
                $ruta = Join-Path $dir.FullName $sub
                if ((Test-Path $ruta) -and (Test-Path (Join-Path $dir.FullName "terminal.exe"))) {
                    $encontrados += [PSCustomObject]@{
                        Plataforma  = $p
                        Datos       = $dir.FullName
                        Instalacion = $dir.FullName
                    }
                }
            }
        }
    }
    return $encontrados
}

#--- 3. Localizar MetaEditor ---------------------------------------------------
function Buscar-MetaEditor {
    param($terminal)

    $candidatos = @()

    # el terminal guarda su carpeta de instalacion en origin.txt
    $origin = Join-Path $terminal.Datos "origin.txt"
    if (Test-Path $origin) {
        try {
            $ruta = (Get-Content $origin -Raw -ErrorAction SilentlyContinue).Trim()
            if ($ruta -ne "") { $candidatos += $ruta }
        } catch { }
    }
    if ($terminal.Instalacion -ne "") { $candidatos += $terminal.Instalacion }

    foreach ($pf in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if ([string]::IsNullOrEmpty($pf) -or -not (Test-Path $pf)) { continue }
        foreach ($dir in Get-ChildItem $pf -Directory -ErrorAction SilentlyContinue) {
            $candidatos += $dir.FullName
        }
    }

    $nombres = if ($terminal.Plataforma -eq "mt5") {
        @("metaeditor64.exe","metaeditor.exe")
    } else {
        @("metaeditor.exe","metaeditor64.exe")
    }

    foreach ($c in $candidatos) {
        if ([string]::IsNullOrEmpty($c) -or -not (Test-Path $c)) { continue }
        foreach ($n in $nombres) {
            $exe = Join-Path $c $n
            if (Test-Path $exe) { return $exe }
        }
    }
    return ""
}

#--- 4. Copiar -----------------------------------------------------------------
function Copiar-Archivos {
    param($origen, $terminal)

    $copiados = 0
    foreach ($rel in (Lista $terminal.Plataforma)) {
        $src = Join-Path $origen ($rel -replace "/", "\")
        if (-not (Test-Path $src)) {
            Escribir "  ! falta en el origen: $rel" "Yellow"
            continue
        }
        $dst = Join-Path $terminal.Datos ($rel -replace "/", "\")
        $carpeta = Split-Path $dst -Parent
        if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta -Force | Out-Null }
        Copy-Item $src $dst -Force
        $copiados++
    }
    return $copiados
}

#--- 5. Compilar ---------------------------------------------------------------
function Compilar {
    param($editor, $terminal)

    $carpetaRaiz = if ($terminal.Plataforma -eq "mt5") { "MQL5" } else { "MQL4" }
    $raiz = Join-Path $terminal.Datos $carpetaRaiz
    $errores = 0

    foreach ($rel in (Lista $terminal.Plataforma)) {
        if ($rel -notmatch "\.mq[45]$") { continue }   # los .mqh no se compilan sueltos
        $archivo = Join-Path $terminal.Datos ($rel -replace "/", "\")
        $log     = [IO.Path]::ChangeExtension($archivo, ".log")
        $nombre  = Split-Path $archivo -Leaf

        $argumentos = @("/compile:`"$archivo`"", "/inc:`"$raiz`"", "/log:`"$log`"")
        $p = Start-Process -FilePath $editor -ArgumentList $argumentos -Wait -PassThru -WindowStyle Hidden

        $texto = ""
        if (Test-Path $log) {
            try   { $texto = Get-Content $log -Raw -Encoding Unicode -ErrorAction SilentlyContinue }
            catch { $texto = Get-Content $log -Raw -ErrorAction SilentlyContinue }
        }

        if ($p.ExitCode -eq 0) {
            Escribir "  OK  $nombre" "Green"
        } else {
            $errores++
            Escribir "  ERROR al compilar $nombre (codigo $($p.ExitCode))" "Red"
            if ($texto -ne "") {
                foreach ($linea in ($texto -split "`r?`n")) {
                    if ($linea -match "error|warning") { Escribir "        $linea" "DarkYellow" }
                }
            }
        }
    }
    return $errores
}

#--- Programa ------------------------------------------------------------------
Escribir ""
Escribir "== Instalador de senales para MetaTrader ==" "White"
Escribir ""

$origen = Obtener-Origen $Origen

$terminales = @(Buscar-Terminales)
if ($Plataforma -ne "ambas") {
    $terminales = @($terminales | Where-Object { $_.Plataforma -eq $Plataforma })
}

if ($terminales.Count -eq 0) {
    Escribir "No se ha encontrado ningun MetaTrader instalado." "Red"
    Escribir "Abre el terminal, ve a Archivo > Abrir carpeta de datos y copia los" "Gray"
    Escribir "archivos a mano siguiendo docs/INSTALACION.md." "Gray"
    exit 1
}

Escribir ("Terminales encontrados: " + $terminales.Count) "Cyan"
Escribir ""

$fallos = 0
foreach ($t in $terminales) {
    Escribir ("[" + $t.Plataforma.ToUpper() + "] " + $t.Datos) "White"

    $n = Copiar-Archivos $origen $t
    Escribir "  $n archivos copiados." "Gray"

    if ($SinCompilar) {
        Escribir "  (compilacion omitida)" "Gray"
    } else {
        $editor = Buscar-MetaEditor $t
        if ($editor -eq "") {
            Escribir "  No se encontro MetaEditor: compila a mano con F4 y F7." "Yellow"
            $fallos++
        } else {
            Escribir "  MetaEditor: $editor" "Gray"
            $fallos += (Compilar $editor $t)
        }
    }
    Escribir ""
}

if ($fallos -eq 0) {
    Escribir "Listo. Reinicia MetaTrader (o clic derecho en el Navegador > Actualizar)," "Green"
    Escribir "abre un grafico y arrastra el indicador encima." "Green"
    if (-not $ConRobot) {
        Escribir ""
        Escribir "Se han instalado solo los indicadores: dibujan y avisan, no operan." "Gray"
    }
} else {
    Escribir "Terminado con $fallos incidencia(s). Revisa los mensajes de arriba." "Yellow"
}
Escribir ""
