package FourierD;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.util.*;

public class EllipticFD_ implements PlugInFilter {
	ImagePlus imp;
	ImageProcessor ip;
	boolean showResults = false;
	boolean showReconstruction = false;
	int nFD = 10;
  
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL + ROI_REQUIRED;
	}

	public void run(ImageProcessor ip) {
    this.ip = ip;
    
	  String options = Macro.getOptions();
    
	  if ((options != null) && (options.length() != 0)){
	     if (parseOptions(options) == false) { return; } 
	  } else {
      if (showDialog() == false) { return; }
	  }

    runEFD();

	}//run
	
	private void runEFD(){
	 
	 
  PolygonRoi roi = (PolygonRoi) imp.getRoi();
  Rectangle rect = roi.getBounds();
  
  int n = roi.getNCoordinates();
  double[] x = new double[n];
  double[] y = new double[n];
  
  int[] xp = roi.getXCoordinates();
  int[] yp = roi.getYCoordinates();
  
  for (int i = 0; i < n; i++){
    x[i] = (double) (rect.x + xp[i]);
    y[i] = (double) (rect.y + yp[i]); 
  }
  
  EllipticFD efd = new EllipticFD(x, y, nFD);
  
  
  if (showReconstruction){ displayReconstruction(efd);}
  
  if (showResults) {displayResults(efd);};
  
	} //runEFD
	
/**
  * Creates a second image with the reconstructed polygon drawn
  */	  
	public void displayReconstruction(EllipticFD efd){
    double[][] xy = efd.createPolygon();
    int n = xy.length;
    int[] xefd = new int[n];
    int[] yefd = new int[n];
    for (int i = 0; i < n; i++){
     xefd[i] = (int) Math.floor(xy[i][0]);
     yefd[i] = (int) Math.floor(xy[i][1]);
    }
    PolygonRoi roi2 = new PolygonRoi(xefd, yefd, n, Roi.FREEROI);
    ImageProcessor ip2 = (ImageProcessor) ip.duplicate();
    ImagePlus imp2 = new ImagePlus(imp.getShortTitle() + "-EFD", ip2);
    imp2.setRoi(roi2);
    imp2.show();	 
	 
	}

/**
  * Creates a results table with the Descriptors shown
  */		
 	public void displayResults(EllipticFD efd) {
		ResultsTable rt = new ResultsTable();
		rt.reset();
		int row = 0;
		for (int i = 0; i < efd.nFD; i++) {
			rt.incrementCounter();
			rt.addValue("ax", efd.ax[i]);
			rt.addValue("ay", efd.ay[i]);
			rt.addValue("bx", efd.bx[i]);
			rt.addValue("by", efd.by[i]);
			rt.addValue("efd", efd.efd[i]);
		}
		rt.show("Results-EFD-" + imp.getShortTitle());
	} 
 
 /**
  * parses the macro options
  */
   private boolean parseOptions(String options){
    boolean ok = true;

    String[] s = options.split(" ");
    String[] t; 
    
    //Number=n Results Reconstruction
     for (int i = 0; i < s.length; i++){
      t = s[i].split("=");
      if (t[0].equalsIgnoreCase("Number")){
        nFD = (int) Tools.parseDouble(t[1], Double.POSITIVE_INFINITY);
      } else if (t[0].equalsIgnoreCase("Results")){
        showResults = true;
      } else if (t[0].equalsIgnoreCase("Reconstruction")){
        showReconstruction = true;
      }  else {
        IJ.log("oops! unrecognized argument: " + s[i]);
      } 
     } //i-loop
     return ok;
   } 
   
  /**
    * Dialog for configuring options
    */ 
   private boolean showDialog(){
    boolean ok = true;
    GenericDialog gd = new GenericDialog("EllipticFD");
    gd.addNumericField("Number of descriptors", (double) nFD, 0);
    gd.addCheckbox("Results table shown?", true);
    gd.addCheckbox("Reconstruction shown?", true);
    gd.showDialog();
    if (gd.wasCanceled()){return !ok;}
    nFD = (int) gd.getNextNumber();
    showResults = gd.getNextBoolean();
    showReconstruction = gd.getNextBoolean();
    return ok;
   }
   
}