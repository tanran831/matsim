/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.polettif.multiModalMap.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.LinkCandidate;
import playground.polettif.multiModalMap.mapping.pseudoPTRouter.PseudoRouteStop;
import playground.polettif.multiModalMap.mapping.router.Router;
import playground.polettif.multiModalMap.tools.MiscUtils;
import playground.polettif.multiModalMap.tools.NetworkTools;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides some static tools for PTMapper.
 *
 * @author polettif
 */
public class PTMapperUtils {

	// TODO cleanup and doc

	protected static Logger log = Logger.getLogger(PTMapperUtils.class);
	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where
	 * no link can be found within nodeSearchRadius an artificial node and loop
	 * link (from and to the new node) is created {@link NetworkTools#createArtificialStopFacilityLink(TransitStopFacility, Network, String)}.
	 * For each link candiate a child stop facility is generated and referenced to
	 * the link. Link candidates for different modes with the same link use the same
	 * child stop facility. Child stop facilities are not created and added to the schedule!
	 * If a stop facility already has a referenced link, this link is used as the only link
	 * candidate.<p/>
	 *
	 * @param schedule with stopFacilities, not modified.
	 * @param network the network where link candidates should be looked for, is modified
	 *                for stop facilities without a link nearby
	 * @param config containing the modeRoutingAssignments and params for link searching
	 * @return a map with all link candidates for a stop facilitiy and
	 * the scheduleTransportMode as top level key.
	 */
	public static Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> generateModeLinkCandidates(TransitSchedule schedule, Network network, PublicTransportMapConfigGroup config) {
		Map<String, Map<TransitStopFacility, Set<LinkCandidate>>> tree = new HashMap<>();

		NetworkImpl networkImpl = ((NetworkImpl) network);

		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String scheduleTransportMode = transitRoute.getTransportMode();
					TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

					Set<LinkCandidate> modeLinkCandidates = MapUtils.getSet(stopFacility, MapUtils.getMap(scheduleTransportMode, tree));

					// if no link candidates for the current stop and mode have been generated
					if(modeLinkCandidates.size() == 0) {

						// if stop facilty already has a referenced link
						if(stopFacility.getLinkId() != null) {
							modeLinkCandidates.add(new LinkCandidate(network.getLinks().get(stopFacility.getLinkId()), stopFacility));
						} else {
							// limits number of links, for all links within search radius use networkTools.findClosestLinks()
							Set<Link> closestLinks = NetworkTools.findClosestLinksByMode(networkImpl, stopFacility.getCoord(), scheduleTransportMode, config);

							// if no close links are nearby, a loop link is created and referenced to the facility.
							if(closestLinks.size() == 0) {
								Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network, config.getPrefixArtificial());
								closestLinks.add(loopLink);
							}

							/**
							 * generate a LinkCandidate for each close link
							 */
							for(Link link : closestLinks) {
								modeLinkCandidates.add(new LinkCandidate(link, stopFacility));
							}
						}
					}
				}
			}
		}
		return tree;
	}

	/**
	 * Generates link candidates for all stopFacilities. For stop facilities where
	 * which no link can be found within nodeSearchRadius an artificial node and loop
	 * link (from and to the new node) are created {@link NetworkTools#createArtificialStopFacilityLink(TransitStopFacility, Network, String)}.
	 * For each link candiate a child stop facility is generated and referenced to
	 * the link. Link candidates for different modes with the same link use the same
	 * child stop facility. Child stop facilities are added to the schedule.
	 *
	 * @param schedule with stopFacilities, is modified.
	 * @param network  where link candidates should be mapped, is modified.
	 * @return the LinkCandidates for each stopFacility, split by modes.
	 */
	public static Map<TransitStopFacility, Map<String, Set<LinkCandidate>>> generateModeSeparatedLinkCandidates(TransitSchedule schedule, Network network, PublicTransportMapConfigGroup config) {

		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		Map<TransitStopFacility, Map<String, Set<LinkCandidate>>> tree = new HashMap<>();
		Map<String, TransitStopFacility> childFacilities = new HashMap<>();

		NetworkImpl networkImpl = ((NetworkImpl) network);

		/**
		 * get closest links for each stop facility (separated by mode)
		 */
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String scheduleTransportMode = transitRoute.getTransportMode();
					TransitStopFacility stopFacility = transitRouteStop.getStopFacility();

					Map<String, Set<LinkCandidate>> parentFacilityLinkCandidates = MapUtils.getMap(stopFacility, tree);
					Set<LinkCandidate> modeLinkCandidates = MapUtils.getSet(scheduleTransportMode, parentFacilityLinkCandidates);

					// if no link candidates for the current stop and mode have been generated
					if(modeLinkCandidates.size() == 0) {
						// limits number of links, for all links within search radius use networkTools.findClosestLinks()
						Set<Link> closestLinks = NetworkTools.findClosestLinksByMode(networkImpl, stopFacility.getCoord(), scheduleTransportMode, config);

						// if no close links are nearby, a loop link is created and referenced to the facility.
						if(closestLinks.size() == 0) {
							Link loopLink = NetworkTools.createArtificialStopFacilityLink(stopFacility, network, config.getPrefixArtificial());
							loopLink.setLength(10);
							stopFacility.setLinkId(loopLink.getId());
							closestLinks.add(loopLink);
						}

						/**
						 * generate child stop facility for each linkcandidate and reference them
						 */
						for(Link link : closestLinks) {
							LinkCandidate newLinkCandidate = new LinkCandidate(link, stopFacility);

							String id = stopFacility.getId() + config.getSuffixChildStopFacilities() + link.getId();

							// if child stop for this link has already been generated
							if(childFacilities.containsKey(id)) {
								newLinkCandidate.setChildStop(childFacilities.get(id));
							} else {
								TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
										Id.create(id, TransitStopFacility.class),
										stopFacility.getCoord(),
										stopFacility.getIsBlockingLane()
								);
								newFacility.setLinkId(link.getId());
								newFacility.setName(stopFacility.getName());
								newFacility.setStopPostAreaId(stopFacility.getStopPostAreaId());
								childFacilities.put(id, newFacility);

								newLinkCandidate.setChildStop(newFacility);
							}

							MapUtils.getSet(scheduleTransportMode, MapUtils.getMap(stopFacility, tree)).add(newLinkCandidate);
						}

					}
				}
			}
		}

		/**
		 * assign new facilities to schedule
		 */
		childFacilities.values().forEach(schedule::addStopFacility);

		return tree;
	}

	/**
	 * Using the pseudo schedule, the facilities in the stop sequences of the actual schedule
	 * are replaced with child facilities. Child facilities are created in this step and
	 * added to the schedule.
	 * @param schedule where the facilities should be replaced
	 * @param pseudoSchedule defines the actual sequence of pseudoRouteStops
	 * @param childStopFacilitySuffix what suffix the child facility should get in the id
	 */
	public static void createAndReplaceFacilities(TransitSchedule schedule, Map<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> pseudoSchedule, String childStopFacilitySuffix) {
		log.info("Replacing parent StopFacilities with child StopFacilities...");
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		List<Tuple<TransitLine, TransitRoute>> newRoutes = new ArrayList<>();

		for(Map.Entry<TransitLine, Map<TransitRoute, List<PseudoRouteStop>>> lineEntry : pseudoSchedule.entrySet()) {
			for(Map.Entry<TransitRoute, List<PseudoRouteStop>> routeEntry : lineEntry.getValue().entrySet()) {

				List<PseudoRouteStop> pseudoStopSequence = routeEntry.getValue();
				List<TransitRouteStop> newStopSequence = new ArrayList<>();

				for(PseudoRouteStop pseudoStop : pseudoStopSequence) {
					Id<TransitStopFacility> childStopFacilityId = Id.create(pseudoStop.getParentStopFacilityId() + childStopFacilitySuffix + pseudoStop.getLinkIdStr(), TransitStopFacility.class);

					// if child stop facility for this link has not yet been generated
					if(!schedule.getFacilities().containsKey(childStopFacilityId)) {
						TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
								Id.create(childStopFacilityId, TransitStopFacility.class),
								pseudoStop.getCoord(),
								pseudoStop.getIsBlockingLane()
						);
						newFacility.setLinkId(Id.createLinkId(pseudoStop.getLinkIdStr()));
						newFacility.setName(pseudoStop.getFacilityName());
						newFacility.setStopPostAreaId(pseudoStop.getStopPostAreaId());
						schedule.addStopFacility(newFacility);
					}

					// create new TransitRouteStop and add it to the newStopSequence
					TransitRouteStop newTransitRouteStop = scheduleFactory.createTransitRouteStop(
							schedule.getFacilities().get(childStopFacilityId),
							pseudoStop.getArrivalOffset(),
							pseudoStop.getDepartureOffset());
					newTransitRouteStop.setAwaitDepartureTime(pseudoStop.isAwaitDepartureTime());
					newStopSequence.add(newTransitRouteStop);
				}

				// create a new transitRoute
				TransitRoute newRoute = scheduleFactory.createTransitRoute(routeEntry.getKey().getId(), null, newStopSequence, routeEntry.getKey().getTransportMode());

				// add departures
				routeEntry.getKey().getDepartures().values().forEach(newRoute::addDeparture);

				// remove the old route
				schedule.getTransitLines().get(lineEntry.getKey().getId()).removeRoute(routeEntry.getKey());

				// add new route to container
				newRoutes.add(new Tuple<>(lineEntry.getKey(), newRoute));
			}
		}

		// add transit lines and routes again
		for(Tuple<TransitLine, TransitRoute> entry : newRoutes) {
			schedule.getTransitLines().get(entry.getFirst().getId()).addRoute(entry.getSecond());
		}
	}

	/**
	 * Removes all stop facilities not used by a transit route. Modifies the schedule.
	 *
	 * @param schedule the schedule in which the facilities should be removed
	 */
	public static void removeNonUsedStopFacilities(TransitSchedule schedule) {
		log.info("... Removing non used stop facilities");
		int removed = 0;

		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				for(TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for(Id<TransitStopFacility> facilityId : schedule.getFacilities().keySet()) {
			if(!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for(TransitStopFacility facility : unusedStopFacilites) {
			schedule.removeStopFacility(facility);
			removed++;
		}

		log.info("    "+removed+" stop facilities removed");
	}


	/**
	 * @return the linkIds of the links in path
	 */
	public static List<Id<Link>> getLinkIdsFromPath(LeastCostPathCalculator.Path path) {
//		List<Id<Link>> list = new ArrayList<>();
//		for(Link link : path.links) {
//			list.add(link.getId());
//		}
		return path.links.stream().map(Link::getId).collect(Collectors.toList());
	}

	/**
	 * Removes routes without link sequences
	 */
	public static int removeTransitRoutesWithoutLinkSequences(TransitSchedule schedule) {
		log.info("... Removing transit routes without link sequences");

		int removed = 0;

		for(TransitLine line : schedule.getTransitLines().values()) {
			Set<TransitRoute> toRemove = new HashSet<>();
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				boolean removeRoute = false;
				NetworkRoute networkRoute = transitRoute.getRoute();
				if(networkRoute == null) {
					removeRoute = true;
				} else if(networkRoute.getStartLinkId() == null || networkRoute.getEndLinkId() == null) {
					removeRoute = true;

					for(Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
						if(linkId == null) {
							removeRoute = true;
						}
					}
				}

				if(removeRoute) {
					toRemove.add(transitRoute);
				}
			}

			removed += toRemove.size();

			if(!toRemove.isEmpty()) {
				for(TransitRoute transitRoute : toRemove) {
					line.removeRoute(transitRoute);
				}
			}
		}
		return removed;
	}

	/**
	 * Add mode "pt" to any link of the network that is
	 * passed by any transitRoute of the schedule.
	 */
	public static void addPTModeToNetwork(TransitSchedule schedule, Network network) {
		log.info("... Adding mode \"pt\" to all links with public transit");

		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();
				transitLinkIds.add(networkRoute.getStartLinkId());
				transitLinkIds.addAll(networkRoute.getLinkIds());
				transitLinkIds.add(networkRoute.getEndLinkId());
			}
		}

		for(Id<Link> transitLinkId : transitLinkIds) {
			Link transitLink = networkLinks.get(transitLinkId);
			if(!transitLink.getAllowedModes().contains(TransportMode.pt)) {
				Set<String> modes = new HashSet<>();
				modes.addAll(transitLink.getAllowedModes());
				modes.add(TransportMode.pt);
				transitLink.setAllowedModes(modes);
			}
		}
	}

	/**
	 * Generates link sequences for all transit routes in the schedule, modifies the schedule.
	 * All stopFacilities used by a route must have a link referenced.
	 *
	 * @param schedule where transitRoutes should be routed
	 * @param network  the network where the routes should be routed
	 * @param routers  A map defining the Router for each scheduleTransportMode (the mode
	 *                 defined in the transitRoute).
	 */
	public static void routeSchedule(TransitSchedule schedule, Network network, Map<String, Router> routers) {
		Counter counterRoute = new Counter("route # ");

		log.info("Routing all routes with referenced links...");

		if(routers == null) {
			log.error("No routers given, routing cannot be completed!");
			return;
		}

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(routers.containsKey(transitRoute.getTransportMode()) && transitRoute.getStops().size() > 0) {
					Router router = routers.get(transitRoute.getTransportMode());

					counterRoute.incCounter();

					List<TransitRouteStop> routeStops = transitRoute.getStops();
					List<Id<Link>> linkSequence = new ArrayList<>();

					// add very first link
					linkSequence.add(routeStops.get(0).getStopFacility().getLinkId());

					// route
					for(int i = 0; i < routeStops.size() - 1; i++) {
						if(routeStops.get(i).getStopFacility().getLinkId() == null) {
							log.error("stop facility " + routeStops.get(i).getStopFacility().getName() + " (" + routeStops.get(i).getStopFacility().getId() + " not referenced!");
						}
						if(routeStops.get(i + 1).getStopFacility().getLinkId() == null) {
							log.error("stop facility " + routeStops.get(i - 1).getStopFacility().getName() + " (" + routeStops.get(i + 1).getStopFacility().getId() + " not referenced!");
						}

						Id<Link> currentLinkId = Id.createLinkId(routeStops.get(i).getStopFacility().getLinkId().toString());

						Link currentLink = network.getLinks().get(currentLinkId);
						Link nextLink = network.getLinks().get(routeStops.get(i + 1).getStopFacility().getLinkId());

						List<Id<Link>> path = PTMapperUtils.getLinkIdsFromPath(router.calcLeastCostPath(currentLink.getToNode(), nextLink.getFromNode()));

						if(path != null)
							linkSequence.addAll(path);

						linkSequence.add(nextLink.getId());
					}

					// add link sequence to schedule
					transitRoute.setRoute(RouteUtils.createNetworkRoute(linkSequence, network));
				}
			}
		}
		log.info("Routing all routes with referenced links... done");
	}

	/**
	 * Removes links that are not used by public transit. Links which have a mode defined
	 * in modesToKeep are kept regardless of public transit usage.
	 */
	public static void removeNotUsedTransitLinks(TransitSchedule schedule, Network network, Set<String> modesToKeep) {
		log.info("... Removing links that are not used by public transit");
		int removed = 0;

		Set<Id<Link>> usedTransitLinkIds = new HashSet<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				if(route.getRoute() != null)
					usedTransitLinkIds.addAll(route.getRoute().getLinkIds());
			}
		}

		Set<Id<Link>> linksToRemove = new HashSet<>();
		for(Link link : network.getLinks().values()) {
			// only remove link if there are only modes to remove on it
			if(!MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				linksToRemove.add(link.getId());
			}
			// only retain modes that are actually used
			else if(MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), modesToKeep) && !usedTransitLinkIds.contains(link.getId())) {
				link.setAllowedModes(MiscUtils.getSharedSetStringEntries(link.getAllowedModes(), modesToKeep));
			}
		}

		for(Id<Link> linkId : linksToRemove) {
			network.removeLink(linkId);
		}

		log.info("    "+removed+" links removed");
	}

	/**
	 * Adds mode the schedule transport mode to links. Removes all network
	 * modes elsewhere. Adds mode "artificial" to artificial
	 * links. Used for debugging and visualization since networkModes
	 * should be combined to pt anyway.
	 */
	public static void assignScheduleModesToLinks(TransitSchedule schedule, Network network) {
		log.debug("... Assigning schedule transport mode to network");

		Map<Id<Link>, Set<String>> transitLinkNetworkModes = new HashMap<>();

		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				Set<Id<Link>> linkIds = new HashSet<>();
				linkIds.add(route.getRoute().getStartLinkId());
				linkIds.addAll(route.getRoute().getLinkIds());
				linkIds.add(route.getRoute().getEndLinkId());
				for(Id<Link> linkId : linkIds) {
					MapUtils.getSet(linkId, transitLinkNetworkModes).add(route.getTransportMode());
				}
			}
		}

		for(Link link : network.getLinks().values()) {
			if(transitLinkNetworkModes.containsKey(link.getId())) {
				Set<String> modes = new HashSet<>();
				Set<String> linkModes = transitLinkNetworkModes.get(link.getId());
				linkModes.addAll(link.getAllowedModes());

				for(String m : linkModes) {
					modes.add(m);
				}

				link.setAllowedModes(modes);
			}
		}
	}


	/**
	 *
	 * @param schedule
	 * @param network
	 */
	public static void replaceNonCarModesWithPT(TransitSchedule schedule, Network network) {
		log.info("... Replacing all non-car link modes with \"pt\"");

		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Set<Id<Link>> transitLinkIds = new HashSet<>();

		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().size() > 0 && link.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				modes.add(TransportMode.pt);
				link.setAllowedModes(modes);
			}
		}
	}

	/**
	 * If link candidates have the same link for boths stop facilities, the link candidate is
	 * assigned to the stop facility that is closer (i.e. removed from the other set).
	 */
	public static void separateLinkCandidates(Set<LinkCandidate> linkCandidatesCurrent, Set<LinkCandidate> linkCandidatesNext) {
		Set<LinkCandidate> removeFromCurrent = new HashSet<>();
		Set<LinkCandidate> removeFromNext = new HashSet<>();

		for(LinkCandidate lcCurrent : linkCandidatesCurrent) {
			for(LinkCandidate lcNext : linkCandidatesNext) {
				if(lcCurrent.getLinkIdStr().equals(lcNext.getLinkIdStr())) {
					if(lcCurrent.getStopFacilityDistance() > lcNext.getStopFacilityDistance()) {
						removeFromCurrent.add(lcCurrent);
					} else {
						removeFromNext.add(lcNext);
					}
				}
			}
		}
		removeFromCurrent.forEach(linkCandidatesCurrent::remove);
		removeFromNext.forEach(linkCandidatesNext::remove);
	}
}