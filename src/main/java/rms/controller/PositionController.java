package rms.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import rms.model.PositionInfo;
import rms.service.PositionService;

@Controller
@RequestMapping(value = "/")
public class PositionController {

	@Autowired
	PositionService positionservice;

	@RequestMapping(value = "/createposition", method = RequestMethod.GET)
	public ModelAndView createPosition() {
		ModelAndView mv = new ModelAndView("createposition");
		PositionInfo positioninfo = new PositionInfo();
		mv.addObject("positioninfo", positioninfo);

		return mv;
	}

	@RequestMapping(value = "/saveposition", method = RequestMethod.POST)
	public ModelAndView save(
			@ModelAttribute("positioninfo") PositionInfo positioninfo) {

		if (positioninfo != null && positioninfo.getPositionkey() > 0) {
			positionservice.updatePosition(positioninfo);
		} else {
			positionservice.addPosition(positioninfo);
		}

		return new ModelAndView("redirect:/viewpositionlist");

	}

	@RequestMapping(value = "/viewpositionlist", method = RequestMethod.GET)
	public ModelAndView viewPosition() {
		ModelAndView mv = new ModelAndView("viewposition");
		List<PositionInfo> positionlist = new ArrayList<PositionInfo>();
		positionlist = positionservice.getAllPosition();
		mv.addObject("positionlist", positionlist);
		return mv;
	}

	@RequestMapping(value = "/updateposition/{positionkey}", method = RequestMethod.GET)
	public ModelAndView update(@PathVariable("positionkey") int positionkey) {
		ModelAndView mv = new ModelAndView("createposition");

		PositionInfo positioninfo = positionservice
				.findPositionById(positionkey);
		mv.addObject("positioninfo", positioninfo);

		return mv;

	}

	@RequestMapping(value = "/deleteposition/{positionkey}", method = RequestMethod.GET)
	public ModelAndView delete(@PathVariable("positionkey") int positionkey) {

		positionservice.deletePosition(positionkey);

		return new ModelAndView("redirect:/viewpositionlist");
	}
}
