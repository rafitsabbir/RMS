package rms.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The rules for an uploaded document file: PDF, JPG or PNG, at most 5 MB, and the name's extension, the type the
 * browser declared and the file's first bytes must all agree. The upload's name is only shown, never used as a
 * path (rms.dao.DocumentFileStore).
 */
public final class DocumentRules {

	public static final long MAX_BYTES = 5L * 1024 * 1024;
	/** candidate_document.originalname */
	public static final int MAX_NAME = 255;

	/** The file kinds RMS accepts, with their content type, extensions and first bytes. */
	public enum FileKind {
		PDF("application/pdf", List.of("pdf"), new byte[] { '%', 'P', 'D', 'F', '-' }),
		JPEG("image/jpeg", List.of("jpg", "jpeg"), new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
		PNG("image/png", List.of("png"),
				new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });

		private final String contenttype;
		private final List<String> extensions;
		private final byte[] magic;

		FileKind(String contenttype, List<String> extensions, byte[] magic) {
			this.contenttype = contenttype;
			this.extensions = extensions;
			this.magic = magic;
		}

		public String getContenttype() {
			return contenttype;
		}

		boolean startsTheContent(byte[] content) {
			return content.length >= magic.length && Arrays.equals(content, 0, magic.length, magic, 0, magic.length);
		}

		/** The kind with this extension (any case), or null. */
		static FileKind ofExtension(String extension) {
			for (FileKind kind : values()) {
				if (kind.extensions.contains(extension.toLowerCase(Locale.ROOT))) {
					return kind;
				}
			}
			return null;
		}
	}

	private DocumentRules() {
	}

	/**
	 * The first problem with the file, as a message for the profile page, or null. name is the display name
	 * (displayName), declaredType the browser's Content-Type for the part.
	 */
	public static String problem(String name, String declaredType, byte[] content) {
		if (content == null || content.length == 0) {
			return "Please choose a file.";
		}
		if (content.length > MAX_BYTES) {
			return "The file is larger than 5 MB.";
		}
		FileKind kind = kindOf(name);
		if (kind == null) {
			return "Only PDF, JPG and PNG files can be uploaded.";
		}
		if (!kind.contenttype.equals(baseType(declaredType))) {
			return "The file's type doesn't match its name. Only PDF, JPG and PNG files can be uploaded.";
		}
		if (!kind.startsTheContent(content)) {
			return "The file's content isn't a PDF, JPG or PNG file.";
		}
		return null;
	}

	/** The kind of file the name's extension says, or null. */
	public static FileKind kindOf(String name) {
		if (name == null) {
			return null;
		}
		int dot = name.lastIndexOf('.');
		return dot < 0 ? null : FileKind.ofExtension(name.substring(dot + 1));
	}

	/**
	 * The name to show and to offer on download: the last part of whatever path the browser sent, without control
	 * or invisible formatting characters (such as a right-to-left override that disguises the extension), at most
	 * 255 characters with the extension kept. "document" when nothing is left of it.
	 */
	public static String displayName(String originalname) {
		String name = originalname == null ? "" : originalname;
		name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
		name = name.replaceAll("[\\p{Cc}\\p{Cf}]", "").trim();
		if (name.isEmpty() || name.startsWith(".")) {
			name = "document" + name;
		}
		if (name.length() > MAX_NAME) {
			int dot = name.lastIndexOf('.');
			String extension = dot > 0 && name.length() - dot <= 10 ? name.substring(dot) : "";
			name = name.substring(0, MAX_NAME - extension.length()) + extension;
		}
		return name;
	}

	/** "image/PNG; charset=x" as "image/png". */
	private static String baseType(String declaredType) {
		if (declaredType == null) {
			return "";
		}
		int semicolon = declaredType.indexOf(';');
		return (semicolon < 0 ? declaredType : declaredType.substring(0, semicolon)).trim().toLowerCase(Locale.ROOT);
	}
}
