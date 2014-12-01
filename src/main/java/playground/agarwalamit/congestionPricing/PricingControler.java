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
package playground.agarwalamit.congestionPricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.internalizationCar.TollDisutilityCalculatorFactory;
import playground.ikaddoura.internalizationCar.TollHandler;
import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;

/**
 * @author amit
 */

public class PricingControler {

	public static void main(String[] args) {

		String configFile = args[0];
		String outputDir = args[1];
		String congestionPricing = args[2];
		
		Scenario sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		sc.getConfig().controler().setOutputDirectory(outputDir);
		
		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		controler.setDumpDataAtEnd(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		TollHandler tollHandler = new TollHandler(sc);
		TollDisutilityCalculatorFactory fact = new TollDisutilityCalculatorFactory(tollHandler);
		controler.setTravelDisutilityFactory(fact);
		
		switch (congestionPricing) {
		case "implV4":
			{
				controler.addControlerListener(new CongestionPricingContolerListner(sc, tollHandler, new MarginalCongestionHandlerImplV4(controler.getEvents(), sc)));
				Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 4.");
			}
			break;
		
		case "implV5":
		{
			controler.addControlerListener(new CongestionPricingContolerListner(sc, tollHandler, new MarginalCongestionHandlerImplV5(controler.getEvents(), sc)));
			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 5.");
		}
		break;
		
		case "implV6":
		{
			controler.addControlerListener(new CongestionPricingContolerListner(sc, tollHandler, new MarginalCongestionHandlerImplV6(controler.getEvents(), sc)));
			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 6.");
		}
		break;
		
		default:
			throw new RuntimeException("Congestion pricing implementation does not match. Possible options are implV4 implV5 implV6. Aborting ...");
		}
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.run();
		
	}

}
