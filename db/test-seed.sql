-- Synthetic test data for the DAO tests and local dev. Not real people; not for production.
-- The passwords are throwaway test values. U1 and U2 keep legacy plain-text rows (G13); U3-U5 are {bcrypt}
-- hashes, the way RMS stores every new or reset password (of test-only-3, test-only-4 and test-only-5).
-- One user per role (admin.role), plus an inactive one. isinterviewer follows the role (rms.model.Role).

INSERT INTO users (userid, username, password, mustchangepassword) VALUES
	('U1', 'test.admin', 'test-only-1', 0),
	('U2', 'test.interviewer', 'test-only-2', 0),
	('U3', 'test.hr', '{bcrypt}$2a$10$hPPOx1g/z8sRaP7kM/UyfuwFt4bOo/zkT5FSBQgN52a0mXiII8f6q', 0),
	('U4', 'test.manager', '{bcrypt}$2a$10$IzGDPypgejXUNe/5c2eZbeoDCi.OqXeBrgygI7eAN3YaWw/PQT2Oq', 0),
	('U5', 'test.former', '{bcrypt}$2a$10$k9dc9YGFsUw0.DlNIcHOROuo/hK49y16gaVGIQbWpZ39eFD2gHnbS', 0);

INSERT INTO admin (userid, username, isactive, firstname, lastname, email, phone, designation, isinterviewer, role) VALUES
	('U1', 'test.admin', 1, 'Ada', 'Admin', 'admin@example.test', '000-0001', 'HR Manager', 'N', 'SUPER_ADMIN'),
	('U2', 'test.interviewer', 1, 'Ivan', 'Interviewer', 'interviewer@example.test', '000-0002', 'Engineer', 'Y', 'INTERVIEWER'),
	('U3', 'test.hr', 1, 'Hana', 'Recruiter', 'hr@example.test', '000-0003', 'Recruiter', 'Y', 'HR'),
	('U4', 'test.manager', 1, 'Max', 'Manager', 'manager@example.test', '000-0004', 'Engineering Manager', 'Y', 'HIRING_MANAGER'),
	('U5', 'test.former', 0, 'Fay', 'Former', 'former@example.test', '000-0005', 'Engineer', 'Y', 'INTERVIEWER');

INSERT INTO position (positionkey, positionname, isactive) VALUES
	(1, 'SOFTWARE ENGINEER', 1),
	(2, 'QA ENGINEER', 1),
	(3, 'RETIRED ROLE', 0);

INSERT INTO language (languagekey, languagename, isactive) VALUES
	(1, 'JAVA', 1),
	(2, 'PYTHON', 1),
	(3, 'COBOL', 0);

INSERT INTO candidate (candidateid, firstname, lastname, positionkey, languagekey, candidatestatus) VALUES
	('C1', 'Carla', 'Candidate', 1, 1, 'S'),
	('C2', 'Cody', 'Candidate', 2, 2, 'R');

INSERT INTO marks (isactive, interviewerid, candidateid, workexp, techknowledge, leadership, decision,
		probsolving, stress, education, comskill, attitude, personality) VALUES
	(1, 'U2', 'C1', 8, 9, 7, 8, 9, 6, 8, 7, 9, 8),
	(1, 'U2', 'C2', 3, 4, 2, 3, 4, 5, 3, 2, 4, 3);
