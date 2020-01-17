package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.LanguageDao;
import rms.model.LanguageInfo;

@Service
public class LanguageServiceImpl implements LanguageService {
	
	LanguageDao languagedao;

	@Autowired
	public void setLanguagedao(LanguageDao languagedao) {
		this.languagedao = languagedao;
	}	

	@Override
	public void updateLanguage(LanguageInfo languageinfo) {
		// TODO Auto-generated method stub
		languagedao.updateLanguage(languageinfo);
	}

	@Override
	public void addLanguage(LanguageInfo languageinfo) {
		// TODO Auto-generated method stub
		languagedao.addLanguage(languageinfo);
		
	}

	@Override
	public List<LanguageInfo> getAllLanguage() {
		// TODO Auto-generated method stub
		return languagedao.getAllLanguage();
	}

	@Override
	public LanguageInfo findLanguageById(int languagekey) {
		// TODO Auto-generated method stub
		return languagedao.findLanguageById(languagekey);
	}

	@Override
	public void deleteLanguage(int languagekey) {
		// TODO Auto-generated method stub
		languagedao.deleteLanguage(languagekey);
	}

}
