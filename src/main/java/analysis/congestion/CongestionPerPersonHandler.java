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
package analysis.congestion;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author amit
 */
public class CongestionPerPersonHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private final Logger logger = Logger.getLogger(CongestionPerPersonHandler.class);

	private Map<Double, Map<Id, Double>> linkId2DelaysPerLink = new HashMap<Double, Map<Id, Double>>();
	private Map<Id, Map<Id, Double>> linkId2PersonIdLinkEnterTime = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Double> linkId2FreeSpeedLinkTravelTime = new HashMap<Id, Double>();
	private Map<Double, Map<Id, Double>> time2linkIdLeaveCount = new HashMap<Double, Map<Id,Double>>();
	private double totalDelay;

	private final double timeBinSize;

	public CongestionPerPersonHandler(int noOfTimeBins, double simulationEndTime, Scenario scenario){

		this.timeBinSize = simulationEndTime / noOfTimeBins;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			this.linkId2PersonIdLinkEnterTime.put(link.getId(), new HashMap<Id, Double>());
			Double freeSpeedLinkTravelTime = Double.valueOf(Math.floor(link.getLength()/link.getFreespeed())+1);
			this.linkId2FreeSpeedLinkTravelTime.put(link.getId(), freeSpeedLinkTravelTime);
		}

		for(int i =0;i<noOfTimeBins;i++){
			this.linkId2DelaysPerLink.put(this.timeBinSize*(i+1), new HashMap<Id, Double>());
			this.time2linkIdLeaveCount.put(this.timeBinSize*(i+1), new HashMap<Id, Double>());

			for(Link link : scenario.getNetwork().getLinks().values()) {
				Map<Id, Double>	delayOnLink = this.linkId2DelaysPerLink.get(this.timeBinSize*(i+1));
				delayOnLink.put(link.getId(), Double.valueOf(0.));
				Map<Id, Double> countOnLink = this.time2linkIdLeaveCount.get(this.timeBinSize*(i+1));
				countOnLink.put(link.getId(), Double.valueOf(0.));
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2DelaysPerLink.clear();
		logger.info("Resetting link delays to   " + this.linkId2DelaysPerLink);
		this.linkId2PersonIdLinkEnterTime.clear();
		this.linkId2FreeSpeedLinkTravelTime.clear();
		this.time2linkIdLeaveCount.clear();
		logger.info("Resetting linkLeave counter to " + this.time2linkIdLeaveCount);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		if(this.linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId)){
			// Person is already on the link. Cannot happen.
			throw new RuntimeException();
		} 

		Map<Id, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		double derivedLinkEnterTime = event.getTime()+1-this.linkId2FreeSpeedLinkTravelTime.get(linkId);
		personId2LinkEnterTime.put(personId, derivedLinkEnterTime);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double time = event.getTime(); 
		if(time ==0.0) time = this.timeBinSize;
		double endOfTimeInterval = 0.0;
		endOfTimeInterval = Math.ceil(time/this.timeBinSize)*this.timeBinSize;
		if(endOfTimeInterval<=0.0)endOfTimeInterval=this.timeBinSize;


		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();

		double actualTravelTime = event.getTime()-this.linkId2PersonIdLinkEnterTime.get(linkId).get(personId);
		this.linkId2PersonIdLinkEnterTime.get(linkId).remove(personId);
		double freeSpeedTime = this.linkId2FreeSpeedLinkTravelTime.get(linkId);
		double currentDelay =	actualTravelTime-freeSpeedTime;

		Map<Id, Double> delayOnLink = this.linkId2DelaysPerLink.get(endOfTimeInterval);
		Map<Id, Double> countTotal = this.time2linkIdLeaveCount.get(endOfTimeInterval);

		double delaySoFar = delayOnLink.get(linkId);

		if(currentDelay<1.)  currentDelay=0.;

		double delayNewValue = currentDelay+delaySoFar;
		this.totalDelay+=currentDelay;	

		delayOnLink.put(linkId, Double.valueOf(delayNewValue));

		double countsSoFar = countTotal.get(linkId);
		double newValue = countsSoFar + 1.;
		countTotal.put(linkId, Double.valueOf(newValue));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double time = event.getTime();
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();

		if(this.linkId2PersonIdLinkEnterTime.get(linkId).containsKey(personId)){
			// Person is already on the link. Cannot happen.
			logger.warn("Person "+personId+" is entering on link "+linkId+" two times without leaving from the same. Link leave times are "+time+" and "+this.linkId2PersonIdLinkEnterTime.get(linkId).get(personId));
			throw new RuntimeException();
		} 

		Map<Id, Double> personId2LinkEnterTime = this.linkId2PersonIdLinkEnterTime.get(linkId);
		personId2LinkEnterTime.put(personId, time);
		this.linkId2PersonIdLinkEnterTime.put(linkId, personId2LinkEnterTime);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.linkId2PersonIdLinkEnterTime.get(event.getLinkId()).remove(event.getPersonId());
	}

	public Map<Double, Map<Id, Double>> getDelayPerLinkAndTimeInterval(){
		return this.linkId2DelaysPerLink;
	}

	public double getTotalDelayInHours(){
		return totalDelay/3600;
	}
	public Map<Double, Map<Id, Double>> getTime2linkIdLeaveCount() {
		return this.time2linkIdLeaveCount;
	}
}