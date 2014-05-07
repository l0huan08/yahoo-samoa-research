/*
 *    AttributeClassObserver.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package com.yahoo.labs.samoa.learners.classifiers.hl.attributeclassobservers;

import java.io.Serializable;


/**
 * Interface for observing the class data distribution for an attribute.
 * This observer monitors the class distribution of a given attribute.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $ 
 *
 * Modified by hl 2014.3   Del useless inherents
 */
public interface AttributeClassObserver extends Serializable {

    /**
     * Updates statistics of this observer given an attribute value, a class
     * and the weight of the instance observed
     *
     * @param attVal the value of the attribute
     * @param classVal the class
     * @param weight the weight of the instance
     */
    public void observeAttributeClass(double attVal, int classVal, double weight);

    /**
     * Gets the probability for an attribute value given a class
     *
     * @param attVal the attribute value
     * @param classVal the class
     * @return probability for an attribute value given a class
     */
    public double probabilityOfAttributeValueGivenClass(double attVal,
            int classVal);

    
}
