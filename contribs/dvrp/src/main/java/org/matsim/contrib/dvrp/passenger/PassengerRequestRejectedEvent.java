/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.data.Request;

/**
 * @author michalm
 */
public class PassengerRequestRejectedEvent extends Event {
	public static final String EVENT_TYPE = "PassengersRequest rejected";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_REQUEST = "request";
	public static final String ATTRIBUTE_CAUSE = "cause";

	private final String mode;
	private final Id<Request> requestId;
	private final String cause;

	public PassengerRequestRejectedEvent(double time, String mode, Id<Request> requestId, String cause) {
		super(time);
		this.mode = mode;
		this.requestId = requestId;
		this.cause = cause;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getMode() {
		return mode;
	}

	/**
	 * the ID of the initial request submitted
	 */
	public Id<Request> getRequestId() {
		return requestId;
	}

	public String getCause() {
		return cause;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		attr.put(ATTRIBUTE_CAUSE, cause);
		return attr;
	}
}
