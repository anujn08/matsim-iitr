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

package playground.agarwalamit.opdyts.equil;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import opdytsintegration.MATSimSimulator2;
import opdytsintegration.MATSimStateFactoryImpl;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.opdyts.analysis.DecisionVariableAndBestSolutionPlotter;
import playground.agarwalamit.opdyts.analysis.OpdytsConvergencePlotter;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.opdytsintegration.modechoice.EveryIterationScoringParameters;

/**
 * @author amit
 */

public class MatsimOpdytsEquilMixedTrafficIntegration {

	private static String EQUIL_DIR = "./examples/scenarios/equil-mixedTraffic/";
	private static final OpdytsScenario EQUIL_MIXEDTRAFFIC = OpdytsScenario.EQUIL_MIXEDTRAFFIC;

	private static boolean isPlansRelaxed = false;
	private static double startingASCforBicycle = 2.0;

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.GROUP_NAME,OpdytsConfigGroup.class);
		String OUT_DIR ;

		if (args.length > 0) {
			opdytsConfigGroup.setVariationSizeOfRamdomizeDecisionVariable(Double.valueOf(args[0]));
			opdytsConfigGroup.setNumberOfIterationsForConvergence(Integer.valueOf(args[1]));
			opdytsConfigGroup.setNumberOfIterationsForAveraging(Integer.valueOf(args[4]));
			opdytsConfigGroup.setSelfTuningWeight(Double.valueOf(args[5]));
			opdytsConfigGroup.setRandomSeedToRandomizeDecisionVariable(Integer.valueOf(args[7]));
			opdytsConfigGroup.setPopulationSize(Integer.valueOf(args[9]));

			EQUIL_DIR = args[2];
			OUT_DIR = args[3]+"/equil_car,bicycle_holes_variance"+ opdytsConfigGroup.getVariationSizeOfRamdomizeDecisionVariable() +"_"+opdytsConfigGroup.getNumberOfIterationsForConvergence()+"its/";
			isPlansRelaxed = Boolean.valueOf(args[6]);
			startingASCforBicycle = Double.valueOf(args[8]);
		} else {
			OUT_DIR = "./playgrounds/agarwalamit/output/equil_car,bicycle_holes_KWM_variance"+ opdytsConfigGroup.getVariationSizeOfRamdomizeDecisionVariable() +"_"+opdytsConfigGroup.getNumberOfIterationsForConvergence()+"its/";
		}

		List<String> modes2consider = Arrays.asList("car","bicycle");

		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 
		String configFile = EQUIL_DIR+"/config.xml";
		ConfigUtils.loadConfig(config,configFile);
		config.setContext(IOUtils.getUrlFromFileOrResource(configFile));
		config.plans().setInputFile("plans2000.xml.gz");

		//== default config has limited inputs
		StrategyConfigGroup strategies = config.strategy();
		strategies.clearStrategySettings();

		config.changeMode().setModes( modes2consider.toArray(new String [modes2consider.size()]));
		StrategySettings modeChoice = new StrategySettings();
		modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name());
		modeChoice.setWeight(0.1);
		config.strategy().addStrategySettings(modeChoice);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		expChangeBeta.setWeight(0.9);
		config.strategy().addStrategySettings(expChangeBeta);
		//==

		//== planCalcScore params (initialize will all defaults).
		for ( PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.relative );
		}

		// remove other mode params
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
		for ( PlanCalcScoreConfigGroup.ModeParams params : planCalcScoreConfigGroup.getModes().values() ) {
			planCalcScoreConfigGroup.removeParameterSet(params);
		}

		PlanCalcScoreConfigGroup.ModeParams mpCar = new PlanCalcScoreConfigGroup.ModeParams("car");
		PlanCalcScoreConfigGroup.ModeParams mpBike = new PlanCalcScoreConfigGroup.ModeParams("bicycle");
		mpBike.setMarginalUtilityOfTraveling(0.);
		mpBike.setConstant(startingASCforBicycle);

		planCalcScoreConfigGroup.addModeParams(mpCar);
		planCalcScoreConfigGroup.addModeParams(mpBike);
		//==

		//==
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.withHoles );
		config.qsim().setUsingFastCapacityUpdate(true);

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		//==

		//
		config.controler().setOutputDirectory(OUT_DIR+"/relaxingPlans_"+startingASCforBicycle+"asc/");

		if(! isPlansRelaxed) {
			config.controler().setLastIteration(50);
			config.strategy().setFractionOfIterationsToDisableInnovation(1.0);

			Scenario scenarioPlansRelaxor = ScenarioUtils.loadScenario(config);
			// following is taken from KNBerlinControler.prepareScenario(...);
			// modify equil plans:
			double time = 6*3600. ;
			for ( Person person : scenarioPlansRelaxor.getPopulation().getPersons().values() ) {
				Plan plan = person.getSelectedPlan() ;
				Activity activity = (Activity) plan.getPlanElements().get(0) ;
				activity.setEndTime(time);
				time++ ;
			}

			Controler controler = new Controler(scenarioPlansRelaxor);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC)));
				}
			});
			controler.run();

			FileUtils.deleteIntermediateIterations(config.controler().getOutputDirectory(),controler.getConfig().controler().getFirstIteration(), controler.getConfig().controler().getLastIteration());
		}

		// set back settings for opdyts
		File file = new File(config.controler().getOutputDirectory()+"/output_plans.xml.gz");
		config.plans().setInputFile(file.getAbsoluteFile().getAbsolutePath());
		OUT_DIR = OUT_DIR+"/calibration_"+ opdytsConfigGroup.getNumberOfIterationsForAveraging() +"Its_"+opdytsConfigGroup.getSelfTuningWeight()+"weight_"+startingASCforBicycle+"asc/";

		config.controler().setOutputDirectory(OUT_DIR);
		opdytsConfigGroup.setOutputDirectory(OUT_DIR);
		config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//****************************** mainly opdyts settings ******************************

		TimeDiscretization timeDiscretization = new TimeDiscretization(opdytsConfigGroup.getStartTime(), opdytsConfigGroup.getBinSize(), opdytsConfigGroup.getBinCount());

		DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,distanceDistribution);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario, timeDiscretization, new HashSet<>(modes2consider));
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// add here whatever should be attached to matsim controler
//				addTravelTimeBinding("bicycle").to(networkTravelTime());
//				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);

				addControlerListenerBinding().toInstance(new ShutdownListener() {
					@Override
					public void notifyShutdown(ShutdownEvent event) {
						// remove the unused iterations
						String dir2remove = event.getServices().getControlerIO().getOutputPath()+"/ITERS/";
						IOUtils.deleteDirectoryRecursively(new File(dir2remove).toPath());
					}
				});
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(distanceDistribution);

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC, EQUIL_MIXEDTRAFFIC, null, modes2consider);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, modes2consider, EQUIL_MIXEDTRAFFIC);

		// what would decide the convergence of the objective function
		ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(opdytsConfigGroup.getNumberOfIterationsForConvergence(), opdytsConfigGroup.getNumberOfIterationsForAveraging());

		RandomSearch<ModeChoiceDecisionVariable> randomSearch = new RandomSearch<>(
				simulator,
				decisionVariableRandomizer,
				initialDecisionVariable,
				convergenceCriterion,
				opdytsConfigGroup.getMaxIteration(), // this many times simulator.run(...) and thus controler.run() will be called.
				opdytsConfigGroup.getMaxTransition(),
				opdytsConfigGroup.getPopulationSize(),
				MatsimRandom.getRandom(),
				opdytsConfigGroup.isInterpolate(),
				objectiveFunction,
				opdytsConfigGroup.isIncludeCurrentBest()
				);

		// probably, an object which decide about the inertia
		SelfTuner selfTuner = new SelfTuner(opdytsConfigGroup.getInertia());
		selfTuner.setWeightScale(opdytsConfigGroup.getSelfTuningWeight());

		randomSearch.setLogPath(opdytsConfigGroup.getOutputDirectory());

		// run it, this will eventually call simulator.run() and thus controler.run
		randomSearch.run(selfTuner );


		OpdytsConvergencePlotter opdytsConvergencePlotter = new OpdytsConvergencePlotter();
		opdytsConvergencePlotter.readFile(OUT_DIR+"/opdyts.con");
		opdytsConvergencePlotter.plotData(OUT_DIR+"/convergence_"+ opdytsConfigGroup.getNumberOfIterationsForAveraging() +"Its_"+opdytsConfigGroup.getSelfTuningWeight()+"weight_"+startingASCforBicycle+"asc.png");

		DecisionVariableAndBestSolutionPlotter decisionVariableAndBestSolutionPlotter = new DecisionVariableAndBestSolutionPlotter("bicycle");
		decisionVariableAndBestSolutionPlotter.readFile(OUT_DIR+"/opdyts.log");
		decisionVariableAndBestSolutionPlotter.plotData(OUT_DIR+"/decisionVariableVsASC_"+ opdytsConfigGroup.getNumberOfIterationsForAveraging() +"Its_"+opdytsConfigGroup.getSelfTuningWeight()+"weight_"+startingASCforBicycle+"asc.png");
	}
}
