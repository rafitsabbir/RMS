package rms.model;

public class MarksInfo {

	private int isactive = 0;
	private String interviewerid = null;
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
	private String position = null;
	private String language = null;
	private String candidatestatus = null;

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

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCandidateStatus() {
		return candidatestatus;
	}

	public void setCandidateStatus(String candidatestatus) {
		this.candidatestatus = candidatestatus;
	}

	@Override
	public String toString() {
		return "MarksInfo [isactive=" + isactive + ", interviewerid="
				+ interviewerid + ", candidateid=" + candidateid + ", workexp="
				+ workexp + ", techknowledge=" + techknowledge
				+ ", leadership=" + leadership + ", decision=" + decision
				+ ", probsolving=" + probsolving + ", stress=" + stress
				+ ", education=" + education + ", comskill=" + comskill
				+ ", attitude=" + attitude + ", personality=" + personality
				+ ", position=" + position + ", language=" + language
				+ ", candidatestatus=" + candidatestatus + "]";
	}

}
