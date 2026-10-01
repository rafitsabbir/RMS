package rms.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;
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

		String name = positioninfo.getPositionname();
		if (name == null || name.trim().isEmpty()) {
			return form(positioninfo, "Please enter a position name.");
		}

		boolean saved;
		if (positioninfo.getPositionkey() > 0) {
			saved = positionservice.updatePosition(positioninfo);
		} else {
			saved = positionservice.addPosition(positioninfo);
		}
		if (!saved) {
			return form(positioninfo, "Position " + name.trim().toUpperCase() + " already exists.");
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
		if (positioninfo == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		mv.addObject("positioninfo", positioninfo);

		return mv;

	}

	@RequestMapping(value = "/deleteposition/{positionkey}", method = RequestMethod.POST)
	public ModelAndView delete(@PathVariable("positionkey") int positionkey) {

		positionservice.deletePosition(positionkey);

		return new ModelAndView("redirect:/viewpositionlist");
	}

	/** The create/update form again, with the entered values and an error. */
	private ModelAndView form(PositionInfo positioninfo, String errorMessage) {
		ModelAndView mv = new ModelAndView("createposition");
		mv.addObject("positioninfo", positioninfo);
		mv.addObject("errorMessage", errorMessage);
		return mv;
	}
}
