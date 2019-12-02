package rms.dao;

import java.util.List;

import rms.model.LanguageInfo;

public interface LanguageDao {	

	public List<LanguageInfo> getAllLanguage();
	
	public void addLanguage(LanguageInfo languageinfo);
	
	public LanguageInfo findLanguageById(int languagekey);
	
	public void updateLanguage(LanguageInfo languageinfo);
	
	public void deleteLanguage(int languagekey);

}
