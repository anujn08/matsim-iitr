/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.emission.filtering;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.munich.analysis.userGroup.EmissionsPerPersonPerUserGroup;
import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.GeometryUtils;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;

/**
 * @author amit
 */

public class FilteredColdEmissionHandler implements ColdEmissionEventHandler{
	private static final Logger LOGGER = Logger.getLogger(FilteredColdEmissionHandler.class.getName());

	private final EmissionsPerLinkColdEventHandler delegate;
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private final Collection<SimpleFeature> features ;
	private Network network;
	private final UserGroup ug ;

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final String shapeFile, 
			final Network network, final UserGroup userGroup){
		this.delegate = new EmissionsPerLinkColdEventHandler(simulationEndTime,noOfTimeBins);

		if(shapeFile!=null) this.features = new ShapeFileReader().readFileAndInitialize(shapeFile);
		else this.features = null;

		this.network = network;
		this.ug=userGroup;
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only. Another class 
	 * {@link EmissionsPerPersonPerUserGroup} could give more detailed results based on person id for all user groups.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final UserGroup userGroup){
		this(simulationEndTime,noOfTimeBins,null,null,userGroup);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
		LOGGER.warn( "This could be achieved from the other class \"EmissionsPerPersonPerUserGroup\", alternatively verify your results with the other class.");
	}

	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final String shapeFile, final Network network){
		this(simulationEndTime,noOfTimeBins,shapeFile,network,null);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins){
		this(simulationEndTime,noOfTimeBins,null,null);
		LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {

		if(this.ug!=null){ 
			Id<Person> driverId = Id.createPersonId(event.getVehicleId());
			if ( this.features!=null ) { // filtering for both
				Link link = network.getLinks().get(event.getLinkId());
				if ( this.pf.isPersonIdFromUserGroup(driverId, ug)  && GeometryUtils.isLinkInsideCity(features, link) ) {
					delegate.handleEvent(event);
				}
			} else { // filtering for user group only
				if ( this.pf.isPersonIdFromUserGroup(driverId, ug)  ) {
					delegate.handleEvent(event);
				}
			}
		} else {
			if( this.features!=null ) { // filtering for area only
				Link link = network.getLinks().get(event.getLinkId());
				if(GeometryUtils.isLinkInsideCity(features, link) ) {
					delegate.handleEvent(event);
				}
			} else { // no filtering at all
				delegate.handleEvent(event);
			}
		} 
	}

	public Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> getColdEmissionsPerLinkAndTimeInterval() {
		return delegate.getColdEmissionsPerLinkAndTimeInterval();
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}
}