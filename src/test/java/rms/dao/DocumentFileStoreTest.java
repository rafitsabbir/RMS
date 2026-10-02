package rms.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The document files in RMS_DOC_DIR, on a temporary folder. No Docker. */
class DocumentFileStoreTest {

	@TempDir
	Path folder;

	@Test
	void storesUnderARandomNameInATwoCharacterSubfolder() throws IOException {
		DocumentFileStore store = new DocumentFileStore(folder.toString());

		String first = store.store("%PDF-1".getBytes());
		String second = store.store("%PDF-2".getBytes());

		assertThat(first).matches("[0-9a-f]{32}").isNotEqualTo(second);
		Path path = folder.resolve(first.substring(0, 2)).resolve(first);
		assertThat(path).hasBinaryContent("%PDF-1".getBytes());
		assertThat(store.find(first)).isEqualTo(path);
		// No temporary file is left beside it
		try (var files = Files.list(path.getParent())) {
			assertThat(files.filter(p -> p.getFileName().toString().endsWith(".tmp"))).isEmpty();
		}
	}

	@Test
	void findOnlyResolvesNamesRmsMakes() throws IOException {
		DocumentFileStore store = new DocumentFileStore(folder.toString());
		Files.writeString(folder.resolve("secret.txt"), "x");

		for (String name : new String[] { "../secret.txt", "secret.txt", "..", "", null,
				"A0000000000000000000000000000001", "a000000000000000000000000000001/", "a0/../../secret.txt" }) {
			assertThat(store.find(name)).as(String.valueOf(name)).isNull();
		}
		// A valid name with no file
		assertThat(store.find("a0000000000000000000000000000001")).isNull();
	}

	@Test
	void deleteRemovesTheFileAndIgnoresMissingOrForeignNames() throws IOException {
		DocumentFileStore store = new DocumentFileStore(folder.toString());
		String name = store.store("%PDF-1".getBytes());
		Files.writeString(folder.resolve("secret.txt"), "x");

		store.delete(name);
		store.delete(name);
		store.delete("../secret.txt");

		assertThat(store.find(name)).isNull();
		assertThat(folder.resolve("secret.txt")).exists();
	}

	@Test
	void notConfiguredWithoutAUsableFolder() throws IOException {
		Path file = Files.writeString(folder.resolve("a-file"), "x");
		for (String value : new String[] { null, "", "  ", "relative/folder", folder.resolve("missing").toString(),
				file.toString() }) {
			assertThat(new DocumentFileStore(value).isConfigured()).as(String.valueOf(value)).isFalse();
		}
		assertThat(new DocumentFileStore(folder.toString()).isConfigured()).isTrue();
	}

	@Test
	void notConfiguredStoreRefusesToWork() {
		DocumentFileStore store = new DocumentFileStore((String) null);

		assertThatThrownBy(() -> store.store(new byte[] { 1 })).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> store.find("a0000000000000000000000000000001"))
				.isInstanceOf(IllegalStateException.class);
	}
}
