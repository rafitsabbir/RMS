# Flow: Candidate Profile and Documents

Purpose: Traces the candidate profile page and the candidate documents: upload, replace, download, delete and the Super Admin's permanent delete.
Last updated: 2026-10-02 (Phase 3: interviewers download their assigned candidates' documents). 2026-10-02 (Phase 2 of the roles plan: first version; migration 004, `RMS_DOC_DIR`)
Read this when: you're changing the candidate profile, document rules or storage, or anything that reads or serves an uploaded file.

Evidence: `rms/controller/CandidateController.java` (`profile`), `rms/controller/DocumentController.java`, `rms/service/DocumentServiceImpl.java`, `rms/service/DocumentRules.java`, `rms/dao/DocumentDaoImpl.java`, `rms/dao/DocumentFileStore.java`, `rms/model/DocumentType.java`, `DocumentInfo.java`, `DocumentUpload.java`, `rms/config/WebInitializer.java` (multipart limits), `rms/config/SecurityConfig.java` (access, `tooLargeUpload`), `WEB-INF/jsp/candidateprofile.jsp`, `viewcandidate.jsp` (completeness column). Tests: `DocumentRulesTest`, `DocumentFileStoreTest`, `DocumentServiceImplTest`, `DocumentControllerTest`, `CandidateControllerTest` (profile), `SecurityConfigTest` (access, oversize upload), `DocumentDaoImplTest` (needs Docker), `SmokeTest.jobAndProfilePagesRender`.

```mermaid
sequenceDiagram
  participant B as Browser
  participant S as SecurityConfig
  participant C as DocumentController
  participant V as DocumentServiceImpl
  participant F as DocumentFileStore (RMS_DOC_DIR)
  participant D as DocumentDaoImpl
  participant DB as MySQL candidate_document
  B->>S: POST /uploaddocument?candidateid=C1 (multipart: doctype, file, title, issuer, issueyear, _csrf)
  alt over 5 MB (file) or 6 MB (request): the container refuses the parts
    S-->>B: redirect /viewcandidate?candidateid=C1&toolarge ("The file is larger than 5 MB.")
  else not Super Admin or HR
    S-->>B: HTTP 403
  end
  S->>C: upload
  C->>V: upload(DocumentUpload, acting userid)
  V->>V: storage configured? kind? title/issuer/year? DocumentRules: extension, declared type, magic bytes, size
  V->>D: countDocuments (a 6th professional certificate is refused here)
  V->>F: store(bytes) -> RMS_DOC_DIR/ab/ab12...(32 random hex)
  V->>D: addDocument(row, maxActive) in one transaction
  D->>DB: single-slot: UPDATE active row of the kind SET isactive=0, deletedby, deletedat
  D->>DB: INSERT candidate_document (..., uploadedat=now())
  Note over V,F: refused by the insert or a database error: the new file is removed again
  C-->>B: redirect /viewcandidate?candidateid=C1 + flash message
  B->>C: GET /downloaddocument/7 (staff)
  C->>V: findDocument (active, of an active candidate), findFile
  C-->>B: the file, Content-Disposition: attachment; filename*=UTF-8''..., X-Content-Type-Options: nosniff
```

## Endpoints
| Action | URL | Who (`SecurityConfig`) | Handler |
|---|---|---|---|
| Profile | `GET /viewcandidate?candidateid=…` (optional `doctype=` preselects the upload kind; `toolarge` shows the size message) | Super Admin, HR, Hiring Manager; Interviewers for assigned candidates (Phase 3) | `CandidateController.profile` → `candidateprofile.jsp`; 404 for a missing or deleted candidate |
| Upload / replace | `POST /uploaddocument` (multipart; `candidateid` in the URL and the body) | Super Admin, HR | `DocumentController.upload` → redirect to the profile with `documentMessage` or `documentError` |
| Download | `GET /downloaddocument/{documentkey}` | Super Admin, HR, Hiring Manager; Interviewers for assigned candidates (Phase 3) | `DocumentController.download`; 404 for an unknown or inactive document, a deleted candidate or a missing file; 503 without storage |
| Delete (soft) | `POST /deletedocument/{documentkey}` | Super Admin, HR | `DocumentController.delete` → redirect to the profile |
| Permanent delete | `POST /purgedocuments` (`candidateid`, `reason`) | Super Admin | `DocumentController.purge` → redirect to the profile; for a deleted candidate → redirect to `GET /viewdeletedcandidates` (the Deleted Candidates page lists them, [reports-activity.md](reports-activity.md)) |

Since Phase 3 interviewers can open the profile and download the documents of candidates assigned to them (checked in `CandidateController.profile` and `DocumentController.download`; 403 and a WARN with keys otherwise), and the evaluation form lists the documents too. They never see the upload, delete or permanent-delete controls.

## The profile page (`candidateprofile.jsp`)
- **Details:** e-mail, phone, position, language, job (with "open", "closed" or "deleted"), source, applied date, status.
- **Documents**, grouped by kind in `DocumentType` order. A kind without a document shows "Not uploaded" (the CV: "Missing") and an Upload link; each document shows its name (the download link), size, the professional certificate's title, issuer and year, when and by whom it was uploaded, and Replace (single-slot kinds) and Delete buttons for Super Admin and HR. A document of a kind this version doesn't know isn't shown.
- **Completeness:** "CV ✓ · n/6 documents" in the header and on the Candidates list (`viewcandidate.jsp`, column Documents): CV present or not, and how many of the six single-slot kinds have an active document. Only the CV is required (decision G of the plan); professional certificates are counted separately ("2 professional").
- **Upload form** (Super Admin and HR, only when storage is configured): kind, file, and title, issuer and year for professional certificates. The candidate ID is only in the form's URL: a hidden field as well would reach the server as "C1,C1". `rms.js` stops a file over 5 MB before it is sent (`data-rms-maxbytes`); the server checks again. The Replace and Upload links reload the page with that kind preselected (`?doctype=…#upload`); no JavaScript.
- **Permanently delete documents** (Super Admin only): a reason field and a confirm prompt.
- **Interviewers, evaluations and decisions:** a "coming in the next release" card until Phase 3.
- Without `RMS_DOC_DIR` the page says so ("Document storage isn't configured…") and hides the upload form.

## Rules
- **Kinds** (`DocumentType`): CV, SSC, HSC, BSc / Honours, Masters and PhD certificates hold one active document each; uploading again **replaces** it: the old row is soft-deleted (`deletedby`, `deletedat`) and its file kept. Professional certificates: up to 5 active, each with a required title (at most 100 characters), an optional issuer (at most 100) and an optional year (1950 to this year). A sixth is refused: "This candidate already has 5 professional certificates. Delete one before adding another."
- **Files** (`DocumentRules`): PDF, JPG/JPEG or PNG, at most 5 MB, not empty. The name's extension, the browser's declared content type and the file's first bytes (`%PDF-`, `FF D8 FF`, `89 50 4E 47 0D 0A 1A 0A`) must all agree, or the upload is refused with a message. The stored content type is the one the checks agreed on, not the browser's text.
- **Names:** the name the browser sent is only shown and offered on download. It's cut to its last path part, stripped of control and invisible formatting characters (such as a right-to-left override), and shortened to 255 characters keeping the extension (`DocumentRules.displayName`). It's never part of a path.
- **Order of work** (`DocumentServiceImpl.upload`): checks, then a count (a sixth certificate is refused before anything is written), then the file, then the row in one transaction that first locks the candidate's row (`SELECT … FOR UPDATE`, so uploads for one candidate run one after the other, also across Tomcat nodes; `synchronized` as well, for one node if `candidate` isn't InnoDB). If the insert refuses (the limit was reached meanwhile) or fails, the new file is removed again.
- **Delete** is soft: the row gets `isactive = 0`, `deletedby`, `deletedat`, and the file stays until a permanent delete. HR's delete never removes a file. A document deleted meanwhile by someone else gives "The document was already deleted."

## Storage (`DocumentFileStore`)
- Files live in the folder named by the environment variable **`RMS_DOC_DIR`**, outside the web root, as `RMS_DOC_DIR/<first 2 characters>/<32 random hex characters>` (`UUID`). They're written to a temporary file beside the target and then moved, so a half-written file never has the final name.
- A stored name must match `[0-9a-f]{32}` before it is turned into a path, so a row can't point outside the folder.
- **Without `RMS_DOC_DIR`**, or when it isn't an absolute, existing, writable folder, RMS still starts: one ERROR is logged at startup, uploads and the permanent delete answer "Document storage isn't configured…", and downloads answer HTTP 503.
- Tomcat needs read, write and delete rights on the folder. The database account still needs only SELECT, INSERT and UPDATE. Ops details: [build-run.md](../build-run.md) ("Document storage").

## Downloads
- Only through `GET /downloaddocument/{key}`; there is no static URL for the folder.
- `Content-Disposition: attachment` with the original name (RFC 6266 `filename*`, UTF-8), the stored content type, `X-Content-Type-Options: nosniff`, and Spring Security's `Cache-Control: no-store`.
- A row whose file is missing (for example the seed's metadata-only rows) gives 404 and a WARN with the document key.

## Permanent delete (Super Admin)
- `POST /purgedocuments` with a required reason (at most 255 characters), after a confirm prompt.
- Removes the files of **all** the candidate's documents whose file is still there: active, replaced and deleted ones (`getUnpurgedDocuments`: `purgedat IS NULL`). Each row is marked after its file is gone: `isactive = 0`, `purgedby`, `purgedat`, `purgereason` (and `deletedby`/`deletedat` if it wasn't deleted before). Rows are never deleted.
- A file that can't be removed keeps its row unmarked, so a later purge tries again; the page says how many were kept. If the file is removed but the row can't be marked (a database error), that counts as not done too; a later purge marks it.
- **RMS never deletes documents automatically.** Only this action removes files.
- Reachable only from the profile, so only for active candidates: purge before deleting a candidate (open question #28).

## Over-size uploads
- `WebInitializer.customizeRegistration`: the container accepts at most 5 MB per file and 6 MB per request.
- Over that, the container doesn't read the form at all, so its CSRF token is missing too. `SecurityConfig.refuse` recognises that case (`tooLargeUpload`: a multipart POST to `/uploaddocument` whose parts failed with a size error) and redirects to the profile with "The file is larger than 5 MB.", using the candidate ID from the form's URL. Nothing is changed. Any other missing token is still a 403.
- Tomcat swallows at most 2 MB (`maxSwallowSize`) of a refused body; a much larger upload can end in a reset connection instead of the message. Ops can raise `maxSwallowSize` on the connector (open question #28).

## Logging
Only keys and counts: "Document {key} uploaded by {userid}", "deleted by", "downloaded by", "Documents of candidate {id} deleted permanently by {userid}: {count}". No file names, titles, issuers or reasons.

## Known limits
- The kind limits are checked in code, not by the database; across Tomcat nodes they rely on the row lock, which needs `candidate` to be InnoDB (migration 004, open question #28). `storedname` is unique in the database.
- No virus scan: recommended for production, not built ([build-run.md](../build-run.md)).
- Upload times use the database server's clock (`now()`).
- File names with characters outside ISO-8859-1 depend on how the browser encodes them on these ISO-8859-1 pages (G34); RMS only shows them.

Known gaps: G1 (candidates, extended), G4 (jobs, [jobs.md](jobs.md)), G22 (inferred schema: `DocumentDaoImplTest` hasn't run against Docker yet).
