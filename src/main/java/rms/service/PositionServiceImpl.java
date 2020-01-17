package rms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rms.dao.PositionDao;
import rms.model.PositionInfo;

@Service
public class PositionServiceImpl implements PositionService {

	PositionDao positiondao;

	@Autowired
	public void setPositionDao(PositionDao positiondao) {
		this.positiondao = positiondao;
	}

	@Override
	public void updatePosition(PositionInfo positioninfo) {
		// TODO Auto-generated method stub
		positiondao.updatePosition(positioninfo);
	}

	@Override
	public void addPosition(PositionInfo positioninfo) {
		// TODO Auto-generated method stub
		positiondao.addPosition(positioninfo);
	}

	@Override
	public List<PositionInfo> getAllPosition() {
		// TODO Auto-generated method stub
		return positiondao.getAllPosition();
	}

	@Override
	public void deletePosition(int positionkey) {
		// TODO Auto-generated method stub
		positiondao.deletePosition(positionkey);
	}

	@Override
	public PositionInfo findPositionById(int positionkey) {
		// TODO Auto-generated method stub
		return positiondao.findPositionById(positionkey);
	}

}
