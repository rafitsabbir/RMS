package rms.model;

public class LanguageInfo {

	private int languagekey = 0;
	private String languagename = null;

	@Override
	public String toString() {
		return "LanguageInfo [languagekey="
				+ languagekey + ", languagename=" + languagename + "]";
	}

	public int getLanguagekey() {
		return languagekey;
	}

	public void setLanguagekey(int languagekey) {
		this.languagekey = languagekey;
	}

	public String getLanguagename() {
		return languagename;
	}

	public void setLanguagename(String languagename) {
		this.languagename = languagename;
	}
}
