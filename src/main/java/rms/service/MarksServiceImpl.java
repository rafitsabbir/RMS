package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.MarksDao;
import rms.model.MarksInfo;

@Service
public class MarksServiceImpl implements MarksService {
	MarksDao marksdao;

	@Autowired
	public void setMarksDao(MarksDao marksdao) {
		this.marksdao = marksdao;
	}

	public void saveMarks(MarksInfo marksinfo) {
		// TODO Auto-generated method stub
		marksdao.saveMarks(marksinfo);
	}

	public List<MarksInfo> getAllMarksByInterviewer(String userid) {
		// TODO Auto-generated method stub
		return marksdao.getAllMarksByInterviewer(userid);
	}

	public List<MarksInfo> getAllMarksByAdmin() {
		// TODO Auto-generated method stub
		return marksdao.getAllMarksByAdmin();
	}

}
