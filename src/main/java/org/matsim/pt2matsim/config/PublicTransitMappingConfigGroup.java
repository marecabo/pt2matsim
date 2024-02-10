/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.pt2matsim.config;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.*;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt2matsim.run.PublicTransitMapper;

import jakarta.validation.constraints.Positive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup.TravelCostType.travelTime;


/**
 * Config Group used by {@link PublicTransitMapper}. Defines parameters for
 * mapping public transit to a network.
 *
 * @author polettif
 */
public class PublicTransitMappingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "PublicTransitMapping";

	public enum TravelCostType { linkLength, travelTime }

	private static final String TRAVEL_COST_TYPE = "travelCostType";
	private static final String MAX_TRAVEL_COST_FACTOR = "maxTravelCostFactor";

	private static final String N_LINK_THRESHOLD = "nLinkThreshold";
	private static final String CANDIDATE_DISTANCE_MULTIPLIER = "candidateDistanceMultiplier";

	private static final String ROUTING_WITH_CANDIDATE_DISTANCE = "routingWithCandidateDistance";

	// default values
	private static final int DEFAULT_N_LINK_THRESHOLD = 6;
	private Map<String, Set<String>> transportModeAssignment = new HashMap<>();

	@Parameter
	@Comment("After the schedule has been mapped, the free speed of links can be set according to the necessary travel \n" +
			"\t\ttimes given by the transit schedule. The freespeed of a link is set to the minimal value needed by all \n" +
			"\t\ttransit routes passing using it. This is recommended for \"" + PublicTransitMappingStrings.ARTIFICIAL_LINK_MODE + "\", additional \n" +
			"\t\tmodes (especially \"rail\", if used) can be added, separated by commas.")
	public Set<String> scheduleFreespeedModes = PublicTransitMappingStrings.ARTIFICIAL_LINK_MODE_AS_SET;

	@Parameter
	@Comment("All links that do not have a transit route on them are removed, except the ones \n" +
			"\t\tlisted in this set (typically only car). Separated by comma.")
	public Set<String> modesToKeepOnCleanUp = new HashSet<>();

	@Positive
	private double maxTravelCostFactor = 5.0;

	@Parameter
	@Positive
	@Comment("Defines the number of numOfThreads that should be used for pseudoRouting. Default: 2.")
	public int numOfThreads = 2;

	@Parameter
	@Comment("If true, stop facilities that are not used by any transit route are removed from the schedule. Default: true")
	public boolean removeNotUsedStopFacilities = true;

	@Parameter
	@Comment("Path to the input network file. Not needed if PTMapper is called within another class.")
	public String inputNetworkFile = null;

	@Parameter
	@Comment("Path to the input schedule file. Not needed if PTMapper is called within another class.")
	public String inputScheduleFile = null;

	@Parameter
	@Comment("Path to the output network file. Not needed if PTMapper is used within another class.")
	public String outputNetworkFile = null;

	@Parameter
	@Comment("Path to the output car only network file. The input multimodal map is filtered. \n" +
			"\t\tNot needed if PTMapper is used within another class.")
	public String outputStreetNetworkFile = null;

	@Parameter
	@Comment("Path to the output schedule file. Not needed if PTMapper is used within another class.")
	public String outputScheduleFile = null;

	private TravelCostType travelCostType = TravelCostType.linkLength;

	@Parameter
	public boolean routingWithCandidateDistance = true;

	@Parameter
	@Positive
	@Comment("Number of link candidates considered for all stops, depends on accuracy of stops and desired \n" +
			"\t\tperformance. Somewhere between 4 and 10 seems reasonable for bus stops, depending on the\n" +
			"\t\taccuracy of the stop facility coordinates and performance desires. Default: " + DEFAULT_N_LINK_THRESHOLD)
	public int nLinkThreshold = DEFAULT_N_LINK_THRESHOLD;

	@Parameter
	@Positive
	@Comment("The maximal distance [meter] a link candidate is allowed to have from the stop facility.\n" +
			"\t\tNo link candidates beyond this distance are added.")
	public double maxLinkCandidateDistance = 90;

	private double candiateDistanceMulitplier = 1.6;

	public PublicTransitMappingConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * @return a new default public transit mapping config
	 */
	public static PublicTransitMappingConfigGroup createDefaultConfig() {
		PublicTransitMappingConfigGroup config = new PublicTransitMappingConfigGroup();
		Set<String> newModesToKeepOnCleanup = new HashSet<>(config.modesToKeepOnCleanUp);
		newModesToKeepOnCleanup.add("car");
		config.modesToKeepOnCleanUp = newModesToKeepOnCleanup;

		TransportModeAssignment tmaBus = new TransportModeAssignment("bus");
		tmaBus.networkModes = Set.of("car", "bus");
		TransportModeAssignment tmaRail = new TransportModeAssignment("rail");
		tmaRail.networkModes = Set.of("rail", "light_rail");
		config.addParameterSet(tmaBus);
		config.addParameterSet(tmaRail);

		return config;
	}

	/**
	 * Loads a Public Transit Mapper Config File<p/>
	 *
	 * @param configFile the PublicTransitMapping config file (xml)
	 */
	public static PublicTransitMappingConfigGroup loadConfig(String configFile) {
		Config configAll = ConfigUtils.loadConfig(configFile, new PublicTransitMappingConfigGroup());
		return ConfigUtils.addOrGetModule(configAll, PublicTransitMappingConfigGroup.GROUP_NAME, PublicTransitMappingConfigGroup.class);
	}

	public void writeToFile(String filename) {
		Config matsimConfig = ConfigUtils.createConfig();
		matsimConfig.addModule(this);
		Set<String> toRemove = matsimConfig.getModules().keySet().stream().filter(module -> !module.equals(PublicTransitMappingConfigGroup.GROUP_NAME)).collect(Collectors.toSet());
		toRemove.forEach(matsimConfig::removeModule);
		new ConfigWriter(matsimConfig).write(filename);
	}


	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(TRAVEL_COST_TYPE,
				"Defines which link attribute should be used for routing. Possible values \"" + TravelCostType.linkLength + "\" (default) \n" +
				"\t\tand \"" + travelTime + "\".");
		map.put(MAX_TRAVEL_COST_FACTOR,
				"If all paths between two stops have a [travelCost] > [" + MAX_TRAVEL_COST_FACTOR + "] * [minTravelCost], \n" +
				"\t\tan artificial link is created. If " + TRAVEL_COST_TYPE + " is " + travelTime + ", minTravelCost is the travel time\n" +
				"\t\tbetween stops from the schedule. If " + TRAVEL_COST_TYPE + " is \n" +
				"\t\t" + TravelCostType.linkLength + " minTravel cost is the beeline distance.");
		map.put(ROUTING_WITH_CANDIDATE_DISTANCE,
				"The travel cost of a link candidate can be increased according to its distance to the\n" +
				"\t\tstop facility x2. This tends to give more accurate results. If "+ TRAVEL_COST_TYPE +" is "+ travelTime +", freespeed on \n" +
				"\t\tthe link is applied to the beeline distance.");

		// link candidates
		map.put(CANDIDATE_DISTANCE_MULTIPLIER,
				"After " + N_LINK_THRESHOLD + " link candidates have been found, additional link \n" +
				"\t\tcandidates within [" + CANDIDATE_DISTANCE_MULTIPLIER + "] * [distance to the Nth link] are added to the set.\n" +
				"\t\tMust be >= 1.");
		return map;
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch(type) {
			case TransportModeAssignment.SET_NAME:
				return new TransportModeAssignment();
			default:
				throw new IllegalArgumentException("Unknown parameterset name!");
		}
	}

	@Override
	public void addParameterSet(final ConfigGroup set) {
		super.addParameterSet(set);
		loadParameterSets();
	}

	/**
	 * Loads the parameter set for TransportModeAssignment for easier access
	 */
	private void loadParameterSets() {
		for(ConfigGroup e : this.getParameterSets(TransportModeAssignment.SET_NAME)) {
			TransportModeAssignment mra = (TransportModeAssignment) e;
			transportModeAssignment.put(mra.scheduleMode, mra.networkModes);
		}
	}

	/**
	 * References transportModes from the schedule (key) and the
	 * allowed network mode of a link from the network (value). <p/>
	 * <p/>
	 * Schedule transport mode should be in gtfs categories:
	 * <ul>
	 * <li>0 - Tram, Streetcar, Light rail. Any light rail or street level system within a metropolitan area.</li>
	 * <li>1 - Subway, Metro. Any underground rail system within a metropolitan area.</li>
	 * <li>2 - Rail. Used for intercity or long-distance travel.</li>
	 * <li>3 - Bus. Used for short- and long-distance bus routes.</li>
	 * <li>4 - Ferry. Used for short- and long-distance boat service.</li>
	 * <li>5 - Cable car. Used for street-level cable cars where the cable runs beneath the car.</li>
	 * <li>6 - Gondola, Suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.</li>
	 * <li>7 - Funicular. Any rail system designed for steep inclines.</li>
	 * </ul>
	 */
	public Map<String, Set<String>> getTransportModeAssignment() {
		return transportModeAssignment;
	}

	public void setTransportModeAssignment(Map<String, Set<String>> transportModeAssignment) {
		this.transportModeAssignment = transportModeAssignment;
	}

	/**
	 *
	 */
	@StringGetter(TRAVEL_COST_TYPE)
	public TravelCostType getTravelCostType() {
		return travelCostType;
	}

	@StringSetter(TRAVEL_COST_TYPE)
	public void setTravelCostType(TravelCostType type) {
		this.travelCostType = type;
	}


	/**
	 * If all paths between two stops have a length > maxTravelCostFactor * beelineDistance,
	 * an artificial link is created.
	 */
	@StringGetter(MAX_TRAVEL_COST_FACTOR)
	public double getMaxTravelCostFactor() {
		return maxTravelCostFactor;
	}

	@StringSetter(MAX_TRAVEL_COST_FACTOR)
	public void setMaxTravelCostFactor(double maxTravelCostFactor) {
		checkMaxTravelCostFactor(maxTravelCostFactor);
		this.maxTravelCostFactor = maxTravelCostFactor;
	}

	/*
	Link Candidates
	 */

	/**
	 * Defines the radius [meter] from a stop facility within nodes are searched.
	 * Mainly a maximum value for performance.
	 */
	@StringGetter(CANDIDATE_DISTANCE_MULTIPLIER)
	public double getCandidateDistanceMultiplier() {
		return candiateDistanceMulitplier;
	}

	@StringSetter(CANDIDATE_DISTANCE_MULTIPLIER)
	public void setCandidateDistanceMultiplier(double multiplier) {
		this.candiateDistanceMulitplier = multiplier < 1 ? 1 : multiplier;
	}

	@Override
	protected void checkConsistency(Config config) {
		checkMaxTravelCostFactor(this.maxTravelCostFactor);
		super.checkConsistency(config);
	}

	private static void checkMaxTravelCostFactor(double maxTravelCostFactor) {
		if (maxTravelCostFactor < 1) {
			throw new RuntimeException("maxTravelCostFactor cannot be less than 1!");
		}
	}


	/**
	 * Parameterset that define which network transport modes the router
	 * can use for each schedule transport mode. If no networkModes are set, the
	 * transit route is mapped artificially<p/>
	 * <p>
	 * Network transport modes are the ones in {@link Link#getAllowedModes()}, schedule
	 * transport modes are from {@link TransitRoute#getTransportMode()}.
	 */
	public static class TransportModeAssignment extends ReflectiveConfigGroup implements MatsimParameters {

		public static final String SET_NAME = "transportModeAssignment";

		@Parameter
		public String scheduleMode;

		@Parameter
		@Comment("Transit Routes with the given scheduleMode can only use links with at least one of the network modes\n" +
				"\t\t\tdefined here. Separate multiple modes by comma. If no network modes are defined, the transit route will\n" +
				"\t\t\tuse artificial links.")
		public Set<String> networkModes;

		public TransportModeAssignment() {
			super(SET_NAME);
		}

		public TransportModeAssignment(String scheduleMode) {
			super(SET_NAME);
			this.scheduleMode = scheduleMode;
		}

	}

}
