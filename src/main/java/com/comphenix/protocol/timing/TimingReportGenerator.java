package com.comphenix.protocol.timing;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.timing.TimedListenerManager.ListenerType;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.TreeSet;

public class TimingReportGenerator {
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String META_STARTED = "Started: %s" + NEWLINE;
	private static final String META_STOPPED = "Stopped: %s (after %s seconds)" + NEWLINE;
	private static final String PLUGIN_HEADER = "=== PLUGIN %s ===" + NEWLINE;
	private static final String LISTENER_HEADER = " TYPE: %s " + NEWLINE;
	private static final String SEPERATION_LINE = " " + Strings.repeat("-", 139) + NEWLINE;
	private static final String STATISTICS_HEADER =
		" Protocol:      Name:                         ID:                 Count:       Min (ms):       " +
		"Max (ms):       Mean (ms):      Std (ms): " + NEWLINE;
	private static final String STATISTICS_ROW =    " %-15s %-29s %-19s %-12d %-15.6f %-15.6f %-15.6f %.6f " + NEWLINE;
	private static final String SUM_MAIN_THREAD = " => Time on main thread: %.6f ms" + NEWLINE;

	public void saveTo(File destination, TimedListenerManager manager) throws IOException {
		final Date started = manager.getStarted();
		final Date stopped = manager.getStopped();
		final long seconds = Math.abs((stopped.getTime() - started.getTime()) / 1000);

		try (BufferedWriter writer = Files.newWriter(destination, StandardCharsets.UTF_8)) {
			// Write some timing information
			writer.write(String.format(META_STARTED, started));
			writer.write(String.format(META_STOPPED, stopped, seconds));
			writer.write(NEWLINE);

			for (String plugin : manager.getTrackedPlugins()) {
				writer.write(String.format(PLUGIN_HEADER, plugin));

				for (ListenerType type : ListenerType.values()) {
					TimedTracker tracker = manager.getTracker(plugin, type);

					// We only care if it has any observations at all
					if (tracker.getObservations() > 0) {
						writer.write(String.format(LISTENER_HEADER, type));

						writer.write(SEPERATION_LINE);
						saveStatistics(writer, tracker, type);
						writer.write(SEPERATION_LINE);
					}
				}
				// Next plugin
				writer.write(NEWLINE);
			}
		}
	}

	private void saveStatistics(Writer destination, TimedTracker tracker, ListenerType type) throws IOException {
		TimedListenerManager timedListenerManager= new TimedListenerManager();
		Map<PacketType, StatisticsStream> streams = timedListenerManager.getStatistics();
		StatisticsStream sum = new StatisticsStream();
		int count = 0;

		destination.write(STATISTICS_HEADER);
		destination.write(SEPERATION_LINE);

		// Write every packet ID that we care about
		for (PacketType key : new TreeSet<>(streams.keySet())) {
			final StatisticsStream stream = streams.get(key);

			if (stream != null && stream.getCount() > 0) {
				printStatistic(destination, key, stream);

				// Add it
				count++;
				sum = sum.add(stream);
			}
		}

		// Write the sum - if its useful
		if (count > 1) {
			printStatistic(destination, null, sum);
		}
		// These are executed on the main thread
		if (type == ListenerType.SYNC_SERVER_SIDE) {
			destination.write(String.format(SUM_MAIN_THREAD,
				toMilli(sum.getCount() * sum.getMean())
			));
		}
	}

	private void printStatistic(Writer destination, PacketType key, final StatisticsStream stream) throws IOException {
		destination.write(String.format(STATISTICS_ROW,
			key != null ? key.getProtocol() : "SUM",
			key != null ? key.name() : "-",
			key != null ? getPacketId(key) : "-",
			stream.getCount(),
			toMilli(stream.getMinimum()),
			toMilli(stream.getMaximum()),
			toMilli(stream.getMean()),
			toMilli(stream.getStandardDeviation())
		));
	}

	private String getPacketId(PacketType type) {
		return Strings.padStart(Integer.toString(type.getCurrentId()), 2, '0');
	}

	/**
	 * Convert a value in nanoseconds to milliseconds.
	 * @param value - the value.
	 * @return The value in milliseconds.
	 */
	private double toMilli(double value) {
		return value / 1000000.0;
	}
}
