package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.JobDao;
import rms.model.JobInfo;

@Service
public class JobServiceImpl implements JobService {

	JobDao jobdao;

	@Autowired
	public void setJobDao(JobDao jobdao) {
		this.jobdao = jobdao;
	}

	@Override
	public void addJob(JobInfo jobinfo) {
		jobdao.addJob(jobinfo);
	}

	@Override
	public void updateJob(JobInfo jobinfo) {
		jobdao.updateJob(jobinfo);
	}

	@Override
	public List<JobInfo> getAllJob() {
		return jobdao.getAllJob();
	}

	@Override
	public List<JobInfo> getOpenJob() {
		return jobdao.getOpenJob();
	}

	@Override
	public JobInfo findJobById(int jobkey) {
		return jobdao.findJobById(jobkey);
	}

	@Override
	public void deleteJob(int jobkey) {
		jobdao.deleteJob(jobkey);
	}

}
