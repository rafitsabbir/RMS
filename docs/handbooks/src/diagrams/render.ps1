# Renders each *.mmd in this folder to a cropped PNG with headless Edge and Mermaid (CDN).
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$dir = $PSScriptRoot
$edge = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
# Edge's scratch profile stays outside the repo
$profileDir = Join-Path $env:TEMP 'rms-handbook-edge-profile'

foreach ($mmd in Get-ChildItem $dir -Filter *.mmd) {
	$name = $mmd.BaseName
	$src = [System.IO.File]::ReadAllText($mmd.FullName)
	$src = $src.Replace('&', '&amp;').Replace('<br/>', '#BR#').Replace('<', '&lt;').Replace('>', '&gt;').Replace('#BR#', '<br/>')
	$html = @"
<!doctype html><html><head><meta charset="utf-8">
<style>body{margin:0;padding:12px;background:#fff}</style>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.min.js"></script></head><body>
<pre class="mermaid">$src</pre>
<script>
mermaid.initialize({ startOnLoad: true, theme: 'base', fontFamily: 'Segoe UI, Arial, sans-serif',
  flowchart: { htmlLabels: true, curve: 'basis' },
  themeVariables: { primaryColor: '#e6eef8', primaryBorderColor: '#1f5fa8', primaryTextColor: '#1d2733',
    lineColor: '#56677b', secondaryColor: '#e2f4f0', tertiaryColor: '#f4f6fa', fontSize: '15px',
    actorBkg: '#e6eef8', actorBorder: '#1f5fa8', noteBkgColor: '#fdf1dc' } });
</script></body></html>
"@
	$htmlPath = Join-Path $dir "$name.html"
	[System.IO.File]::WriteAllText($htmlPath, $html, (New-Object System.Text.UTF8Encoding $false))
	$raw = Join-Path $dir "$name.raw.png"
	if (Test-Path $raw) { Remove-Item $raw }
	$url = 'file:///' + ($htmlPath -replace '\\', '/')
	$p = Start-Process -FilePath $edge -PassThru -WindowStyle Hidden -ArgumentList @(
		'--headless=new', '--disable-gpu', '--no-first-run', "--user-data-dir=`"$profileDir`"",
		'--virtual-time-budget=10000', '--window-size=1400,1600', '--force-device-scale-factor=2',
		"--screenshot=`"$raw`"", $url)
	if (-not $p.WaitForExit(60000)) { $p.Kill(); throw "Edge timed out on $name" }
	if (-not (Test-Path $raw)) { throw "No screenshot for $name" }

	# Crop to the non-white area, plus a small margin
	$bmp = [System.Drawing.Bitmap]::FromFile($raw)
	$minX = $bmp.Width; $minY = $bmp.Height; $maxX = -1; $maxY = -1
	$rect = New-Object System.Drawing.Rectangle 0, 0, $bmp.Width, $bmp.Height
	$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
	$bytes = New-Object byte[] ($data.Stride * $bmp.Height)
	[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
	$bmp.UnlockBits($data)
	for ($y = 0; $y -lt $bmp.Height; $y += 2) {
		$row = $y * $data.Stride
		for ($x = 0; $x -lt $bmp.Width; $x += 2) {
			$i = $row + $x * 4
			if ($bytes[$i] -lt 245 -or $bytes[$i + 1] -lt 245 -or $bytes[$i + 2] -lt 245) {
				if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
				if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
			}
		}
	}
	$m = 16
	$cx = [Math]::Max(0, $minX - $m); $cy = [Math]::Max(0, $minY - $m)
	$cw = [Math]::Min($bmp.Width - $cx, $maxX - $minX + 2 * $m); $ch = [Math]::Min($bmp.Height - $cy, $maxY - $minY + 2 * $m)
	$crop = $bmp.Clone((New-Object System.Drawing.Rectangle $cx, $cy, $cw, $ch), $bmp.PixelFormat)
	$bmp.Dispose()
	$out = Join-Path $dir "$name.png"
	$crop.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
	"{0}: {1}x{2}" -f $name, $crop.Width, $crop.Height
	$crop.Dispose()
	Remove-Item $raw, $htmlPath
}
