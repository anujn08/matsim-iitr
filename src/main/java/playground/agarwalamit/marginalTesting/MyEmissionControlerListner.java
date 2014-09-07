/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.marginalTesting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author amit
 */
public class MyEmissionControlerListner  implements StartupListener, IterationStartsListener, ShutdownListener, IterationEndsListener {
	private final Logger logger = Logger.getLogger(MyEmissionControlerListner.class);

	private Controler controler;
	private String emissionEventOutputFile;
	private Integer lastIteration;
	private MyEmissionModule emissionModule;
	private EventWriterXML emissionEventWriter;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.controler = event.getControler();
		this.lastIteration = this.controler.getConfig().controler().getLastIteration();
		this.logger.info("emissions will be calculated for iteration " + this.lastIteration);

		Scenario scenario = this.controler.getScenario() ;
		this.emissionModule = new MyEmissionModule(scenario);
		this.emissionModule.createLookupTables();
		this.emissionModule.createEmissionHandler();

		EventsManager eventsManager = this.controler.getEvents();
		eventsManager.addHandler(this.emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(this.emissionModule.getColdEmissionHandler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Integer iteration = event.getIteration();
		if(this.lastIteration.equals(iteration)){
			this.emissionEventOutputFile = this.controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			this.logger.info("creating new emission events writer...");
			this.emissionEventWriter = new EventWriterXML(this.emissionEventOutputFile);
			this.logger.info("adding emission events writer to emission events stream...");
			this.emissionModule.getEmissionEventsManager().addHandler(this.emissionEventWriter);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.logger.info("closing emission events file...");
		this.emissionEventWriter.closeFile();
		this.emissionModule.writeEmissionInformation(this.emissionEventOutputFile);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.logger.info("\n \n \t \t Total Delays in hours is "+this.emissionModule.getTotalDelaysInHours()+"\n \n");
		
	}
}