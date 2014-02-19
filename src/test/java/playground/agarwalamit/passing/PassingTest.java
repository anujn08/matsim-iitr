/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
package playground.agarwalamit.passing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassingVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNode;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Tests that a faster vehicle can pass slower vehicle on the same link
 * 
 */
public class PassingTest {


	/* a bike enters at t=0; and a car at t=5sec link length = 1000m
	 *Assume car speed = 20 m/s, bike speed = 5 m/s
	 *tt_car = 50 sec; tt_bike = 200 sec
	 */

	@Test 
	public void test4PassingInFreeFlowState(){

		SimpleNetwork net = new SimpleNetwork();

		//=== build plans; two persons; one with car and another with bike; car leave 5 secs after bike
		String transportModes [] = new String [] {"bike","car"};

		for(int i=0;i<2;i++){
			PersonImpl p = new PersonImpl(new IdImpl(i));
			PlanImpl plan = p.createAndAddPlan(true);
			ActivityImpl a1 = plan.createAndAddActivity("h",net.link1.getId());
			a1.setEndTime(8*3600+i*5);
			LegImpl leg=plan.createAndAddLeg(transportModes[i]);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", net.link3.getId());
			net.population.addPerson(p);
		}

		Map<Id, Map<Id, Double>> personLinkTravelTimes = new HashMap<Id, Map<Id, Double>>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));


		QSim qSim = createQSim(net,manager);
		qSim.run();

		Map<Id, Double> travelTime1 = personLinkTravelTimes.get(new IdImpl("0"));
		Map<Id, Double> travelTime2 = personLinkTravelTimes.get(new IdImpl("1"));

		int bikeTravelTime = travelTime1.get(new IdImpl("2")).intValue(); 
		int carTravelTime = travelTime2.get(new IdImpl("2")).intValue();

		//		Assert.assertEquals("wrong number of links.", 3, net.network.getLinks().size());
		//		Assert.assertEquals("wrong number of persons.", 2, net.population.getPersons().size());
		Assert.assertEquals("Passing is not implemented", 150, bikeTravelTime-carTravelTime);

	}

	private QSim createQSim (SimpleNetwork net, EventsManager manager){
		Scenario sc = net.scenario;
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1, new NetsimNetworkFactory<QNode, QLinkImpl>() {

			@Override
			public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
				return new QLinkImpl(link, network, toQueueNode, new PassingVehicleQ());
			}

			@Override
			public QNode createNetsimNode(final Node node, QNetwork network) {
				return new QNode(node, network);
			}
		});
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(new IdImpl("car"));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(new IdImpl("bike"));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);
		modeVehicleTypes.put("bike", bike);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}


	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final NetworkImpl network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			config.qsim().setMainModes(Arrays.asList("car","bike"));


			network = (NetworkImpl) scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(-100.0,0.0));
			Node node2 = network.createAndAddNode(scenario.createId("2"), scenario.createCoord( 0.0,  0.0));
			Node node3 = network.createAndAddNode(scenario.createId("3"), scenario.createCoord( 0.0,1000.0));
			Node node4 = network.createAndAddNode(scenario.createId("4"), scenario.createCoord( 0.0,1100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList("car","bike"));

			link1 = network.createAndAddLink(scenario.createId("1"), node1, node2, 100, 25, 60, 1, null, "22"); //capacity is 1 PCU per second.
			link2 = network.createAndAddLink(scenario.createId("2"), node2, node3, 1000, 25, 60, 1, null, "22");	
			link3 = network.createAndAddLink(scenario.createId("3"), node3, node4, 100, 25, 60, 1, null, "22");

			population = scenario.getPopulation();
		}
	}
	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id, Map<Id, Double>> personLinkTravelTimes;

		public PersonLinkTravelTimeEventHandler(Map<Id, Map<Id, Double>> agentTravelTimes) {
			this.personLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id, Double> travelTimes = this.personLinkTravelTimes.get(event.getPersonId());
			if (travelTimes == null) {
				travelTimes = new HashMap<Id, Double>();
				this.personLinkTravelTimes.put(event.getPersonId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id, Double> travelTimes = this.personLinkTravelTimes.get(event.getPersonId());
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}
