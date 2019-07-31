package FourierD;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;
import java.io.*;
import java.util.Arrays;

/**
 * Compute Fourier Descriptor and curvature values
 *
 * @author thomas.boudier@snv.jussieu.fr
 * @created 11 mars 2004
 */
public class Fourier_ implements PlugInFilter {

    ImagePlus imp;

    /**
     * Main processing method for the Fourier_ object
     *
     * @param ip image
     */
    @Override
    public void run(ImageProcessor ip) {
        int NbPoints;
        int four = 5;
        double cou;
        boolean kval = true;
        boolean cali = true;
        int scale_cur = 5;

        GenericDialog gd = new GenericDialog("Fourier", IJ.getInstance());
        gd.addNumericField("Fourier descriptors:", four, 0);
        gd.addNumericField("Scale cur", scale_cur, 0);
        gd.addCheckbox("Save curvature values", kval);
        gd.addCheckbox("Use calibration", cali);
        gd.showDialog();
        four = (int) gd.getNextNumber();
        scale_cur = (int) gd.getNextNumber();
        kval = gd.getNextBoolean();
        cali = gd.getNextBoolean();

        if (gd.wasCanceled()) {
            return;
        }

        int width = ip.getWidth();
        int height = ip.getHeight();

        Roi r = imp.getRoi();
        Fourier fourier = new Fourier();
        double reso = 1;
        if ((cali) && (imp.getCalibration() != null)) {
            reso = imp.getCalibration().pixelWidth;
        }
        fourier.Init(r, reso);
        ImageProcessor res = ip.createProcessor(width, height);
        res.insert(ip, 0, 0);

        Roi proi = r;
        if (fourier.closed()) {
            //Calcul de Fourier            
            if (four > 0) {
                fourier.computeFourier(four);
                proi = fourier.drawFourier(res, four, reso);
            }
        } else {
            IJ.log("the Roi must be closed");
        }

        NbPoints = fourier.getNbPoints();

        ImagePlus plus = new ImagePlus("Fourier", res);
        plus.setRoi(proi);
        plus.show();

        //Calcul de la courbure
        double[] xc = new double[NbPoints];
        double[] yc = new double[NbPoints];
        double[] xcf = new double[NbPoints];
        double[] ycf = new double[NbPoints];

        double minK = Double.MAX_VALUE;
        double maxK = Double.MIN_VALUE;
        double minKf = Double.MAX_VALUE;
        double maxKf = Double.MIN_VALUE;

        for (int i = 0; i < NbPoints; i++) {
            // normal
            xc[i] = i;
            cou = fourier.curvature(i, scale_cur, false);
            yc[i] = cou;
            if (cou < minK) {
                minK = cou;
            }
            if (cou > maxK) {
                maxK = cou;
            }
            // fourier
            xcf[i] = i;
            cou = fourier.curvature(i, scale_cur, true);
            ycf[i] = cou;
            if (cou < minKf) {
                minKf = cou;
            }
            if (cou > maxKf) {
                maxKf = cou;
            }
        }

        IJ.log("Min-Max : " + minK + " " + maxK + " " + minKf + " " + maxKf);

        Plot pw = new Plot("Curvature values", "Point", "Curv.", xc, yc);
        pw.setColor(Color.BLUE);
        pw.show();

        Plot pwf = new Plot("Curvature values Fourier", "Point", "Curv.", xcf, ycf);
        pwf.setColor(Color.BLUE);
        pwf.show();

        fourier.displayValues(four);

        // PRINT MIN AND MAX VALUES
        if (kval) {
            FloatProcessor cur = new FloatProcessor(width, height);
            FloatProcessor curf = new FloatProcessor(width, height);
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter("curvature.txt"));
                BufferedWriter outf = new BufferedWriter(new FileWriter("curvatureFourier.txt"));
                out.write("nb\tX\tY\tCurv.");
                outf.write("nb\tX\tY\tCurv.");
                for (int i = 0; i < NbPoints; i++) {
                    double xx = fourier.getXPoint(i);
                    double yy = fourier.getYPoint(i);
                    double xxf = fourier.getXPointFourier(i);
                    double yyf = fourier.getYPointFourier(i);
                    out.write("\n" + i + "\t" + xx + "\t" + yy + "\t" + yc[i]);
                    outf.write("\n" + i + "\t" + xxf + "\t" + yyf + "\t" + ycf[i]);
                    // image
                    cur.putPixelValue((int) Math.round(xx / reso), (int) Math.round(yy / reso), yc[i]);
                    curf.putPixelValue((int) Math.round(xxf / reso), (int) Math.round(yyf / reso), ycf[i]);
                }
                out.close();
                outf.close();
            } catch (IOException e) {
            }
            // display an image with curvature values
            ImagePlus plusC = new ImagePlus("Curvatures", cur);
            if ((cali) && (imp.getCalibration() != null)) {
                plusC.setCalibration(imp.getCalibration());
            }
            plusC.show();
            ImagePlus plusCF = new ImagePlus("Curvatures_Fourier", curf);
            if ((cali) && (imp.getCalibration() != null)) {
                plusCF.setCalibration(imp.getCalibration());
            }
            plusCF.show();
        }
    }

    /**
     * setup
     *
     * @param arg arguments
     * @param imp image plus
     * @return setup
     */
    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL + ROI_REQUIRED;
    }
}

/**
 * point 2D class
 *
 * @author thomas.boudier@snv.jussieu.fr
 * @created 11 mars 2004
 */
class Point2d {

    double x;
    double y;

    /**
     * Constructor for the Point2d object
     */
    public Point2d() {
        x = 0.0;
        y = 0.0;
    }

}

/**
 * main fourier class
 *
 * @author thomas.boudier@snv.jussieu.fr
 * @created 11 mars 2004
 */
class Fourier {

    double ax[], ay[], bx[], by[];
    Point2d points[];
    Point2d points_fourier[];
    int NPT;
    boolean closed;
    int NMAX = 50000;

    /**
     * Constructor for the Fourier object
     */
    public Fourier() {
    }

    /**
     * Gets the nbPoints attribute of the Fourier object
     *
     * @return The nbPoints value
     */
    public int getNbPoints() {
        return NPT;
    }

    /**
     * Gets the xPoint attribute of the Fourier object
     *
     * @param i Description of the Parameter
     * @return The xPoint value
     */
    public double getXPoint(int i) {
        return points[i].x;
    }

    public double getXPointFourier(int i) {
        return points_fourier[i].x;
    }

    /**
     * Gets the yPoint attribute of the Fourier object
     *
     * @param i Description of the Parameter
     * @return The yPoint value
     */
    public double getYPoint(int i) {
        return points[i].y;
    }

    public double getYPointFourier(int i) {
        return points_fourier[i].y;
    }

    /**
     * roi is closed
     *
     * @return closed ?
     */
    public boolean closed() {
        return closed;
    }

    /**
     * initialisation of the fourier points
     *
     * @param R the roi
     */
    public void Init(Roi R, double res) {
        double Rx;
        double Ry;
        int i;
        double a;
        NPT = 0;

        points = new Point2d[NMAX];
        points_fourier = new Point2d[NMAX];

        for (i = 0; i < NMAX; i++) {
            points[i] = new Point2d();
            points_fourier[i] = new Point2d();
        }

        if ((R.getType() == Roi.OVAL) || (R.getType() == Roi.RECTANGLE)) {
            closed = true;
            Rectangle Rect = R.getBounds();
            int xc = Rect.x + Rect.width / 2;
            int yc = Rect.y + Rect.height / 2;
            Rx = Rect.width / 2;
            Ry = Rect.height / 2;
            double theta = 2.0 / (double) ((Rx + Ry) / 2);

            i = 0;
            for (a = 0.0; a < 2 * Math.PI; a += theta) {
                points[i].x = (xc + Rx * Math.cos(a)) * res;
                points[i].y = (yc + Ry * Math.sin(a)) * res;
                i++;
            }

            NPT = i;
            points = Arrays.copyOfRange(points, 0, NPT);
        } // Rectangle
        else if (R.getType() == Roi.LINE) {
            closed = false;
            Line l = (Line) R;
            Rx = (l.x2 - l.x1);
            Ry = (l.y2 - l.y1);
            a = Math.sqrt(Rx * Rx + Ry * Ry);
            Rx /= a;
            Ry /= a;
            int ind = 1;
            for (i = 0; i <= l.getLength(); i++) {
                points[ind].x = (l.x1 + Rx * i) * res;
                points[ind].y = (l.y1 + Ry * i) * res;
                ind++;
            }
            NPT = ind;
            points = Arrays.copyOfRange(points, 0, NPT);
        } // Line
        else if (R.getType() == Roi.POLYGON) {
            closed = true;
            PolygonRoi pl = (PolygonRoi) R;
            closed = true;
            PolygonRoi p = (PolygonRoi) R;
            Rectangle rectBound = p.getBounds();
            Point l = rectBound.getLocation();
            int NBPT = p.getNCoordinates();
            int pointsX[] = p.getXCoordinates();
            int pointsY[] = p.getYCoordinates();
            for (i = 0; i < NBPT; i++) {
                points[i].x = (pointsX[i] + l.getX()) * res;
                points[i].y = (pointsY[i] + l.getY()) * res;
            }
            NPT = i;
            points = Arrays.copyOfRange(points, 0, NPT);
        } else if (R.getType() == Roi.FREEROI) {
            closed = true;
            PolygonRoi p = (PolygonRoi) (R);
            //Rectangle rectBound = p.getBounds();
            int NBPT = p.getNCoordinates();
            IJ.log("Polygin " + p.isSplineFit());
            float pointsX[] = p.getFloatPolygon().xpoints;
            float pointsY[] = p.getFloatPolygon().ypoints;
            for (i = 0; i < NBPT; i++) {
                points[i].x = (pointsX[i]) * res;
                points[i].y = (pointsY[i]) * res;
            }
            NPT = i;
            points = Arrays.copyOfRange(points, 0, NPT);
        } // PolyLine
        else {
            IJ.log("Selection type not supported " + R.getType() + " " + R.getTypeAsString());
        }
    }

    /**
     * curvature computation
     *
     * @param iref number of the point
     * @param scale scale for curvature computation
     * @return curvature value
     */
    public double curvature(int iref, int scale, boolean fourier) {
        double da;
        double a;
        Point2d U;
        Point2d V;
        Point2d W;
        Point2d pos;
        Point2d norm;
        int i = iref;

        U = new Point2d();
        V = new Point2d();
        W = new Point2d();
        pos = new Point2d();
        norm = new Point2d();

        Point2d[] points_cur;
        if (fourier) {
            points_cur = points_fourier;
        } else {
            points_cur = points;
        }

        if ((iref > scale) && (iref < NPT - scale)) {
            U.x = points_cur[i - scale].x - points_cur[i].x;
            U.y = points_cur[i - scale].y - points_cur[i].y;
            V.x = points_cur[i].x - points_cur[i + scale].x;
            V.y = points_cur[i].y - points_cur[i + scale].y;
            W.x = points_cur[i - scale].x - points_cur[i + scale].x;
            W.y = points_cur[i - scale].y - points_cur[i + scale].y;
            pos.x = (points_cur[i - scale].x + points_cur[i].x + points_cur[i + scale].x) / 3;
            pos.y = (points_cur[i - scale].y + points_cur[i].y + points_cur[i + scale].y) / 3;
        }
        if ((iref <= scale) && (closed)) {
            U.x = points_cur[NPT - 1 + i - scale].x - points_cur[i].x;
            U.y = points_cur[NPT - 1 + i - scale].y - points_cur[i].y;
            V.x = points_cur[i].x - points_cur[i + scale].x;
            V.y = points_cur[i].y - points_cur[i + scale].y;
            W.x = points_cur[NPT - 1 + i - scale].x - points_cur[i + scale].x;
            W.y = points_cur[NPT - 1 + i - scale].y - points_cur[i + scale].y;
            pos.x = (points_cur[NPT - 1 + i - scale].x + points_cur[i].x + points_cur[i + scale].x) / 3;
            pos.y = (points_cur[NPT - 1 + i - scale].y + points_cur[i].y + points_cur[i + scale].y) / 3;
        }
        if ((iref > NPT - scale - 1) && (closed)) {
            U.x = points_cur[i - scale].x - points_cur[i].x;
            U.y = points_cur[i - scale].y - points_cur[i].y;
            V.x = points_cur[i].x - points_cur[(i + scale) % (NPT - 1)].x;
            V.y = points_cur[i].y - points_cur[(i + scale) % (NPT - 1)].y;
            W.x = points_cur[i - scale].x - points_cur[(i + scale) % (NPT - 1)].x;
            W.y = points_cur[i - scale].y - points_cur[(i + scale) % (NPT - 1)].y;
            pos.x = (points_cur[i - scale].x + points_cur[i].x + points_cur[(i + scale) % (NPT - 1)].x) / 3;
            pos.y = (points_cur[i - scale].y + points_cur[i].y + points_cur[(i + scale) % (NPT - 1)].y) / 3;
        }
        double l = Math.sqrt(W.x * W.x + W.y * W.y);
        da = ((U.x * V.x + U.y * V.y) / ((Math.sqrt(U.x * U.x + U.y * U.y) * (Math.sqrt(V.x * V.x + V.y * V.y)))));
        a = Math.acos(da);

        if (l == 0) {
            return 0;
        }
        if (!inside(pos)) {
            return (-1.0 * a / l);
        } else {
            return (a / l);
        }
    }

    /**
     * Fourier descriptor X coeff a
     *
     * @param k number of fourier descriptor
     * @return the fourier value
     */
    public double FourierDXa(int k) {
        double som = 0.0;
        for (int i = 0; i < NPT; i++) {
            som += points[i].x * Math.cos(2 * k * Math.PI * i / NPT);
        }
        return (som * 2 / NPT);
    }

    /**
     * Fourier descriptor X coeff b
     *
     * @param k number of fourier descriptor
     * @return the fourier value
     */
    public double FourierDXb(int k) {
        double som = 0.0;
        for (int i = 0; i < NPT; i++) {
            som += points[i].x * Math.sin(2 * k * Math.PI * i / NPT);
        }
        return (som * 2 / NPT);
    }

    /**
     * Fourier descriptor Y coeff a
     *
     * @param k number of fourier descriptor
     * @return the fourier value
     */
    public double FourierDYa(int k) {
        double som = 0.0;
        for (int i = 0; i < NPT; i++) {
            som += points[i].y * Math.cos(2 * k * Math.PI * i / NPT);
        }
        return (som * 2 / NPT);
    }

    /**
     * Fourier descriptor Y coeff b
     *
     * @param k number of fourier descriptor
     * @return the fourier value
     */
    public double FourierDYb(int k) {
        double som = 0.0;
        for (int i = 0; i < NPT; i++) {
            som += points[i].y * Math.sin(2 * k * Math.PI * i / NPT);
        }
        return (som * 2 / NPT);
    }

    /**
     * Computes curve associated with first kmax fourier descriptors
     *
     * @param kmax number of Fourier descriptors
     */
    public void computeFourier(int kmax) {
        ax = new double[kmax + 1];
        bx = new double[kmax + 1];
        ay = new double[kmax + 1];
        by = new double[kmax + 1];
        for (int i = 0; i <= kmax; i++) {
            ax[i] = FourierDXa(i);
            bx[i] = FourierDXb(i);
            ay[i] = FourierDYa(i);
            by[i] = FourierDYb(i);
        }
    }

    /**
     * Display kmax fourier descriptors
     *
     * @param kmax number of Fourier descriptors
     */
    public void displayValues(int kmax) {
        ResultsTable rt = ResultsTable.getResultsTable();
        rt.reset();
        for (int i = 0; i <= kmax; i++) {
            rt.incrementCounter();
            rt.addValue("ax", ax[i]);
            rt.addValue("ay", ay[i]);
            rt.addValue("bx", bx[i]);
            rt.addValue("by", by[i]);
        }
        rt.show("Results");
    }

    /**
     * draw fourier dexcriptors curve
     *
     * @param A image
     * @param kmax number of fourier desciptors
     * @return Description of the Return Value
     */
    public Roi drawFourier(ImageProcessor A, int kmax, double res) {
        double posx;
        double posy;
        double max = A.getMax();

        float tempx[] = new float[NPT];
        float tempy[] = new float[NPT];

        for (int l = 0; l < NPT; l++) {
            posx = ax[0] / 2.0;
            posy = ay[0] / 2.0;
            for (int k = 1; k <= kmax; k++) {
                posx += ax[k] * Math.cos(2 * Math.PI * k * l / NPT) + bx[k] * Math.sin(2 * Math.PI * k * l / NPT);
                posy += ay[k] * Math.cos(2 * Math.PI * k * l / NPT) + by[k] * Math.sin(2 * Math.PI * k * l / NPT);
            }
            points_fourier[l].x = posx;
            points_fourier[l].y = posy;
            tempx[l] = (float) (posx / res);
            tempy[l] = (float) (posy / res);
        }
        PolygonRoi proi = new PolygonRoi(tempx, tempy, NPT, Roi.FREEROI);

        return proi;
    }

    /**
     * check if point inside the roi
     *
     * @param pos point
     * @return inside ?
     */
    boolean inside(Point2d pos) {
        int count;
        int i;
        double bden;
        double bnum;
        double bres;
        double ares;
        double lnorm;
        Point2d norm = new Point2d();
        Point2d ref = new Point2d();

        ref.x = 0.0;
        ref.y = 0.0;
        norm.x = ref.x - pos.x;
        norm.y = ref.y - pos.y;
        lnorm = Math.sqrt(norm.x * norm.x + norm.y * norm.y);
        norm.x /= lnorm;
        norm.y /= lnorm;

        count = 0;
        for (i = 1; i < NPT - 1; i++) {
            bden = (-norm.x * points[i + 1].y + norm.x * points[i].y + norm.y * points[i + 1].x - norm.y * points[i].x);
            bnum = (-norm.x * pos.y + norm.x * points[i].y + norm.y * pos.x - norm.y * points[i].x);
            if (bden != 0) {
                bres = (bnum / bden);
            } else {
                bres = 5.0;
            }
            if ((bres >= 0.0) && (bres <= 1.0)) {
                ares = -(-points[i + 1].y * pos.x + points[i + 1].y * points[i].x
                        + points[i].y * pos.x + pos.y * points[i + 1].x - pos.y * points[i].x
                        - points[i].y * points[i + 1].x) / (-norm.x * points[i + 1].y
                        + norm.x * points[i].y + norm.y * points[i + 1].x - norm.y * points[i].x);
                if ((ares > 0.0) && (ares < lnorm)) {
                    count++;
                }
            }
        }
        return (count % 2 == 1);
    }

}
