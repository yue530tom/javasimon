package org.javasimon.utils;

import org.javasimon.Simon;
import org.javasimon.SimonManager;

import java.text.*;
import java.util.Locale;

/**
 * SimonUtils class holds static utility methods.
 *
 * @author <a href="mailto:virgo47@gmail.com">Richard "Virgo" Richter</a>
 * @created Aug 6, 2008
 */
public final class SimonUtils {
	private static final int UNIT_PREFIX_FACTOR = 1000;

	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.US);
	private static final DecimalFormat UNDER_TEN_FORMAT = new DecimalFormat("0.00", DECIMAL_FORMAT_SYMBOLS);
	private static final DecimalFormat UNDER_HUNDRED_FORMAT = new DecimalFormat("00.0", DECIMAL_FORMAT_SYMBOLS);
	private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("000", DECIMAL_FORMAT_SYMBOLS);

	private SimonUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns nano-time in human readable form with unit. Number is always from 10 to 9999
	 * except for seconds that are the biggest unit used.
	 *
	 * @param nanos time in nanoseconds
	 * @return human readable time string
	 */
	public static String presentNanoTime(long nanos) {
		if (nanos == Long.MAX_VALUE) {
			return "undef";
		}
		if (nanos < UNIT_PREFIX_FACTOR) {
			return nanos + " ns";
		}

		double time = nanos;
		time /= UNIT_PREFIX_FACTOR;
		if (time < UNIT_PREFIX_FACTOR) {
			return formatTime(time, " us");
		}

		time /= UNIT_PREFIX_FACTOR;
		if (time < UNIT_PREFIX_FACTOR) {
			return formatTime(time, " ms");
		}

		time /= UNIT_PREFIX_FACTOR;
		return formatTime(time, " s");
	}

	private static String formatTime(double time, String unit) {
		if (time < 10) {
			return UNDER_TEN_FORMAT.format(time) + unit;
		}
		if (time < 100) {
			return UNDER_HUNDRED_FORMAT.format(time) + unit;
		}
		return DEFAULT_FORMAT.format(time) + unit;
	}

	public static void printSimonTree(Simon simon) {
		StringBuilder sb = new StringBuilder();
		printSimonTree(0, simon, sb);
		System.out.println(sb);
	}

	private static void printSimonTree(int level, Simon simon, StringBuilder sb) {
		printSimon(level, simon, sb);
		for (Simon child : simon.getChildren()) {
			printSimonTree(level + 1, child, sb);
		}
	}

	private static void printSimon(int level, Simon simon, StringBuilder sb) {
		for (int i = 0; i < level; i++) {
			sb.append("  ");
		}
		sb.append(localName(simon.getName()))
			.append('(')
			.append(simon.isEnabled() ? '+' : '-')
			.append("): ")
			.append(simon.toString())
			.append('\n');
	}

	private static String localName(String name) {
		int ix = name.lastIndexOf(SimonManager.HIERARCHY_DELIMITER);
		if (ix == -1) {
			return name;
		}
		return name.substring(ix + 1);
	}
}
