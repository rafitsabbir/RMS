package rms.dao;

import java.util.List;

import rms.model.LanguageInfo;

public interface LanguageDao {	

	public List<LanguageInfo> getAllLanguage();
	
	public boolean addLanguage(LanguageInfo languageinfo);
	
	public LanguageInfo findLanguageById(int languagekey);
	
	public boolean updateLanguage(LanguageInfo languageinfo);
	
	public void deleteLanguage(int languagekey);

}
