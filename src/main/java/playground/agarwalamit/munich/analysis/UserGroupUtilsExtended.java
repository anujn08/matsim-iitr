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
package playground.agarwalamit.munich.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * @author amit
 */
public class UserGroupUtilsExtended {
	private final Logger logger = Logger.getLogger(UserGroupUtilsExtended.class);
	
	public SortedMap<String, Double> calculateTravelMode2Mean(Map<String, Map<Id<Person>, Double>> inputMap, Population relavantPop){	
		this.logger.info("Calculating mean(average) travel time for all travel modes.");
		SortedMap<String, Double> mode2Mean = new TreeMap<String, Double>();
		List<Double> allTravelTimes = new ArrayList<Double>();
		for(String mode : inputMap.keySet()){
			List<Double> travelTimes = new ArrayList<Double>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					travelTimes.add(inputMap.get(mode).get(id));
					allTravelTimes.add(inputMap.get(mode).get(id));
				}
			}
			mode2Mean.put(mode, calculateMean(travelTimes));
		}
		mode2Mean.put("allModes", calculateMean(allTravelTimes));
		return mode2Mean;
	}
	
	public SortedMap<String, Double> calculateTravelMode2MeanFromLists(Map<String, Map<Id<Person>, List<Double>>> inputMap, Population relavantPop){	
		this.logger.info("Calculating mean(average) travel time for all travel modes.");
		SortedMap<String, Double> mode2Mean = new TreeMap<String, Double>();
		List<Double> allTravelTimes = new ArrayList<Double>();
		for(String mode : inputMap.keySet()){
			List<Double> travelTimes = new ArrayList<Double>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					travelTimes.addAll(inputMap.get(mode).get(id));
					allTravelTimes.addAll(inputMap.get(mode).get(id));
				}
			}
			mode2Mean.put(mode, calculateMean(travelTimes));
		}
		mode2Mean.put("allModes", calculateMean(allTravelTimes));
		return mode2Mean;
	}

	public SortedMap<String, Double> calculateTravelMode2Median(Map<String, Map<Id<Person>, Double>> inputMap, Population relavantPop){
		this.logger.info("Calculating median travel time for all travel modes.");
		SortedMap<String, Double> mode2Median = new TreeMap<String, Double>();
		List<Double> allTravelTimes = new ArrayList<Double>();
		for(String mode : inputMap.keySet()){
			List<Double> travelTimes = new ArrayList<Double>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					travelTimes.add(inputMap.get(mode).get(id));
					allTravelTimes.add(inputMap.get(mode).get(id));
				}
			}
			mode2Median.put(mode, calculateMedian(travelTimes));
		}
		mode2Median.put("allModes", calculateMedian(allTravelTimes));
		return mode2Median;
	}
	
	public SortedMap<String, Double> calculateTravelMode2MedianFromLists(Map<String, Map<Id<Person>, List<Double>>> inputMap, Population relavantPop){
		this.logger.info("Calculating median travel time for all travel modes.");
		SortedMap<String, Double> mode2Median = new TreeMap<String, Double>();
		List<Double> allTravelTimes = new ArrayList<Double>();
		for(String mode : inputMap.keySet()){
			List<Double> travelTimes = new ArrayList<Double>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(relavantPop.getPersons().keySet().contains(id)){
					travelTimes.addAll(inputMap.get(mode).get(id));
					allTravelTimes.addAll(inputMap.get(mode).get(id));
				}
			}
			mode2Median.put(mode, calculateMedian(travelTimes));
		}
		mode2Median.put("allModes", calculateMedian(allTravelTimes));
		return mode2Median;
	}

	public double calculateMedian(List<Double> inputList){
		if(inputList.size()==0){
			return 0.;
		} else {
			Collections.sort(inputList);
			int middle = inputList.size()/2;
			if (inputList.size()%2 == 1) {
				return inputList.get(middle);
			} else {
				return (inputList.get(middle-1) + inputList.get(middle)) / 2.0;
			}
		}
	}

	public double calculateMean(List<Double> inputList){
		if(inputList.size()==0){
			return 0.;
		} else {
			double sum = 0;
			for(double d:inputList){
				sum +=d;
			}
			return sum/inputList.size();
		}
	}
}
