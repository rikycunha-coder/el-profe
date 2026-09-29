<#
    Comprobacion del entorno local: que hay instalado de Obsidian y de
    Claude en este PC, y donde estan los vaults.

    Solo lee. No instala ni cambia nada.

    USO:
        powershell -ExecutionPolicy Bypass -File comprobar-entorno.ps1
#>

"=== OBSIDIAN ==="
$ob = "$env:LOCALAPPDATA\Obsidian\Obsidian.exe"
if (Test-Path $ob) { "  app: INSTALADA" } else { "  app: no encontrada" }

$oj = "$env:APPDATA\obsidian\obsidian.json"
if (Test-Path $oj) {
    "  vaults abiertos:"
    try {
        $j = Get-Content $oj -Raw | ConvertFrom-Json
        foreach ($v in $j.vaults.PSObject.Properties) { "    " + $v.Value.path }
    } catch { "    (no se pudo leer la lista)" }
} else {
    "  vaults: ninguno todavia"
}

"=== CLIENTE DE CLAUDE EN ESTE PC ==="
$cd = "$env:LOCALAPPDATA\AnthropicClaude\claude.exe"
if (Test-Path $cd) { "  Claude Desktop: INSTALADO" } else { "  Claude Desktop: no encontrado" }

$cfg = "$env:APPDATA\Claude\claude_desktop_config.json"
if (Test-Path $cfg) { "  config de Desktop: existe" } else { "  config de Desktop: aun no existe" }

if (Get-Command claude -ErrorAction SilentlyContinue) {
    "  Claude Code (CLI): " + (claude --version)
} else {
    "  Claude Code (CLI): no instalado"
}

if (Get-Command node -ErrorAction SilentlyContinue) {
    "  Node.js: " + (node --version)
} else {
    "  Node.js: no instalado"
}

if (Get-Command git -ErrorAction SilentlyContinue) {
    "  Git: " + (git --version)
} else {
    "  Git: no instalado"
}
