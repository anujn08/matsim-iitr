package playground.anuj.DestinationChoiceSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastes;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;

public class RunDestinationChoiceModified {

    public static void main(String[] args) {
        // Create a new MATSim config object
        Config config = ConfigUtils.createConfig();
        config.controler().setLastIteration(10);
        config.controler().setWritePlansInterval(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.network().setInputFile("DestinationChoiceTest/input/coded_network.xml");
        config.plans().setInputFile("DestinationChoiceTest/input/coded_population_1000.xml");//ppl changed to 250
        config.facilities().setInputFile("DestinationChoiceTest/input/modified_facilities_ep100_ppl1000.xml");  // just for check purpose
        config.controler().setOutputDirectory("DestinationChoiceTest/output_changeS4location_capa500");


        // Enable Destination Choice Module
        DestinationChoiceConfigGroup dcConfig = new DestinationChoiceConfigGroup();
        dcConfig.setAlgorithm(DestinationChoiceConfigGroup.Algotype.random);
        dcConfig.setPlanSelector("ChangeExpBeta");
        dcConfig.setFlexibleTypes("shopping");
        dcConfig.setEpsilonScaleFactors("shopping");
        config.addModule(dcConfig);

        FrozenTastesConfigGroup ftcg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class);
        ftcg.setEpsilonScaleFactors("100.0"); //initial 100 changed to 10
        ftcg.setFlexibleTypes("shopping");
        ftcg.setDestinationSamplePercent(0.8); //initial 5 change to 0.8

        new ConfigWriter(config).write("DestinationChoiceTest/input/modified_config_ep10_ppl1000.xml");

        config.qsim().setEndTime(24 * 3600);
//        config.network().setTimeVariantNetwork(true);
        // Configure Scoring


        PlanCalcScoreConfigGroup scoring = config.planCalcScore();
        scoring.setLearningRate(1.0);
        scoring.setBrainExpBeta(2.0);

        // Activity scoring settings
        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 3600);
        scoring.addActivityParams(homeActivity);


        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(8 * 3600);
        scoring.addActivityParams(work);


        PlanCalcScoreConfigGroup.ActivityParams shoppingActivity = new PlanCalcScoreConfigGroup.ActivityParams("shopping");
        shoppingActivity.setTypicalDuration(2 * 3600);
        scoring.addActivityParams(shoppingActivity);
//        shoppingActivity.setScalingFactor(1.0);  // Ensures duration-based scoring
//        shoppingActivity.
//        shoppingActivity.setMarginalUtilityOfWaiting(-6.0);  //

        // Configure Strategy
        config.strategy().setMaxAgentPlanMemorySize(4);

        StrategyConfigGroup.StrategySettings stratDest = new StrategyConfigGroup.StrategySettings();
        stratDest.setStrategyName(FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY);
        stratDest.setWeight(0.8);
        config.strategy().addStrategySettings(stratDest);

        StrategyConfigGroup.StrategySettings strat = new StrategyConfigGroup.StrategySettings();
        strat.setStrategyName("BestScore");
        strat.setWeight(0.8); //increased wt to 0.8 from 0.5
        config.strategy().addStrategySettings(strat);

        config.qsim().setUsingFastCapacityUpdate(true); //this was addded as a new one



        // Load the scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getPopulation().getPersons().values().forEach(p -> p.getAttributes().putAttribute("shopping", 100.0));
//
//        ActivityFacility af1 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s1", ActivityFacility.class), new Coord(1000, 0));
//        af1.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));
//        af1.getActivityOptions().get("shopping").setCapacity(4);
//        af1.getActivityOptions().get("shopping").addOpeningTime(new OpeningTimeImpl(14 * 3600, 18 * 3600));
//
//
//        ActivityFacility af2 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s2", ActivityFacility.class), new Coord(2000, 0));
//        af2.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));
//        af2.getActivityOptions().get("shopping").setCapacity(5);
//        af2.getActivityOptions().get("shopping").addOpeningTime(new OpeningTimeImpl(14 * 3600, 18 * 3600));
//
//
//        ActivityFacility af3 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s3", ActivityFacility.class), new Coord(0, 500));
//        af3.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));
//        af3.getActivityOptions().get("shopping").setCapacity(5);
//        af3.getActivityOptions().get("shopping").addOpeningTime(new OpeningTimeImpl(14 * 3600, 18 * 3600));
//
//        ActivityFacility af4 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s4", ActivityFacility.class), new Coord(1000, 10000));
//        af4.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));
//        af4.getActivityOptions().get("shopping").setCapacity(5);
//        af4.getActivityOptions().get("shopping").addOpeningTime(new OpeningTimeImpl(14 * 3600, 18 * 3600));
//
//
//        ActivityFacility af5 = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("s5", ActivityFacility.class), new Coord(2000, 1000));
//        af5.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("shopping"));
//        af5.getActivityOptions().get("shopping").setCapacity(5);
//        af5.getActivityOptions().get("shopping").addOpeningTime(new OpeningTimeImpl(14 * 3600, 18 * 3600));
//
//
//        ActivityFacility workActivity = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("w", ActivityFacility.class), new Coord(3000, 1000));
//        workActivity.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("work"));
//        workActivity.getActivityOptions().get("work").setCapacity(5);
//        workActivity.getActivityOptions().get("work").addOpeningTime(new OpeningTimeImpl(8 * 3600, 16 * 3600));
//
//
//        scenario.getActivityFacilities().addActivityFacility(af1);
//        scenario.getActivityFacilities().addActivityFacility(af2);
//        scenario.getActivityFacilities().addActivityFacility(af3);
//        scenario.getActivityFacilities().addActivityFacility(af4);
//        scenario.getActivityFacilities().addActivityFacility(af5);
//        scenario.getActivityFacilities().addActivityFacility(workActivity);

//        config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
//        config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);


        Controler controler = new Controler(scenario);

        FrozenTastes.configure(controler);

//        new FacilitiesWriter(scenario.getActivityFacilities()).write("DestinationChoiceTest/input/modified_facilities_ep100_ppl1000.xml");

        controler.run();
    }
}