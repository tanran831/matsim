/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractWithinActivityDurations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialDemand;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


public class ExtractWithinActivityDurations {

	/**
	 * @param args
	 */
	private static final String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";

	public static void main(String[] args) {
		final double withinThreshold = 0.90;
		
		ArrayList<Integer> withinVehicles = new ArrayList<Integer>();
		ArrayList<Integer> withinDurations = new ArrayList<Integer>();
		
		// build ArrayList of 'within' vehicles
		String fileToRead = ROOT + "Gauteng/Activities/GautengVehiclestats.txt";
		
		withinVehicles = buildWithinVehicleIdList(fileToRead, withinThreshold);
		
		System.out.print("Building ArrayList of 'within' activity durations... ");
		
		try {
			File activityFile = new File(ROOT + "Gauteng/Activities/GautengMinorLocations.txt");
//			File activityFile = new File(ROOT + "CommercialDemand/InputData/Test.txt");
			Scanner durationScan = new Scanner(new BufferedReader(new FileReader(activityFile)));
			@SuppressWarnings("unused")
			String header = durationScan.nextLine();

			while(durationScan.hasNextLine()){
				String [] line = durationScan.nextLine().split(",");
				if(line.length == 5){
					int vehicleId = Integer.parseInt(line[0]);
					int duration = Integer.parseInt(line[4]);
					if(withinVehicles.contains(vehicleId)){
						withinDurations.add(duration);
					}
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.printf("Done. (%d activities)\n", withinDurations.size());
		
		System.out.print("Writing 'within' activity durations to file... ");
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(ROOT + "CommercialDemand/Inputdata/gautengWithindurations.txt")));
			try{
				for (int i = 0; i < withinDurations.size(); i++) {
					output.write(withinDurations.get(i).toString());
					output.newLine();
				}
			}finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Done.\n\nCompleted successfully!");
	}

	public static ArrayList<Integer> buildWithinVehicleIdList(String fileToRead, double thresHold) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		try {
			System.out.print("Building an ArrayList of 'within' vehicle IDs... ");
			Scanner withinScan = new Scanner(new BufferedReader(new FileReader(new File(fileToRead))));
			
			@SuppressWarnings("unused")
			String header = withinScan.nextLine();
			while(withinScan.hasNextLine()){
				String [] line = withinScan.nextLine().split(",");
				int vehicleId = Integer.parseInt(line[0]);
				double percentage = Double.parseDouble(line[8]);
				if(percentage >= thresHold){
					list.add(vehicleId);
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.printf("Done (%d vehicles)\n", list.size());
		return list;
	}

}
