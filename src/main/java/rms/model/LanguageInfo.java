package rms.model;

public class LanguageInfo {

	private int isactive = 0;
	private int languagekey = 0;
	private String languagename = null;

	@Override
	public String toString() {
		return "LanguageInfo [isactive=" + isactive + ", languagekey="
				+ languagekey + ", languagename=" + languagename + "]";
	}

	public int getIsactive() {
		return isactive;
	}

	public void setIsactive(int isactive) {
		this.isactive = isactive;
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
