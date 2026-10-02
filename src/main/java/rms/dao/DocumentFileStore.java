package rms.dao;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * The document files, in the folder named by the environment variable RMS_DOC_DIR, outside the web root. Each file
 * is stored as RMS_DOC_DIR/&lt;first 2 characters&gt;/&lt;32 random hex characters&gt;; the name the user gave
 * is never part of a path (candidate_document.originalname is only shown).
 * <p>
 * Without RMS_DOC_DIR, or when it isn't a writable folder, RMS still starts: this logs an ERROR, and uploads and
 * downloads answer that document storage isn't configured. Tomcat needs read, write and delete rights on the
 * folder; RMS never removes a file except in a Super Admin's permanent delete.
 */
@Repository
public class DocumentFileStore {
	private static final Logger log = LoggerFactory.getLogger(DocumentFileStore.class);

	public static final String ENVIRONMENT_VARIABLE = "RMS_DOC_DIR";

	private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f]{32}");

	private final Path root;

	/** The folder from RMS_DOC_DIR. */
	public DocumentFileStore() {
		this(System.getenv(ENVIRONMENT_VARIABLE));
	}

	/** The folder given; null or blank means none. */
	public DocumentFileStore(String folder) {
		this.root = check(folder);
	}

	/** The folder, or null (logged at ERROR) when it isn't set or isn't a writable absolute folder. */
	private static Path check(String folder) {
		if (folder == null || folder.isBlank()) {
			log.error("{} is not set: document uploads and downloads are off", ENVIRONMENT_VARIABLE);
			return null;
		}
		Path path;
		try {
			path = Path.of(folder.trim());
		} catch (RuntimeException e) {
			log.error("{} is not a valid path: document uploads and downloads are off", ENVIRONMENT_VARIABLE);
			return null;
		}
		if (!path.isAbsolute() || !Files.isDirectory(path) || !Files.isWritable(path)) {
			log.error("{} is not an absolute, existing, writable folder: document uploads and downloads are off",
					ENVIRONMENT_VARIABLE);
			return null;
		}
		return path.normalize();
	}

	public boolean isConfigured() {
		return root != null;
	}

	/** Writes the content under a new random name and returns that name. */
	public String store(byte[] content) throws IOException {
		requireConfigured();
		String storedname = UUID.randomUUID().toString().replace("-", "");
		Path target = pathOf(storedname);
		Files.createDirectories(target.getParent());
		// Written beside the target first, so a half-written file never has the final name
		Path temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
		try {
			Files.write(temporary, content);
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, target);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
		return storedname;
	}

	/** The stored file, or null when the name isn't one RMS makes or the file isn't there. */
	public Path find(String storedname) {
		requireConfigured();
		if (!isStoredName(storedname)) {
			return null;
		}
		Path path = pathOf(storedname);
		return Files.isRegularFile(path) ? path : null;
	}

	/** Removes the stored file, if it is there. A name RMS doesn't make has no file to remove. */
	public void delete(String storedname) throws IOException {
		requireConfigured();
		if (isStoredName(storedname)) {
			Files.deleteIfExists(pathOf(storedname));
		}
	}

	static boolean isStoredName(String storedname) {
		return storedname != null && STORED_NAME.matcher(storedname).matches();
	}

	/** Only for names that passed isStoredName: hex only, so the path can't leave the folder. */
	private Path pathOf(String storedname) {
		return root.resolve(storedname.substring(0, 2)).resolve(storedname);
	}

	private void requireConfigured() {
		if (root == null) {
			throw new IllegalStateException(ENVIRONMENT_VARIABLE + " is not configured");
		}
	}
}
