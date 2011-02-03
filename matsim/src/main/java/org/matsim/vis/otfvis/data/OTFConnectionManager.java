/* *********************************************************************** *
 * project: org.matsim.*
 * OTFConnectionManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.data.fileio.queuesim.OTFQueueSimLinkAgentsWriter;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisNode;

/**
 * The OTFConnectionManager is the most important class when building an OTFVis instance.
 * It holds pairs of classes. Each of this class-pairs yields as a "From" -> "To" connection between classes.
 * The whole from-to connections established in a OTFConnectionManager describe the route all data has to
 * take from the source (normally a QueueLink/Node, etc.) to the actual display on screen.
 * It is the programmer's responsibility to define a complete chain of responsible objects for all data sent.
 *
 *  A chain of responsibility normally consists of
 *  a DataSource (e.g. QueueLink),
 *  a DataWriter (e.g. OTFDefaultLinkHandler.Writer)
 *  a DataReader (e.g. OTFDefaultLinkHandler)
 *  a Visualizer class (e.g. SimpleStaticNetLayer.QuadDrawer)
 *  and possibly a layer this Drawer belongs to (e.g. SimpleStaticNetLayer)
 *
 * @author dstrippgen
 */
public class OTFConnectionManager implements Cloneable, Serializable {

	private static final long serialVersionUID = 6481835753628883014L;

	private static final Logger log = Logger.getLogger(OTFConnectionManager.class);

	private static class Entry implements Serializable {

		private static final long serialVersionUID = -2260651735789627280L;

		Class<?> from, to;

		public Entry(Class<?> from, Class<?> to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public String toString() {
			return "(" + from.toString() + "," + to.toString() + ") ";
		}

		public Class<?> getTo(){
			return this.to;
		}

		public Class<?> getFrom(){
			return this.from;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Entry)) {
				return false;
			}
			Entry other = (Entry) obj;
			return from.equals(other.from) && to.equals(other.to);
		}

		@Override
		public int hashCode() {
			return from.hashCode() * to.hashCode();
		}

	}

	private final List<Entry> connections = new LinkedList<Entry>();

	@Override
	public OTFConnectionManager clone() {
		OTFConnectionManager clone = new OTFConnectionManager();
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			Entry entry1 = new Entry(entry.from, entry.to);
			clone.connections.add(entry1);
		}
		return clone;
	}

	public void connectQNodeToWriter(Class<? extends OTFDataWriter<? extends VisNode>> writer) {
		Entry entry = new Entry(VisNode.class, writer);
		connections.add(entry);
	}

	public void connectQLinkToWriter(Class<? extends OTFDataWriter<? extends VisLink>> writer) {
		Entry entry = new Entry(VisLink.class, writer);
		connections.add(entry);
	}

	public void connectQueueLinkToWriter(Class<? extends OTFQueueSimLinkAgentsWriter> writer) {
		Entry entry = new Entry(VisLink.class, writer);
		connections.add(entry);
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFWriterFactory<VisLink>>> getQueueLinkEntries() {
		Collection<Class<OTFWriterFactory<VisLink>>> result = new ArrayList<Class<OTFWriterFactory<VisLink>>>();
		for (Class<?> clazz : getToEntries(VisLink.class)) {
			result.add((Class<OTFWriterFactory<VisLink>>) clazz);
		}
		return result;
	}

	public void connectWriterToReader(Class<? extends OTFDataWriter> writer, Class<? extends OTFDataReader> reader) {
		Collection<Class<?>> readerClasses = this.getToEntries(writer);
		if (!readerClasses.isEmpty()) {
			throw new RuntimeException("We already have a reader for this writer.");
		}
		Entry entry = new Entry(writer, reader);
		connections.add(entry);
	}

	public void connectReaderToReceiver(Class<? extends OTFDataReader> reader, Class<? extends OTFDataReceiver> receiver) {
		Collection<Class<?>> receiverClasses = this.getToEntries(reader);
		if (receiverClasses.contains(receiver)) {
			log.warn("Trying to connect the same receiver twice. Ignoring: "+receiver);
		} else {
			Entry entry = new Entry(reader, receiver);
			connections.add(entry);
		}
	}

	public void connectReceiverToLayer(Class<? extends OTFDataReceiver> receiver, Class<? extends SceneLayer> layer) {
		Collection<Class<?>> layerClasses = this.getToEntries(receiver);
		if (layerClasses.contains(layer)) {
			log.warn("Trying to connect the same layer twice. Ignoring: "+receiver);
		} else {
			Entry entry = new Entry(receiver, layer);
			connections.add(entry);
		}
	}

	public void remove(Class<?> from) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (entry.from == from) iter.remove();
		}
	}

	public Collection<Class<?>> getToEntries(Class<?> srcClass) {
		List<Class<?>> classList = new LinkedList<Class<?>>();
		for(Entry entry : connections) {
			if (entry.from.equals(srcClass)) {
			  classList.add(entry.to);
			}
		}
		return classList;
	}

	@SuppressWarnings("unchecked")
	public Collection<OTFDataReceiver> getReceiversForReader(Class<? extends OTFDataReader> reader, SceneGraph graph) {
		Collection<Class<?>> classList = getToEntries(reader);
		Collection<OTFDataReceiver> receivers = new HashSet<OTFDataReceiver>();
		for(Class<?> entry : classList) {
			try {
				receivers.add(graph.newInstanceOf((Class<? extends OTFDataReceiver>) entry));
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return receivers;
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFDataReader>> getReadersForWriter(Class<? extends OTFDataWriter> writer) {
		Collection<Class<?>> classList = getToEntries(writer);
		Collection<Class<OTFDataReader>> readers = new HashSet<Class<OTFDataReader>>();
		for(Class<?> entry : classList) {
			readers.add((Class<OTFDataReader>) entry);
		}
		return readers;
	}

	public void fillLayerMap(Map<Class<?>, SceneLayer> layers) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (SceneLayer.class.isAssignableFrom(entry.to))
				try {
					layers.put(entry.from, (SceneLayer)(entry.to.newInstance()));
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		}
	}

	public void addEntriesFrom(OTFConnectionManager connect2) {
		Iterator<Entry> iter = connect2.connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			log.info("updating entry: " + entry.from.getCanonicalName() + " to " + entry.to.getName());
			this.connections.add(entry);
		}
	}

	public void logEntries() {
		for (Entry e : this.connections){
			log.info("writing entry: " + e.getFrom().getCanonicalName() + " to " + e.getTo().getName());
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFWriterFactory<VisNode>>> getQNodeEntries() {
		Collection<Class<OTFWriterFactory<VisNode>>> result = new ArrayList<Class<OTFWriterFactory<VisNode>>>();
		for (Class<?> clazz : getToEntries(VisNode.class)) {
			result.add((Class<OTFWriterFactory<VisNode>>) clazz);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFWriterFactory<VisLink>>> getQLinkEntries() {
		Collection<Class<OTFWriterFactory<VisLink>>> result = new ArrayList<Class<OTFWriterFactory<VisLink>>>();
		for (Class<?> clazz : getToEntries(VisLink.class)) {
			result.add((Class<OTFWriterFactory<VisLink>>) clazz);
		}
		return result;
	}

}
