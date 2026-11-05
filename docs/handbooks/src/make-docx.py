"""Builds a .docx from a small line-based markup, without Word: the same markup and output as make-docx.ps1, for
machines without PowerShell (Python 3, standard library only).

Markup: "# " title, "@ " subtitle line, "## " heading 1, "### " heading 2, "- " bullet, "  - " bullet 2, "1. " numbered
(literal), "> " note, "|a|b|" table (first row = header; "|---" rows ignored), ``` code block ```,
"![caption](file.png)" image, "@@pagebreak", blank line = paragraph break. Inline: **bold**, `code`.

Usage: python3 make-docx.py <source.txt> <output.docx> [<image dir>]
"""
import re
import struct
import sys
import zipfile
from xml.dom import minidom
from xml.sax.saxutils import escape as _escape

# The XML parts are copied from make-docx.ps1; keep both scripts in step
STYLES = open(__file__.replace("make-docx.py", "make-docx.ps1"), encoding="utf-8").read()
STYLES = STYLES[STYLES.index("$styles = @'") + len("$styles = @'"):]
STYLES = STYLES[:STYLES.index("'@")].strip("\r\n")


def esc(text):
	# SecurityElement.Escape: & < > " '
	return _escape(text, {'"': "&quot;", "'": "&apos;"})


def runs(text, extra_rpr=""):
	out = []
	for part in re.split(r"(\*\*[^*]+\*\*|`[^`]+`)", text):
		if part == "":
			continue
		m = re.match(r"^\*\*(.+)\*\*$", part)
		if m:
			out.append('<w:r><w:rPr><w:b/>%s</w:rPr><w:t xml:space="preserve">%s</w:t></w:r>' % (extra_rpr, esc(m.group(1))))
			continue
		m = re.match(r"^`(.+)`$", part)
		if m:
			out.append('<w:r><w:rPr><w:rStyle w:val="InlineCode"/>%s</w:rPr><w:t xml:space="preserve">%s</w:t></w:r>'
					% (extra_rpr, esc(m.group(1))))
			continue
		out.append('<w:r><w:rPr>%s</w:rPr><w:t xml:space="preserve">%s</w:t></w:r>' % (extra_rpr, esc(part)))
	return "".join(out)


def para(style, text, ppr_extra=""):
	return '<w:p><w:pPr><w:pStyle w:val="%s"/>%s</w:pPr>%s</w:p>' % (style, ppr_extra, runs(text))


def png_size(path):
	with open(path, "rb") as f:
		head = f.read(24)
	if head[:8] != b"\x89PNG\r\n\x1a\n":
		raise ValueError("not a PNG: " + path)
	return struct.unpack(">II", head[16:24])


def build(source, output, image_dir):
	with open(source, encoding="utf-8-sig") as f:
		lines = f.read().splitlines()
	body = []
	images = []
	title = ""
	i = 0
	while i < len(lines):
		line = lines[i]
		if re.match(r"^```", line):
			i += 1
			while i < len(lines) and not re.match(r"^```", lines[i]):
				body.append('<w:p><w:pPr><w:pStyle w:val="Code"/></w:pPr><w:r><w:t xml:space="preserve">%s</w:t></w:r></w:p>'
						% esc(lines[i]))
				i += 1
			i += 1
			continue
		if re.match(r"^\|", line):
			rows = []
			while i < len(lines) and re.match(r"^\|", lines[i]):
				if not re.match(r"^\|\s*-{3}", lines[i]):
					rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
				i += 1
			cols = len(rows[0])
			tbl = ['<w:tbl><w:tblPr><w:tblStyle w:val="RmsTable"/><w:tblW w:w="5000" w:type="pct"/><w:tblLook w:val="04A0" '
					'w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="1" w:noVBand="1"/></w:tblPr>'
					'<w:tblGrid>']
			col_w = int(round(9752 / cols))
			tbl.extend('<w:gridCol w:w="%d"/>' % col_w for _ in range(cols))
			tbl.append("</w:tblGrid>")
			for r, row in enumerate(rows):
				hdr = "<w:trPr><w:tblHeader/><w:cantSplit/></w:trPr>" if r == 0 else "<w:trPr><w:cantSplit/></w:trPr>"
				tbl.append("<w:tr>" + hdr)
				for c in range(cols):
					cell = row[c] if c < len(row) else ""
					shade = '<w:shd w:val="clear" w:color="auto" w:fill="E6EEF8"/>' if r == 0 else ""
					rpr = "<w:b/>" if r == 0 else ""
					tbl.append("<w:tc><w:tcPr>%s</w:tcPr>" % shade)
					for cp in cell.split("<br>"):
						tbl.append('<w:p><w:pPr><w:pStyle w:val="TableText"/></w:pPr>%s</w:p>' % runs(cp, rpr))
					tbl.append("</w:tc>")
				tbl.append("</w:tr>")
			tbl.append("</w:tbl>")
			body.append("".join(tbl))
			body.append('<w:p><w:pPr><w:pStyle w:val="Spacer"/></w:pPr></w:p>')
			continue
		m = re.match(r"^!\[(.*)\]\((.+)\)$", line)
		if m:
			caption, path = m.group(1), image_dir + "/" + m.group(2)
			width, height = png_size(path)
			w_in = min(6.2, width / 2 / 96)
			h_in = w_in * height / width
			if h_in > 8.3:
				h_in = 8.3
				w_in = h_in * width / height
			n = len(images) + 1
			rid = "rIdImg%d" % n
			images.append((path, "image%d.png" % n, rid))
			cx, cy = int(round(w_in * 914400)), int(round(h_in * 914400))
			body.append('<w:p><w:pPr><w:pStyle w:val="Figure"/></w:pPr><w:r><w:drawing><wp:inline distT="0" distB="0" '
					'distL="0" distR="0"><wp:extent cx="%d" cy="%d"/><wp:docPr id="%d" name="Diagram %d" descr="%s"/>'
					'<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData '
					'uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic '
					'xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="%d" '
					'name="image%d.png"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="%s"/><a:stretch>'
					'<a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="%d" cy="%d"/>'
					'</a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData>'
					'</a:graphic></wp:inline></w:drawing></w:r></w:p>' % (cx, cy, n, n, esc(caption), n, n, rid, cx, cy))
			if caption:
				body.append(para("Caption", caption))
			i += 1
			continue
		if line == "@@pagebreak":
			body.append('<w:p><w:r><w:br w:type="page"/></w:r></w:p>')
			i += 1
			continue
		for pattern, style, prefix in ((r"^# (.+)", "Title", None), (r"^@ (.+)", "Subtitle", None),
				(r"^## (.+)", "Heading1", None), (r"^### (.+)", "Heading2", None), (r"^- (.+)", "Bullet", "•\t"),
				(r"^  - (.+)", "Bullet2", "–\t"), (r"^> (.+)", "Note", None)):
			m = re.match(pattern, line)
			if m:
				if style == "Title":
					title = m.group(1)
				body.append(para(style, (prefix or "") + m.group(1)))
				break
		else:
			m = re.match(r"^(\d+)\. (.+)", line)
			if m:
				body.append(para("Numbered", m.group(1) + ".\t" + m.group(2)))
			elif line.strip() != "":
				body.append(para("Normal", line))
		i += 1

	body_xml = "".join(body).replace("\t", '</w:t><w:tab/><w:t xml:space="preserve">')
	document = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<w:document '
			'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
			'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
			'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">\n<w:body>%s<w:sectPr>'
			'<w:footerReference w:type="default" r:id="rIdFooter"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" '
			'w:right="1077" w:bottom="1134" w:left="1077" w:header="567" w:footer="567" w:gutter="0"/></w:sectPr></w:body>'
			'</w:document>' % body_xml)
	footer = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<w:ftr '
			'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:pPr><w:pStyle w:val="Footer"/>'
			'</w:pPr><w:r><w:t xml:space="preserve">%s · page </w:t></w:r><w:r><w:fldChar w:fldCharType="begin"/></w:r>'
			'<w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/>'
			'</w:r><w:r><w:t>1</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:ftr>' % esc(title))
	rels = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships '
			'xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdStyles" '
			'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
			'<Relationship Id="rIdFooter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" '
			'Target="footer1.xml"/>']
	rels.extend('<Relationship Id="%s" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" '
			'Target="media/%s"/>' % (rid, name) for _, name, rid in images)
	rels.append("</Relationships>")
	rels = "".join(rels)
	content_types = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types '
			'xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" '
			'ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" '
			'ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override '
			'PartName="/word/document.xml" '
			'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override '
			'PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>'
			'<Override PartName="/word/footer1.xml" '
			'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/><Override '
			'PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/></Types>')
	root_rels = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships '
			'xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" '
			'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
			'Target="word/document.xml"/><Relationship Id="rId2" '
			'Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" '
			'Target="docProps/core.xml"/></Relationships>')
	core = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties '
			'xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" '
			'xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" '
			'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>%s</dc:title><dc:creator>RMS project</dc:creator>'
			'<dcterms:created xsi:type="dcterms:W3CDTF">2026-10-02T00:00:00Z</dcterms:created></cp:coreProperties>'
			% esc(title))

	# Check the XML parts are well-formed before zipping
	for part in (document, STYLES, footer, rels, content_types, core):
		minidom.parseString(part.encode("utf-8"))

	with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as z:
		z.writestr("[Content_Types].xml", content_types)
		z.writestr("_rels/.rels", root_rels)
		z.writestr("docProps/core.xml", core)
		z.writestr("word/document.xml", document)
		z.writestr("word/styles.xml", STYLES)
		z.writestr("word/footer1.xml", footer)
		z.writestr("word/_rels/document.xml.rels", rels)
		for path, name, _ in images:
			z.write(path, "word/media/" + name)
	print("%s: %d images" % (output.rsplit("/", 1)[-1], len(images)))


if __name__ == "__main__":
	if len(sys.argv) < 3:
		sys.exit(__doc__)
	build(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else ".")
