<#
    Diagnostico: dice en que estado ha quedado la instalacion y saca los
    errores del compilador, para poder pegarlos y que los arreglemos.

    USO:
        powershell -ExecutionPolicy Bypass -File diagnostico.ps1
#>

function Linea($t, $c = "Gray") { Write-Host $t -ForegroundColor $c }

Linea ""
Linea "===== DIAGNOSTICO DE LA INSTALACION =====" "White"
Linea ""

$terminales = @()

$base = ""
if (-not [string]::IsNullOrEmpty($env:APPDATA)) { $base = Join-Path $env:APPDATA "MetaQuotes\Terminal" }
if ($base -ne "" -and (Test-Path $base)) {
    foreach ($dir in Get-ChildItem $base -Directory -ErrorAction SilentlyContinue) {
        if ($dir.Name -eq "Common") { continue }
        foreach ($p in @("MQL4","MQL5")) {
            if (Test-Path (Join-Path $dir.FullName $p)) {
                $terminales += [PSCustomObject]@{ Carpeta = $p; Datos = $dir.FullName }
            }
        }
    }
}
foreach ($pf in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
    if ([string]::IsNullOrEmpty($pf) -or -not (Test-Path $pf)) { continue }
    foreach ($dir in Get-ChildItem $pf -Directory -ErrorAction SilentlyContinue) {
        foreach ($p in @("MQL4","MQL5")) {
            if ((Test-Path (Join-Path $dir.FullName $p)) -and (Test-Path (Join-Path $dir.FullName "terminal.exe"))) {
                $terminales += [PSCustomObject]@{ Carpeta = $p; Datos = $dir.FullName }
            }
        }
    }
}

if ($terminales.Count -eq 0) {
    Linea "No se ha encontrado ningun MetaTrader." "Red"
    exit
}

foreach ($t in $terminales) {
    Linea ("--- " + $t.Carpeta + "  ->  " + $t.Datos) "Cyan"

    $ext  = if ($t.Carpeta -eq "MQL4") { "mq4" } else { "mq5" }
    $comp = if ($t.Carpeta -eq "MQL4") { "ex4" } else { "ex5" }

    # includes
    $inc = Join-Path $t.Datos ($t.Carpeta + "\Include\GoldSignals")
    if (Test-Path $inc) {
        $n = (Get-ChildItem $inc -Filter *.mqh -ErrorAction SilentlyContinue).Count
        Linea ("  Include\GoldSignals: $n archivos .mqh") "Gray"
    } else {
        Linea "  Include\GoldSignals: NO EXISTE  <-- el compilador no encontrara las librerias" "Red"
    }

    # indicadores: fuente y compilado
    foreach ($nombre in @("SenalesCompraVenta","GoldSignalsRealtime","PriceActionPatterns")) {
        $src = Join-Path $t.Datos ($t.Carpeta + "\Indicators\$nombre.$ext")
        $bin = Join-Path $t.Datos ($t.Carpeta + "\Indicators\$nombre.$comp")
        $log = Join-Path $t.Datos ($t.Carpeta + "\Indicators\$nombre.log")

        $estado = ""
        if (-not (Test-Path $src)) { $estado = "falta el .$ext" }
        elseif (Test-Path $bin)    { $estado = "COMPILADO (.$comp presente)" }
        else                        { $estado = "SIN COMPILAR (no hay .$comp)" }

        $color = if ($estado -like "COMPILADO*") { "Green" } else { "Yellow" }
        Linea ("  $nombre : $estado") $color

        if (Test-Path $log) {
            $texto = ""
            try   { $texto = Get-Content $log -Raw -Encoding Unicode -ErrorAction SilentlyContinue }
            catch { }
            if ([string]::IsNullOrWhiteSpace($texto)) {
                try { $texto = Get-Content $log -Raw -ErrorAction SilentlyContinue } catch { }
            }
            if (-not [string]::IsNullOrWhiteSpace($texto)) {
                Linea "    --- log del compilador ---" "DarkGray"
                $n = 0
                foreach ($l in ($texto -split "`r?`n")) {
                    $l = $l.Trim()
                    if ($l -eq "") { continue }
                    if ($n -ge 40) { Linea "    ... (recortado)" "DarkGray"; break }
                    Linea ("    " + $l) "DarkYellow"
                    $n++
                }
            }
        }
    }
    Linea ""
}

# MetaEditor
Linea "--- MetaEditor ---" "Cyan"
$encontrado = $false
foreach ($t in $terminales) {
    $rutas = @()
    $origin = Join-Path $t.Datos "origin.txt"
    if (Test-Path $origin) {
        try { $rutas += ((Get-Content $origin -Raw -ErrorAction SilentlyContinue).Trim()) } catch { }
    }
    $rutas += $t.Datos
    foreach ($r in $rutas) {
        if ([string]::IsNullOrEmpty($r) -or -not (Test-Path $r)) { continue }
        foreach ($n in @("metaeditor.exe","metaeditor64.exe")) {
            $exe = Join-Path $r $n
            if (Test-Path $exe) { Linea ("  encontrado: " + $exe) "Green"; $encontrado = $true }
        }
    }
}
if (-not $encontrado) { Linea "  NO se ha encontrado metaeditor.exe (por eso no se compilo solo)" "Yellow" }

Linea ""
Linea "Copia todo este texto y pegalo en el chat." "White"
Linea ""
