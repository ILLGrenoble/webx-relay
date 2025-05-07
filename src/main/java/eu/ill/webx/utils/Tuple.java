/*
 * WebX Relay
 * Copyright (C) 2023 Institut Laue-Langevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ill.webx.utils;

/**
 * A simple tuple class to hold two values.
 * @param <X> the first value type
 * @param <Y> the second value type
 */
public class Tuple<X, Y> {
    private final X x;
    private final Y y;

    /**
     * Constructor to create a tuple with two values.
     * @param x the first value
     * @param y the second value
     */
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Get the first value of the tuple.
     * @return the first value
     */
    public X getX() {
        return x;
    }

    /**
     * Get the second value of the tuple.
     * @return the second value
     */
    public Y getY() {
        return y;
    }
}
