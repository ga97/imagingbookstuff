/*******************************************************************************
 * This software is provided as a supplement to the authors' textbooks on digital
 * image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause 
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause). 
 * Copyright (c) 2006-2015 Wilhelm Burger, Mark J. Burge. 
 * All rights reserved. Visit http://www.imagingbook.com for additional details.
 *  
 *******************************************************************************/

package imagingbook.lib.interpolation;
import imagingbook.lib.image.ImageAccessor;


public class NearestNeighborInterpolator extends PixelInterpolator {
	
	public NearestNeighborInterpolator() {
	}
	
	@Override
	public float getInterpolatedValue(ImageAccessor.Scalar ia, double x, double y) {
		final int u = (int) Math.rint(x);
		final int v = (int) Math.rint(y);
		return ia.getVal(u, v);
	}
	
}
