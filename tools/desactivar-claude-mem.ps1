<#
    Desactiva los plugins claude-mem en la configuracion global de Claude Code.

    No desinstala nada: solo pone su interruptor en false, asi que se puede
    volver a activar cambiando false por true.

    Hace una copia de seguridad del archivo antes de tocarlo.

    USO:
        powershell -ExecutionPolicy Bypass -File desactivar-claude-mem.ps1
#>

$f = Join-Path $env:USERPROFILE ".claude\settings.json"

if (-not (Test-Path $f)) {
    Write-Host "No existe $f - nada que hacer."
    exit
}

$copia = "$f.backup"
Copy-Item $f $copia -Force
Write-Host "Copia de seguridad: $copia"

try {
    $j = Get-Content $f -Raw | ConvertFrom-Json
} catch {
    Write-Host "No se pudo leer el JSON. No se ha cambiado nada."
    exit
}

if (-not $j.PSObject.Properties.Name.Contains("enabledPlugins")) {
    Write-Host "No hay plugins activados en el archivo. Nada que desactivar."
    exit
}

$cambiados = 0
foreach ($p in @($j.enabledPlugins.PSObject.Properties)) {
    if ($p.Name -like "claude-mem*") {
        if ($p.Value -ne $false) {
            $j.enabledPlugins.($p.Name) = $false
            Write-Host ("desactivado: " + $p.Name)
            $cambiados++
        } else {
            Write-Host ("ya estaba desactivado: " + $p.Name)
        }
    }
}

if ($cambiados -eq 0) {
    Write-Host "No habia ningun claude-mem activo."
} else {
    $j | ConvertTo-Json -Depth 20 | Set-Content $f -Encoding UTF8
    Write-Host ""
    Write-Host "--- settings.json resultante ---"
    Get-Content $f | ForEach-Object { Write-Host $_ }
}

Write-Host ""
Write-Host "Cierra y vuelve a abrir Claude Code para que surta efecto."
