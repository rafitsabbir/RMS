package rms.service;

import java.util.List;

import rms.model.JobInfo;

public interface JobService {
	public void addJob(JobInfo jobinfo);
	public void updateJob(JobInfo jobinfo);
	public List<JobInfo> getAllJob();
	public List<JobInfo> getOpenJob();
	public JobInfo findJobById(int jobkey);
	public void deleteJob(int jobkey);
}
