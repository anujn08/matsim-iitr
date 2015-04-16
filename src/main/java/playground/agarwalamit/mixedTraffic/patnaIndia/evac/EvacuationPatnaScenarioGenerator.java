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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.grips.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.old.DefaultRoutingModules;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.LoadMyScenarios;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author amit
 */

public class EvacuationPatnaScenarioGenerator {

	private Collection <String> mainModes = Arrays.asList("car","motorbike","bike");
	private String dir = "../../../repos/runs-svn/patnaIndia/";

	private String networkFile = dir+"/inputs/networkUniModal.xml";
	private String outNetworkFile = dir+"/run105/input/evac_network.xml.gz";

	private String popFile = dir+"/inputs/SelectedPlansOnly.xml";
	private String outPopFile = dir+"/run105/input/evac_plans.xml.gz";

	private String areShapeFile = dir+"/run105/input/area_epsg24345.shp";
	private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Patna");

	private Scenario scenario;
	private Geometry evavcuationArea;

	public static void main(String[] args) {
		new EvacuationPatnaScenarioGenerator().run();
	}

	public Config getPatnaEvacConfig(){
		return this.scenario.getConfig();
	}
	
	void run(){
		scenario =  ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		getEvacNetwork();

		//config params
		Config config = scenario.getConfig();
		config.network().setInputFile(outNetworkFile);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setMobsim("qsim");
		config.controler().setWriteEventsInterval(20);
		config.controler().setWritePlansInterval(20);
		
		config.global().setCoordinateSystem("EPSG:24345");
		config.travelTimeCalculator().setTraveltimeBinSize(900);
		
		config.qsim().setFlowCapFactor(0.011);	
		config.qsim().setStorageCapFactor(0.033);
		config.qsim().setSnapshotPeriod(5*60);
		config.qsim().setEndTime(30*3600);
		config.qsim().setStuckTime(100000);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.name());
		config.qsim().setMainModes(mainModes);
		config.qsim().setTrafficDynamics(QSimConfigGroup.TRAFF_DYN_W_HOLES);

		StrategySettings expChangeBeta = new StrategySettings(Id.create("1",StrategySettings.class));
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.9);

		StrategySettings reRoute = new StrategySettings(Id.create("2",StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);

		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(expChangeBeta);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

		//vsp default
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.setParam("TimeAllocationMutator", "mutationAffectsDuration", "false");
		config.setParam("TimeAllocationMutator", "mutationRange", "7200.0");
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		//vsp default
		
		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(homeAct);
		
		ActivityParams evacAct = new ActivityParams("evac");
		evacAct.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(evacAct);

		config.planCalcScore().setPerforming_utils_hr(6.0);
		config.planCalcScore().setTraveling_utils_hr(0);
		config.planCalcScore().setTravelingBike_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(0);
		config.planCalcScore().setTravelingPt_utils_hr(0);
		config.planCalcScore().setTravelingWalk_utils_hr(0);

		config.planCalcScore().setConstantCar(-3.50);
		config.planCalcScore().setConstantOther(-2.2);
		config.planCalcScore().setConstantBike(0);
		config.planCalcScore().setConstantPt(-3.4);
		config.planCalcScore().setConstantWalk(-0.0);
		
		config.plansCalcRoute().setNetworkModes(mainModes);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		config.plansCalcRoute().setTeleportedModeSpeed("walk", 4/3.6); 
		config.plansCalcRoute().setTeleportedModeSpeed("pt", 20/3.6);
		
		if(EvacPatnaControler.isUsingSeepage){
			config.setParam("seepage", "isSeepageAllowed", "true");
			config.setParam("seepage", "seepMode", "bike");
			config.setParam("seepage", "isSeepModeStorageFree", "false");
			String outputDir = dir+"run105/evac_seepage/";
			config.controler().setOutputDirectory(outputDir);
		} else {
			String outputDir = dir+"run105/evac_passing/";
			config.controler().setOutputDirectory(outputDir);
		}

		// population
		ScenarioUtils.loadScenario(scenario);
		getEvacPopulation();
		config.plans().setInputFile(outPopFile);
	}

	private Scenario getEvacNetwork(){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(popFile, networkFile);
		//read shape file and get area
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(areShapeFile);
		evavcuationArea = (Geometry) features.iterator().next().getDefaultGeometry();

		// will create a network connecting with safe node.
		EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(sc, evavcuationArea, safeLinkId);
		net.run();

		new NetworkWriter(sc.getNetwork()).write(outNetworkFile);
		return sc;
	}

	private void getEvacPopulation() {
		// population, (home - evac)
		Population popOut = scenario.getPopulation();
		PopulationFactory popFact = popOut.getFactory();

		Scenario scIn = LoadMyScenarios.loadScenarioFromPlans(popFile);
		
		for(Person p : scIn.getPopulation().getPersons().values()){

			PlanElement actPe = p.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
			Activity home = popFact.createActivityFromLinkId(((Activity)actPe).getType(), ((Activity)actPe).getLinkId());

			//check if the person is in the area shape, if not leave them out
			if(! evavcuationArea.contains(MGC.coord2Point(((Activity)actPe).getCoord()))){
				continue;
			}

			// also exclude any home activity starting on link which is not included in evac network
			if(! scenario.getNetwork().getLinks().containsKey(home.getLinkId())){
				continue;
			}

			Person pOut = popFact.createPerson(p.getId());
			Plan planOut = popFact.createPlan();
			pOut.addPlan(planOut);

			planOut.addActivity(home);
			home.setEndTime(9*3600);

			PlanElement legPe = p.getSelectedPlan().getPlanElements().get(1);
			Leg leg = popFact.createLeg(((Leg)legPe).getMode());
			planOut.addLeg(leg);

			Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkId);
			planOut.addActivity(evacAct);

			if(mainModes.contains(leg.getMode())){
				ModeRouteFactory routeFactory = new ModeRouteFactory();
				routeFactory.setRouteFactory(leg.getMode(), new LinkNetworkRouteFactory());

				TripRouter router = new TripRouter();
				router.setRoutingModule(leg.getMode(), DefaultRoutingModules.createNetworkRouter(leg.getMode(), popFact, scenario.getNetwork(), new Dijkstra(scenario.getNetwork(), new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()) , new FreeSpeedTravelTime())));
				List<? extends PlanElement> routeInfo = router.calcRoute(leg.getMode(), new ActivityWrapperFacility(home), new ActivityWrapperFacility(evacAct), home.getEndTime(), pOut);

				Route route = ((Leg)routeInfo.get(0)).getRoute();
				route.setStartLinkId(home.getLinkId());
				route.setEndLinkId(evacAct.getLinkId());

				leg.setRoute(route);
				leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());

			} else {
				continue;
				//probably, re-create home and evac activities with coord here to include them in simulation.
			//				ModeRouteFactory routeFactory = new ModeRouteFactory();
			//				routeFactory.setRouteFactory(leg.getMode(), new GenericRouteFactory());
			//				
			//				TripRouter router = new TripRouter();
			//				router.setRoutingModule(leg.getMode(), DefaultRoutingModules.createTeleportationRouter(leg.getMode(), popFact, scOut.getConfig().plansCalcRoute().getModeRoutingParams().get(leg.getMode())));
			//				List<? extends PlanElement> routeInfo = router.calcRoute(leg.getMode(), new ActivityWrapperFacility(home), new ActivityWrapperFacility(evacAct), home.getEndTime(), pOut);
			//				
			//				Route route = ((Leg)routeInfo.get(0)).getRoute();
			////				Route route = routeFactory.createRoute(leg.getMode(), home.getLinkId(), evacAct.getLinkId());
			//				leg.setRoute(route);
			//				leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());
			}
			popOut.addPerson(pOut);
		}
		new PopulationWriter(popOut).write(outPopFile);		
	}
}
