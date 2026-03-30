param(
  [int]$Port = 8081
)

$ErrorActionPreference = "SilentlyContinue"

$line = netstat -ano | Select-String (":$Port\s") | Select-Object -First 1
if ($line) {
  $parts = ($line.ToString() -split "\s+") | Where-Object { $_ -ne "" }
  $pid = $parts[-1]
  if ($pid -and $pid -match "^\d+$") {
    Write-Host "Stopping PID $pid on port $Port..."
    taskkill /PID $pid /F | Out-Null
    exit 0
  }
}

Write-Host "No process found on port $Port."

