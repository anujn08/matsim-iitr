package playground.agarwalamit.Chandigarh;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;

import org.matsim.core.scoring.functions.ScoringParameters;

import javax.inject.Inject;
public class CadytsInt{
	private static final String network = "C:\\Users\\DELL\\git\\matsim-iitr-main\\test\\input\\playground\\agarwalamit\\ModalCountsCadytsIT\\network.xml";
    private static final String plans = "C:\\Users\\DELL\\git\\matsim-iitr-main\\test\\input\\playground\\agarwalamit\\ModalCountsCadytsIT\\plans.xml";
    private static final String countsFile = "C:\\Users\\DELL\\git\\matsim-iitr-main\\test\\input\\playground\\agarwalamit\\ModalCountsCadytsIT\\countsCarBike.xml";
    private static final String output = "C:\\Users\\DELL\\Desktop\\output";
    
	public static void main(String [] args) {	
		Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network);
        config.plans().setInputFile(plans);
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setDumpDataAtEnd(true);
        config.counts().setInputFile(countsFile);
        config.counts().setWriteCountsInterval(5);
        config.counts().setOutputFormat("all");	
        PlanCalcScoreConfigGroup.ActivityParams startAct = new PlanCalcScoreConfigGroup.ActivityParams(ChandigarhConstants.start_act_type);
        startAct.setTypicalDuration(6*3600.);
        PlanCalcScoreConfigGroup.ActivityParams endAct = new PlanCalcScoreConfigGroup.ActivityParams(ChandigarhConstants.end_act_type);
        endAct.setTypicalDuration(16*3600.);
        config.planCalcScore().addActivityParams(startAct);
        config.planCalcScore().addActivityParams(endAct);

//        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
//        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute); // though, this must not have any effect.
//        reRoute.setWeight(0.3);
//        config.strategy().addStrategySettings(reRoute);

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		scenario.getNetwork().getLinks().values().stream().filter(l->l.getCapacity()<=600.).forEach(l->l.setCapacity(1500.));
        scenario.getNetwork().getLinks().values().stream().filter(l->l.getFreespeed()<=60./3.6).forEach(l->l.setFreespeed(60./3.6));
		// ---

		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CadytsCarModule());

		// include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CadytsContext cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);
				
				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork(), config.transit().getTransitModes()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;

		
		controler.run() ;
	}
}