<#
    Radiografia de un vault de Obsidian: que hay dentro, si ya tiene
    configuracion de Claude Code y de donde sale el hook que pide Bun.

    Solo lee. No instala ni modifica nada. No consume cuota de Claude.

    USO:
        powershell -ExecutionPolicy Bypass -File ver-vault.ps1
        powershell -ExecutionPolicy Bypass -File ver-vault.ps1 -Vault "C:\ruta\al\vault"
#>

param([string]$Vault = "")

function L($t) { Write-Host $t }

#--- localizar el vault -------------------------------------------------------
if ($Vault -eq "") {
    $oj = "$env:APPDATA\obsidian\obsidian.json"
    if (Test-Path $oj) {
        try {
            $j = Get-Content $oj -Raw | ConvertFrom-Json
            foreach ($v in $j.vaults.PSObject.Properties) {
                if ($Vault -eq "") { $Vault = $v.Value.path }
            }
        } catch { }
    }
}

if ($Vault -eq "" -or -not (Test-Path $Vault)) {
    L "No se ha encontrado el vault. Pasa la ruta con -Vault ""C:\ruta"""
    exit
}

L ""
L "=== VAULT: $Vault ==="

#--- tamano -------------------------------------------------------------------
$md = @(Get-ChildItem $Vault -Filter *.md -Recurse -File -ErrorAction SilentlyContinue)
$dirs = @(Get-ChildItem $Vault -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne ".obsidian" })
L ("  notas .md: " + $md.Count)
L ("  carpetas de primer nivel: " + $dirs.Count)
if (Test-Path (Join-Path $Vault ".git")) { L "  es repositorio git: SI" } else { L "  es repositorio git: no" }

#--- primer nivel -------------------------------------------------------------
L ""
L "--- primer nivel ---"
$n = 0
foreach ($i in Get-ChildItem $Vault -ErrorAction SilentlyContinue) {
    if ($n -ge 40) { L "  ... (recortado)"; break }
    if ($i.PSIsContainer) { L ("  [dir]  " + $i.Name) } else { L ("         " + $i.Name) }
    $n++
}

#--- notas mas recientes ------------------------------------------------------
L ""
L "--- 15 notas modificadas mas recientemente ---"
foreach ($f in ($md | Sort-Object LastWriteTime -Descending | Select-Object -First 15)) {
    $rel = $f.FullName.Substring($Vault.Length).TrimStart('\')
    L ("  " + $f.LastWriteTime.ToString("yyyy-MM-dd") + "  " + $rel)
}

#--- configuracion de Claude Code --------------------------------------------
L ""
L "--- configuracion de Claude Code en el vault ---"
$cl = Join-Path $Vault ".claude"
if (Test-Path $cl) {
    foreach ($f in Get-ChildItem $cl -Recurse -File -ErrorAction SilentlyContinue) {
        $rel = $f.FullName.Substring($Vault.Length).TrimStart('\')
        L ("  archivo: " + $rel)
    }
    foreach ($nombre in @("settings.json","settings.local.json")) {
        $s = Join-Path $cl $nombre
        if (Test-Path $s) {
            L ""
            L ("  --- contenido de .claude\" + $nombre + " ---")
            foreach ($l in (Get-Content $s -ErrorAction SilentlyContinue)) { L ("    " + $l) }
        }
    }
} else {
    L "  no hay carpeta .claude"
}

$cm = Join-Path $Vault "CLAUDE.md"
if (Test-Path $cm) {
    L ""
    L "  --- CLAUDE.md (primeras 30 lineas) ---"
    foreach ($l in (Get-Content $cm -TotalCount 30 -ErrorAction SilentlyContinue)) { L ("    " + $l) }
} else {
    L "  no hay CLAUDE.md"
}

#--- de donde sale Bun --------------------------------------------------------
L ""
L "--- menciones de Bun ---"
# se recogen primero los archivos que existen: una ruta inexistente
# hacia fallar toda la busqueda y daba un falso "no hay menciones"
$candidatos = @()
if (Test-Path $cl) {
    $candidatos += @(Get-ChildItem $cl -Recurse -File -ErrorAction SilentlyContinue)
}
$candidatos += @(Get-ChildItem $Vault -Filter *.json -File -ErrorAction SilentlyContinue)

$hit = @()
if ($candidatos.Count -gt 0) {
    $hit = @($candidatos | Select-String -Pattern "bun" -SimpleMatch -ErrorAction SilentlyContinue)
}
if ($hit.Count -eq 0) {
    L "  ninguna en la configuracion del vault (el hook puede estar en tu config global de usuario)"
} else {
    foreach ($h in $hit) { L ("  " + $h.Filename + ":" + $h.LineNumber + "  " + $h.Line.Trim()) }
}

$gl = "$env:USERPROFILE\.claude\settings.json"
if (Test-Path $gl) {
    L ""
    L "  --- config global del usuario (~\.claude\settings.json) ---"
    foreach ($l in (Get-Content $gl -ErrorAction SilentlyContinue)) { L ("    " + $l) }
}

L ""
L "Copia todo esto y pegalo en el chat."
L ""
