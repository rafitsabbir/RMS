# Build the local RMS database (rms_local) on a local MySQL server:
#   00-create-database.sql -> ..\schema.sql -> ..\test-seed.sql -> row counts.
# DESTRUCTIVE: drops and recreates rms_local. See README.md.
#
# Environment (all optional):
#   RMS_DB_HOST (default 127.0.0.1), RMS_DB_PORT (3306), RMS_DB_USER (root)
#   RMS_DB_PASSWORD      if set, passed through MYSQL_PWD; otherwise mysql prompts
#   RMS_DB_ALLOW_REMOTE  set to true to allow a non-loopback host
#
# Run: powershell -ExecutionPolicy Bypass -File db\local\setup-local-db.ps1
$ErrorActionPreference = 'Stop'

$here = $PSScriptRoot
$dbDir = Split-Path -Parent $here

$dbHost = if ($env:RMS_DB_HOST) { $env:RMS_DB_HOST } else { '127.0.0.1' }
$dbPort = if ($env:RMS_DB_PORT) { $env:RMS_DB_PORT } else { '3306' }
$dbUser = if ($env:RMS_DB_USER) { $env:RMS_DB_USER } else { 'root' }

if (@('localhost', '127.0.0.1', '::1') -notcontains $dbHost -and $env:RMS_DB_ALLOW_REMOTE -ne 'true') {
	Write-Error "Refusing non-local host '$dbHost'. Set RMS_DB_ALLOW_REMOTE=true only for a throwaway server."
}

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
	Write-Error 'mysql client not found on PATH.'
}

$files = @(
	(Join-Path $here '00-create-database.sql'),
	(Join-Path $dbDir 'schema.sql'),
	(Join-Path $dbDir 'test-seed.sql')
)
foreach ($f in $files) {
	if (-not (Test-Path $f)) { Write-Error "Missing $f" }
}

$counts = @'
SELECT 'users' AS tbl, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'admin', COUNT(*) FROM admin
UNION ALL SELECT 'position', COUNT(*) FROM position
UNION ALL SELECT 'language', COUNT(*) FROM language
UNION ALL SELECT 'candidate', COUNT(*) FROM candidate
UNION ALL SELECT 'marks', COUNT(*) FROM marks;
'@

$sql = (($files | ForEach-Object { Get-Content -Raw -Encoding UTF8 $_ }) + $counts) -join "`n"

$mysqlArgs = @('--protocol=TCP', '-h', $dbHost, '-P', $dbPort, '-u', $dbUser, '--default-character-set=utf8mb4', '--table')
if ($env:RMS_DB_PASSWORD) {
	$env:MYSQL_PWD = $env:RMS_DB_PASSWORD
} else {
	$mysqlArgs += '-p'
}

Write-Host "Rebuilding rms_local on ${dbHost}:${dbPort} as $dbUser ..."
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
try {
	$sql | & mysql @mysqlArgs
	if ($LASTEXITCODE -ne 0) { Write-Error "mysql exited with code $LASTEXITCODE" }
} finally {
	Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host 'Done. Expected rows: users 2, admin 2, position 3, language 3, candidate 2, marks 2.'
