param(
  [int]$Port = 8081
)

$ErrorActionPreference = "Stop"

function Import-DotEnv([string]$envPath) {
  if (-not (Test-Path $envPath)) { return }
  Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if (-not $line) { return }
    if ($line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()
    if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
      $val = $val.Substring(1, $val.Length - 2)
    }
    if (-not $key) { return }
    Set-Item -Path "Env:$key" -Value $val
  }
}

function Stop-PortProcess([int]$p) {
  $line = netstat -ano | Select-String (":$p\s") | Select-Object -First 1
  if (-not $line) { return }
  $parts = ($line.ToString() -split "\s+") | Where-Object { $_ -ne "" }
  $portPid = $parts[-1]
  if ($portPid -and $portPid -match "^\d+$") {
    Write-Host "Stopping PID $portPid on port $p..."
    taskkill /PID $portPid /F | Out-Null
  }
}

Stop-PortProcess -p $Port

Import-DotEnv -envPath (Join-Path (Split-Path $PSScriptRoot -Parent) ".env")

Write-Host "Building jar (skip tests)..."
& .\mvnw.cmd -DskipTests package

$jarRel = ".\target\springboot-backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarRel)) {
  throw "Jar not found: $jarRel"
}

Write-Host "Running: $jarRel"
java -jar $jarRel

