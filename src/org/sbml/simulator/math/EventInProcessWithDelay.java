/*
 * SBMLsqueezer creates rate equations for reactions in SBML files
 * (http://sbml.org).
 * Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.sbml.simulator.math;

import java.util.LinkedList;

/**
 * <p>
 * This class represents a compilation of all information calculated during
 * simulation concering events. An EventInProcessWithDelay especially stands for
 * an event with delay, so it can has multiple times of execution and therefore
 * multiple arrays of values from trigger time.
 * </p>
 * 
 * @author Alexander D&ouml;rr
 * @since 1.4
 * @date 2011-03-04
 */
public class EventInProcessWithDelay extends EventInProcess {

	private LinkedList<Double> execTimes;
	private LinkedList<Double[]> values;

	/**
	 * Creates a new EventInProcessWithDelay with the given boolean value
	 * indicating whether or not it can fire at time point 0d.
	 * 
	 * @param fired
	 */
	EventInProcessWithDelay(boolean fired) {
		super(fired);
		this.execTimes = new LinkedList<Double>();
		this.values = new LinkedList<Double[]>();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.simulator.math.EventInProcess#addValues(java.lang.Double[],
	 * double)
	 */
	@Override
	public void addValues(Double[] values, double time) {
		int index;
		index = insertTime(time);
		this.execTimes.add(index, time);
		this.values.add(index, values);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.simulator.math.EventInProcess#executed()
	 */
	@Override
	public void executed() {
		values.poll();
		execTimes.poll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.simulator.math.EventInProcess#getTime()
	 */
	@Override
	public double getTime() {
		return execTimes.peek();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.simulator.math.EventInProcess#getValues()
	 */
	@Override
	public Double[] getValues() {
		return values.peek();
	}

	/**
	 * Due to the fact that events with delay can trigger multiple times before
	 * execution, the time of execution and the corresponding values have to be
	 * inserted at the chronological correct position in the list.
	 * 
	 * @param time
	 * @return the index where time has been inserted
	 */
	private int insertTime(double time) {
		if (execTimes.isEmpty()) {
			return 0;
		}

		for (int i = 0; i < execTimes.size(); i++) {
			if (time < execTimes.get(i)) {
				return i;
			}
		}

		return execTimes.size();

	}

}
