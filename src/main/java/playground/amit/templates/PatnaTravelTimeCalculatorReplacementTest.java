/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.amit.templates;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.amit.utils.FileUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by amit on 05.04.17.
 */


public class PatnaTravelTimeCalculatorReplacementTest {

    private static final String configDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/opdyts/input/";

    private static String OUT_DIR = FileUtils.RUNS_SVN+"/patnaIndia/run111/opdyts/outputTravelTime_withoutReplacement/";

    private static Map<String, TravelTime> modalTravelTimeForReplacement = new HashMap<>();

    public static void main(String[] args) {
        String configFile = configDir + "/config_urban_1pct.xml";

        //=========== 1 ==================
        // run for 20 itertaions

        {
            Config config= ConfigUtils.loadConfig(configFile);
            config.linkStats().setAverageLinkStatsOverIterations(1);
            config.controller().setOutputDirectory(OUT_DIR+"/noReplacementOfTravelTime/");
            config.controller().setFirstIteration( 0 );
            config.controller().setLastIteration( 40 );
            config.controller().setCreateGraphs(true);
            config.replanning().setFractionOfIterationsToDisableInnovation(1); // this is must
            Controler controler = getControler(config);
            controler.run();
        }

        //=========== 2 ==================
        // run for 10 itertaions; replace travel times; run again for 10 iterations

        {
            modalTravelTimeForReplacement.clear();
            Config config1= ConfigUtils.loadConfig(configFile);
            config1.linkStats().setAverageLinkStatsOverIterations(1);
            config1.controller().setOutputDirectory(OUT_DIR+"/beforeReplacementOfTravelTime/");
            config1.controller().setFirstIteration( 0 );
            config1.controller().setLastIteration( 20 );
            config1.controller().setCreateGraphs(true);
            config1.replanning().setFractionOfIterationsToDisableInnovation(1); // this is must
            Controler controler1 = getControler(config1);
            controler1.run();

            Config config2= ConfigUtils.loadConfig(configFile);
            config2.linkStats().setAverageLinkStatsOverIterations(1);
            config2.controller().setOutputDirectory(OUT_DIR+"/afterReplacementOfTravelTime2/");
            config2.controller().setFirstIteration( 20 );
            config2.controller().setLastIteration( 40 );
            config2.controller().setCreateGraphs(true);
            config2.replanning().setFractionOfIterationsToDisableInnovation(1); // this is must

            config2.plans().setInputFile( config1.controller().getOutputDirectory()+"/output_plans.xml.gz" );

            Controler controler2 = getControler(config2);

//            controler2.addOverridingModule(new AbstractModule() {
//                @Override
//                public void install() {
//
//                    modalTravelTimeForReplacement.entrySet().forEach(
//                            entry -> {
//                                addTravelTimeBinding(entry.getKey()).toInstance(entry.getValue());
//                            }
//                    );
//                }
//            });

            controler2.run();
        }
    }

    private static Controler getControler(Config config){

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        List<String> modes2consider = Arrays.asList("car","bike","motorbike","pt","walk");

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // add here whatever should be attached to matsim controler

                // some stats
                addControlerListenerBinding().to(KaiAnalysisListener.class);

                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        // adding pt fare system based on distance
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().to(PtFareEventHandler.class);
            }
        });
        // for above make sure that util_dist and monetary dist rate for pt are zero.
        ScoringConfigGroup.ModeParams mp = controler.getConfig().scoring().getModes().get("pt");
        mp.setMarginalUtilityOfDistance(0.0);
        mp.setMonetaryDistanceRate(0.0);

//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                this.addControlerListenerBinding().to(MyControlerListener.class);
//            }
//        });

        return controler;
    }

    private static class MyControlerListener implements ShutdownListener, IterationStartsListener, StartupListener {
        @Inject
        Map<String, TravelTime> modalTravelTimes ;

        @Override
        public void notifyShutdown(ShutdownEvent event) {
            modalTravelTimeForReplacement.putAll( modalTravelTimes );
        }

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            MatsimRandom.reset();
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            MatsimRandom.reset();
        }
    }
}
