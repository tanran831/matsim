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

package org.matsim.contrib.taxi.benchmark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.network.FixedIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;


/**
 * For a fair and consistent benchmarking of taxi dispatching algorithms we assume that link travel
 * times are deterministic. To simulate this property, we remove (1) all other traffic, and (2) link
 * capacity constraints (e.g. by increasing the capacities by 100+ times), as a result all vehicles
 * move with the free-flow speed (which is the effective speed).
 * <p>
 * </p>
 * To model the impact of traffic, we can use a time-variant network, where we specify different
 * free-flow speeds for each link over time. The default approach is to specify free-flow speeds in
 * each time interval (usually 15 minutes).
 */
public class RunTaxiBenchmark
{
    public static void run(String configFile, int runs)
    {
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup());
        createControler(config, runs).run();
    }


    public static Controler createControler(Config config, int runs)
    {
        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
        config.controler().setLastIteration(runs - 1);
        config.addConfigConsistencyChecker(new TaxiBenchmarkConfigConsistencyChecker());
        config.checkConsistency();

        Scenario scenario = loadBenchmarkScenario(config, 15 * 60, 30 * 3600);

        final FleetImpl fleet = new FleetImpl();
        new VehicleReader(scenario.getNetwork(), fleet).parse(taxiCfg.getTaxisFileUrl(config.getContext()));

        Controler controler = new Controler(scenario);
        controler.setModules(new TaxiBenchmarkControlerModule());
        controler.addOverridingModule(new TaxiModule());
        controler.addOverridingModule(new DvrpModule(TaxiModule.TAXI_MODE, fleet, new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(DefaultTaxiOptimizerProvider.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
			}
		}, TaxiOptimizer.class));

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install()
            {
                addControlerListenerBinding().to(TaxiBenchmarkStats.class).asEagerSingleton();
            };
        });

        return controler;
    }


    public static Scenario loadBenchmarkScenario(Config config, int interval, int maxTime)
    {
        Scenario scenario = new ScenarioBuilder(config).build();

        if (config.network().isTimeVariantNetwork()) {// TODO use guice to choose between TimeVariantLinkFactories?
            ((Network)scenario.getNetwork()).getFactory()
                    .setLinkFactory(new FixedIntervalTimeVariantLinkFactory(interval, maxTime));
        }

        ScenarioUtils.loadScenario(scenario);
        return scenario;
    }


    public static void main(String[] args)
    {
        run("./src/main/resources/one_taxi_benchmark/one_taxi_benchmark_config.xml", 20);
    }
}
