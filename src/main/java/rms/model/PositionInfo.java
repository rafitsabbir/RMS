package rms.model;

public class PositionInfo {

	private int positionkey = 0;
	private String positionname = null;

	@Override
	public String toString() {
		return "PositionInfo [positionkey="
				+ positionkey + ", positionname=" + positionname
				+ ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
				+ ", toString()=" + super.toString() + "]";
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
