package rms.service;

import java.util.List;

import rms.model.MarksInfo;

public interface MarksService {
	public void saveMarks(MarksInfo marksinfo);
	public List<MarksInfo>  getAllMarksByInterviewer(String userid);
	public List<MarksInfo>  getAllMarksByAdmin();

}
