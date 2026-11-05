package rms.dao;

import java.util.List;

import rms.model.JobInfo;

public interface JobDao {

	public void addJob(JobInfo jobinfo);

	public void updateJob(JobInfo jobinfo);

	/** Active jobs, open and closed. */
	public List<JobInfo> getAllJob();

	/** Active open jobs: the ones a candidate can be linked to. */
	public List<JobInfo> getOpenJob();

	/** An active job, open or closed, or null. */
	public JobInfo findJobById(int jobkey);

	/** Active jobs with their pipeline (F6): candidates per stage, rejected ones, and the last hire's date. */
	public List<JobInfo> getJobsWithPipeline();
	/** Soft delete: sets isactive=0; candidates linked to it keep the link. */
	public void deleteJob(int jobkey);
}
