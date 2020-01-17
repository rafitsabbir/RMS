package rms.service;

import java.util.List;

import rms.model.PositionInfo;

public interface PositionService {
	public void updatePosition(PositionInfo positioninfo);
	public void addPosition(PositionInfo positioninfo);
	public List<PositionInfo> getAllPosition();
	public void deletePosition(int positionkey);
	public PositionInfo findPositionById(int positionkey);

}
