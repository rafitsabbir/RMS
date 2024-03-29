package rms.service;

import java.util.List;

import rms.model.LanguageInfo;

public interface LanguageService {

	public List<LanguageInfo> getAllLanguage();

	public void updateLanguage(LanguageInfo languageinfo);

	public void addLanguage(LanguageInfo languageinfo);

	public LanguageInfo findLanguageById(int languagekey);

	public void deleteLanguage(int languagekey);
}
