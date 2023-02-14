/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.evacuationgui.model.process;

import org.matsim.evacuationgui.control.Controller;
import org.matsim.evacuationgui.model.AbstractToolBox;
import org.matsim.evacuationgui.model.process.BasicProcess;

public class SetToolBoxProcess extends BasicProcess
{
	
	private AbstractToolBox toolBox;
	
	public SetToolBoxProcess(Controller controller, AbstractToolBox toolBox)
	{
		super(controller);
		this.toolBox = toolBox;
	}
	
	@Override
	public void start()
	{
		//set tool box
		if ((controller.getActiveToolBox()==null) || (!(controller.getActiveToolBox().getClass().isInstance(toolBox))))
			this.controller.setActiveToolBox(toolBox);
		
		if (!controller.getActiveToolBox().isVisible())
			controller.getActiveToolBox().setVisible(true);
	}	

}
