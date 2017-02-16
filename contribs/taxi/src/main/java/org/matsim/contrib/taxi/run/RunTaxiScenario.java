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

package org.matsim.contrib.taxi.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.AbstractModule;


public class RunTaxiScenario
{
    public static void run(String configFile, boolean otfvis)
    {
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(),
                new OTFVisConfigGroup());
        createControler(config, otfvis).run();
    }


    public static Controler createControler(Config config, boolean otfvis)
    {
        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
        config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
        config.checkConsistency();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        FleetImpl fleet = new FleetImpl();
        new VehicleReader(scenario.getNetwork(), fleet).parse(taxiCfg.getTaxisFileUrl(config.getContext()));

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new TaxiModule());
        double expAveragingAlpha = 0.05;//from the AV flow paper 
        controler.addOverridingModule(
                VrpTravelTimeModules.createTravelTimeEstimatorModule(expAveragingAlpha));

        controler.addOverridingModule(new DvrpModule(TaxiModule.TAXI_MODE, fleet, new AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(DefaultTaxiOptimizerProvider.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
			}
		}, TaxiOptimizer.class));

        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule());
        }

        return controler;
    }


    public static void main(String[] args)
    {
        String configFile = "one_taxi/one_taxi_config.xml";
        //for a different scenario:
        //String configFile = "mielec_2014_02/config.xml";
        RunTaxiScenario.run(configFile, true);
    }
}
