package rms.dao;

import java.util.List;

import rms.model.MarksInfo;

public interface MarksDao {

	public void saveMarks(MarksInfo marksinfo);
	public int getFullmarks(MarksInfo marksinfo);
	public List<MarksInfo>  getAllMarksByInterviewer(String userid);
	public List<MarksInfo>  getAllMarksByAdmin();
}
