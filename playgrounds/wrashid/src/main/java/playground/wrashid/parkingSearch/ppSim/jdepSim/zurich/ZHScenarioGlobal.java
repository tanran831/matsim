/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;

import java.util.LinkedList;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.ParkingEventDetails;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.StrategyScoreStats;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.score.ParkingScoreEvaluator;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class ZHScenarioGlobal {

	public static ParkingScoreEvaluator parkingScoreEvaluator;
	public static final String outputFolder = "C:/data/parkingSearch/psim/zurich/output/run3/";;
	public static StrategyScoreStats strategyScoreStats=new StrategyScoreStats();
	public static int iteration=0;
	public static int numberOfIterations=1000;
	public static int writeEachNthIteration = 1000;
	public static int skipOutputInIteration = 0;
	public static LinkedList<ParkingEventDetails> parkingEventDetails ;
	
	
	public static void reset(){
		parkingEventDetails=new LinkedList<ParkingEventDetails>();
	}
}

