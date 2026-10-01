package rms.service;

import java.util.List;

import rms.model.LanguageInfo;

public interface LanguageService {

	public List<LanguageInfo> getAllLanguage();

	public boolean updateLanguage(LanguageInfo languageinfo);

	public boolean addLanguage(LanguageInfo languageinfo);

	public LanguageInfo findLanguageById(int languagekey);

	public void deleteLanguage(int languagekey);
}
