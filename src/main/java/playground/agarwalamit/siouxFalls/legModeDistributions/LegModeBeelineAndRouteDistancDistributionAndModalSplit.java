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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.legModeHandler.LegModeRouteDistanceDistributionHandler;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author amit
 */
public class LegModeBeelineAndRouteDistancDistributionAndModalSplit {

	private final static String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/";//outputModalSplitSetUp
//	private final static String run = "/run201/";
	private final static String [] runs = {"run33","run205","run206","run207","run208"};
	//	private  String initialPlanFile = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/input/SiouxFalls_population_probably_v3.xml";
	//	private  String initialPlanFile = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/run33/output_plans.xml.gz";
//	private static String finalPlanFileLocation = runDir+run+"/ITERS/";

	public static void main(String[] args) {
		LegModeBeelineAndRouteDistancDistributionAndModalSplit ms= new LegModeBeelineAndRouteDistancDistributionAndModalSplit();

		//		for(int i=1;i<2;i++){
		//			String itNr = String.valueOf(i*100);
		//			String finalPlanFile = finalPlanFileLocation+"it."+itNr+"/"+itNr+".plans.xml.gz";
		//			ms.runBeelineDistance(itNr, finalPlanFile);
		//		}
		
		for(String str:runs){
			String configFile = runDir+str+"/output_config.xml";
			int lastIteration = LoadMyScenarios.getLastIteration(configFile);
			String finalPlanFile = runDir+str+"/ITERS/it."+lastIteration+"/"+lastIteration+".plans.xml.gz";
			ms.runRoutesDistance(str, finalPlanFile);
//			ms.runBeelineDistance(str, finalPlanFile);
		}
	}
	
	/**
	 * It will write legModeShare and beeline distance distribution	
	 */
	private void runBeelineDistance(String runNr,String finalPlanFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(finalPlanFile);
		LegModeDistanceDistribution	lmdd = new LegModeDistanceDistribution();
		lmdd.init(sc);
		lmdd.preProcessData();
		lmdd.postProcessData();
		lmdd.writeResults(runDir+runNr+"/analysis/legModeDistributions/"+runNr+".");
	}
	/**
	 * It will route distance distribution	
	 */
	private void runRoutesDistance(String runNr,String finalPlanFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(finalPlanFile);
		LegModeRouteDistanceDistributionHandler	lmdfed = new LegModeRouteDistanceDistributionHandler();
		lmdfed.init(sc);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		lmdfed.writeResults(runDir+"/analysis500Its/legModeDistributions/"+runNr+".");
	}
}
