import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.MIN_PRIORITY;

/*
 * Seeds_Analysis è una classe volta alla misurazione di elementi all'interno di una immagine (binaria+grey+RGB).
 * Si appoggia al Plugin "ThresholdAdjuster" per poter individuare le zone e gli oggetti da analizzare.
 * Implementa Plugin ma al suo interno vi è una classe Cells_Analyzer che estende a sua volta il PlugInFilter "ParticleAnalyzer"
 * aggiungendo misure non presenti in quest'ultimo.
 */
public class SeedsAnalysis_NoThresh implements PlugIn
{

    /*  Attributes */
    protected ImagePlus impLightBackground, impDarkBackground, impLightBackgroundOriginal;
    protected ImagePlus mask;
    protected ImagePlus imp;
    protected ImagePlus impColor;
    protected ImagePlus impGray;
    protected ImagePlus[] impRGB;
    protected ImagePlus[] impHSB;
    protected ImageProcessor ipColor, ipGray, ip;
    protected ImageProcessor ipLightBackground, ipDarkBackground;
    protected ImageProcessor ipMask;
    protected boolean typeGray, typeRGB;
    protected Cells_Analyzer cells_analyzer;
    protected GLCM glcm;
    protected int flags;

    protected ColorProcessor cpRGB;
    protected int lin, col, i, j;
    protected float min, max, hue = 0, sat = 0, bri = 0;
    protected int[] RGB = new int[3];
    protected int[] GRAY = new int[3];
    protected float[] hsb = new float[3];
    protected int R = 0, G = 1, B = 2;

    protected boolean measurementsAlreadySet = false;
    protected String currentFilename;

    protected boolean[] mIJ = new boolean[3];
    protected boolean[] mBW = new boolean[29];
    protected boolean[] mG = new boolean[14];
    protected boolean[] mRGB = new boolean[16];
    protected int mPhi = 0;



    /* Methods */
    public void run(String arg)
    {

        imp = IJ.getImage();
        if (imp.getType() == ImagePlus.COLOR_RGB)
            typeRGB = true;
        if(typeRGB){
            ChannelSplitter ch = new ChannelSplitter();
            impColor = new Duplicator().run(imp);
            impRGB = ch.split(impColor);
            impHSB = getImagePlusHSB(imp);

        }

        cells_analyzer = new Cells_Analyzer(0, mIJ, mBW, mG, mRGB, mPhi, "Test");
        flags = cells_analyzer.setup("", imp);

        //IJ.run("8-bit");
        impGray = new Duplicator().run(imp);
        ImageConverter ic = new ImageConverter(impGray);
        ic.convertToGray8();
        //ipGray = imp.getProcessor();

        glcm = new GLCM(impGray);

        if(flags == PlugInFilter.DONE)
        {
            return;
        }


        cells_analyzer.run(imp.getProcessor());
        Analyzer.getResultsTable().show("Results");
        mIJ = cells_analyzer.getMeasuresImageJ();
        mBW = cells_analyzer.getMeasuresBW();
        mG = cells_analyzer.getMeasuresGray();
        mRGB = cells_analyzer.getMeasuresRGB();
        mPhi = cells_analyzer.getPhi();
    }


    /* RGB to HSB */
    private ImagePlus[] getImagePlusHSB(ImagePlus imagePlus)
    {
        ImagePlus [] imagePluses = new ImagePlus[3];
        int w = imagePlus.getWidth();
        int h = imagePlus.getHeight();

        ImageStack hsbStack = imagePlus.getStack();
        ImageStack hueStack = new ImageStack(w,h);
        ImageStack satStack = new ImageStack(w,h);
        ImageStack brightStack = new ImageStack(w,h);

        byte[] hue,s,b;

        ColorProcessor cp;
        int n = hsbStack.getSize();

        for (int i=1; i<=n; i++)
        {
            hue = new byte[w*h];
            s = new byte[w*h];
            b = new byte[w*h];
            cp = (ColorProcessor)hsbStack.getProcessor(1);
            cp.getHSB(hue,s,b);
            hueStack.addSlice(null,hue);
            satStack.addSlice(null,s);
            brightStack.addSlice(null,b);
        }

        imagePluses[0] = new ImagePlus("hue", hueStack);
        imagePluses[1] = new ImagePlus("sat", satStack);
        imagePluses[2] = new ImagePlus("bright", brightStack);

        return imagePluses;
    }

    /* Inner class Parasite che estende la classe ParticleAnalyzer
     * Al suo interno, attraverso gli Override,
     * si estendono i metodi gia' presenti nella classe ParticleAnalyzer */
    class Cells_Analyzer extends ParticleAnalyzer
    {
        int measure = 0;
        double pigreco = Math.PI;
        protected int phi = 0;

        /* Feature di default ImageJ */
        protected boolean[] measureImageJ = new boolean[3];

        /* Feature morfologiche implementate ex novo da Marta */
        protected boolean[] measuresBW = new boolean[28];

        /* CheckBox features morfologiche */
        private boolean doArea, doPerimeter, doFeret, doConvexArea, doConvexPerimeter, doArEquivD,
                doAspRatio, doPerEquivD, doMINRMAXR,
                doRoundness, doEquivEllAr, doCompactness,
                doSolidity, doThinnessRatio, doRFactor,
                doConvexity, doConcavity, doArBBox,
                doRectang, doModRatio, doSphericity,
                doElongation, doNormPeriIndex, doHaralickRatio,
                doBendingEnergy, doDistanceCMFB,
                doJaggedness, doEndocarp, doBreadth,
                doAvgRadius, doVarianceR, doCircularity;

        /* Feature texturali implementate ex novo da Marta */
        protected boolean[] measuresGrey = new boolean[12];
        /* CheckBox features texturali */
        private boolean doMean, doSkewness, doKurtois,
                doMode, doMedian, doEntropy, doIntensitySum, doUniformity, doStandardDeviation,
                doSmothness, doMinandMax, doGLCM;

        /* Feature di colore implementate ex novo da Marta */
        protected boolean[] measureRGB = new boolean[16];
        /* CheckBox features di colore */
        private boolean doMeanRed, doMeanGreen, doMeanBlue,
                doStdDeviationRed, doStdDeviationGreen, doStdDeviationBlue,
                doSquareRootMeanR, doSquareRootMeanG, doSquareRootMeanB, doAverageRGBcolors,
                doMeanHue, doMeanSaturation, doMeanBrightness, doStdDeviationHue, doStdDeviationS,doStdDeviationBr;


        public Cells_Analyzer(int imageNumber, boolean[] mIJ, boolean[] mBW, boolean[] mG, boolean[] mRGB, int mPhi, String mfileName)
        {

            currentFilename = mfileName;

            if(imageNumber > 0)
            {
                measurementsAlreadySet = true;
                measureImageJ = mIJ;
                measuresBW  = mBW;
                measuresGrey = mG;
                measureRGB = mRGB;
                phi = mPhi;
            }
        }



        /* showDialog:
         * settaggio della opzione per visualizzare contorni e etichetta numerata
         * settaggio nelle misure necessarie
         * richiamo della genericDialog creata per poter settare le misure da aggiungere */
        @Override
        public boolean showDialog()
        {
            boolean flag = false;

            if(!measurementsAlreadySet)
            {
                flag = super.showDialog();
                if(flag)
                {
                    flag = dialogSetMeasurements();
                    setMeasurementsExtended();
                    return flag;
                }
            }
            else
            {
                flag = super.showDialog();
                flag = dialogSetMeasurements();
                setMeasurementsExtended();

                return flag;
            }
            return false;
        }

        private void setBW(String[] labels, boolean[] states)
        {
            labels[0] = "Area               ";
            states[0] = false;
            labels[1] = "Perimeter          ";
            states[1] = false;
            labels[2] = "Feret (F)          ";
            states[2] = false;
            labels[3] = "Breadth (B)        ";
            states[3] = false;
            labels[4] = "AspRatio           ";
            states[4] = false;
            labels[5] = "RFactor            ";
            states[5] = false;
            labels[6] = "Convex Area        ";
            states[6] = false;
            labels[7] = "Convex Perimeter   ";
            states[7] = false;
            labels[8] = "ArEquivD           ";
            states[8] = false;
            labels[9] = "PerEquivD          ";
            states[9] = false;
            labels[10] = "MinR and MaxR     ";
            states[10] = false;
            labels[11] = "AvgRadius         ";
            states[11] = false;
            labels[12] = "VarianceRadius    ";
            states[12] = false;
            labels[13] = "EquivEllAr        ";
            states[13] = false;
            labels[14] = "ModRatio          ";
            states[14] = false;
            labels[15] = "HaralickRatio     ";
            states[15] = false;
            labels[16] = "ThinnessRatio     ";
            states[16] = false;
            labels[17] = "Roundness         ";
            states[17] = false;
            labels[18] = "Compactness       ";
            states[18] = false;
            labels[19] = "Solidity          ";
            states[19] = false;
            labels[20] = "Convexity         ";
            states[20] = false;
            labels[21] = "Concavity         ";
            states[21] = false;
            labels[22] = "ArBBox            ";
            states[22] = false;
            labels[23] = "Rectang           ";
            states[23] = false;
            labels[24] = "Sphericity        ";
            states[24] = false;
            labels[25] = "Elongation        ";
            states[25] = false;
            labels[26] = "Bending Energy    ";
            states[26] = false;
            labels[27] = "Jaggedness        ";
            states[27] = false;
            labels[28] = "Circularity       ";
            states[28] = false;
            labels[29] = "Endocarp          ";
            states[29] = false;
            labels[30] = "Distance CM-FB    ";
            states[30] = false;
        }

        private void setGray(String[] labelsG, boolean[] statesG)
        {
            labelsG[0] = "Min and Max              ";
            statesG[0] = false;
            labelsG[1] = "Mean                      ";
            statesG[1] = false;
            labelsG[2] = "StD                       ";
            statesG[2] = false;
            labelsG[3] = "Median                    ";
            statesG[3] = false;
            labelsG[4] = "Mode                      ";
            statesG[4] = false;
            labelsG[5] = "Skewness                  ";
            statesG[5] = false;
            labelsG[6] = "Kurtosis                  ";
            statesG[6] = false;
            labelsG[7] = "Intensity Sum            ";
            statesG[7] = false;
            labelsG[8] = "Uniformity               ";
            statesG[8] = false;
            labelsG[9] = "Entropy                 ";
            statesG[9] = false;
            labelsG[10] = "Smoothness R            ";
            statesG[10] = false;
            labelsG[11] = "Haralick                ";
            statesG[11] = false;

        }

        private void setRGB(String[] labelsRGB, boolean[] statesRGB)
        {
            labelsRGB[0] = "Mean Red                    ";
            statesRGB[0] = false;
            labelsRGB[1] = "StD Red                     ";
            statesRGB[1] = false;
            labelsRGB[2] = "Sqrt Mean Red               ";
            statesRGB[2] = false;
            labelsRGB[3] = "Mean Green                  ";
            statesRGB[3] = false;
            labelsRGB[4] = "StD Green                   ";
            statesRGB[4] = false;
            labelsRGB[5] = "Sqrt Mean Green             ";
            statesRGB[5] = false;
            labelsRGB[6] = "Mean Blue                   ";
            statesRGB[6] = false;
            labelsRGB[7] = "StD Blue                    ";
            statesRGB[7] = false;
            labelsRGB[8] = "Sqrt Mean Blue              ";
            statesRGB[8] = false;
            labelsRGB[9] = "Sum Mean RGB                ";
            statesRGB[9] = false;
            labelsRGB[10] = "Mean Hue                   ";
            statesRGB[10] = false;
            labelsRGB[11] = "StD Hue                    ";
            statesRGB[11] = false;
            labelsRGB[12] = "Mean Sat                   ";
            statesRGB[12] = false;
            labelsRGB[13] = "StD Sat                    ";
            statesRGB[13] = false;
            labelsRGB[14] = "Mean Val                   ";
            statesRGB[14] = false;
            labelsRGB[15] = "StD Val                    ";
            statesRGB[15] = false;
        }

        protected boolean[] getMeasuresImageJ()
        {
            return measureImageJ;
        }

        protected boolean[] getMeasuresBW()
        {
            return measuresBW;
        }

        protected boolean[] getMeasuresGray()
        {
            return measuresGrey;
        }

        protected boolean[] getMeasuresRGB()
        {
            return measureRGB;
        }

        protected int getPhi()
        {
            return phi;
        }

        /* Metodo della creazione della finestra di dialogo, visualizzazione del:
         * - checkbox per selezionare ogni misura
         * - checkbox per selezionare misure singole */
        private boolean dialogSetMeasurements()
        {
            boolean spam = false;

            GenericDialog gd = new GenericDialog("Features", IJ.getInstance());
            Font font = new Font("font", Font.ITALIC, 12);
            Font fontSpace = new Font("font", Font.PLAIN, 8);

            gd.addMessage("Select the morphological features", font, Color.black);
            String[] labelsBW = new String[31];
            boolean[] statesBW = new boolean[31];
            gd.addCheckbox("Select All", false);
            setBW(labelsBW, statesBW);
            gd.setInsets(0, 0, 0);
            gd.addCheckboxGroup(8, 4, labelsBW, statesBW);
            gd.addMessage(" ", fontSpace, Color.black);

            gd.addMessage("Select the textural features", font, Color.GRAY);
            String[] labelsG = new String[12];
            boolean[] statesG = new boolean[12];
            gd.addCheckbox("Select All", false);
            setGray(labelsG, statesG);
            gd.setInsets(0, 0, 0);
            gd.addCheckboxGroup(3, 4, labelsG, statesG);

            String[] angles={"0", "45", "90", "135"};
            gd.addChoice("Haralick step:", angles, Integer.toString(phi));

            if(typeRGB)
            {
                gd.addMessage("Select the colour features", font, Color.red);
                String[] labelsRGB = new String[16];
                boolean[] statesRGB = new boolean[16];
                gd.addCheckbox("Select All", false);
                setRGB(labelsRGB, statesRGB);
                gd.setInsets(0, 0, 0);
                gd.addCheckboxGroup(4, 4, labelsRGB, statesRGB);
            }

            if(!measurementsAlreadySet)
            {
                gd.showDialog();
                if(gd.wasCanceled())
                {
                    return false;
                }

                phi = Integer.parseInt(gd.getNextChoice());

                // BW
                if(gd.getNextBoolean())     // Selected all BW
                {
                    for(int i = 0; i < measureImageJ.length; i++)
                    {
                        measureImageJ[i] = true;
                        spam = gd.getNextBoolean();
                    }
                    for(int i = 0; i < measuresBW.length; i++) {
                        measuresBW[i] = true;
                        spam = gd.getNextBoolean();
                    }
                }
                else                        // Not selected all BW
                {
                    for(int i = 0; i < measureImageJ.length; i++)
                    {
                        measureImageJ[i] = gd.getNextBoolean();
                    }
                    for(int i = 0; i < measuresBW.length; i++)
                    {
                        measuresBW[i] = gd.getNextBoolean();
                    }
                }

                // Gray
                if(gd.getNextBoolean())     // Selected all Gray
                {
                    for (int i = 0; i < measuresGrey.length; i++)
                    {
                        measuresGrey[i] = true;
                        spam = gd.getNextBoolean();
                    }
                }
                else                        // Not selected all Gray
                {
                    for(int i = 0; i < measuresGrey.length; i++)
                    {
                        measuresGrey[i] =  gd.getNextBoolean();
                    }
                }

                if(typeRGB)
                {
                    if(gd.getNextBoolean())     // Selected all RGB
                    {
                        for (int i = 0; i < measureRGB.length; i++)
                        {
                            measureRGB[i] = true;
                            spam = gd.getNextBoolean();
                        }
                    }
                    else                        // Not selected all RGB
                    {
                        for(int i = 0; i < measureRGB.length; i++)
                        {
                            measureRGB[i] = gd.getNextBoolean();
                        }
                    }
                }
            }

            return true;
        }

        private void setMeasurementsExtended()
        {
            // BW
            doArea = measureImageJ[0];
            doPerimeter = measureImageJ[1];
            doFeret = measureImageJ[2];

            doBreadth = measuresBW[0];
            doAspRatio = measuresBW[1];
            doRFactor = measuresBW[2];
            doConvexArea = measuresBW[3];
            doConvexPerimeter = measuresBW[4];
            doArEquivD = measuresBW[5];
            doPerEquivD = measuresBW[6];
            doMINRMAXR = measuresBW[7];
            doAvgRadius = measuresBW[8];
            doVarianceR = measuresBW[9];
            doEquivEllAr = measuresBW[10];
            doModRatio = measuresBW[11];
            doHaralickRatio = measuresBW[12];
            doThinnessRatio = measuresBW[13];
            doRoundness = measuresBW[14];
            doCompactness = measuresBW[15];
            doSolidity = measuresBW[16];
            doConvexity = measuresBW[17];
            doConcavity = measuresBW[18];
            doArBBox = measuresBW[19];
            doRectang = measuresBW[20];
            doSphericity = measuresBW[21];
            doElongation = measuresBW[22];
            doBendingEnergy = measuresBW[23];
            doJaggedness = measuresBW[24];
            doCircularity = measuresBW[25];
            doEndocarp = measuresBW[26];
            doDistanceCMFB = measuresBW[27];

            // Grey
            doMinandMax = measuresGrey[0]; //if(doMinandMax){ measure += MIN_MAX; }
            doMean = measuresGrey[1];
            doStandardDeviation = measuresGrey[2];
            doMedian = measuresGrey[3]; //if(doMedian){ measure += MEDIAN; }
            doMode = measuresGrey[4];
            doSkewness = measuresGrey[5]; //if(doSkewness){ measure += SKEWNESS; }
            doKurtois = measuresGrey[6]; //if(doKurtois){ measure += KURTOSIS; }
            doIntensitySum = measuresGrey[7];
            doUniformity = measuresGrey[8];
            doEntropy = measuresGrey[9];
            doSmothness = measuresGrey[10];
            doGLCM = measuresGrey[11];

            // RGB
            doMeanRed = measureRGB[0];
            doStdDeviationRed = measureRGB[1];
            doSquareRootMeanR = measureRGB[2];
            doMeanGreen = measureRGB[3];
            doStdDeviationGreen = measureRGB[4];
            doSquareRootMeanG = measureRGB[5];
            doMeanBlue = measureRGB[6];
            doStdDeviationBlue = measureRGB[7];
            doSquareRootMeanB = measureRGB[8];
            doAverageRGBcolors = measureRGB[9];
            doMeanHue= measureRGB[10];
            doStdDeviationHue= measureRGB[11];
            doMeanSaturation= measureRGB[12];
            doStdDeviationS= measureRGB[13];
            doMeanBrightness= measureRGB[14];
            doStdDeviationBr= measureRGB[15];
        }

        /* saveResults:
         * richiamo della funzione originale e aggiunta delle nuove misure */
        protected void saveResults(ImageStatistics stats, Roi roi)
        {
            super.saveResults(stats, roi);

            //imp.setRoi(roi);
            impGray.setRoi(roi);
            stats = impGray.getAllStatistics();

            /*Settaggio delle misure da aggiungere date dalla checkBox*/
            /*Oggetti e dati necessari per alcune misure*/
            Polygon polygon = roi.getConvexHull();
            double area = stats.area;
            double perim = roi.getLength();
            double[] feret = roi.getFeretValues(); //0 --> feret , 1 --> angle, 2 --> feret min
            double[] centroid = getCentroid(roi); // normalized to width and length of image
            double[] radiiValues = getRadiiValues(roi, centroid[0], centroid[1]);
            double convexArea = getArea(polygon);
            double convexPerimeter = getPerimeter(polygon);
            int[] hist = stats.histogram;

            rt.addValue("Label", currentFilename);

            if(doArea)
            {
                rt.addValue("Area", area);
            }

            if(doPerimeter)
            {
                rt.addValue("Perimeter", perim);
            }

            if(doFeret)
            {
                rt.addValue("*Feret", feret[0]);
            }

            if(doBreadth)
            {
                rt.addValue("*Breadth", feret[2]);
            }

            if(doAspRatio)
            {
                //Aspect ratio = Feret/Breadth = L/W also called Feret ratio or Eccentricity or Rectangular ratio
                rt.addValue("*AspRatio", feret[0] / feret[2]);
            }

            if(doRFactor)
            {
                //RFactor = Convex_Area /(Feret*pi)
                rt.addValue("*RFactor", convexArea / (feret[0] * Math.PI));
            }

            if(doConvexArea)
            {
                // Area of the convex hull polygon
                rt.addValue("*ConvexArea", convexArea);
            }

            if(doConvexPerimeter)
            {
                // Perimeter of the convex hull polygon
                rt.addValue("*ConvexPerimeter", convexPerimeter);
            }

            if(doArEquivD)
            {
                //Diameter of a circle with equivalent area,
                rt.addValue("*ArEquivD", Math.sqrt((4 / pigreco) * stats.area));
            }

            if(doPerEquivD)
            {
                //Diameter of a circle with equivalent perimeter,  Area/pi
                rt.addValue("*PerEquivD", perim / pigreco);
            }

            if(doMINRMAXR)
            {
                rt.addValue("*MinR", feret[2] / 2); //DA RIVEDERE Radius of the inscribed circle centred at the middle of mass
                rt.addValue("*MaxR", feret[0] / 2); //DA RIVEDERE Radius of the enclosing circle centred at the middle of mass
            }

            if(doAvgRadius)
            {
                rt.addValue("*avgR", radiiValues[2]);
            }

            if(doVarianceR)
            {
                rt.addValue("*VarianceR", radiiValues[3]);
            }

            if(doEquivEllAr)
            {
                //Area of the ellipse with Feret and Breath as major and minor axis,  = (?�Feret�Breadth)/4
                rt.addValue("*EquivEllAr", (pigreco * feret[0] * feret[2]) / 4);
            }

            if(doModRatio)
            {
                //Modification ratio = (2*MinR)/Feret
                rt.addValue("*ModRatio", (feret[2] / feret[0])); //2 * MinR / Feret DA RIVEDERE danno risultati uguali
            }

            if(doHaralickRatio)
            {
                double haralickRatio = getHaralickRatio(radiiValues[2], radiiValues[4]);
                rt.addValue("*HaralickRatio", haralickRatio);
            }

            if(doThinnessRatio)
            {
                //Shape = Perimeter^2/Area also called Thinness ratio
                rt.addValue("*ThinnessR", (perim * perim) / stats.area);
            }

            if(doRoundness)
            {
                //Roundness = 4*Area/(Feret^2)
                rt.addValue("*Roundness", (stats.area * 4) / ((pigreco) * (feret[0] * feret[0])));
            }

            if(doCompactness)
            {
                //Compactness = sqrt((4/pi)*Area)/Feret
                rt.addValue("*Compactness", (Math.sqrt((4 / pigreco) * stats.area)) / feret[0]);
            }

            if(doSolidity)
            {
                //Solidity = Area/ConvexArea
                rt.addValue("*Solidity", (stats.area / convexArea));
            }

            if(doConvexity)
            {
                //Convexity = Convex_Perim/Perimeter also called rugosity or roughness
                rt.addValue("*Convexity", convexPerimeter / perim);
            }

            if(doConcavity)
            {
                //Concavity ConvexArea-Area
                rt.addValue("*Concavity", convexArea - stats.area);
            }

            if(doArBBox)
            {
                //Area of the bounding box along the Feret diameter = Feret�Breadth
                rt.addValue("*ArBBox", feret[0] * feret[2]);
            }

            if(doRectang)
            {
                // Rectangularity = Area/ArBBox also called Extent
                rt.addValue("*Rectang", stats.area / (feret[0] * feret[2]));
            }

            if(doSphericity)
            {
                //Sphericity = MinR/MaxR also called Radius ratio
                rt.addValue("*Sphericity", (feret[2] / 2) / (feret[0] / 2)); //MinR / MaxR DA RIVEDERE danno risultati uguali
            }

            if(doElongation)
            {
                //The inverse of the circularity,  Perim2/(4�?�Area)
                rt.addValue("*Elongation", (perim * perim) / (4 * pigreco * stats.area));
            }

            if(doBendingEnergy)
            {
                double be = getBendingEnergy(polygon);
                rt.addValue("*Bending Energy", be);
            }

            if(doJaggedness)
            {
                rt.addValue("*Jaggedness", (2*Math.sqrt(Math.PI*stats.area))/perim);
            }

            if(doCircularity)
            {
                // 4pi*area/perimeter^2
                rt.addValue("*Circularity", (4*Math.PI*stats.area)/Math.pow(perim,2));
            }

            if(doEndocarp)
            {
                rt.addValue("*Endocarp", stats.area - perim);
            }

            if(doDistanceCMFB)
            {
                double[] intersectionIS = getIS(roi);
                double[] cent = getCentroid(roi);
                rt.addValue("Distance CM-FB",getDS(roi, intersectionIS, cent[0], cent[1]));
            }

            /* Grey */
            if(doMinandMax)
            {
                rt.addValue("Min", getMin(hist));
                rt.addValue("Max", stats.max);
            }

            if(doMean)
            {
                rt.addValue("Mean", getMean(hist));
            }

            if(doStandardDeviation)
            {
                rt.addValue("StD", stats.stdDev);
            }

            if(doMedian)
            {
                rt.addValue("Median", getMedian(hist));
            }

            if(doMode)
            {
                rt.addValue("Mode", getMode(hist));
            }

            if(doSkewness)
            {
                rt.addValue("Skewness", stats.skewness);
            }

            if(doKurtois)
            {
                rt.addValue("Kurtosis", stats.kurtosis);
            }

            if(doIntensitySum)
            {
                int intensitySum = getIntensitySum(hist);
                rt.addValue("Intensity sum", intensitySum);
            }

            if(doUniformity)
            {
                rt.addValue("Uniformity", getUniformity(hist, stats.area));
            }

            if(doEntropy)
            {
                double e = getEntropy(hist, stats.area);
                rt.addValue("Entropy", e);
            }

            if(doSmothness)
            {
                rt.addValue("Smothness R", getSmoothness(Math.pow(stats.stdDev, 2)));
            }

            if(doGLCM)
            {
                glcm.exec(roi, phi);
                rt.addValue("GLCM-Contrast", glcm.getContrast());
                rt.addValue("GLCM-Correlation", glcm.getCorrelation());
                rt.addValue("GLCM-Energy", glcm.getEnergy());
                rt.addValue("GLCM-Homogeneity", glcm.getHomogeneity());
            }

            if(typeRGB)
            {
                impRGB[0].setRoi(roi); //red
                impRGB[1].setRoi(roi); //green
                impRGB[2].setRoi(roi); //blue

                ImageStatistics stats_r = impRGB[0].getAllStatistics();
                ImageStatistics stats_g = impRGB[1].getAllStatistics();
                ImageStatistics stats_b = impRGB[2].getAllStatistics();

                if(doMeanRed) {
                    rt.addValue("Mean Red", stats_r.mean);
                }

                if(doSquareRootMeanR){
                    rt.addValue("Sqrt Mean Red", Math.sqrt(stats_r.mean));
                }

                if(doStdDeviationRed){
                    rt.addValue("StD Red", stats_r.stdDev);
                }

                if(doMeanGreen){
                    rt.addValue("Mean Green", stats_g.mean);
                }

                if(doSquareRootMeanG){
                    rt.addValue("Sqrt Mean Green", Math.sqrt(stats_g.mean));
                }

                if(doStdDeviationGreen){
                    rt.addValue("StD Green", stats_g.stdDev);
                }

                if(doMeanBlue){
                    rt.addValue("Mean Blue", stats_b.mean);
                }

                if(doSquareRootMeanB){
                    rt.addValue("Sqrt Mean Blue", Math.sqrt(stats_b.mean));
                }

                if(doStdDeviationBlue){
                    rt.addValue("StD Blue", stats_b.stdDev);
                }

                if(doAverageRGBcolors){
                    rt.addValue("Mean RGB", (stats_r.mean+stats_g.mean+stats_b.mean)/3);
                }

                impHSB[0].setRoi(roi); //hue
                impHSB[1].setRoi(roi); //saturation
                impHSB[2].setRoi(roi); //brightness

                ImageStatistics stats_hu = impHSB[0].getAllStatistics();
                ImageStatistics stats_sa = impHSB[1].getAllStatistics();
                ImageStatistics stats_br = impHSB[2].getAllStatistics();

                if(doMeanHue)
                    rt.addValue("Mean H", stats_hu.mean);
                if(doStdDeviationHue)
                    rt.addValue("StD H", stats_hu.stdDev);

                if(doMeanSaturation)
                    rt.addValue("Mean S", stats_sa.mean);
                if(doStdDeviationS)
                    rt.addValue("StD S", stats_sa.stdDev);

                if(doMeanBrightness)
                    rt.addValue("Mean V", stats_br.mean);
                if(doStdDeviationBr)
                    rt.addValue("StD V", stats_br.stdDev);
            }
        }

        private double getArea(Polygon p)
        {
            if (p == null) return Double.NaN;
            double carea = 0.0;

            for (int i = 0; i <= p.npoints - 2; i++) {
                carea += (p.xpoints[i] * p.ypoints[i+1]) - (p.xpoints[i+1] * p.ypoints[i]);
            }
            return (Math.abs(carea / 2.0));
        }

        private double getPerimeter(Polygon p)
        {
            if (p == null) return Double.NaN;
            double cperimeter = 0.0;
            int firstPoint = 0;
            int lastPoint = p.npoints - 1;

            for (int i = 0; i <= p.npoints - 2; i++) {
                cperimeter += distance(p.xpoints[i+1], p.ypoints[i+1], p.xpoints[i], p.ypoints[i]);
            }
            cperimeter += distance(p.xpoints[lastPoint], p.ypoints[lastPoint], p.xpoints[firstPoint], p.ypoints[firstPoint]);

            return cperimeter;
        }

        private double getBendingEnergy(Polygon polygon)
        {
            //SBAGLIATO DA RICONTROLLARE
            double bendingEnergy = 0;
            double[] k = new double[polygon.npoints];
            int i;

            /*Trasformazione in double dei vettori x e y..
             * */
            double[] x = new double[polygon.npoints];
            for (i = 0; i < x.length; i++) {
                x[i] = (double) polygon.xpoints[i];
            }

            double[] y = new double[polygon.npoints];
            for (i = 0; i < y.length; i++) {
                y[i] = (double) polygon.ypoints[i];
            }

            k = divVector( diffVector ( moltVector( (diff(x)), (diff(diff(y))) ),
                    moltVector( (diff(y)), (diff(diff(x))) ) ),
                    elevationVector( (sumVector( elevationVector(diff(x), 2), elevationVector(diff(y), 2))), 1.5 ) );

            for (i = 0; i < k.length; i++)
            {
                bendingEnergy = bendingEnergy + Math.pow(k[i], 2);
            }
            return bendingEnergy * Math.pow(k.length, -1);
        }

        private double getHaralickRatio(double avg, double stdDev)
        {
            return avg/stdDev;
        }

        private double getEntropy(int[] hist, double area)
        {
            double sum = 0.0;
            for(int i = 0; i < hist.length; i++)
            {
                if (hist[i] != 0) sum += (double) (hist[i] / area) * log2((double) (hist[i] / area));
            }

            return -sum;
        }

        private int getMin(int[] hist)
        {
            int min = 0, i = 0;
            boolean found = false;
            while(!found && i < hist.length)
            {
                if( hist[i] > 0 )
                {
                    found = true;
                    min = i;
                }

                i++;
            }

            return min;
        }

        private double getMean(int[] hist)
        {
            int sum = 0;
            double mean = 0.0;
            for(int i = 0; i < hist.length; i++)
            {
                sum += i * hist[i];
            }

            mean = sum / hist.length;
            return mean;
        }

        private int getMode(int[] hist)
        {
            int maxValue = 0, maxCount = 0, i, j, n;
            n = hist.length;

            for (i = 0; i < n; ++i)
            {
                int count = 0;
                for (j = 0; j < n; ++j)
                {
                    if (hist[j] == hist[i])
                        ++count;
                }

                if (count > maxCount)
                {
                    maxCount = count;
                    maxValue = hist[i];
                }
            }

            return maxValue;
        }

        private double getMedian(int hist[])
        {
            int n = hist.length;

            // check for even case
            if (n % 2 != 0)
                return (double)hist[n / 2];

            return (double)(hist[(n - 1) / 2] + hist[n / 2]) / 2.0;
        }

        private int getIntensitySum(int[] hist)
        {
            int sum = 0;
            for(int i = 0; i < hist.length; i++)
            {
                sum += i * hist[i];
            }
            return sum;
        }

        private double getUniformity(int[] hist, double area)
        {
            double uniformity = 0.0;

            for(int i = 0; i < hist.length; i++)
            {
                uniformity += Math.pow(((double) hist[i] / area), 2);
            }

            return uniformity;
        }

        private double getSmoothness(double variance)
        {
            //the variance in this measure is normalized to the rage [0, 1] by dividing it by (L-1)^2
            //L --> grey level
            //only grey level in the object or in the histagram?
            int greyLevel = 256;
            double varianceNormalized = variance / Math.pow(greyLevel - 1, 2);
            return 1 - (1 / (1 + varianceNormalized));
        }

        private double getEndocarp(double area, double perimeter)
        {
            return area - perimeter;
        }

        /*Funzione di appoggio per il calcolo della distanza*/
        public double distance(int argx1, int argy1, int argx2, int argy2)
        {
            return Math.sqrt((argx1 - argx2) * (argx1 - argx2) + (argy1 - argy2) * (argy1 - argy2));
        }

        // log2:  Logarithm base 2
        public double log2(double d)
        {
            return Math.log(d) / Math.log(2.0);
        }

        /* METODI PER I VETTORI */
        public double[] diff(double[] z)
        {
            double first = z[0];
            double ultimade = z[z.length - 1];
            double[] result = new double[z.length];

            for (int i = 0; i < z.length; i++) {
                if (i == (z.length - 1)) {
                    result[i] = (ultimade - first);
                } else {
                    result[i] = (z[i + 1] - z[i]);
                }
            }
            return result;
        }

        public double[] diffVector(double[] a, double[] b)
        {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] - b[i];
            }

            return result;
        }

        public double[] moltVector(double[] a, double[] b)
        {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] * b[i];
            }

            return result;
        }

        public double[] divVector(double[] a, double[] b)
        {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = a[i] * Math.pow(b[i], -1);
            }
            return result;
        }

        public double[] sumVector(double[] a, double[] b)
        {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = (a[i] + b[i]);
            }
            return result;
        }

        public double[] elevationVector(double[] a, double elevation)
        {
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = Math.pow(a[i], elevation);
            }
            return result;
        }

        /* Metodi Andre */
        private double[] getCentroid(Roi roi)
        {
            double[] centroid = new double[2]; // x, y
            centroid[0] = roi.getStatistics().xCentroid;// - roi.getStatistics().roiX;
            centroid[1] = roi.getStatistics().yCentroid;// - roi.getStatistics().roiY;
            return centroid;
        }

        /* Metodi Giorgia */
        private double[] getRadiiValues(Roi roi, double x_cg, double y_cg)
        {
            Polygon p = roi.getPolygon();
            int n = p.npoints;
            int i;
            int impHeight = imp.getHeight();
            double sumR = 0;
            double variance = 0;
            double[] radii = new double[n];
            double[] radiiValues = new double[5]; // minR, maxR, media, variance, stdDev
            radiiValues[0] = Double.MAX_VALUE;    // min
            radiiValues[1] = -1;                  //max

            for(i = 0; i < n; i++){
                radii[i] = distance((int)x_cg, (int)y_cg, p.xpoints[i], p.ypoints[i]);

                if(radii[i] < radiiValues[0])
                    radiiValues[0] = radii[i];
                if(radii[i] > radiiValues[1])
                    radiiValues[1] = radii[i];
                sumR += radii[i];
            }

            double media = sumR/n;
            radiiValues[2] = media;

            for(i = 0; i < n; i++){
                variance += (radii[i] - media) * (radii[i] - media);
            }
            variance /= (n - 1);
            radiiValues[3] = variance;
            radiiValues[4] = Math.sqrt(variance); //std deviation
            return  radiiValues;
        }

        private double getDS(Roi roi, double[] is, double x_cg, double y_cg)
        {
            Point2D.Double pIs = new Point2D.Double(is[0], is[1]);
            Point2D.Double pCg = new Point2D.Double(x_cg, y_cg);
            return pIs.distance(pCg);
        }

        private double[] getIS(Roi roi)
        {
            int[] startH = new int[2];
            int[] endH = new int[2];
            int[] startW = new int[2];
            int[] endW = new int[2];
            double[] feretValues = roi.getFeretValues();
            double[] IS = new double[2];
            double s1, s2;

            IS[0] = 0.0;
            IS[1] = 0.0;

            try
            {
                startH[0] = (int) feretValues[8]; // FeretX startpoint
                startH[1] = (int) feretValues[9]; // FeretY startpoint
                endH[0] = (int) feretValues[10];  // FeretX endpoint
                endH[1] = (int) feretValues[11];  // FeretY endpoint
                startW[0] = (int) feretValues[12]; // minFeretX startpoint
                startW[1] = (int) feretValues[13]; // minFeretY startpoint
                endW[0] = (int) feretValues[14];  // minFeretX endpoint
                endW[1] = (int) feretValues[15]; // minFeretY endpoint

                s1 = (0.5) * ((startW[0] - endW[0]) * (startH[1] - endW[1]) - (startW[1] - endW[1]) * (startH[0] - endW[0]));
                s2 = (0.5) * ((startW[0] - endW[0]) * (endW[1] - endH[1]) - (startW[1] - endW[1]) * (endW[0] - endH[0]));

                IS[0] = (int)(startH[0] + (s1 * (endH[0] - startH[0]))/(s1 + s2));
                IS[1] = (int)(startH[1] + (s1 * (endH[1] - startH[1]))/(s1 + s2));

            }
            catch(ArrayIndexOutOfBoundsException e)
            {
                IJ.log("Errore sul recupero dei valori di Feret");
            }

            return IS;
        }

    }

    class GLCM implements PlugInFilter
    {
        int d = 1;
        int phi = 0;
        boolean symmetry = true;

        boolean doContrast = true;
        boolean doCorrelation = true;
        boolean doEnergy = true;
        boolean doHomogeneity = true;

        protected ImageProcessor ip;

        protected double contrast = 0.0;
        protected double correlation= 0.0;
        protected double energy = 0.0;
        protected double homogeneity = 0.0;

        protected double sum = 0.0;

        public int setup(String arg, ImagePlus imp)
        {
            if(imp != null)
                return DONE;

            return DOES_8G+DOES_STACKS+SUPPORTS_MASKING;
        }

        public void run(ImageProcessor ii)
        {

        }

        public GLCM(ImagePlus i)
        {
            i.show();
            ip = i.getProcessor();
        }


        public double getContrast()
        {
            return contrast;
        }

        public double getCorrelation()
        {
            return correlation;
        }

        public double getEnergy()
        {
            return energy;
        }

        public double getHomogeneity()
        {
            return homogeneity;
        }

        public double getSum()
        {
            return sum;
        }

        public void exec(Roi r, int p)
        {
            int roiWidth = (int) r.getFloatWidth();
            int roiHeight = (int) r.getFloatHeight();
            int roiX = (int) r.getXBase();
            int roiY = (int) r.getYBase();

            // use the bounding rectangle ROI to roughly limit processing
            Rectangle roi = ip.getRoi();

            // get byte arrays for the image pixels and mask pixels
            int width = roiWidth; //ip.getWidth();
            int height = roiHeight; //ip.getHeight();
            byte [] pixels = (byte []) ip.getPixels();
            byte [] mask = ip.getMaskArray();

            // value = value at pixel of interest; dValue = value of pixel at offset
            int value;
            int dValue;
            double totalPixels = width * height; //roi.height * roi.width;
            if (symmetry) totalPixels = totalPixels * 2;
            double pixelProgress = 0;
            double pixelCount = 0;

            //====================================================================================================
            // compute the Gray Level Correlation Matrix

            int offsetX = 1;
            int offsetY = 0;
            double[][] glcm = new double [256][256];

            phi = p;

            // set our offsets based on the selected angle
            if(phi == 0)
            {
                offsetX = d;
                offsetY = 0;
            }
            else if(phi == 45)
            {
                offsetX = d;
                offsetY = -d;
            }
            else if(phi == 90)
            {
                offsetX = 0;
                offsetY = -d;
            }
            else if(phi == 135)
            {
                offsetX = -d;
                offsetY = -d;
            }
            else
            {
                // the angle is not one of the options
                IJ.showMessage("The requested angle,"+phi+", is not one of the supported angles (0,45,90,135)");
            }


            // loop through the pixels in the ROI bounding rectangle
            for(int y=roiY; y<(roiY + roiHeight); y++)
            {
                for(int x=roiX; x<(roiX + roiWidth); x++)
                {
                    // check to see if the pixel is in the mask (if it exists)
                    if ((mask == null) || ((0xff & mask[(((y-roiY)*roiWidth)+(x-roiX))]) > 0) )
                    {
                        // check to see if the offset pixel is in the roi
                        int dx = x + offsetX;
                        int dy = y + offsetY;
                        if( ((dx >= roiX) && (dx < (roiX+roiWidth))) && ((dy >= roiY) && (dy < (roiY+roiHeight))) )
                        {
                            // check to see if the offset pixel is in the mask (if it exists)
                            if((mask == null) || ((0xff & mask[(((dy-roiY)*roiWidth)+(dx-roiX))]) > 0) )
                            {
                                value = 0xff & pixels[(y*width)+x];
                                dValue = 0xff & pixels[(dy*width) + dx];
                                glcm [value][dValue]++;
                                pixelCount++;
                            }
                            // if symmetry is selected, invert the offsets and go through the process again
                            if(symmetry)
                            {
                                dx = x - offsetX;
                                dy = y - offsetY;
                                if( ((dx >= roiX) && (dx < (roiX+roiWidth))) && ((dy >= roiY) && (dy < (roiY+roiHeight))) )
                                {
                                    // check to see if the offset pixel is in the mask (if it exists)
                                    if((mask == null) || ((0xff & mask[(((dy-roiY)*roiWidth)+(dx-roiX))]) > 0) )
                                    {
                                        value = 0xff & pixels[(y*width)+x];
                                        dValue = 0xff & pixels[(dy*width) + dx];
                                        glcm [dValue][value]++;
                                        pixelCount++;
                                    }
                                }
                            }
                        }
                    }
                    pixelProgress++;
                }
            }


            //=====================================================================================================

            // convert the GLCM from absolute counts to probabilities
            for(int i=0; i<256; i++)
            {
                for(int j=0; j<256; j++)
                {
                    glcm[i][j] = (glcm[i][j])/(pixelCount);
                }
            }

            //=====================================================================================================
            // calculate meanx, meany, stdevx and stdevy for the glcm
            double[] px = new double[256];
            double[] py = new double[256];
            double meanx = 0.0;
            double meany = 0.0;
            double stdevx = 0.0;
            double stdevy = 0.0;

            // Px(i) and Py(j) are the marginal-probability matrix; sum rows (px) or columns (py)
            // First, initialize the arrays to 0
            for(int i=0;  i<256; i++)
            {
                px[i] = 0.0;
                py[i] = 0.0;
            }

            // sum the glcm rows to Px(i)
            for(int i=0;  i<256; i++)
            {
                for (int j=0; j<256; j++)
                {
                    px[i] += glcm[i][j];
                }
            }

            // sum the glcm columns to Py(j)
            for (int j=0; j<256; j++)
            {
                for (int i=0; i<256; i++)
                {
                    py[j] += glcm[i][j];
                }
            }

            // calculate meanx and meany
            for (int i=0;  i<256; i++)
            {
                meanx += (i*px[i]);
                meany += (i*py[i]);
            }

            // calculate stdevx and stdevy
            for (int i=0;  i<256; i++)
            {
                stdevx += ((Math.pow((i-meanx),2))*px[i]);
                stdevy += ((Math.pow((i-meany),2))*py[i]);
            }


            // calculate the contrast (Haralick, et al. 1973)
            // similar to the inertia, except abs(i-j) is used

            if(doContrast)
            {
                contrast = 0.0;
                for (int i=0; i<256; i++)
                {
                    for (int j=0; j<256; j++)
                    {
                        contrast += Math.pow(Math.abs(i-j),2)*(glcm[i][j]);
                    }
                }
            }

            if(doCorrelation)
            {
                correlation = 0.0;

                for (int i=0;  i<256; i++)
                {
                    for (int j=0; j<256; j++)
                    {
                        //Walker, et al. 1995
                        correlation += ((((i-meanx)*(j-meany))/Math.sqrt(stdevx*stdevy))*glcm[i][j]);
                    }
                }
            }

            // calculate the energy
            if(doEnergy)
            {
                energy = 0.0;
                for (int i=0; i<256; i++)
                {
                    for (int j=0; j<256; j++)
                    {
                        energy += Math.pow(glcm[i][j], 2);
                    }
                }
            }

            // calculate the homogeneity (Parker)
            // "Local Homogeneity" from Conners, et al., 1984 is calculated the same as IDM above
            // Parker's implementation is below; absolute value of i-j is taken rather than square
            if(doHomogeneity)
            {
                homogeneity = 0.0;
                for(int i=0; i<256; i++)
                {
                    for(int j=0; j<256; j++)
                    {
                        homogeneity += glcm[i][j]/(1.0+Math.abs(i - j));
                    }
                }
            }

            //===============================================================================================
            // calculate the sum of all glcm elements

            sum = 0.0;
            for(int i=0; i<256; i++)
            {
                for(int j=0; j<256; j++)
                {
                    sum = sum + glcm[i][j];
                }
            }
        }
    }
}
