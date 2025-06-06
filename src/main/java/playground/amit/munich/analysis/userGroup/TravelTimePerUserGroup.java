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
package playground.amit.munich.analysis.userGroup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.amit.munich.utils.MunichPersonFilter;
import playground.amit.munich.utils.UserGroupUtilsExtended;
import playground.amit.utils.LoadMyScenarios;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author amit
 */
public class TravelTimePerUserGroup  {

	public TravelTimePerUserGroup() {
        String configFile = outputDir + "/output_config.xml";
        String networkFile = outputDir + "/output_network.xml.gz";
        String populationFile = outputDir + "/output_plans.xml.gz";
        this.sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(populationFile, networkFile, configFile);
		
		this.usrGrpExtended = new UserGroupUtilsExtended();
		this.userGrpToBoxPlotData = new TreeMap<>();
		
		int lastIteration = LoadMyScenarios.getLastIteration(configFile);
		String eventsFile = this.outputDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		this.travelTimeAnalyzer = new ModalTravelTimeAnalyzer(eventsFile);
	}

	private final ModalTravelTimeAnalyzer travelTimeAnalyzer;
	private final Scenario sc; 
	private static final Logger LOGGER = LogManager.getLogger(TravelTimePerUserGroup.class);
	private Map<String, Map<Id<Person>, List<Double>>> mode2PersonId2TravelTimes;
	
	private final String outputDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/exposure/25ExI/";

    private final SortedMap<MunichPersonFilter.MunichUserGroup, SortedMap<String, Double>> usrGrp2Mode2MeanTime = new TreeMap<>();
	private final SortedMap<MunichPersonFilter.MunichUserGroup, SortedMap<String, Double>> usrGrp2Mode2MedianTime = new TreeMap<>();
	
	private final UserGroupUtilsExtended usrGrpExtended;
	private final SortedMap<MunichPersonFilter.MunichUserGroup, List<Double>> userGrpToBoxPlotData;

	public static void main(String[] args) {

		TravelTimePerUserGroup ttUG = new TravelTimePerUserGroup();
		ttUG.run();
	}

	private void run(){
		this.travelTimeAnalyzer.run();
		
		String mainMode = TransportMode.car;
		this.mode2PersonId2TravelTimes = this.travelTimeAnalyzer.getMode2PesonId2TripTimes();
		getUserGroupTravelMeanAndMeadian();
		createBoxPlotData(this.travelTimeAnalyzer.getMode2PersonId2TotalTravelTime().get(mainMode));
		
		writeResults(this.outputDir+"/analysis/");
	}

	public void writeResults(String outputFolder) {
		LOGGER.info("Writing user group to travel mode to mean and mediam time.");
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/usrGrp2TravelMode2MeanAndMedianTravelTime.txt");
		try {
			writer.write("UserGroup \t travelMode \t MeanTravelTime \t MedianTravelTime \n");
			for(MunichPersonFilter.MunichUserGroup ug:this.usrGrp2Mode2MeanTime.keySet()){
				for(String travelMode:this.usrGrp2Mode2MeanTime.get(ug).keySet()){
					writer.write(ug+"\t"+travelMode+"\t"+this.usrGrp2Mode2MeanTime.get(ug).get(travelMode)+"\t"+this.usrGrp2Mode2MedianTime.get(ug).get(travelMode)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}

		LOGGER.info("Writing data for box plots for each user group.");
		try {
			String outputFile = outputFolder+"/boxPlot/";
			new File(outputFile).mkdirs();
			for(MunichPersonFilter.MunichUserGroup ug :this.userGrpToBoxPlotData.keySet()){
				writer = IOUtils.getBufferedWriter(outputFile+"/travelTime_"+ug+".txt");
				writer.write(ug+"\n");
				for(double d :this.userGrpToBoxPlotData.get(ug)){
					writer.write(d+"\n");
				}
				writer.close();
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}
		LOGGER.info("Writing data is finished.");
	}

	private void createBoxPlotData (Map<Id<Person>, Double> map){
		MunichPersonFilter pf = new MunichPersonFilter();
		for(MunichPersonFilter.MunichUserGroup ug: MunichPersonFilter.MunichUserGroup.values()){
			Population relevantPop = pf.getPopulation(sc.getPopulation(), ug);
			userGrpToBoxPlotData.put(ug, this.usrGrpExtended.getTotalStatListForBoxPlot(map, relevantPop));
		}
	}
	
	private void getUserGroupTravelMeanAndMeadian(){
		MunichPersonFilter pf = new MunichPersonFilter();
		for(MunichPersonFilter.MunichUserGroup ug: MunichPersonFilter.MunichUserGroup.values()){
			Population pop = pf.getPopulation(sc.getPopulation(), ug);
			this.usrGrp2Mode2MeanTime.put(ug, this.usrGrpExtended.calculateTravelMode2MeanFromLists(this.mode2PersonId2TravelTimes, pop));
			this.usrGrp2Mode2MedianTime.put(ug, this.usrGrpExtended.calculateTravelMode2MedianFromLists(this.mode2PersonId2TravelTimes, pop));
		}
	}
}
