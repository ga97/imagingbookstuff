/*******************************************************************************
 * This software is provided as a supplement to the authors' textbooks on digital
 *  image processing published by Springer-Verlag in various languages and editions.
 * Permission to use and distribute this software is granted under the BSD 2-Clause 
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause). 
 * Copyright (c) 2006-2016 Wilhelm Burger, Mark J. Burge. All rights reserved. 
 * Visit http://imagingbook.com for additional details.
 *******************************************************************************/

package imagingbook.pub.regions;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ij.IJ;
import ij.process.ByteProcessor;

/**
 * 2014-11-12: changed to implement ContourTracer.
 *
 */
public class RegionContourLabeling extends RegionLabeling implements ContourTracer { 
	
	static final int VISITED = -1;
	
	private List<Contour> outerContours;
	private List<Contour> innerContours;
	
	// constructors
	
	public RegionContourLabeling (ByteProcessor ip) {
		super(ip);	// all work is done by the constructor of the superclass
	}
	
	// public methods required by interface ContourTracer (others are in inherited from super-class)
	
	public List<Contour> getOuterContours() {
		return copyContours(outerContours, false);
	}
	
	public List<Contour> getOuterContours(boolean sort) {
		return copyContours(outerContours, sort);
	}
	
	public List<Contour> getInnerContours() {
		return copyContours(innerContours, false);
	}
	
	public List<Contour> getInnerContours(boolean sort) {
		return copyContours(innerContours, sort);
	}
	

	// non-public methods
	
	void initialize() {
		// Create a label array which is "padded" by 1 pixel, i.e., 
		// 2 rows and 2 columns larger than the image:
		labelArray = new int[width+2][height+2];	// initialized to zero
		outerContours = new ArrayList<Contour>();
		innerContours = new ArrayList<Contour>();
	}
	
	void applyLabeling() {
		resetLabel();
		// scan top to bottom, left to right
		for (int v = 0; v < height; v++) {
			int label = 0;	// reset label, scan through horiz. line:
			for (int u = 0; u < width; u++) {
				if (ip.getPixel(u, v) > 0) {	// unlabeled FOREGROUND pixel
					if (label != 0) { // keep using the same label
						setLabel(u, v, label);
					}
					else {	// label == zero
						label = getLabel(u, v);
						if (label == 0) {	// new (unlabeled) region is hit
							label = getNextLabel(); // assign a new region label
							Contour oc = traceContour(u, v, 0, label);
							outerContours.add(oc);
							setLabel(u, v, label);
						}
					}
				} 
				else {	// BACKGROUND pixel
					if (label != 0) { // exiting a region
						if (getLabel(u, v) == BACKGROUND) { // unlabeled - new inner contour
							Contour ic = traceContour(u - 1, v, 1, label);
							innerContours.add(ic);
						}
						label = 0;
					}
				}
			}
		}
	}
	
	// Trace one contour starting at (xS,yS) 
	// in direction dS with label label
	// trace one contour starting at (xS,yS) in direction dS	
	Contour traceContour(int xS, int yS, int dS, int label) {
		Contour contr = new Contour(label);
		int xT, yT; // T = successor of starting point (xS,yS)
		int xP, yP; // P = previous contour point
		int xC, yC; // C = current contour point
		Point pt = new Point(xS, yS); 
		int dNext = findNextPoint(pt, dS);
		contr.addPoint(pt); 
		xP = xS; yP = yS;
		xC = xT = pt.x;
		yC = yT = pt.y;
		
		boolean done = (xS == xT && yS == yT);  // true if isolated pixel
		while (!done) {
			setLabel(xC, yC, label);
			pt = new Point(xC, yC);
			int dSearch = (dNext + 6) % 8;
			dNext = findNextPoint(pt, dSearch);
			xP = xC;  yP = yC;	
			xC = pt.x; yC = pt.y; 
			// are we back at the starting position?
			done = (xP==xS && yP==yS && xC==xT && yC==yT);
			if (!done) {
				contr.addPoint(pt);
			}
		}
		return contr;
	}
	
	static final int[][] delta = {
			{ 1,0}, { 1, 1}, {0, 1}, {-1, 1}, 
			{-1,0}, {-1,-1}, {0,-1}, { 1,-1}};
	
	int findNextPoint (Point pt, int dir) { 
		// Starts at Point pt in direction dir,
		// returns the resulting tracing direction
		// and modifies pt.
		for (int i = 0; i < 7; i++) {
			int x = pt.x + delta[dir][0];
			int y = pt.y + delta[dir][1];
			if (ip.getPixel(x, y) == BACKGROUND) {
				setLabel(x, y, VISITED);	// mark surrounding background pixels
				dir = (dir + 1) % 8;
			} 
			else {	// found a non-background pixel (next pixel to follow)
				pt.x = x; 
				pt.y = y; 
				break;
			}
		}
		return dir;
	}
	
	void collectRegions() {
		super.collectRegions();	// collect region pixels and calculate statistics
		attachOuterContours();	// attach each outer contours to the corresponding region
		attachInnerContours();	// attach all inner contours to the corresponding regions
	}
	
	void attachOuterContours() {
		for (Contour c : outerContours) {
			int label = c.getLabel();
			BinaryRegion r = findRegion(label);
			if (r == null) {
				IJ.log("Error: Could not associate outer contour with label " + label);
			}
			else {
				r.setOuterContour(c);
			}
		}
	}
	
	void attachInnerContours() {
		for (BinaryRegion r : regions) {
			r.makeInnerContours();	// ensure that every region has a (empty) list of inner contours
		}
		for (Contour c : innerContours) {
			int label = c.getLabel();
			BinaryRegion r = findRegion(label);
			if (r == null) {
				IJ.log("Error: Could not associate inner contour with label " + label);
			}
			else {
				r.addInnerContour(c);
			}
		}
	}

	// access methods to the label array (which is padded!)
	public int getLabel(int u, int v) {	// (u,v) are image coordinates
		if (u >= -1 && u <= width && v >= -1 && v <= height)
			return labelArray[u + 1][v + 1]; 	// label array is padded (offset = 1)
		else
			return BACKGROUND;
		//return labelArray[u + 1][v + 1];	// original version
	}
	
	void setLabel(int u, int v, int label) { // (u,v) are image coordinates
		if (u >= -1 && u <= width && v >= -1 && v <= height) {
			labelArray[u + 1][v + 1] = label;
		}
	}
	
	private List<Contour> copyContours(List<Contour> cntrs, boolean sort) {
		if (cntrs == null)
			return Collections.emptyList(); 
		else {
			List<Contour> cc = new ArrayList<Contour>(cntrs);
			if (sort) {
				Collections.sort(cc);
			}
			return cc;
		}
	}
	
}

