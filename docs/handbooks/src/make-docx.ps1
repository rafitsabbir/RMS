# Builds a .docx from a small line-based markup, without Word.
# Markup: "# " title, "@ " subtitle line, "## " heading 1, "### " heading 2, "- " bullet, "1. " numbered (literal),
# "> " note, "|a|b|" table (first row = header; "|---" rows ignored), ``` code block ```, "![caption](file.png)" image,
# "@@pagebreak", blank line = paragraph break. Inline: **bold**, `code`.
param([Parameter(Mandatory)] [string] $Source, [Parameter(Mandatory)] [string] $Output, [string] $ImageDir)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem, System.Drawing

function Esc([string] $s) { [System.Security.SecurityElement]::Escape($s) }

function Runs([string] $text, [string] $extraRpr = '') {
	$out = New-Object System.Text.StringBuilder
	foreach ($part in [regex]::Split($text, '(\*\*[^*]+\*\*|`[^`]+`)')) {
		if ($part -eq '') { continue }
		if ($part -match '^\*\*(.+)\*\*$') {
			[void]$out.Append("<w:r><w:rPr><w:b/>$extraRpr</w:rPr><w:t xml:space=`"preserve`">$(Esc $Matches[1])</w:t></w:r>")
		} elseif ($part -match '^`(.+)`$') {
			[void]$out.Append("<w:r><w:rPr><w:rStyle w:val=`"InlineCode`"/>$extraRpr</w:rPr><w:t xml:space=`"preserve`">$(Esc $Matches[1])</w:t></w:r>")
		} else {
			[void]$out.Append("<w:r><w:rPr>$extraRpr</w:rPr><w:t xml:space=`"preserve`">$(Esc $part)</w:t></w:r>")
		}
	}
	$out.ToString()
}

function Para([string] $style, [string] $text, [string] $pprExtra = '') {
	"<w:p><w:pPr><w:pStyle w:val=`"$style`"/>$pprExtra</w:pPr>$(Runs $text)</w:p>"
}

$body = New-Object System.Text.StringBuilder
$images = @()
$lines = [System.IO.File]::ReadAllLines($Source)
$i = 0
$title = ''
while ($i -lt $lines.Count) {
	$line = $lines[$i]
	if ($line -match '^```') {
		$i++
		while ($i -lt $lines.Count -and $lines[$i] -notmatch '^```') {
			[void]$body.Append("<w:p><w:pPr><w:pStyle w:val=`"Code`"/></w:pPr><w:r><w:t xml:space=`"preserve`">$(Esc $lines[$i])</w:t></w:r></w:p>")
			$i++
		}
		$i++; continue
	}
	if ($line -match '^\|') {
		$rows = @()
		while ($i -lt $lines.Count -and $lines[$i] -match '^\|') {
			if ($lines[$i] -notmatch '^\|\s*-{3}') { $rows += , ($lines[$i].Trim().Trim('|') -split '\|' | ForEach-Object { $_.Trim() }) }
			$i++
		}
		$cols = $rows[0].Count
		$tbl = New-Object System.Text.StringBuilder
		[void]$tbl.Append('<w:tbl><w:tblPr><w:tblStyle w:val="RmsTable"/><w:tblW w:w="5000" w:type="pct"/><w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="1" w:noVBand="1"/></w:tblPr><w:tblGrid>')
		$colW = [int](9752 / $cols); for ($c = 0; $c -lt $cols; $c++) { [void]$tbl.Append("<w:gridCol w:w=`"$colW`"/>") }
		[void]$tbl.Append('</w:tblGrid>')
		for ($r = 0; $r -lt $rows.Count; $r++) {
			$hdr = if ($r -eq 0) { '<w:trPr><w:tblHeader/><w:cantSplit/></w:trPr>' } else { '<w:trPr><w:cantSplit/></w:trPr>' }
			[void]$tbl.Append("<w:tr>$hdr")
			for ($c = 0; $c -lt $cols; $c++) {
				$cell = if ($c -lt $rows[$r].Count) { $rows[$r][$c] } else { '' }
				$shade = if ($r -eq 0) { '<w:shd w:val="clear" w:color="auto" w:fill="E6EEF8"/>' } else { '' }
				$rpr = if ($r -eq 0) { '<w:b/>' } else { '' }
				[void]$tbl.Append("<w:tc><w:tcPr>$shade</w:tcPr>")
				foreach ($cp in ($cell -split '<br>')) {
					[void]$tbl.Append("<w:p><w:pPr><w:pStyle w:val=`"TableText`"/></w:pPr>$(Runs $cp $rpr)</w:p>")
				}
				[void]$tbl.Append('</w:tc>')
			}
			[void]$tbl.Append('</w:tr>')
		}
		[void]$tbl.Append('</w:tbl>')
		[void]$body.Append($tbl.ToString())
		[void]$body.Append('<w:p><w:pPr><w:pStyle w:val="Spacer"/></w:pPr></w:p>')
		continue
	}
	if ($line -match '^!\[(.*)\]\((.+)\)$') {
		$caption = $Matches[1]; $file = Join-Path $ImageDir $Matches[2]
		$img = [System.Drawing.Image]::FromFile($file)
		$wIn = [Math]::Min(6.2, $img.Width / 2 / 96); $hIn = $wIn * $img.Height / $img.Width
		if ($hIn -gt 8.3) { $hIn = 8.3; $wIn = $hIn * $img.Width / $img.Height }
		$img.Dispose()
		$n = $images.Count + 1; $rid = "rIdImg$n"
		$images += , @($file, "image$n.png", $rid)
		$cx = [long]($wIn * 914400); $cy = [long]($hIn * 914400)
		[void]$body.Append("<w:p><w:pPr><w:pStyle w:val=`"Figure`"/></w:pPr><w:r><w:drawing><wp:inline distT=`"0`" distB=`"0`" distL=`"0`" distR=`"0`"><wp:extent cx=`"$cx`" cy=`"$cy`"/><wp:docPr id=`"$n`" name=`"Diagram $n`" descr=`"$(Esc $caption)`"/><a:graphic xmlns:a=`"http://schemas.openxmlformats.org/drawingml/2006/main`"><a:graphicData uri=`"http://schemas.openxmlformats.org/drawingml/2006/picture`"><pic:pic xmlns:pic=`"http://schemas.openxmlformats.org/drawingml/2006/picture`"><pic:nvPicPr><pic:cNvPr id=`"$n`" name=`"image$n.png`"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed=`"$rid`"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x=`"0`" y=`"0`"/><a:ext cx=`"$cx`" cy=`"$cy`"/></a:xfrm><a:prstGeom prst=`"rect`"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>")
		if ($caption) { [void]$body.Append((Para 'Caption' $caption)) }
		$i++; continue
	}
	if ($line -eq '@@pagebreak') { [void]$body.Append('<w:p><w:r><w:br w:type="page"/></w:r></w:p>'); $i++; continue }
	if ($line -match '^# (.+)') { $title = $Matches[1]; [void]$body.Append((Para 'Title' $Matches[1])); $i++; continue }
	if ($line -match '^@ (.+)') { [void]$body.Append((Para 'Subtitle' $Matches[1])); $i++; continue }
	if ($line -match '^## (.+)') { [void]$body.Append((Para 'Heading1' $Matches[1])); $i++; continue }
	if ($line -match '^### (.+)') { [void]$body.Append((Para 'Heading2' $Matches[1])); $i++; continue }
	if ($line -match '^- (.+)') { [void]$body.Append((Para 'Bullet' ([string][char]0x2022 + "`t" + $Matches[1]))); $i++; continue }
	if ($line -match '^  - (.+)') { [void]$body.Append((Para 'Bullet2' ([string][char]0x2013 + "`t" + $Matches[1]))); $i++; continue }
	if ($line -match '^(\d+)\. (.+)') { [void]$body.Append((Para 'Numbered' ($Matches[1] + ".`t" + $Matches[2]))); $i++; continue }
	if ($line -match '^> (.+)') { [void]$body.Append((Para 'Note' $Matches[1])); $i++; continue }
	if ($line.Trim() -eq '') { $i++; continue }
	[void]$body.Append((Para 'Normal' $line)); $i++
}
# Tabs inside runs: turn the literal tab into a w:tab
$bodyXml = $body.ToString().Replace("`t", '</w:t><w:tab/><w:t xml:space="preserve">')

$document = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
<w:body>$bodyXml<w:sectPr><w:footerReference w:type="default" r:id="rIdFooter"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1077" w:bottom="1134" w:left="1077" w:header="567" w:footer="567" w:gutter="0"/></w:sectPr></w:body></w:document>
"@

$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Calibri" w:cs="Calibri"/><w:color w:val="1D2733"/><w:sz w:val="21"/><w:szCs w:val="21"/><w:lang w:val="en-GB"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="276" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="80"/></w:pPr><w:rPr><w:rFonts w:ascii="Cambria" w:hAnsi="Cambria"/><w:b/><w:color w:val="0F2A44"/><w:sz w:val="48"/><w:szCs w:val="48"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="60"/></w:pPr><w:rPr><w:color w:val="56677B"/><w:sz w:val="22"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:keepNext/><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="4" w:color="1F5FA8"/></w:pBdr><w:spacing w:before="420" w:after="160"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:rFonts w:ascii="Cambria" w:hAnsi="Cambria"/><w:b/><w:color w:val="1F5FA8"/><w:sz w:val="32"/><w:szCs w:val="32"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="100"/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:b/><w:color w:val="0F2A44"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Bullet"><w:name w:val="List Bullet"/><w:basedOn w:val="Normal"/><w:pPr><w:tabs><w:tab w:val="left" w:pos="360"/></w:tabs><w:spacing w:after="60"/><w:ind w:left="360" w:hanging="360"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Bullet2"><w:name w:val="List Bullet 2"/><w:basedOn w:val="Normal"/><w:pPr><w:tabs><w:tab w:val="left" w:pos="720"/></w:tabs><w:spacing w:after="60"/><w:ind w:left="720" w:hanging="360"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Numbered"><w:name w:val="List Number"/><w:basedOn w:val="Normal"/><w:pPr><w:tabs><w:tab w:val="left" w:pos="400"/></w:tabs><w:spacing w:after="80"/><w:ind w:left="400" w:hanging="400"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Note"><w:name w:val="Note"/><w:basedOn w:val="Normal"/><w:pPr><w:pBdr><w:left w:val="single" w:sz="24" w:space="8" w:color="12907B"/></w:pBdr><w:shd w:val="clear" w:color="auto" w:fill="E2F4F0"/><w:spacing w:before="120" w:after="160"/><w:ind w:left="200" w:right="100"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Code"><w:name w:val="Code"/><w:basedOn w:val="Normal"/><w:pPr><w:shd w:val="clear" w:color="auto" w:fill="F1F4F8"/><w:spacing w:after="0" w:line="240" w:lineRule="auto"/><w:ind w:left="120"/></w:pPr><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr></w:style>
<w:style w:type="character" w:styleId="InlineCode"><w:name w:val="Inline Code"/><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:color w:val="133D6D"/><w:sz w:val="19"/><w:shd w:val="clear" w:color="auto" w:fill="EEF3FA"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="TableText"><w:name w:val="Table Text"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="0" w:line="252" w:lineRule="auto"/></w:pPr><w:rPr><w:sz w:val="19"/><w:szCs w:val="19"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Spacer"><w:name w:val="Spacer"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="60"/></w:pPr><w:rPr><w:sz w:val="8"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Figure"><w:name w:val="Figure"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/><w:spacing w:before="120" w:after="60"/><w:jc w:val="center"/></w:pPr></w:style>
<w:style w:type="paragraph" w:styleId="Caption"><w:name w:val="caption"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="200"/><w:jc w:val="center"/></w:pPr><w:rPr><w:i/><w:color w:val="56677B"/><w:sz w:val="18"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Footer"><w:name w:val="footer"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="0"/><w:jc w:val="center"/></w:pPr><w:rPr><w:color w:val="56677B"/><w:sz w:val="16"/></w:rPr></w:style>
<w:style w:type="table" w:styleId="RmsTable"><w:name w:val="RMS Table"/><w:tblPr><w:tblBorders><w:top w:val="single" w:sz="4" w:color="C9D3E0"/><w:left w:val="single" w:sz="4" w:color="C9D3E0"/><w:bottom w:val="single" w:sz="4" w:color="C9D3E0"/><w:right w:val="single" w:sz="4" w:color="C9D3E0"/><w:insideH w:val="single" w:sz="4" w:color="C9D3E0"/><w:insideV w:val="single" w:sz="4" w:color="C9D3E0"/></w:tblBorders><w:tblCellMar><w:top w:w="60" w:type="dxa"/><w:left w:w="100" w:type="dxa"/><w:bottom w:w="60" w:type="dxa"/><w:right w:w="100" w:type="dxa"/></w:tblCellMar></w:tblPr></w:style>
</w:styles>
'@

$footer = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:pPr><w:pStyle w:val="Footer"/></w:pPr><w:r><w:t xml:space="preserve">$(Esc $title) $([char]0x00B7) page </w:t></w:r><w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>1</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:ftr>
"@

$rels = New-Object System.Text.StringBuilder
[void]$rels.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rIdFooter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>')
foreach ($img in $images) { [void]$rels.Append("<Relationship Id=`"$($img[2])`" Type=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image`" Target=`"media/$($img[1])`"/>") }
[void]$rels.Append('</Relationships>')

$contentTypes = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/></Types>'
$rootRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/></Relationships>'
$core = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?><cp:coreProperties xmlns:cp=`"http://schemas.openxmlformats.org/package/2006/metadata/core-properties`" xmlns:dc=`"http://purl.org/dc/elements/1.1/`" xmlns:dcterms=`"http://purl.org/dc/terms/`" xmlns:xsi=`"http://www.w3.org/2001/XMLSchema-instance`"><dc:title>$(Esc $title)</dc:title><dc:creator>RMS project</dc:creator><dcterms:created xsi:type=`"dcterms:W3CDTF`">2026-10-02T00:00:00Z</dcterms:created></cp:coreProperties>"

# Check the XML parts are well-formed before zipping
foreach ($x in @($document, $styles, $footer, $rels.ToString(), $contentTypes, $core)) { [void]([xml]$x) }

if (Test-Path $Output) { Remove-Item $Output }
$zip = [System.IO.Compression.ZipFile]::Open($Output, 'Create')
$utf8 = New-Object System.Text.UTF8Encoding $false
function AddText($name, $text) {
	$e = $zip.CreateEntry($name); $w = New-Object System.IO.StreamWriter($e.Open(), $utf8); $w.Write($text); $w.Close()
}
AddText '[Content_Types].xml' $contentTypes
AddText '_rels/.rels' $rootRels
AddText 'docProps/core.xml' $core
AddText 'word/document.xml' $document
AddText 'word/styles.xml' $styles
AddText 'word/footer1.xml' $footer
AddText 'word/_rels/document.xml.rels' $rels.ToString()
foreach ($img in $images) {
	$e = $zip.CreateEntry("word/media/$($img[1])"); $s = $e.Open(); $b = [System.IO.File]::ReadAllBytes($img[0]); $s.Write($b, 0, $b.Length); $s.Close()
}
$zip.Dispose()
"{0}: {1:N0} bytes, {2} images" -f (Split-Path $Output -Leaf), (Get-Item $Output).Length, $images.Count
