-- Synthetic test data for the DAO tests and local dev. Not real people; not for production.
-- The passwords are throwaway test values (login stores plain text today, G13).

INSERT INTO users (userid, username, password) VALUES
	('U1', 'test.admin', 'test-only-1'),
	('U2', 'test.interviewer', 'test-only-2');

INSERT INTO admin (userid, username, isactive, firstname, lastname, email, phone, designation, isinterviewer) VALUES
	('U1', 'test.admin', 1, 'Ada', 'Admin', 'admin@example.test', '000-0001', 'HR Manager', 'N'),
	('U2', 'test.interviewer', 1, 'Ivan', 'Interviewer', 'interviewer@example.test', '000-0002', 'Engineer', 'Y');

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
