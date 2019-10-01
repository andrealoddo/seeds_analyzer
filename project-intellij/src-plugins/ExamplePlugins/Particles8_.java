import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.MeasurementsWriter;
import java.util.*;

// Particles8_  by Gabriel Landini, G.Landini@bham.ac.uk
// Copyright (c) G. Landini
// 1.0   released
// 1.1   background is white when labelling black particles, cleaned code
// 1.2   added centre of mass & coordinates. Cleaned mess with newcolor and 253
// 1.3   fixed string.equals instead of ==
// 1.4   fixed problem with wrong labelling 253 if deleting borders
// 1.5   fixed results table erased when no output was selected
// 1.6   changed dialog entries similar to those in Particle Analyzer
//         data is sent to ResultsTable so it can be read by macros
//         asks before overwriting Results
//         added option to overwrite Results (so macros do not stop)
// 1.7   added Feret's diameter, Convex hull & area, Breadth & its coordinates,
//         coordinates & angle of Feret with horizontal, minimal bounding circle radius & centre,
//         MaxRadius, MinRadius, ROI coordinates. Included feret routine in CHull one
// 1.8   30/Apr/2005 fixed a bug to properly delete single pixels at borders if "exclude edge particles" is checked
//         added CountCorrect factor for unbiased counting (must exclude edge particles to be correct).
//         See Russ' Image Processing Handbook 3rd ed, p 529.
// 1.8a 21/May/2005 fixed a bug in the minimal bounding circle routine
// 1.9   2/July/2005 added Label (original image name in a stack) changed names so they do not get messed up in SPSS
// 1.10 2/Dec/2005 test for squared pixels from calibration values
// 2.0   17/Dec/2005 Redirection to a greyscale image, added particle greyscale statistics
// 2.1   14/May/2006 Remove the "\n" from the slice label, otherwise the Results table is not formatted correctly
// 2.2   23/Aug/2006 Better use of Results Table.
// 2.3   16/Jun/2007 added redirection to RGB
// 2.4   25/Aug/2007 if Overwrite is false, the data is appended to the Results table.
//         When run from the IJ menu and Overwrite is true, it will ask to save any data
//         in the Results Table. When running from a macro, no questions are asked.
// 2.5   10/Oct/2007  Fixed a bug in the centre of mass coordinate that overflowed when applied to very large particles.
// 2.6   18/May/2008 Speed up using flood fill instead of scanning up and down the blob bounding box
// 2.7   21/May/2008 Single plugin to do morphology or fast analysis. Renamed Particles8_
// 2.8   28/May/2008 fast erasing if using filtering, removed feret drawing (use a macro instead)
// 2.9   17/Jul/2008 does the CentreOfMass at the end, otherwise the point might fall in a near particle and that
//         locks the labelling. Coordinates are done at the end too.
//2.10    19/Feb/2009 Faster results table, thanks to Wayne Rasband
//2.11    19/Feb/2009 fixed bug introduced in 2.10
//2.12  14/Sep/2010 allow redirection to 16 bits images

public class Particles8_ implements PlugInFilter {
	protected String selectedOption;
	protected boolean doIwhite;
	protected boolean doIdeleteborders;
	protected boolean doIminmax;
	protected boolean doIshowstats;
	protected boolean doIlabel;
	protected boolean doImorpho;
	protected boolean doIoverwrite;
	protected int stk=0, mi, ma;
	protected String redirected;
	protected ResultsTable rt= ResultsTable.getResultsTable();

	protected ImageStack istk;
	protected ImagePlus imp2;
	protected ImageProcessor ip2;
	protected int bits;

	public int setup(String arg, ImagePlus imp) {

		int i=0, j=0;

		if (imp==null) {
			IJ.error("No image found.");
			return DONE;
		}
		int  thisimage=imp.getID();
		//ImagePlus imp = WindowManager.getCurrentImage();
		Calibration cal = imp.getCalibration();
		if(cal.pixelWidth!=cal.pixelHeight){
			IJ.error("Image pixels are not squared.");
			return DONE;
		}

		//Recupera delle statistiche globali dell'immagine quali istogramma,
		ImageStatistics stats;
		stats=imp.getStatistics();

		//verifica se l'immagine è binaria
		if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return DONE;
		}

		istk = imp.getStack();
		int[] wList = WindowManager.getIDList();

		String[] titles = new String[wList.length];
		String none = "None";
		titles[0] = none;

		// avoid redirecting to itself
		for (i=0; i<wList.length; i++) {
			if (wList[i]!=thisimage){
				j++;
				imp = WindowManager.getImage(wList[i]);//getImage
				titles[j] = imp!=null?imp.getTitle():"";
				//IJ.log(titles[j]);
			}
		}

		if (arg.equals("about"))
			{showAbout(); return DONE;}
		if (IJ.versionLessThan("1.42j")){
			IJ.error("ImageJ 1.42j or newer required.");
			return DONE;
		}

		GenericDialog gd = new GenericDialog("Particles8_", IJ.getInstance());
		gd.addMessage("Particles8_ v2.12 by G. Landini");
		gd.addMessage("Only \'square pixel\' images supported!");

		gd.addCheckbox("White particles [255] on black [0] background",true);
		gd.addCheckbox("Exclude edge particles",false);
		gd.addCheckbox("Label Particles",false);
		gd.addCheckbox("Morphology Parameters",false);
		String [] DisplayOption={"Particles", "Coordinates", "CentreOfMass"};
		gd.addChoice("Show", DisplayOption, DisplayOption[0]);
		gd.addCheckbox("Filter by size (pixels):",false);
		gd.addNumericField ("Minimum size:",  0, 0);
		gd.addNumericField ("Maximum size:",  9999999, 0);
		gd.addCheckbox("Display results",true);
		gd.addCheckbox("Overwrite results",false);
		gd.addChoice("Redirect to", titles, titles[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;

		doIwhite = gd.getNextBoolean ();
		doIdeleteborders = gd.getNextBoolean ();
		doIlabel = gd.getNextBoolean ();
		doImorpho = gd.getNextBoolean ();
		//boolean doReset = doImorpho && rt.getCounter()>1 && rt.getColumnIndex("Roundness")<0; //wr
		selectedOption = gd.getNextChoice ();

		doIminmax = gd.getNextBoolean ();
		mi = (int)gd.getNextNumber();
		ma = (int)gd.getNextNumber();
		doIshowstats = gd.getNextBoolean ();
		doIoverwrite = gd.getNextBoolean ();
		if (doIoverwrite){ //wr
			if (!IJ.macroRunning()){
				if (rt.getCounter()>0) {
					SaveChangesDialog di = new SaveChangesDialog(IJ.getInstance(), "Save "+rt.getCounter()+" measurements?");
					if (di.cancelPressed())
						return DONE;
					else if (di.savePressed())
						new MeasurementsWriter().run("");
				}
			}
			rt.reset();
		}
		//rt.show("Results");

		redirected = gd.getNextChoice ();
		if (!redirected.equals("None")){
			imp2=WindowManager.getImage(redirected);
			ip2 = imp2.getProcessor();
			bits=imp2.getBitDepth();
			if(bits==32) {
				IJ.error("Cannot redirect to 32bit images.");
				return DONE;
			}
		}

		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		stk++;
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		String slabel = istk.getSliceLabel(stk)+"\n"; //add a \n in case it is a null
		        if (slabel.length()>0) slabel=slabel.replaceAll("\\n",""); // remove "\n"
		int X = xe-1, Y = ye-1, XY = X*Y;
		int [] xd = {1,1,1,0,-1,-1,-1,0};
		int [] yd = {-1,0,1,1,1,0,-1,-1};
		int [] g = {6,0,0,2,2,4,4,6};
		double [] z = {1.414213538169861, 1.0, 1.414213538169861, 1.0, 1.414213538169861, 1.0, 1.414213538169861, 1.0};
		double [] chresults = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; // convex hull, cx, cy, radius of minimal bounding circle, convex area, feret, fx1, fy1, fx2, fy2
		int x=0, y=0, x1, y1, q, uy, dy, rx, lx, pixs, fig, x2, y2, py=0, px=0, nn, newcol, f1, fx1=-1, fx2=-1, fy1=-1, fy2=-1, vl, pb1, pb2;
		long m10, m01;
		int pxgrey=0, pxred=0,  pxgreen=0, pxblue=0;
		double nz, narea, mcx, mcy, feret, tferet, rads=180/Math.PI, angle=0.0, breadth1, breadth2, minr, maxr;

		float [] parsR = new float [12];
		float [] parsG = new float [12];
		float [] parsB = new float [12];

		//ResultsTable rt= ResultsTable.getResultsTable();
		Vector vx = new Vector();
		Vector vy = new Vector();

		Vector vr = new Vector();
		Vector vg = new Vector();
		Vector vb = new Vector();


		if (ip2!=null)
			imp2.setSlice(stk); // advance if stack is being redirected

		//double t1 = ip.getMinThreshold();
		//double t2 = ip.getMaxThreshold();

		FloodFiller ff = new FloodFiller(ip);

		// if not white particles then invert
		if (doIwhite==false){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255-ip.getPixel(x,y));
			}
		}

		if(doIdeleteborders){
			ip.setColor(0);
			for (y=0; y<ye; y++){
				if (ip.getPixel(0,y)==255) ff.fill8(0, y);
				if (ip.getPixel(X,y)==255) ff.fill8(X, y);
			}
			for (x=0; x<xe; x++){
				if (ip.getPixel(x,0)==255) ff.fill8(x, 0);
				if (ip.getPixel(x,Y)==255) ff.fill8(x, Y);
			}
		}

		ip.setColor(254);

		fig=0;
		// find first scanned pixel
		for (y=0; y<ye; y++){
			if (stk==1 && (y%25)==0) //wr
				IJ.showProgress(y, ye-1);
			for (x=0; x<xe; x++){
				if (ip.getPixel(x,y)==255){

					vx.clear();
					vy.clear();
					vr.clear();
					vg.clear();
					vb.clear();

					vx.add(new Integer(x));
					vy.add(new Integer(y));

					x1=x;
					y1=y;
					//bounding box
					uy=y;//upper y
					dy=0;//lower y
					rx=0;//right x
					lx=xe;//left x

					nz=0;
					narea=0;
					pixs=0;
					//single pixel?
					if(ip.getPixel(x+1,y)+ip.getPixel(x,y+1)+ip.getPixel(x-1,y+1)+ip.getPixel(x+1,y+1)==0){
						if (doIminmax && (mi>0)) {
							ip.putPixel(x,y,0);
						}
						else{
							ip.putPixel(x,y,((fig  % 251) + 1)); //new colour if labelling
							if (ip2!=null){
								pxgrey = ip2.getPixel(x,y);// look at px,py of redirected
								if (bits==24){
									pxred = (pxgrey & 0xff0000)>>16;
									pxgreen = (pxgrey & 0x00ff00)>>8;
									pxblue = (pxgrey & 0x0000ff);
								}
							}
							pixs=1;
							fig++;

							rt.incrementCounter();
							rt.addLabel(slabel);
							rt.addValue("Slice", stk);
							rt. addValue("Number", fig);
							rt. addValue("XStart", x);
							rt. addValue("YStart", y);
							rt. addValue("Perim", 0);
							rt. addValue("Area", 0);
							rt. addValue("Pixels", pixs);
							rt. addValue("XM", x);
							rt. addValue("YM", y);
							if (doImorpho){
								rt. addValue("ROIX1", x);
								rt. addValue("ROIY1", y);
								rt. addValue("ROIX2", x);
								rt. addValue("ROIY2", y);
								rt. addValue("MinR", 0);
								rt. addValue("MaxR", 0);
								rt. addValue("Feret", 0);
								rt. addValue("FeretX1", x);
								rt. addValue("FeretY1", y);
								rt. addValue("FeretX2", x);
								rt. addValue("FeretY2", y);
								rt. addValue("FAngle", 0);
								rt. addValue("Breadth", 0);
								rt. addValue("BrdthX1", x);
								rt. addValue("BrdthY1", y);
								rt. addValue("BrdthX2", x);
								rt. addValue("BrdthY2", y);
								rt. addValue("CHull", 0);
								rt. addValue("CArea", 0);
								rt. addValue("MBCX", x);
								rt. addValue("MBCY", y);
								rt. addValue("MBCRadius", 0);
								rt. addValue("CountCorrect", 1);

								rt. addValue("AspRatio",-1);
								rt. addValue("Circ", -1);
								rt. addValue("Roundness", -1);
								rt. addValue("ArEquivD", -1);
								rt. addValue("PerEquivD",-1);
								rt. addValue("EquivEllAr",-1);
								rt. addValue("Compactness", -1);
								rt. addValue("Solidity", -1);
								rt. addValue("Concavity", -1);
								rt. addValue("Convexity", -1);
								rt. addValue("Shape",-1);
								rt. addValue("RFactor",-1);
								rt. addValue("ModRatio",-1);
								rt. addValue("Sphericity",-1);
								rt. addValue("ArBBox", -1);
								rt. addValue("Rectang", -1);
							}
							if (ip2!=null){
								if (bits==24){
									rt. addValue("RedIntDen", pxred);
									rt. addValue("RedMin", pxred);
									rt. addValue("RedMax", pxred);
									rt. addValue("RedMode", pxred);
									rt. addValue("RedMedian", pxred);
									rt. addValue("RedAverage",(float) pxred);
									rt. addValue("RedAvDev", (float) 0);
									rt. addValue("RedStDev", (float) 0);
									rt. addValue("RedVar", (float) 0);
									rt. addValue("RedSkew", (float) 0);
									rt. addValue("RedKurt", (float) 0);
									rt. addValue("RedEntr", (float) 0);

									rt. addValue("GreenIntDen", pxgreen);
									rt. addValue("GreenMin", pxgreen);
									rt. addValue("GreenMax", pxgreen);
									rt. addValue("GreenMode", pxgreen);
									rt. addValue("GreenMedian", pxgreen);
									rt. addValue("GreenAverage",(float) pxgreen);
									rt. addValue("GreenAvDev", (float) 0);
									rt. addValue("GreenStDev", (float) 0);
									rt. addValue("GreenVar", (float) 0);
									rt. addValue("GreenSkew", (float) 0);
									rt. addValue("GreenKurt", (float) 0);
									rt. addValue("GreenEntr", (float) 0);

									rt. addValue("BlueIntDen", pxblue);
									rt. addValue("BlueMin", pxblue);
									rt. addValue("BlueMax", pxblue);
									rt. addValue("BlueMode", pxblue);
									rt. addValue("BlueMedian", pxblue);
									rt. addValue("BlueAverage",(float) pxblue);
									rt. addValue("BlueAvDev", (float) 0);
									rt. addValue("BlueStDev", (float) 0);
									rt. addValue("BlueVar", (float) 0);
									rt. addValue("BlueSkew", (float) 0);
									rt. addValue("BlueKurt", (float) 0);
									rt. addValue("BlueEntr", (float) 0);
								}
								else if (bits==8 || bits==16){
									rt. addValue("GrIntDen", pxgrey);
									rt. addValue("GrMin", pxgrey);
									rt. addValue("GrMax", pxgrey);
									rt. addValue("GrMode", pxgrey);
									rt. addValue("GrMedian", pxgrey);
									rt. addValue("GrAverage",(float) pxgrey);
									rt. addValue("GrAvDev", (float) 0);
									rt. addValue("GrStDev", (float) 0);
									rt. addValue("GrVar", (float) 0);
									rt. addValue("GrSkew", (float) 0);
									rt. addValue("GrKurt", (float) 0);
									rt. addValue("GrEntr", (float) 0);
								}
							}
							rt.addResults(); //wr gl
						}
					}
					else { //go around the edge
						x1=x;
						y1=y;
						ip.putPixel(x,y,254);
						q=0;
						while(true){
							if (ip.getPixel(x1+xd[q],y1+yd[q])>0){
								x2=x1;
								y2=y1;
								x1+=xd[q];
								y1+=yd[q];
								narea+=(double)((y1+y2)*(x2-x1))*.5;
								ip.putPixel(x1,y1,254);
								//update coordinates of ROI box
								if(x1 > rx) rx = x1;
								if(x1 < lx) lx = x1;
								if(y1 > dy) dy = y1;
								if(y1 < uy) uy = y1;
								nz+=z[q]; //perimeter
								q=g[q];

								//check for *that* unique pixel configuration
								if (x1==x){
									if (y1==y){
										if (ip.getPixel(x-1,y+1)!=255){
											break;
										}
									}
								}
								vx.add(new Integer(x1));
								vy.add(new Integer(y1));
							}
							else
								q=(q+1)%8;
						}
						//System.out.println("Debug:"+xe+" "+ye+":   "+ uy+ " "+lx+" "+dy+" "+rx);

						vl=vx.size();
						// Convex hull here
						convh( vx, vy, vl, chresults);
						//IJ.log(""+results[0]+" "+results[1]+" "+results[2]+" "+results[3]);

						// feret diameter here
						feret=chresults[5];
						fx1=(int)chresults[6];
						fy1=(int)chresults[7];
						fx2=(int)chresults[8];
						fy2=(int)chresults[9];

						//IF f2x - fx = 0 THEN THETA = 90 ELSE THETA = -INT(ATN((f2y - fy) / (f2x - fx)) * 57.29583)
						if (fx2==fx1)
							angle=90.0;
						else
							angle=-Math.atan((double)(fy2-fy1)/(double)(fx2-fx1))*rads;
						if (angle<0)
							angle=180+angle;

						// breadth here
						breadth1=0;
						breadth2=0;
						pb1=0;
						pb2=0;
						for (f1=0;f1<vl;f1++){
							tferet=((fy1-fy2)*((Integer)(vx.get(f1))).intValue() +(fx2-fx1)*((Integer)(vy.get(f1))).intValue()+(fx1*fy2 -fx2*fy1))/feret;
							if (tferet>breadth1){
								breadth1=tferet;
								pb1=f1;
							}
							if (tferet<breadth2){
								breadth2=tferet;
								pb2=f1;
							}
						}
						//IJ.log("Breadth1: "+breadth1+"   "+((Integer)(vx.get(pb1))).intValue()+" "+((Integer)(vy.get(pb1))).intValue()+" breadth2: "+breadth2+"   "+((Integer)(vx.get(pb2))).intValue()+" "+((Integer)(vy.get(pb2))).intValue());

						//scan bounding box from top left to bottom right and {flood) with 254 if pixel is 255 and any neighbour pixel is 254
						for(py=uy+1;py<dy;py++) {
							for(px=lx+1;px<rx;px++) {
								if (ip.getPixel(px,py)==255){
									if(ip.getPixel(px-1,py-1)==254 ||
									   ip.getPixel(px,  py-1)==254 ||
									   ip.getPixel(px+1,py-1)==254 ||
									   ip.getPixel(px-1,py  )==254 ||
									   ip.getPixel(px+1,py  )==254 ||
									   ip.getPixel(px+1,py+1)==254 ||
									   ip.getPixel(px,  py+1)==254 ||
									   ip.getPixel(px-1,py+1)==254)
										ff.fill8(px, py);
								}
							}
						}

						newcol = (fig  % 251) + 1; //new colour if labelling
						m10 = 0;
						m01 = 0;
						mcx=0.0;
						mcy=0.0;
						if(doIminmax) //new test
							nn = 253; //must have an extra colour not used elsewhere
						 else
						 	nn = newcol;

						//momentum
						for(py=uy;py<=dy;py++){
							for(px=lx;px<=rx;px++){
								if(ip.getPixel(px,py)== 254){
									pixs+=1;
									m10+=px;
									m01+=py;
									ip.putPixel(px,py,nn); //label particle either newcol or 253 (to be tested for deletion).
									if (ip2!=null){
										if (bits==24){
											pxgrey=ip2.getPixel(px,py);
											vr.add(new Integer(((pxgrey & 0xff0000)>>16)));
											vg.add(new Integer(((pxgrey & 0x00ff00)>>8)));
											vb.add(new Integer((pxgrey & 0x0000ff)));
										}
										else
											vr.add(new Integer(ip2.getPixel(px,py)));// look at px,py of original to get greys, put into vector
									}
								}
							}
						}
						if (ip2!=null){
							if (bits==24){
								stats(pixs, vr, parsR);
								stats(pixs, vg, parsG);
								stats(pixs, vb, parsB);
							}
							else if (bits==8 || bits==16){
								stats(pixs, vr, parsR);
								 //integral, min, max, mode, median, ave, avedev, sdev, var, skew, kurt, entropy
							}
						}

						mcx = ((double)m10 / pixs);
						mcy = ((double)m01 / pixs);

						//Minimal Maximal radii
						minr=Double.MAX_VALUE; //100000000000000.0;
						maxr=-1;
						for (f1=0;f1<vl;f1++){
							tferet=Math.pow(mcx-((Integer)(vx.get(f1))).intValue(),2) +Math.pow(mcy-((Integer)(vy.get(f1))).intValue(),2);
							if (tferet>maxr){
								maxr=tferet;
							}
							if (tferet<minr){
								minr=tferet;
							}
						}
						minr=Math.sqrt(minr);
						maxr=Math.sqrt(maxr);
						//IJ.log("MinR: "+minr+"   MaxR:"+maxr);

						if (doIminmax){ //is colour 253
							if (pixs<mi || pixs>ma){ // erase (filter) particles
								for(py=uy;py<=dy;py++){
									for(px=lx;px<=rx;px++){
										if(ip.getPixel(px,py)== nn) //nn=253, delete it
											ip.putPixel(px,py,0);
									}
								}
							}
							else{ //particle not to be deleted...
								if (selectedOption.equals("Coordinates") || selectedOption.equals("CentreOfMass")){
									ip.setColor(0); //erase it
									ff.fill8(x, y);
									ip.setColor(254);
								}
								else{ //change colour from 253 to newcol
									for(py=uy;py<=dy;py++){
										for(px=lx;px<=rx;px++){
											if(ip.getPixel(px,py)== nn) //253
												ip.putPixel(px,py,newcol);
										}
									}
								}
								fig++;

								rt.incrementCounter();
								rt.addLabel(slabel);
								rt. addValue("Slice" , stk);
								rt. addValue("Number", fig);
								rt. addValue("XStart", x);
								rt. addValue("YStart", y);
								rt. addValue("Perim", (float)nz);
								rt. addValue("Area", (float)narea);
								rt. addValue("Pixels", pixs);
								rt. addValue("XM", (float)mcx);
								rt. addValue("YM", (float)mcy);
								if (doImorpho){
									rt. addValue("ROIX1", lx);
									rt. addValue("ROIY1", uy);
									rt. addValue("ROIX2", rx);
									rt. addValue("ROIY2", dy);
									rt. addValue("MinR", (float)minr);
									rt. addValue("MaxR", (float)maxr);
									rt. addValue("Feret", (float)feret);
									rt. addValue("FeretX1", fx1);
									rt. addValue("FeretY1", fy1);
									rt. addValue("FeretX2", fx2);
									rt. addValue("FeretY2", fy2);
									rt. addValue("FAngle", (float)angle);
									rt. addValue("Breadth",(float)(Math.abs(breadth1)+Math.abs(breadth2)));
									rt. addValue("BrdthX1",((Integer)(vx.get(pb1))).intValue());
									rt. addValue("BrdthY1",((Integer)(vy.get(pb1))).intValue());
									rt. addValue("BrdthX2",((Integer)(vx.get(pb2))).intValue());
									rt. addValue("BrdthY2",((Integer)(vy.get(pb2))).intValue());
									rt. addValue("CHull", (float)chresults[0]);
									rt. addValue("CArea", (float)chresults[4]);
									rt. addValue("MBCX", (float)chresults[1]);
									rt. addValue("MBCY", (float)chresults[2]);
									rt. addValue("MBCRadius", (float)chresults[3]);
									rt. addValue("CountCorrect", (float)((double)XY/((X-(1+rx-lx))*(Y-(1+dy-uy)))));

									rt. addValue("AspRatio", (((Math.abs(breadth1)+Math.abs(breadth2))<0.01)?-1:(float)(feret/(double)(Math.abs(breadth1)+Math.abs(breadth2)))));
									rt. addValue("Circ", (float) (4.0*Math.PI*narea/Math.pow(nz,2)));
									rt. addValue("Roundness", (float) (4.0*narea/(Math.PI*Math.pow(feret,2))));
									rt. addValue("ArEquivD", (float) Math.sqrt((4.0/Math.PI)*narea));
									rt. addValue("PerEquivD", (float) (narea/Math.PI));
									rt. addValue("EquivEllAr", (float) (Math.PI*feret*(Math.abs(breadth1)+Math.abs(breadth2))/4.0));
									rt. addValue("Compactness", (float) (Math.sqrt((4.0/Math.PI)*narea)/feret));
									rt. addValue("Solidity", ((chresults[4]>0)?(float)(narea/chresults[4]):-1));
									rt. addValue("Concavity", (float) (chresults[4]-narea));
									rt. addValue("Convexity", (float) (chresults[0]/nz));
									rt. addValue("Shape", ((narea>0)?(float) ((nz*nz)/narea):-1));
									rt. addValue("RFactor", (float) (chresults[0]/(feret*Math.PI)));
									rt. addValue("ModRatio", (float) ((2.0*minr)/feret));
									rt. addValue("Sphericity", (float) (minr/maxr));
									rt. addValue("ArBBox", (float) (feret*(Math.abs(breadth1)+Math.abs(breadth2))));
									rt. addValue("Rectang",   (((Math.abs(breadth1)+Math.abs(breadth2))<0.01)?-1:(float)(narea/(feret*(Math.abs(breadth1)+Math.abs(breadth2))))));
								}
								if (ip2!=null){
									if (bits==24){
										rt. addValue("RedIntDen", (int) parsR[0]);
										rt. addValue("RedMin",  (int)parsR[1]);
										rt. addValue("RedMax", (int)parsR[2]);
										rt. addValue("RedMode", (int)parsR[3]);
										rt. addValue("RedMedian", (int)parsR[4]);
										rt. addValue("RedAverage", parsR[5]);
										rt. addValue("RedAvDev", parsR[6]);
										rt. addValue("RedStDev", parsR[7]);
										rt. addValue("RedVar", parsR[8]);
										rt. addValue("RedSkew", parsR[9]);
										rt. addValue("RedKurt", parsR[10]);
										rt. addValue("RedEntr", parsR[11]);

										rt. addValue("GreenIntDen", (int)parsG[0]);
										rt. addValue("GreenMin", (int)parsG[1]);
										rt. addValue("GreenMax", (int)parsG[2]);
										rt. addValue("GreenMode", (int)parsG[3]);
										rt. addValue("GreenMedian", (int)parsG[4]);
										rt. addValue("GreenAverage", parsG[5]);
										rt. addValue("GreenAvDev", parsG[6]);
										rt. addValue("GreenStDev", parsG[7]);
										rt. addValue("GreenVar", parsG[8]);
										rt. addValue("GreenSkew", parsG[9]);
										rt. addValue("GreenKurt", parsG[10]);
										rt. addValue("GreenEntr", parsG[11]);

										rt. addValue("BlueIntDen", (int)parsB[0]);
										rt. addValue("BlueMin", (int)parsB[1]);
										rt. addValue("BlueMax", (int)parsB[2]);
										rt. addValue("BlueMode", (int)parsB[3]);
										rt. addValue("BlueMedian", (int)parsB[4]);
										rt. addValue("BlueAverage", parsB[5]);
										rt. addValue("BlueAvDev", parsB[6]);
										rt. addValue("BlueStDev", parsB[7]);
										rt. addValue("BlueVar", parsB[8]);
										rt. addValue("BlueSkew", parsB[9]);
										rt. addValue("BlueKurt", parsB[10]);
										rt. addValue("BlueEntr", parsB[11]);
									}
									else if (bits==8 || bits==16){
										rt. addValue("GrIntDen", (int) parsR[0]);
										rt. addValue("GrMin", (int)parsR[1]);
										rt. addValue("GrMax", (int)parsR[2]);
										rt. addValue("GrMode", (int)parsR[3]);
										rt. addValue("GrMedian", (int)parsR[4]);
										rt. addValue("GrAverage", parsR[5]);
										rt. addValue("GrAvDev", parsR[6]);
										rt. addValue("GrStDev", parsR[7]);
										rt. addValue("GrVar", parsR[8]);
										rt. addValue("GrSkew", parsR[9]);
										rt. addValue("GrKurt", parsR[10]);
										rt. addValue("GrEntr", parsR[11]);
									} //integral, min, max, mode, median, ave, avedev, sdev, var, skew, kurt
								}
								rt.addResults(); //wr gl
							}
						}
						else{ //particle is already the correct colour
							if (selectedOption.equals("Coordinates") || selectedOption.equals("CentreOfMass" )){
								ip.setColor(0); //erase it
								ff.fill8(x, y);
								ip.setColor(254);
							}
							fig++;

							rt.incrementCounter();
							rt.addLabel(slabel);
							rt. addValue("Slice", stk);
							rt. addValue("Number", fig);
							rt. addValue("XStart", x);
							rt. addValue("YStart", y);
							rt. addValue("Perim", (float)nz);
							rt. addValue("Area", (float)narea);
							rt. addValue("Pixels", pixs);
							rt. addValue("XM", (float)mcx);
							rt. addValue("YM", (float)mcy);
							if (doImorpho){
								rt. addValue("ROIX1", lx);
								rt. addValue("ROIY1", uy);
								rt. addValue("ROIX2", rx);
								rt. addValue("ROIY2", dy);
								rt. addValue("MinR", (float)minr);
								rt. addValue("MaxR", (float)maxr);
								rt. addValue("Feret", (float)feret);
								rt. addValue("FeretX1", fx1);
								rt. addValue("FeretY1", fy1);
								rt. addValue("FeretX2", fx2);
								rt. addValue("FeretY2", fy2);
								rt. addValue("FAngle", (float)angle);
								rt. addValue("Breadth",(float)(Math.abs(breadth1)+Math.abs(breadth2)));
								rt. addValue("BrdthX1",((Integer)(vx.get(pb1))).intValue());
								rt. addValue("BrdthY1",((Integer)(vy.get(pb1))).intValue());
								rt. addValue("BrdthX2",((Integer)(vx.get(pb2))).intValue());
								rt. addValue("BrdthY2",((Integer)(vy.get(pb2))).intValue());
								rt. addValue("CHull", (float)chresults[0]);
								rt. addValue("CArea", (float)chresults[4]);
								rt. addValue("MBCX", (float)chresults[1]);
								rt. addValue("MBCY", (float)chresults[2]);
								rt. addValue("MBCRadius", (float)chresults[3]);
								rt. addValue("CountCorrect", (float)((double)XY/((X-(1+rx-lx))*(Y-(1+dy-uy)))));

								rt. addValue("AspRatio", (((Math.abs(breadth1)+Math.abs(breadth2))<0.01)?-1:(float)(feret/(double)(Math.abs(breadth1)+Math.abs(breadth2)))));
								rt. addValue("Circ", (float) (4.0*Math.PI*narea/Math.pow(nz,2)));
								rt. addValue("Roundness", (float) (4.0*narea/(Math.PI*Math.pow(feret,2))));
								rt. addValue("ArEquivD", (float) Math.sqrt((4.0/Math.PI)*narea));
								rt. addValue("PerEquivD", (float) (narea/Math.PI));
								rt. addValue("EquivEllAr", (float) (Math.PI*feret*(Math.abs(breadth1)+Math.abs(breadth2))/4.0));
								rt. addValue("Compactness", (float) (Math.sqrt((4.0/Math.PI)*narea)/feret));
								rt. addValue("Solidity", ((chresults[4]>0)?(float)(narea/chresults[4]):-1));
								rt. addValue("Concavity", (float) (chresults[4]-narea));
								rt. addValue("Convexity", (float) (chresults[0]/nz));
								rt. addValue("Shape", ((narea>0)?(float) ((nz*nz)/narea):-1));
								rt. addValue("RFactor", (float) (chresults[0]/(feret*Math.PI)));
								rt. addValue("ModRatio", (float) ((2.0*minr)/feret));
								rt. addValue("Sphericity", (float) (minr/maxr));
								rt. addValue("ArBBox", (float) (feret*(Math.abs(breadth1)+Math.abs(breadth2))));
								rt. addValue("Rectang", (((Math.abs(breadth1)+Math.abs(breadth2))<0.01)?-1:(float)(narea/(feret*(Math.abs(breadth1)+Math.abs(breadth2))))));
							}

							if (ip2!=null){
								if (bits==24){
									rt. addValue("RedIntDen", (int) parsR[0]);
									rt. addValue("RedMin",  (int)parsR[1]);
									rt. addValue("RedMax", (int)parsR[2]);
									rt. addValue("RedMode", (int)parsR[3]);
									rt. addValue("RedMedian", (int)parsR[4]);
									rt. addValue("RedAverage", parsR[5]);
									rt. addValue("RedAvDev", parsR[6]);
									rt. addValue("RedStDev", parsR[7]);
									rt. addValue("RedVar", parsR[8]);
									rt. addValue("RedSkew", parsR[9]);
									rt. addValue("RedKurt", parsR[10]);
									rt. addValue("RedEntr", parsR[11]);

									rt. addValue("GreenIntDen", (int)parsG[0]);
									rt. addValue("GreenMin", (int)parsG[1]);
									rt. addValue("GreenMax", (int)parsG[2]);
									rt. addValue("GreenMode", (int)parsG[3]);
									rt. addValue("GreenMedian", (int)parsG[4]);
									rt. addValue("GreenAverage", parsG[5]);
									rt. addValue("GreenAvDev", parsG[6]);
									rt. addValue("GreenStDev", parsG[7]);
									rt. addValue("GreenVar", parsG[8]);
									rt. addValue("GreenSkew", parsG[9]);
									rt. addValue("GreenKurt", parsG[10]);
									rt. addValue("GreenEntr", parsG[11]);

									rt. addValue("BlueIntDen", (int)parsB[0]);
									rt. addValue("BlueMin", (int)parsB[1]);
									rt. addValue("BlueMax", (int)parsB[2]);
									rt. addValue("BlueMode", (int)parsB[3]);
									rt. addValue("BlueMedian", (int)parsB[4]);
									rt. addValue("BlueAverage", parsB[5]);
									rt. addValue("BlueAvDev", parsB[6]);
									rt. addValue("BlueStDev", parsB[7]);
									rt. addValue("BlueVar", parsB[8]);
									rt. addValue("BlueSkew", parsB[9]);
									rt. addValue("BlueKurt", parsB[10]);
									rt. addValue("BlueEntr", parsB[11]);
								}
								else if (bits==8 || bits==16){
									rt. addValue("GrIntDen", (int) parsR[0]);
									rt. addValue("GrMin", (int)parsR[1]);
									rt. addValue("GrMax", (int)parsR[2]);
									rt. addValue("GrMode", (int)parsR[3]);
									rt. addValue("GrMedian", (int)parsR[4]);
									rt. addValue("GrAverage", parsR[5]);
									rt. addValue("GrAvDev", parsR[6]);
									rt. addValue("GrStDev", parsR[7]);
									rt. addValue("GrVar", parsR[8]);
									rt. addValue("GrSkew", parsR[9]);
									rt. addValue("GrKurt", parsR[10]);
									rt. addValue("GrEntr", parsR[11]);
								}
							}
							rt.addResults(); //wr gl
						}
					}
				}
			}
		}

		//if not labelling, change the colour back to 255
		if (doIlabel==false){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					if (ip.getPixel(x,y)>0)
						ip.putPixel(x,y,255);
				}
			}
			//since the centre of mass is done at the end, coordinates are done here too
			if(selectedOption.equals("Coordinates")){
				for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
					ip.putPixel((int)rt.getValue("XStart",x),(int)rt.getValue("YStart",x),255);//.. put startPixel
			}

			// do the centre of mass at the end because it may fall inside some other particle and that locks the labelling
			if(selectedOption.equals("CentreOfMass")){
				for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
					ip.putPixel((int)Math.floor(rt.getValue("XM",x)+0.5),(int)Math.floor(rt.getValue("YM",x)+0.5),255);//.. put centre of mass
			}

			// if white particles, invert
			if (doIwhite==false){
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++)
						ip.putPixel(x,y,255-ip.getPixel(x,y));
				}
			}
		}
		else{
			if(selectedOption.equals("Coordinates")){
				y=0;
				for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
					ip.putPixel((int)rt.getValue("XStart",x),(int)rt.getValue("YStart",x),(y++  % 251) + 1);//.. put startPixel
			}

			if(selectedOption.equals("CentreOfMass")){
				y=0;
				for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
					ip.putPixel((int)Math.floor(rt.getValue("XM",x)+0.5),(int)Math.floor(rt.getValue("YM",x)+0.5),(y++  % 251) + 1);//.. put centre of mass
			}

			if (doIwhite==false){
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++){
						if (ip.getPixel(x,y)==0)
							ip.putPixel(x,y,255); //white background
					}
				}
			}
		}
		if (doIshowstats) rt.updateResults(); //wr
		IJ.showProgress(1.0);
	}

	void convh(Vector vx, Vector vy, int vl, double [] results) {

		int i, j, k=0, m;

		int[] x = new int[vl+1];
		int[] y = new int[vl+1];

		results[0]=0.0;
		results[1]=0.0;
		results[2]=0.0;
		results[3]=0.0;
		results[4]=0.0;

		for (j=0;j<vl;j++){
			x[j] = ((Integer)(vx.get(j))).intValue();
			y[j] = ((Integer)(vy.get(j))).intValue();
		}
		//x[vl]=x[0];
		//y[vl]=y[0];

		//cnvx hull
		int n=vl, min = 0, ney=0, h, h2, dx, dy, temp,ax, ay;
		double minangle, th, t, v, zxmi=0;

		for (i=1;i<n;i++){
			if (y[i] < y[min])
				min = i;
		}

		temp = x[0]; x[0] = x[min]; x[min] = temp;
		temp = y[0]; y[0] = y[min]; y[min] = temp;
		min = 0;

		for (i=1;i<n;i++){
			if (y[i] == y[0]){
				ney ++;
				if (x[i] < x[min]) min = i;
			}
		}
		temp = x[0]; x[0] = x[min]; x[min] = temp;
		temp = y[0]; y[0] = y[min]; y[min] = temp;

		min = 0;
		m = -1;
		x[n] = x[min];
		y[n] = y[min];
		if (ney > 0)
			minangle = -1;
		else
			minangle = 0;

		while (min != n ){
			m = m + 1;
			temp = x[m]; x[m] = x[min]; x[min] = temp;
			temp = y[m]; y[m] = y[min]; y[min] = temp;

			min = n ; //+1
			v = minangle;
			minangle = 360.0;
			h2 = 0;

			for (i = m + 1;i<n+1;i++){
				dx = x[i] - x[m];
				ax = Math.abs(dx);
				dy = y[i] - y[m];
				ay = Math.abs(dy);

				if (dx == 0 && dy == 0)
					t = 0.0;
				else
					t = (double)dy / (double)(ax + ay);

				if (dx < 0)
					t = 2.0 - t;
				else {
					if (dy < 0)
						t = 4.0 + t;
				}
				th = t * 90.0;

				if(th > v){
					if(th < minangle){
						min = i;
						minangle = th;
						h2 = dx * dx + dy * dy;
					}
					else{
						if (th == minangle){
							h = dx * dx + dy * dy;
							if (h > h2){
								min = i;
								h2 = h;
							}
						}
					}
				}
			}
			zxmi += Math.sqrt(h2);
		}
		m++;

		int[] hx = new int[m];// ROI polygon array
		int[] hy = new int[m];

		for (i=0;i<m;i++){
			hx[i] =  x[i]; // copy to new polygon array
			hy[i] =  y[i];
			//IJ.log(" "+hx[i]+" "+hy[i]);
			if (i>0)
				results[4] += (double)((hy[i]+hy[i-1])*(hx[i-1]-hx[i]))*.5;
		}
		results[4] += (double)((hy[i-1]+hy[0])*(hx[i-1]-hx[0]))*.5;
		//IJ.log("Hull_points: "+ m);
		//IJ.log("Hull_length: "+(float)zxmi);
		//IJ.log("Hull_area"+results[4]);
		results[0]=(float)zxmi;
		// get the edges between points
		double [] d = new double [(m *(m-1))/2]; // edge lenght
		int[] p1 = new int [(m *(m-1))/2]; // point 1
		int[] p2 = new int [(m *(m-1))/2]; // point 2

		double feret=-1;
		int pferet1=-1, pferet2=-1;
		k=0;
		for (i=0;i<m-1;i++){
			for (j=i+1;j<m;j++){
				d[k]= Math.sqrt(Math.pow(hx[i]-hx[j], 2.0) + Math.pow(hy[i]-hy[j], 2.0));
				if (d[k]>feret) {feret = d[k]; pferet1=i; pferet2=j;}
				p1[k]=i;
				p2[k]=j;
				k++;
			}
		}
		k--;

		results[5]=feret;
		results[6]=hx[pferet1];
		results[7]=hy[pferet1];
		results[8]=hx[pferet2];
		results[9]=hy[pferet2];

		//sort distances
		boolean sw = true;
		double tempd;
		int p3;
		double tt, tttemp, radius, cx, cy;
		while (sw){
			sw=false;
			for(i=0;i<k-1;i++){
				if (d[i]<d[i+1]){
					tempd = d[i]; d[i] = d[i+1]; d[i+1] = tempd;
					temp = p1[i]; p1[i] = p1[i+1]; p1[i+1] = temp;
					temp = p2[i]; p2[i] = p2[i+1]; p2[i+1] = temp;
					sw = true;
				}
			}
		}

		//IJ.log("1:"+d[0]+" "+p1[0]+" "+p2[0]);
		//IJ.log("2:"+d[1]+" "+p1[1]+" "+p2[1]);
		radius=d[0]/2.0;

		cx=(hx[p1[0]]+hx[p2[0]])/2.0;
		cy=(hy[p1[0]]+hy[p2[0]])/2.0;

		// find largest distance from point c
		//sw=false;
		p3=-1;
		tt=radius;
		for (i=0;i<m;i++){
			tttemp=Math.sqrt(Math.pow(hx[i]-cx, 2.0) + Math.pow(hy[i]-cy, 2.0));
			if(tttemp>tt){
				tt=tttemp;
				//IJ.log("Largest from c: "+Math.sqrt(Math.pow(hx[i]-cx, 2.0) + Math.pow(hy[i]-cy, 2.0))+"  "+radius);
				p3=i;
			}
		}

		if (p3>-1){
			// 3 or more- points circle
			//IJ.log("p3:"+p3);
			//IJ.log("osculating circle p1, p2, p3");
			double [] op1 = new double [2];
			double [] op2 = new double [2];
			double [] op3 = new double [2];
			double [] circ = new double [3];
			double tD=Double.MAX_VALUE;
			int tp1=0, tp2=0, tp3=0, z;

			// GL new test: find the smallest osculating circle that contains all convex hull points
			for (i=0; i<m-2; i++){
				for (j=i+1; j<m-1; j++){
					for (k=j+1; k<m; k++){
						op1[0]=hx[i];
						op1[1]=hy[i];
						op2[0]=hx[j];
						op2[1]=hy[j];
						op3[0]=hx[k];
						op3[1]=hy[k];
						osculating(op1, op2, op3, circ);
						// IJ.log(""+i+ " "+j+" "+k+"   "+circ[2]);
						// store a large dummy radius
						if (circ[2]>0){
							sw=true;
							for (z=0;z<m;z++){
								tttemp=(float)Math.sqrt(Math.pow(hx[z]-circ[0], 2.0) + Math.pow(hy[z]-circ[1], 2.0));
								if(tttemp>circ[2]){
									sw=false; // points are outside it
									break; // don't check any more points
								}
							}
							if(sw){ //no CH points outside
								// store radius & coordinates
								// IJ.log(""+i+ " "+j+" "+k+"   "+circ[2]);
								if (circ[2]<tD){
									tp1=i;
									tp2=j;
									tp3=k;
									tD=circ[2];
								}
							}
						}
					}
				}
			}
			op1[0]=hx[tp1];
			op1[1]=hy[tp1];
			op2[0]=hx[tp2];
			op2[1]=hy[tp2];
			op3[0]=hx[tp3];
			op3[1]=hy[tp3];
			// IJ.log("Solved for:"+tp1+ " "+tp2+" "+tp3);
			osculating(op1, op2, op3, circ);
			radius=circ[2];
			//==========================
			// op1[0]=(double)hx[p1[0]];
			// op1[1]=(double)hy[p1[0]];
			// op2[0]=(double)hx[p2[0]];
			// op2[1]=(double)hy[p2[0]];
			// op3[0]=(double)hx[p3];
			// op3[1]=(double)hy[p3];
			//
			// osculating(op1, op2, op3, circ);
			// radius=circ[2];
			// IJ.log("Bounding circle (3 points) x: "+((float)circ[0])+", y: "+((float)circ[1])+", radius: "+ radius);
			results[1]=circ[0];
			results[2]=circ[1];
		}
		else{
			//2-point circle centred at cx, cy radius
			//IJ.log("Bounding circle (2 points) x: "+cx+", y: "+cy+", radius: "+ radius);
			results[1]=cx;
			results[2]=cy;
		}
		results[3]=radius;
	}


	void osculating( double [] pa, double [] pb, double [] pc, double [] centrad){
		// returns 3 double values: the centre (x,y) coordinates & radius
		// of the circle passing through 3 points pa, pb and pc
		double a, b, c, d, e, f, g;
		if ((pa[0]==pb[0] && pb[0]==pc[0]) || (pa[1]==pb[1] && pb[1]==pc[1])){ //colinear coordinates
			centrad[0]=-1; //x
			centrad[1]=-1; //y
			centrad[2]=-1; //radius
			return;
		}

		a = pb[0] - pa[0];
		b = pb[1] - pa[1];
		c = pc[0] - pa[0];
		d = pc[1] - pa[1];

		e = a*(pa[0] + pb[0]) + b*(pa[1] + pb[1]);
		f = c*(pa[0] + pc[0]) + d*(pa[1] + pc[1]);

		g = 2.0 * ( a * (pc[1] - pb[1]) - b * (pc[0] - pb[0]));
		// If g is 0 then the three points are colinear and no finite-radius
		// circle through them exists. Return -1 for the radius. Somehow this does not
		// work as it should (representation of double number?), so it is trapped earlier.
		centrad[0] = (d * e - b * f) / g;
		centrad[1] = (a * f - c * e) / g;
		centrad[2] =(float) Math.sqrt(Math.pow((pa[0] - centrad[0]),2) + Math.pow((pa[1] - centrad[1]),2));
	}


void stats(int nc, Vector vg, float [] pars){
		// integral, min, max, mode, median, average, avg.deviation, std.deviation, variance, skewness, kurtosis, entropy
		int[] data = new int[nc];
		int i;
		float s = 0, min = Float.MAX_VALUE, max = -Float.MAX_VALUE, totl=0, ave=0, adev=0, sdev=0, var=0, skew=0, kurt=0, mode=0, median=0, p, q, msf=0, nco2 = (float)nc/2;
		double log2= Math.log(2), pr;
		for(i=0;i<nc;i++){
			data[i]= ((Integer)(vg.get(i))).intValue();
			totl+= data[i];
			if(data[i]<min) min = data[i];
			if(data[i]>max) max = data[i];
		}

		ave = totl/nc;

		for(i=0;i<nc;i++){
			s = data[i] - ave;
			adev+=Math.abs(s);
			p = s * s;
			var+= p;
			p*=s;
			skew+= p;
			p*= s;
			kurt+= p;
		}

		adev/= nc;
		var/=nc-1;
		sdev = (float) Math.sqrt(var);

		if(var> 0){
			skew = (float)skew / (nc * (float) Math.pow(sdev,3));
			kurt = (float)kurt / (nc * (float) Math.pow(var, 2)) - 3;
		}
		q=0;
		pr=0;

		int[] hist = new int[(int)max+1];
		for(i=0;i<nc;i++){
			hist[data[i]]++; // load histogram
		}

		for (i=0;i<=max;i++){
			if (hist[i]> msf){
				msf=hist[i];
				mode=i;
			}
			if (hist[i]>0)
				pr+=((double)hist[i]/(double)nc)*(Math.log((double)hist[i]/(double)nc)/log2);

			q+=hist[i];
			if (q<=nco2)
				median=i; //simple median
		}
		if (min==max)
			median=max;// median in a single bin case

		if (pr<0)
			pr=-pr;

		pars[0]=totl;
		pars[1]= min;
		pars[2]= max;
		pars[3]= mode;
		pars[4]= median;
		pars[5]=ave;
		pars[6]=adev;
		pars[7]=sdev;
		pars[8]=var;
		pars[9]=skew;
		pars[10]=kurt;
		pars[11]=(float)pr;
	}


	void showAbout() {
		IJ.showMessage("About Particles8_...",
		"Particles8_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for estimating various statistics of 8-connected\n"+
		"particles.\n"+
		"WARNING!: This plugin does not return the same values as the original\n"+
		"ImageJ \'Analyze Particles\' command because it uses a different concept.\n"+
		"Here, \'Perimeter\' & \'Area\' are measured based on the polygon given by\n"+
		"the centres of the boundary pixels. \'Area\' disregards \'holes\' in the particles\n"+
		"(i.e. it returns the area inside the boundary), but \'Pixels\' does not.\n"+
		"CHull & CArea are also calculated from the centres of the pixels in the convex hull.\n"+
		"Note that 1 pixel particles have 0 area, etc.\n"+
		"Filtered particles are deleted from the image");
	}

}
