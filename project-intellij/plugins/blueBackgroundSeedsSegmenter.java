import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

/*
*  Batch seeds segmentation (blue background) - v0.5
* Alessandra Mendes, Andrea Loddo
* It saves the mask it .tiff format
* The input folder should contain only RGB images with a blue background.
* All of them will be processed.
* There must be an output folder Results for storing the output files
*/

public class blueBackgroundSeedsSegmenter implements PlugIn
{
	private static final int BLACK = 0;
	private static final int WHITE = 255;
	private static final int GRAYTHRESH = 100;
	private static final int SATTHRESH = 100;
	private static final int BRITHRESH = 100;
	private static final int BLUEDIFF = 10;
	private static final int HUEMIN = 181;
	private static final int HUEMAX = 300;
	private static final int HUERED = 360;
	private static final int HUEGREEN = 120;
	private static final int HUEBLUE = 240;
	private static final int HUESTANDARD = 60;

	public void run(String args)
	{
		IJ.run("Close All");

		int k, qtd = 0;
		List<String> result;
		String directory, resultsDirectory, fileName;
		File resultsDir;

		OpenDialog od = new OpenDialog("Select a blue blackground image. All images into the folder will be processed.", null);
		directory = od.getDirectory();
		resultsDirectory = directory + "results" + File.separator;
		resultsDir = new File(resultsDirectory);
		resultsDir.mkdir();

		if (null == directory)
		return;

		try
		{
			Stream<Path> walk = Files.walk(Paths.get(directory));
			result = walk.filter(Files::isRegularFile)
			.map(x -> x.toString()).collect(Collectors.toList());
			walk.close();

			qtd = result.size();

			ImagePlus imp;
			ImagePlus impColor[] = new ImagePlus[qtd];
			ImagePlus impGray[] = new ImagePlus[qtd];
			ImagePlus impSegm[] = new ImagePlus[qtd];
			String fileNames[] = new String[qtd];
			ImageProcessor ipColor, ipGray;
			ColorProcessor cpRGB;

			int lin, col, i, j;
			float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
			int[] RGB = new int[3];
			int[] GRAY = new int[3];
			float hsb[] = new float[3];
			int R = 0, G = 1, B = 2; // indices

			for (k=0; k<qtd; k++)
			{

				/* Filename generation */
				fileName = (result.get(k).substring(result.get(k).lastIndexOf(File.separator) + 1));
				fileName = (fileName.substring(0, fileName.indexOf(".")));
				fileNames[k] = fileName;

				/* Generation of 8-bit image */
				IJ.open(result.get(k));
				imp = (ImagePlus) IJ.getImage();
				impColor[k] = (ImagePlus) imp.clone();
				ipColor = imp.getProcessor();
				col = ipColor.getWidth();
				lin = ipColor.getHeight();
				IJ.run("8-bit");
				impGray[k] = new Duplicator().run(imp);
				ipGray = imp.getProcessor();

				// flag - red edges
				ipGray.findEdges();
				for (j=0; j<col; j++)
				{
					for (i=0; i<lin; i++)
					{
						ipGray.getPixel(j, i, GRAY);
						ipColor.getPixel(j, i, RGB);
						if(GRAY[0] > GRAYTHRESH)
						{
							RGB[R] = WHITE;
							RGB[G] = BLACK;
							RGB[B] = BLACK;
							ipColor.putPixel(j, i, RGB);
						}
					}
				}

				// background extraction
				cpRGB = (ColorProcessor) ipColor;

				for (j=0; j<col; j++)
				{
					for (i=0; i<lin; i++)
					{
						cpRGB.getPixel(j, i, RGB);

						//Color HUE (model HLS);
						Color.RGBtoHSB(RGB[R], RGB[G], RGB[B], hsb);
						sat = (int) Math.ceil(hsb[1]*SATTHRESH);
						bri = (int) Math.ceil(hsb[2]*BRITHRESH);

						/* Minimum and Maximum RGB values are used
						* in the HUE calculation
						*/
						min = Math.min(RGB[R], Math.min(RGB[G], RGB[B]));
						max = Math.max(RGB[R], Math.max(RGB[G], RGB[B]));

						// Hue computation
						if (max == min)
						hue = 0;
						else if (max == RGB[R])
						hue = ((HUESTANDARD * (RGB[G] - RGB[B]) / (max - min)) + HUERED) % HUERED;
						else if (max == RGB[G])
						hue = (HUESTANDARD * (RGB[B] - RGB[R]) / (max - min)) + HUEGREEN;
						else if (max == RGB[B])
						hue = (HUESTANDARD * (RGB[R] - RGB[G]) / (max - min)) + HUEBLUE;

						/*
						* 1. if the blue channel is the highest AND
						* 2. the distance between the blue channel and
						* the others is at least 10 intensities values AND
						* 3. if the shade is blue (from cyan to navy blue) AND
						* 4. if saturation >= 20% and brightness >= 20%
						*/
						if( ( ((RGB[B]>RGB[G])&&(RGB[B]>RGB[R]))
						&&( (RGB[B]-RGB[R])>BLUEDIFF) )
						&&( ((hue >= HUEMIN)&&(hue <= HUEMAX))
						&&((sat>=20)&&(bri>=20) ) ) )
						{
							ipGray.putPixel(j, i, WHITE);	// set white pixel
						}
						else
						{
							ipGray.putPixel(j, i, BLACK); // set black pixel
						}
					}
				}

				/* Further mask refinements, e.g., hole filling */
				imp.updateAndDraw();
				ipColor.setAutoThreshold("Otsu");
				IJ.run("Convert to Mask");
				IJ.run("Make Binary");
				IJ.run("Fill Holes");
				IJ.run("Set Measurements...", "area");
				IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");

				TextWindow tw = (TextWindow)WindowManager.getFrame("Results");
				ResultsTable rt = tw.getTextPanel().getResultsTable();
				float areasMedia[] = rt.getColumn(0);
				maior = 0;
				for (i=0; i<areasMedia.length; i++)
				if (areasMedia[i]>maior)
				maior = areasMedia[i];
				tw.close(false); // exit without saving
				IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");
				IJ.run("Convert to Mask"); // conversion for mask

				imp = IJ.getImage();
				impSegm[k] = new Duplicator().run(imp);

				/* closing open windows */
				IJ.getImage().changes = false;
				IJ.getImage().close();
				IJ.getImage().changes = false;
				IJ.getImage().close();
			}

			/* Save all the required segmentation masks */
			for (k=0; k<qtd; k++)
			{
				IJ.saveAs(impColor[k], "Jpeg", resultsDirectory+fileNames[k]+"_Color"); // Original color images
				IJ.saveAs(impGray[k], "Jpeg", resultsDirectory+fileNames[k]+"_Gray");  // Gray scale images
				IJ.saveAs(impSegm[k], "tiff", resultsDirectory+fileNames[k]+"_BWMask");  // Segmentation images (masks TIFF)
			}

			TextWindow tw = (TextWindow)WindowManager.getFrame("Summary");
			tw.close(false);
			IJ.showMessage("End of the plugin");

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
