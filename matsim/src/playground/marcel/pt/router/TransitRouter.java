/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
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

package playground.marcel.pt.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.router.MultiNodeDijkstra.InitialNode;
import playground.marcel.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import playground.marcel.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class TransitRouter {

	private final TransitSchedule schedule;
	private final TransitRouterNetwork transitNetwork;
	private final TransitRouterNetworkWrapper wrappedNetwork;
	private final Map<TransitRouterNetwork.TransitRouterNetworkLink, Tuple<TransitLine, TransitRoute>> linkMappings;
	private final Map<TransitRouterNetwork.TransitRouterNetworkNode, TransitStopFacility> nodeMappings;

	private final MultiNodeDijkstra dijkstra;
	private final TransitRouterConfig defaultConfig = new TransitRouterConfig();

	public TransitRouter(final TransitSchedule schedule) {
		this.schedule = schedule;
		this.linkMappings = new HashMap<TransitRouterNetwork.TransitRouterNetworkLink, Tuple<TransitLine, TransitRoute>>();
		this.nodeMappings = new HashMap<TransitRouterNetwork.TransitRouterNetworkNode, TransitStopFacility>();
		this.transitNetwork = buildNetwork();
		this.wrappedNetwork = new TransitRouterNetworkWrapper(this.transitNetwork);

		TransitRouterNetworkTravelTimeCost c = new TransitRouterNetworkTravelTimeCost();
		this.dijkstra = new MultiNodeDijkstra(this.wrappedNetwork, c, c);
//		new NetworkWriter(this.wrappedNetwork, "wrappedNetwork.xml").write();
	}


	public List<Leg> calcRoute2(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		return calcRoute2(fromCoord, toCoord, departureTime, this.defaultConfig);
	}

	public List<Id> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		return calcRoute(fromCoord, toCoord, departureTime, this.defaultConfig);
	}

	public List<Id> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final TransitRouterConfig config) {
		// find possible start stops
		TransitRouterNetwork.TransitRouterNetworkNode fromNode = this.transitNetwork.getNearestNode(fromCoord);
		TransitRouterNetworkWrapper.NodeWrapper fromNodeWrapped = this.wrappedNetwork.getWrappedNode(fromNode);

		// find possible end stops
		TransitRouterNetwork.TransitRouterNetworkNode toNode = this.transitNetwork.getNearestNode(toCoord);
		TransitRouterNetworkWrapper.NodeWrapper toNodeWrapped = this.wrappedNetwork.getWrappedNode(toNode);

		// find routes between start and end stops

		TransitRouterNetworkTravelTimeCost c = new TransitRouterNetworkTravelTimeCost();
		Dijkstra d = new Dijkstra(this.wrappedNetwork, c, c);
		Path p = d.calcLeastCostPath(fromNodeWrapped, toNodeWrapped, departureTime);
		ArrayList<Id> linkIds = new ArrayList<Id>(p.links.size());
		for (Link l : p.links) {
			linkIds.add(l.getId());
			System.out.println(l.getId().toString());
		}

		// build route
		return linkIds;
	}

	public List<Leg> calcRoute2(final Coord fromCoord, final Coord toCoord, final double departureTime, final TransitRouterConfig config) {
		// find possible start stops
		Collection<TransitRouterNetwork.TransitRouterNetworkNode> fromNodes = this.transitNetwork.getNearestNodes(fromCoord, config.searchRadius);
		List<InitialNode> wrappedFromNodes = new ArrayList<InitialNode>();
		for (TransitRouterNetwork.TransitRouterNetworkNode node : fromNodes) {
			TransitRouterNetworkWrapper.NodeWrapper wrappedNode = this.wrappedNetwork.getWrappedNode(node);
			double distance = CoordUtils.calcDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = departureTime + distance / config.beelineWalkSpeed;
			double initialCost = - (initialTime * config.marginalUtilityOfTravelTimeWalk + distance * config.marginalUtilityOfDistanceWalk);
			wrappedFromNodes.add(new InitialNode(wrappedNode, initialCost, initialTime));
		}

		// find possible end stops
		Collection<TransitRouterNetwork.TransitRouterNetworkNode> toNodes = this.transitNetwork.getNearestNodes(toCoord, config.searchRadius);
		List<InitialNode> wrappedToNodes = new ArrayList<InitialNode>();
		for (TransitRouterNetwork.TransitRouterNetworkNode node : toNodes) {
			TransitRouterNetworkWrapper.NodeWrapper wrappedNode = this.wrappedNetwork.getWrappedNode(node);
			double distance = CoordUtils.calcDistance(fromCoord, node.stop.getStopFacility().getCoord());
			double initialTime = departureTime + distance / config.beelineWalkSpeed;
			double initialCost = - (initialTime * config.marginalUtilityOfTravelTimeWalk + distance * config.marginalUtilityOfDistanceWalk);
			wrappedToNodes.add(new InitialNode(wrappedNode, initialCost, initialTime));
		}

		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes);

		List<Leg> legs = new ArrayList<Leg>();
		Leg leg = null;
		leg = new LegImpl(TransportMode.walk);
		legs.add(leg);

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		Link prevLink = null;
		int transitLegCnt = 0;
		for (Link link : p.links) {
			Tuple<TransitLine, TransitRoute> lineData = this.linkMappings.get(((TransitRouterNetworkWrapper.LinkWrapper) link).link);
			//			TransitStopFacility nodeData = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getToNode()).node);
			if (lineData == null) {
				// it must be one of the "transfer" links. finish the pt leg
				TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getFromNode()).node);
				leg = new LegImpl(TransportMode.pt);
				ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
				leg.setRoute(ptRoute);
				legs.add(leg);
				transitLegCnt++;
				accessStop = egressStop;
				line = null;
				route = null;
			} else {
				if (lineData.getSecond() != route) {
					// the line changed
					TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)link.getFromNode()).node);
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						if ((accessStop != egressStop) && (accessStop != null)) {
							leg = new LegImpl(TransportMode.walk);
							legs.add(leg);
						}
					}
					line = lineData.getFirst();
					route = lineData.getSecond();
					accessStop = egressStop;
				}
			}
			prevLink = link;
		}
		if (route != null) {
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			leg = new LegImpl(TransportMode.pt);
			TransitStopFacility egressStop = this.nodeMappings.get(((TransitRouterNetworkWrapper.NodeWrapper)prevLink.getToNode()).node);
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			legs.add(leg);
			transitLegCnt++;
		}
		if (prevLink != null) {
			leg = new LegImpl(TransportMode.walk);
			legs.add(leg);
		}
		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			legs.add(new LegImpl(TransportMode.walk));
		}

		return legs;
	}

	private TransitRouterNetwork buildNetwork() {
		final TransitRouterNetwork network = new TransitRouterNetwork();

		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					this.nodeMappings.put(node, stop.getStopFacility());
					if (prevNode != null) {
						TransitRouterNetworkLink link = network.createLink(prevNode, node, route, line);
						this.linkMappings.put(link, new Tuple<TransitLine, TransitRoute>(line, route));
					}
					prevNode = node;
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...

		// connect all stops with walking links if they're located less than 100m from each other
		for (TransitRouterNetworkNode node : network.getNodes()) {
			for (TransitRouterNetworkNode node2 : network.getNearestNodes(node.stop.getStopFacility().getCoord(), 100)) {
				if (node != node2) {
					network.createLink(node, node2, null, null); // not sure if null is correct here
				}
			}
		}

		return network;
	}

	/*package*/ void getNextDeparturesAtStop(final TransitStopFacility stop, final double time) {
		Collection<TransitRouterNetworkNode> nodes = this.transitNetwork.getNearestNodes(stop.getCoord(), 0);
		for (TransitRouterNetworkNode node : nodes) {
			double depDelay = node.stop.getDepartureOffset();
			double routeStartTime = time - depDelay;
			double diff = Double.POSITIVE_INFINITY;
			Departure bestDeparture = null;
			for (Departure departure : node.route.getDepartures().values()) {
				if (routeStartTime <= (departure.getDepartureTime()) && ((departure.getDepartureTime() - routeStartTime) < diff)) {
					bestDeparture = departure;
					diff = departure.getDepartureTime() - routeStartTime;
				}
			}
			if (bestDeparture == null) {
				System.out.println("Line: " + node.line.getId().toString()
					+ "  Route: " + node.route.getId()
					+ "  NO DEPARTURE FOUND!");
			} else {
				System.out.println("Line: " + node.line.getId().toString()
						+ "  Route: " + node.route.getId()
						+ "  Departure at: " + Time.writeTime(bestDeparture.getDepartureTime() + depDelay)
						+ "  Waiting time: " + Time.writeTime(diff));
			}
		}
	}

}
