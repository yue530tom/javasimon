package org.javasimon.callback.lastsplits;

import org.javasimon.Split;
import org.javasimon.utils.SimonUtils;

/**
 * Object stored among Stopwatch's attributes in charge of <ul>
 * <li>Managing concurrent access to the inner ring buffer through synchronized blocks</li>
 * <li></li>
 * </ul>
 *
 * @author gquintana
 * @since 3.2.0
 */
public class LastSplits {
	/**
	 * Ring buffer containing splits
	 */
	private final CircularList<Split> splits;

	/**
	 * Constructor with ring buffer size
	 *
	 * @param capacity Buffer size
	 */
	public LastSplits(int capacity) {
		this.splits = new CircularList<Split>(capacity);
	}

	/**
	 * Add split to the buffer
	 *
	 * @param split Split
	 */
	public void add(Split split) {
		synchronized (splits) {
			splits.add(split);
		}
	}

	/**
	 * Remove all splits from buffer
	 */
	public void clear() {
		synchronized (splits) {
			splits.clear();
		}
	}

	/**
	 * Get number of splits in the buffer
	 *
	 * @return Split number
	 */
	public int getCount() {
		synchronized (splits) {
			return splits.size();
		}
	}

	/**
	 * Evaluate a function over the list of splits
	 *
	 * @param <T> Function result type
	 * @param function Function to evaluate
	 * @return Function result, null if no splits
	 */
	private <T> T processFunction(SplitFunction<T> function) {
		synchronized (splits) {
			if (splits.isEmpty()) {
				return null;
			}
			for (Split split : splits) {
				function.evaluate(split);
			}
			return function.result();
		}
	}

	/**
	 * Function
	 *
	 * @param <T> Result type
	 */
	private static interface SplitFunction<T> {
		/**
		 * Called for each split
		 *
		 * @param split Current split
		 */
		void evaluate(Split split);

		/**
		 * Called after all splits
		 *
		 * @return Function result
		 */
		T result();
	}

	/**
	 * Base implementation of functions
	 *
	 * @param <T> Function return type
	 */
	private static abstract class AbstractSplitFunction<T> implements SplitFunction<T> {
		/**
		 * Function result
		 */
		protected T result;

		/**
		 * Initial function result
		 */
		public AbstractSplitFunction(T result) {
			this.result = result;
		}

		/**
		 * Running for duration of the split
		 *
		 * @param runningFor Running for
		 */
		public abstract void evaluate(long runningFor);

		/**
		 * Calls evaluate with split running for duration
		 *
		 * @param split Current split
		 */
		public final void evaluate(Split split) {
			evaluate(split.runningFor());
		}

		/**
		 * Final result
		 */
		public T result() {
			return result;
		}
	}

	/**
	 * Compute mean duration of splits in the buffer
	 *
	 * @return Mean or average
	 */
	public Double getMean() {
		return processFunction(new AbstractSplitFunction<Double>(0.0D) {
			@Override
			public void evaluate(long runningFor) {
				result += (double) runningFor;
			}

			@Override
			public Double result() {
				return result / (double) splits.size();
			}

		});
	}

	/**
	 * Compute the smallest duration of splits in the buffer
	 *
	 * @return Minimum
	 */
	public Long getMin() {
		return processFunction(new AbstractSplitFunction<Long>(Long.MAX_VALUE) {
			@Override
			public void evaluate(long runningFor) {
				if (runningFor < result) {
					result = runningFor;
				}
			}
		});
	}

	/**
	 * Compute the longest duration of splits in the buffer
	 *
	 * @return Maximum
	 */
	public Long getMax() {
		return processFunction(new AbstractSplitFunction<Long>(Long.MIN_VALUE) {
			@Override
			public void evaluate(long runningFor) {
				if (runningFor > result) {
					result = runningFor;
				}
			}
		});
	}

	/**
	 * Compute a trend of duration: the average delta of splits between
	 * 2 splits spaced of at least 1 ms.
	 * Sum(splits(t[n])-splits(t[n-1])/SizeOf(splits)
	 *
	 * @return Trend, average delta of splits
	 */
	public Double getTrend() {
		return getTrend(1000);
	}

	/**
	 * Compute a trend of duration: the average delta of splits between
	 * 2 split spaced of at least the given threshold.
	 * The threshold is only here to avoid computing a delta between 2 splits
	 * occuring at the same time by 2 different threads.
	 * Sum(splits(t[n])-splits(t[n-1])/SizeOf(splits)
	 *
	 * @param timeDeltaThreshold Accepted splits space
	 * @return Trend, average delta of splits
	 */
	public Double getTrend(final long timeDeltaThreshold) {
		return processFunction(new SplitFunction<Double>() {
			Split lastSplit;
			long result;
			int count;

			public void evaluate(Split split) {
				if (lastSplit == null) {
					lastSplit = split;
				} else {
					long timeDelta = split.getStart() - lastSplit.getStart();
					if (timeDelta > timeDeltaThreshold) {
						long durationDelta = split.runningFor() - lastSplit.runningFor();
						result += durationDelta;
						count++;
						lastSplit = split;
					}
				}
			}

			public Double result() {
				return count > 0 ? (result / ((double) count)) : null;
			}
		});
	}

	/**
	 * String containing: count, min, mean, max and trend(1ms).
	 * This method can be expensive, because many computations are done.
	 *
	 * @return String
	 */
	@Override
	public String toString() {
		int count;
		long min = 0, mean = 0, max = 0, trend = 0;
		// First extract data
		synchronized (splits) {
			count = getCount();
			if (count > 0) {
				min = getMin();
				mean = getMean().longValue();
				max = getMax();
			}
			if (count > 1) {
				trend = getTrend().longValue();
			}
		}
		// Then free lock, and format data
		StringBuilder stringBuilder = new StringBuilder("LastSplits[size=");
		stringBuilder.append(count);
		if (count > 0) {
			stringBuilder.append(",min=").append(SimonUtils.presentNanoTime(min))
				.append(",mean=").append(SimonUtils.presentNanoTime(mean))
				.append(",max=").append(SimonUtils.presentNanoTime(max));
		}
		if (count > 1) {
			stringBuilder.append(",trend=").append(SimonUtils.presentNanoTime(trend));
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
