/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQueueSimLinkAgentsWriter
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
package org.matsim.vis.otfvis.data.fileio.queuesim;

import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;

/**All of this now inherits from OTFLinkAgentsHandler.Writer, since that seems to be the same.
 * This class here presumably needs to stay in place because of the mvi file format.  kai, feb'11
 *
 * @author dgrether
 */
@Deprecated // in my view, use OTFLinkAgentsHandler.Writer directly.  kai, jan'11
public class OTFQueueSimLinkAgentsWriter
extends OTFLinkAgentsHandler.Writer {
//extends OTFDataWriter<VisLink> implements OTFWriterFactory<VisLink> {
	// yyyy this class should not be used since it is not clear where the corresponding reader is, and so that last possibility
	// to fix the binary channel has gotten lost.  kai, apr'10
	// by default, binary output of this class is linked to OTFLinkLanesAgentsNoParkingHandler
	// so the class can be continued to be used. marcel, apr'10
	// This is good to know.  But the reader in OTFLinkLanesAgentsNoParkingHandler (in fact inherited from
	// ODFLinkAgentsHander) _has_ been modified.  This has been done in an attempt to remain compatible, but in fact
	// we _have_ problems with old *.mvi files.  kai, jun'10
	
	// I am somewhat hopeful that none of this is needed (since it seems the same material as in 
	// OTFLinkAgentsHandler.Writer).  As usual with otfvis, the class itself needs to stay.  kai, jan'11

	private static final long serialVersionUID = -7916541567386865404L;

//	public static final boolean showParked = false;
//
//	protected static final transient Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
//
//	private void writeAllAgents(ByteBuffer out) {
//		// making this private.  I don't see any reason to make it more public.  kai, jan'11
//		
//		// Write additional agent data
//
//		positions.clear();
//		src.getVisData().getVehiclePositions(positions);
//
//		if (showParked) {
//			out.putInt(positions.size());
//
//			for (AgentSnapshotInfo pos : positions) {
//				writeAgent(pos, out);
//			}
//		} else {
//			int valid = 0;
//			for (AgentSnapshotInfo pos : positions) {
//				if (pos.getAgentState() != AgentState.PERSON_AT_ACTIVITY)
//					valid++;
//			}
//			out.putInt(valid);
//
//			for (AgentSnapshotInfo pos : positions) {
//				if (pos.getAgentState() != AgentState.PERSON_AT_ACTIVITY)
//					writeAgent(pos, out);
//			}
//		}
//
//	}
//
//	@Override
//	public void writeConstData(ByteBuffer out) throws IOException {
//		String id = this.src.getLink().getId().toString();
//		ByteBufferUtils.putString(out, id);
//		Point2D.Double.Double linkStart = new Point2D.Double.Double(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast,
//				this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
//		Point2D.Double.Double linkEnd = new Point2D.Double.Double(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad2.offsetEast,
//				this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
//
//		out.putFloat((float) linkStart.x);
//		out.putFloat((float) linkStart.y);
//		out.putFloat((float) linkEnd.x);
//		out.putFloat((float) linkEnd.y);
//		out.putInt(NetworkUtils.getNumberOfLanesAsInt(0, this.src.getLink()));
//	}
//
//	@Override
//	public void writeDynData(ByteBuffer out) throws IOException {
////		out.putFloat((float)this.src.getVisData().getDisplayableTimeCapValue(SimulationTimer.getTime()));
//		out.putFloat((float)0.) ; // yy I don't know where the corresponding reader is so I don't know how to delete this. kai, apr'10
//		writeAllAgents(out);
//	}
//
//	@Override
//	public OTFDataWriter<VisLink> getWriter() {
//		return new OTFQueueSimLinkAgentsWriter();
//	}
//
//	private void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
//		// making this private.  I don't see any reason to make it more public.  kai, jan'11
//
//		String id = pos.getId().toString();
//		ByteBufferUtils.putString(out, id);
//		out.putFloat((float) (pos.getEasting() - OTFServerQuad2.offsetEast));
//		out.putFloat((float) (pos.getNorthing() - OTFServerQuad2.offsetNorth));
//		out.putInt(pos.getUserDefined());
//		out.putFloat((float) pos.getColorValueBetweenZeroAndOne());
//		out.putInt(pos.getAgentState().ordinal());
//	}

}
