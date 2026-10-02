package rms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One evaluation: a row of marks, one interviewer's 10 scores for one candidate, with comments (Phase 3).
 * interviewername, intervieweractive and assigned are read through subqueries for display. Also the evaluation
 * form: only the scores and comments bind (EvaluationController); the interviewer comes from the session.
 */
public class MarksInfo {

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private int markkey = 0;
	private int isactive = 0;
	private String interviewerid = null;
	private String interviewername = null;
	/** Whether the interviewer's account is active (admin.isactive = 1). */
	private boolean intervieweractive = true;
	/** Whether the interviewer is still assigned to the candidate. */
	private boolean assigned = false;
	private String candidateid = null;
	private int workexp = 0;
	private int techknowledge = 0;
	private int leadership = 0;
	private int decision = 0;
	private int probsolving = 0;
	private int stress = 0;
	private int education = 0;
	private int comskill = 0;
	private int attitude = 0;
	private int personality = 0;
	private String comments = null;
	private String workexpcomment = null;
	private String techknowledgecomment = null;
	private String leadershipcomment = null;
	private String decisioncomment = null;
	private String probsolvingcomment = null;
	private String stresscomment = null;
	private String educationcomment = null;
	private String comskillcomment = null;
	private String attitudecomment = null;
	private String personalitycomment = null;
	private LocalDateTime createdat = null;
	private LocalDateTime updatedat = null;

	/** The scores in Criterion order. */
	public List<Integer> getScores() {
		List<Integer> scores = new ArrayList<Integer>();
		for (Criterion criterion : Criterion.values()) {
			scores.add(getScore(criterion));
		}
		return scores;
	}

	/** The per-criterion comments in Criterion order (null where there is none). */
	public List<String> getCriterioncomments() {
		List<String> list = new ArrayList<String>();
		for (Criterion criterion : Criterion.values()) {
			list.add(getComment(criterion));
		}
		return list;
	}

	/** The sum of the 10 scores, at most 100. */
	public int getTotal() {
		int total = 0;
		for (Criterion criterion : Criterion.values()) {
			total += getScore(criterion);
		}
		return total;
	}

	/** When it was last saved, for display ("" for evaluations from before Phase 3, which have no dates). */
	public String getUpdatedlabel() {
		LocalDateTime when = updatedat != null ? updatedat : createdat;
		return when == null ? "" : when.format(LABEL);
	}

	public int getScore(Criterion criterion) {
		switch (criterion) {
		case WORKEXP:
			return workexp;
		case TECHKNOWLEDGE:
			return techknowledge;
		case LEADERSHIP:
			return leadership;
		case DECISION:
			return decision;
		case PROBSOLVING:
			return probsolving;
		case STRESS:
			return stress;
		case EDUCATION:
			return education;
		case COMSKILL:
			return comskill;
		case ATTITUDE:
			return attitude;
		case PERSONALITY:
			return personality;
		default:
			throw new IllegalArgumentException(criterion.name());
		}
	}

	public void setScore(Criterion criterion, int score) {
		switch (criterion) {
		case WORKEXP:
			workexp = score;
			break;
		case TECHKNOWLEDGE:
			techknowledge = score;
			break;
		case LEADERSHIP:
			leadership = score;
			break;
		case DECISION:
			decision = score;
			break;
		case PROBSOLVING:
			probsolving = score;
			break;
		case STRESS:
			stress = score;
			break;
		case EDUCATION:
			education = score;
			break;
		case COMSKILL:
			comskill = score;
			break;
		case ATTITUDE:
			attitude = score;
			break;
		case PERSONALITY:
			personality = score;
			break;
		default:
			throw new IllegalArgumentException(criterion.name());
		}
	}

	public String getComment(Criterion criterion) {
		switch (criterion) {
		case WORKEXP:
			return workexpcomment;
		case TECHKNOWLEDGE:
			return techknowledgecomment;
		case LEADERSHIP:
			return leadershipcomment;
		case DECISION:
			return decisioncomment;
		case PROBSOLVING:
			return probsolvingcomment;
		case STRESS:
			return stresscomment;
		case EDUCATION:
			return educationcomment;
		case COMSKILL:
			return comskillcomment;
		case ATTITUDE:
			return attitudecomment;
		case PERSONALITY:
			return personalitycomment;
		default:
			throw new IllegalArgumentException(criterion.name());
		}
	}

	public void setComment(Criterion criterion, String comment) {
		switch (criterion) {
		case WORKEXP:
			workexpcomment = comment;
			break;
		case TECHKNOWLEDGE:
			techknowledgecomment = comment;
			break;
		case LEADERSHIP:
			leadershipcomment = comment;
			break;
		case DECISION:
			decisioncomment = comment;
			break;
		case PROBSOLVING:
			probsolvingcomment = comment;
			break;
		case STRESS:
			stresscomment = comment;
			break;
		case EDUCATION:
			educationcomment = comment;
			break;
		case COMSKILL:
			comskillcomment = comment;
			break;
		case ATTITUDE:
			attitudecomment = comment;
			break;
		case PERSONALITY:
			personalitycomment = comment;
			break;
		default:
			throw new IllegalArgumentException(criterion.name());
		}
	}

	/** The keys and scores only: comments and names can be personal data. */
	@Override
	public String toString() {
		return "MarksInfo [markkey=" + markkey + ", interviewerid=" + interviewerid + ", candidateid=" + candidateid
				+ ", scores=" + getScores() + "]";
	}

	public int getMarkkey() {
		return markkey;
	}

	public void setMarkkey(int markkey) {
		this.markkey = markkey;
	}

	public int getIsactive() {
		return isactive;
	}

	public void setIsactive(int isactive) {
		this.isactive = isactive;
	}

	public String getInterviewerid() {
		return interviewerid;
	}

	public void setInterviewerid(String interviewerid) {
		this.interviewerid = interviewerid;
	}

	public String getInterviewername() {
		return interviewername;
	}

	public void setInterviewername(String interviewername) {
		this.interviewername = interviewername;
	}

	public boolean isIntervieweractive() {
		return intervieweractive;
	}

	public void setIntervieweractive(boolean intervieweractive) {
		this.intervieweractive = intervieweractive;
	}

	public boolean isAssigned() {
		return assigned;
	}

	public void setAssigned(boolean assigned) {
		this.assigned = assigned;
	}

	public String getCandidateid() {
		return candidateid;
	}

	public void setCandidateid(String candidateid) {
		this.candidateid = candidateid;
	}

	public int getWorkexp() {
		return workexp;
	}

	public void setWorkexp(int workexp) {
		this.workexp = workexp;
	}

	public int getTechknowledge() {
		return techknowledge;
	}

	public void setTechknowledge(int techknowledge) {
		this.techknowledge = techknowledge;
	}

	public int getLeadership() {
		return leadership;
	}

	public void setLeadership(int leadership) {
		this.leadership = leadership;
	}

	public int getDecision() {
		return decision;
	}

	public void setDecision(int decision) {
		this.decision = decision;
	}

	public int getProbsolving() {
		return probsolving;
	}

	public void setProbsolving(int probsolving) {
		this.probsolving = probsolving;
	}

	public int getStress() {
		return stress;
	}

	public void setStress(int stress) {
		this.stress = stress;
	}

	public int getEducation() {
		return education;
	}

	public void setEducation(int education) {
		this.education = education;
	}

	public int getComskill() {
		return comskill;
	}

	public void setComskill(int comskill) {
		this.comskill = comskill;
	}

	public int getAttitude() {
		return attitude;
	}

	public void setAttitude(int attitude) {
		this.attitude = attitude;
	}

	public int getPersonality() {
		return personality;
	}

	public void setPersonality(int personality) {
		this.personality = personality;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getWorkexpcomment() {
		return workexpcomment;
	}

	public void setWorkexpcomment(String workexpcomment) {
		this.workexpcomment = workexpcomment;
	}

	public String getTechknowledgecomment() {
		return techknowledgecomment;
	}

	public void setTechknowledgecomment(String techknowledgecomment) {
		this.techknowledgecomment = techknowledgecomment;
	}

	public String getLeadershipcomment() {
		return leadershipcomment;
	}

	public void setLeadershipcomment(String leadershipcomment) {
		this.leadershipcomment = leadershipcomment;
	}

	public String getDecisioncomment() {
		return decisioncomment;
	}

	public void setDecisioncomment(String decisioncomment) {
		this.decisioncomment = decisioncomment;
	}

	public String getProbsolvingcomment() {
		return probsolvingcomment;
	}

	public void setProbsolvingcomment(String probsolvingcomment) {
		this.probsolvingcomment = probsolvingcomment;
	}

	public String getStresscomment() {
		return stresscomment;
	}

	public void setStresscomment(String stresscomment) {
		this.stresscomment = stresscomment;
	}

	public String getEducationcomment() {
		return educationcomment;
	}

	public void setEducationcomment(String educationcomment) {
		this.educationcomment = educationcomment;
	}

	public String getComskillcomment() {
		return comskillcomment;
	}

	public void setComskillcomment(String comskillcomment) {
		this.comskillcomment = comskillcomment;
	}

	public String getAttitudecomment() {
		return attitudecomment;
	}

	public void setAttitudecomment(String attitudecomment) {
		this.attitudecomment = attitudecomment;
	}

	public String getPersonalitycomment() {
		return personalitycomment;
	}

	public void setPersonalitycomment(String personalitycomment) {
		this.personalitycomment = personalitycomment;
	}

	public LocalDateTime getCreatedat() {
		return createdat;
	}

	public void setCreatedat(LocalDateTime createdat) {
		this.createdat = createdat;
	}

	public LocalDateTime getUpdatedat() {
		return updatedat;
	}

	public void setUpdatedat(LocalDateTime updatedat) {
		this.updatedat = updatedat;
	}

}
