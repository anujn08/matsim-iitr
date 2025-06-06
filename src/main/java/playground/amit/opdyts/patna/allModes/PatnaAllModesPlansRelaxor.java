/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.amit.opdyts.patna.allModes;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.amit.opdyts.OpdytsScenario;
import playground.amit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.amit.opdyts.patna.PatnaOneBinDistanceDistribution;
import playground.amit.utils.FileUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

import java.util.Arrays;
import java.util.List;

/**
 * @author amit
 */

class PatnaAllModesPlansRelaxor {

	public static void main (String[] args) {

		String configFile = "";
		String outDir = "";

		if ( args.length >0 ){
			configFile = args[0];
			outDir = args[1];
		} else {
			configFile = FileUtils.RUNS_SVN+"/opdyts/patna/allModes/relaxedPlans/inputs/config_allModes.xml";
			outDir = FileUtils.RUNS_SVN+"/opdyts/patna/allModes/relaxedPlans/output/";
		}

		Config config= ConfigUtils.loadConfig(configFile);
		config.controller().setOutputDirectory(outDir);

		new PatnaAllModesPlansRelaxor().run(config);
	}


	public void run (Config config) {

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		List<String> modes2consider = Arrays.asList("car","bike","motorbike","pt","walk");

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				this.addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new PatnaOneBinDistanceDistribution(
						OpdytsScenario.PATNA_1Pct)));

				// adding pt fare system based on distance
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
			}
		});

		// for above make sure that util_dist and monetary dist rate for pt are zero.
		ScoringConfigGroup.ModeParams mp = controler.getConfig().scoring().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controller().getFirstIteration();
		int lastIt = controler.getConfig().controller().getLastIteration();
		FileUtils.deleteIntermediateIterations(config.controller().getOutputDirectory(),firstIt,lastIt);
	}
}