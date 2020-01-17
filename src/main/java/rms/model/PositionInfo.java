package rms.model;

public class PositionInfo {

	private int isactive = 0;
	private int positionkey = 0;
	private String positionname = null;

	@Override
	public String toString() {
		return "PositionInfo [isactive=" + isactive + ", positionkey="
				+ positionkey + ", positionname=" + positionname
				+ ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
				+ ", toString()=" + super.toString() + "]";
	}

	public int getIsactive() {
		return isactive;
	}

	public void setIsactive(int isactive) {
		this.isactive = isactive;
	}

	public int getPositionkey() {
		return positionkey;
	}

	public void setPositionkey(int positionkey) {
		this.positionkey = positionkey;
	}

	public String getPositionname() {
		return positionname;
	}

	public void setPositionname(String positionname) {
		this.positionname = positionname;
	}

}
