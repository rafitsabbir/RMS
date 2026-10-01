package rms.dao;

import java.util.List;

import rms.model.PositionInfo;

public interface PositionDao {

	public boolean updatePosition(PositionInfo fositioninfo);

	public boolean addPosition(PositionInfo fositioninfo);

	public List<PositionInfo> getAllPosition();

	public void deletePosition(int positionkey);

	public PositionInfo findPositionById(int positionkey);
}
