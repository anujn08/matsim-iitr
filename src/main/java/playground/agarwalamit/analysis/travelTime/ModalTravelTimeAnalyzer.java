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
package playground.agarwalamit.analysis.travelTime;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.trip.LegModeTripTravelTimeHandler;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */

public class ModalTravelTimeAnalyzer extends AbstractAnalysisModule {
	private final SortedMap<String, Double> mode2AvgTripTime = new TreeMap<String, Double>();
	private final SortedMap<String, Double> mode2TotalTravelTime = new TreeMap<String, Double>();
	private final LegModeTripTravelTimeHandler travelTimeHandler = new LegModeTripTravelTimeHandler();

	private final String inputEventsFile;
	private static final int ITERATION_NR = 100;
	
	public ModalTravelTimeAnalyzer(final String inputEventsFile) {
		super(ModalTravelTimeAnalyzer.class.getSimpleName());
		this.inputEventsFile = inputEventsFile;
	}
	
	public static void main(String[] args) {
		String dir = "../../../repos/runs-svn/patnaIndia/run105/1pct/evac_passing/";
		String eventFile = dir+"/ITERS/it."+ITERATION_NR+"/"+ITERATION_NR+".events.xml.gz";
		String outputFolder = dir+"/analysis/";
		ModalTravelTimeAnalyzer timeAnalyzer  = new ModalTravelTimeAnalyzer(eventFile);
		timeAnalyzer.preProcessData();
		timeAnalyzer.postProcessData();
		timeAnalyzer.writeResults(outputFolder);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}
	
	@Override
	public void preProcessData() {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		events.addHandler(travelTimeHandler);
		reader.readFile(inputEventsFile);

	}
	@Override
	public void postProcessData() {
		SortedMap<String, Map<Id<Person>, List<Double>>> times = travelTimeHandler.getLegMode2PesonId2TripTimes();
		for(String mode :times.keySet()){
			double tripTimes =0;
			double count = 0;
			for(Id<Person> id : times.get(mode).keySet()){
				for(Double d :times.get(mode).get(id)){
					tripTimes+=d;
					count++;
				}
			}
			mode2TotalTravelTime.put(mode, tripTimes);
			mode2AvgTripTime.put(mode, tripTimes/count);
		}
	}
	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/modalTravelTime_it."+ITERATION_NR+".txt");
		try {
			writer.write("mode \t avgTripTime(min) \t totalTripTime(hr) \n");

			for(String mode:mode2AvgTripTime.keySet()){
				writer.write(mode+"\t"+mode2AvgTripTime.get(mode)/60+"\t"+mode2TotalTravelTime.get(mode)/3600+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	public SortedMap<String, Double> getMode2AvgTripTime() {
		return mode2AvgTripTime;
	}

	public SortedMap<String, Double> getMode2TotalTravelTime() {
		return mode2TotalTravelTime;
	}
}