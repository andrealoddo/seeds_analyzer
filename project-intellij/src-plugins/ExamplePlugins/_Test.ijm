setOption("DebugMode", false);
var ImCOMPARE = 0; 
var ImTemp = 0; 
var _fileName = "";	
var LongName; 
var SubName = ""; 
var PlotName;
var centrX;
var centrY;
var maxpoints = 10000; 
var maxharmon = 500;
// var PI = 3.14159265358979
var NumOrigPoints = 0; 
var NumEquiPoints = 0; 
var SingleHarmSizeFactor = 0;
var NumHarm = 0;
var rotation = 0; 
var starting = 0;
var InterEFA = 0; 
var KX = 0; var KY = 0; var QX = 0; var QY = 0; 
var perim_equi = 0;
var ROI_ptsX = newArray(maxpoints); var ROI_ptsY = newArray(maxpoints); 
var newROIx = newArray(maxpoints); var newROIy = newArray(maxpoints); 
var OUT_ptsX = newArray(maxpoints); var OUT_ptsY = newArray(maxpoints);
var EQUI_fptsX = newArray(maxpoints); var EQUI_fptsY = newArray(maxpoints); 
var EQUI_ptsX = newArray(maxpoints); var EQUI_ptsY = newArray(maxpoints);
var singleX = newArray(maxpoints); var singleY = newArray(maxpoints);
var S = newArray(maxpoints); var T = newArray(maxpoints);	
var G = newArray(maxpoints); var H = newArray(maxpoints); 
var A = newArray(maxharmon); var B = newArray(maxharmon); 
var C = newArray(maxharmon); var D = newArray(maxharmon);
var XA = newArray(maxharmon); var XB = newArray(maxharmon);
var FA = newArray(maxharmon); var TH = newArray(maxharmon); var R = newArray(maxharmon); 
var PER = newArray(maxharmon); var PER_NORM = newArray(maxharmon); strw = newArray(maxharmon); 
var PER_ALLIN = newArray(maxharmon); var PER_NORM_ALLIN = newArray(maxharmon);
var ERR = newArray(maxharmon);
var lista = newArray(2500);
var SingleHarmSizeFactor = 1;

macro "Principale" //===================================================================
{	
	// primo dialog
	scelta = newArray("immagini", "coordinate dei contorni");
	Dialog.create("EFA");
	Dialog.addRadioButtonGroup("dati di origine", scelta, 2, 1, "immagini");
	Dialog.show();
	sRunType = Dialog.getRadioButton(); 
	if(sRunType == "immagini") RunType = 1;
	if(sRunType == "coordinate dei contorni") RunType = 2;	

	// secondo dialog
	Dialog.create("EFA");
	Dialog.addMessage("Tutti i files di questo tipo presenti nella cartella verranno processati\n");
	if(RunType == 1)	Dialog.addString("files immagini da analizzare (tif o bmp): ", "tif");
	if(RunType == 2)	Dialog.addString("files coordinate da analizzare (txt o altro): ", "txt");
	Dialog.addNumber("numero di coordinate equispaziate: ", 300);
	Dialog.addNumber("numero di armoniche: ", 20);
	if(RunType == 1) Dialog.addNumber("dimensione del lato delle immagini (pixels): ", 150);
	Dialog.addString("nome del file dei risultati (senza estensione): ", "EFA001");
	if(RunType == 1) Dialog.addCheckbox("verifica maschera di ogni immagine (rallenta)", false);
	if(RunType == 1) Dialog.addCheckbox("salvo le coordinate dei contorni (txt)", true);
	if(RunType == 2) Dialog.addCheckbox("salvo le coordinate dei contorni (txt)", false);
	Dialog.addCheckbox("salvo l'immagine ri-sintetizzata (tif)", true);
	Dialog.addCheckbox("inizio del contorno dal punto più in alto", true);
	Dialog.addCheckbox("rotazione del contorno in senso orario", true);
	Dialog.show();
	
	TipoFiles = "." + Dialog.getString();
	NumEquiPoints = Dialog.getNumber();
	NumHarm = Dialog.getNumber();
	if(RunType == 1) lato = Dialog.getNumber();
	NomeFileRisultati = Dialog.getString();
	if(RunType == 1) VerificaDellaMaschera = Dialog.getCheckbox(); else VerificaDellaMaschera = false;
	SalvaCoordinate = Dialog.getCheckbox();
	SalvaSintetizzati = Dialog.getCheckbox();
	InizioDalTop = Dialog.getCheckbox();
	ContornoOrario = Dialog.getCheckbox();
	
	showMessage("selezionare un file (uno qualsiasi) tra quelli da analizzare");
	path = File.openDialog("selezionare un file (uno qualsiasi) tra quelli da analizzare"); 
	// path ha anche il nome del file
	// _path_files NON ha slash finali
	_path_files = File.getParent(path);
	// print(path, _path_files);
	_path_files = _path_files + "\\";
	FileRisultati = _path_files + NomeFileRisultati + ".prn";

	// se esiste ... 
  if (File.exists(FileRisultati)) 
  {
  	a = getBoolean("il file esiste - sovrascriverlo?");
  	if(a == true) File.delete(FileRisultati);
  }
	
	f = File.open(FileRisultati);
  stra = " ";
  
	stringa ="Files analizzati: " + TipoFiles; File.append(stringa, FileRisultati);
	stringa ="Cartella: " + _path_files; File.append(stringa, FileRisultati);
	stringa ="N coordinate equispaziate: " + NumEquiPoints; File.append(stringa, FileRisultati);
	stringa ="N armoniche: " + NumHarm;File.append(stringa, FileRisultati);
	if(RunType == 1) 	{	stringa ="Lato: " + lato; File.append(stringa, FileRisultati); }
	stringa ="File risultati: " + NomeFileRisultati; File.append(stringa, FileRisultati);
	if(VerificaDellaMaschera == true) stra = "sì"; else stra = "no";
	if(RunType == 1) 	{	stringa ="Verifica della maschera: " + stra; File.append(stringa, FileRisultati);	}	
	if(SalvaCoordinate == true) stra = "sì"; else stra = "no";
	stringa ="Salva contorni di origine: " +  stra; File.append(stringa, FileRisultati);
	if(SalvaSintetizzati == true) stra = "sì"; else stra = "no";
	stringa ="Salva contorni sintetizzati: " +  stra; File.append(stringa, FileRisultati);	
	if(InizioDalTop == true) stra = "sì"; else stra = "no";
	stringa ="Punto di partenza al top: " + stra; File.append(stringa, FileRisultati);
	if(ContornoOrario == true) stra = "sì"; else stra = "no";
	stringa ="Controllo del senso del contorno: " + stra; File.append(stringa, FileRisultati);

	for (i = 1; i <= NumHarm; i++)
	{ 
		strw[i] = "";
		if(i<10) strw[i] = "0";
		if(i<100) strw[i] = strw[i] + "0";
		strw[i] = strw[i] + toString(i);
	}
	
	stringa = "     \t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "A"+ strw[i] +"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "B" + strw[i] +"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "C" + strw[i] +"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "D" + strw[i] +"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "MinAx" + strw[i] +"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "MajAx" + strw[i] +"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + "PerN&A" + strw[i] +"\t";
	File.append(stringa, FileRisultati);
		
	//File.close(f);
	//exit("  ");
	
  //f = File.open("/Users/wayne/table.txt");
  // use d2s() function (double to string) to specify decimal places 
  // es. print(f, d2s(i,6) + "  \t" + d2s(sin(i),6) + " \t" + d2s(cos(i),6));
	svuota_Roi_manager();
	
	run("Close All");
	lista = getFileList(_path_files);

	if(VerificaDellaMaschera ==  false) setBatchMode("hide");
	
	// PRENDE CICLICAMENTE LE IMMAGINI
	for (z=0; z<lista.length; z++)
	{  
  	run("Close All"); 
		k = lengthOf(lista[z]);
		// ciclo   // se contiene l'estensione... si tratta di una immagine da considerare
		if (substring(lista[z],k-4) == TipoFiles)
		{	

			if(RunType == 1)
			{
				print(lista[z]);
				open(_path_files + "\\" + lista[z]);
				run("Set Scale...", "distance=1 known=1 pixel=NaN unit=pixel global");
				_fileName = getTitle();
				FileNameSemplice = File.nameWithoutExtension;
				LongName = File.directory + File.nameWithoutExtension;
				ImageWidth = round(getWidth());
				ImageHeight = round(getHeight());
				print(_fileName);
				svuota_Roi_manager();
				selectImage(_fileName);
				run("Duplicate...", " ");
				imBinarizzata = getImageID();
				selectImage(imBinarizzata);
				setOption("BlackBackground", false);
				run("Make Binary");	
				setTool("wand");
				selectImage(imBinarizzata);
				run("Select None");
				centrX = lato/2;
				centrY = lato/2;
				doWand(centrX, centrY);
				if(VerificaDellaMaschera != false) 
				{
					waitForUser("stop per verifica ed eventuale modifica del contorno");
				}
				getSelectionCoordinates(ROI_ptsX, ROI_ptsY);
				setTool("hand");
			}			

			if(RunType == 2)
			{
				print(lista[z]);
				name = "open=[" + _path_files + "\\" + lista[z] + "]";
				run("XY Coordinates... ", name);
				run("Set Scale...", "distance=1 known=1 pixel=NaN unit=pixel global");
				_fileName = getTitle();
				FileNameSemplice = File.nameWithoutExtension;
				LongName = File.directory + File.nameWithoutExtension;
				ImageWidth = round(getWidth());
				ImageHeight = round(getHeight());
				print(_fileName);
				svuota_Roi_manager();
				getSelectionCoordinates(ROI_ptsX, ROI_ptsY);
			}
			
			if(InizioDalTop == true)
			{
				// identifica top
				minY = 10000;
				for (i=0; i<ROI_ptsX.length; i++)
				{
					if (ROI_ptsY[i]<minY) 
				  {
						pos_minY=i;
						minY = ROI_ptsY[i]; 
					}
				}

				k = - 1; // per poi partire dal top
				for (i=pos_minY; i<ROI_ptsX.length; i++)
				{ 
					k = k + 1;
					newROIx[k] = ROI_ptsX[i]; 
					newROIy[k] = ROI_ptsY[i]; 
				}
				for (i=0; i<pos_minY; i++)
				{ 
					k = k + 1;
					newROIx[k] = ROI_ptsX[i]; 
					newROIy[k] = ROI_ptsY[i]; 
				}

				print("Controllo: coordinate newROI =", k + 1, "   -   coordinate ROI_pts =", ROI_ptsX.length);

				// riordina le coordinate originali
				for (i=0; i<k+1; i++)
				{
					ROI_ptsX[i] = newROIx[i];
					ROI_ptsY[i] = newROIy[i];
				}
			}

			// Sum over the edges, (x2 − x1)(y2 + y1) 
			// If the result is positive the curve is clockwise, if it's negative the curve is counter-clockwise. 
			// If y0 is at the top, the relation is inverted!!!
			if(ContornoOrario == true)
			{
				sum = 0;
				for (i=0; i<(k-1); i++)
				{
					sum = sum + ((ROI_ptsX[i+1] - ROI_ptsX[i]) * (ROI_ptsY[i+1] + ROI_ptsY[i])) ;
				}
				sum = sum + ((ROI_ptsX[0] - ROI_ptsX[k]) * (ROI_ptsY[0] + ROI_ptsY[k])) ;
				
				if(sum > 0) // se conterclockwise, inverte partendo dalla prima ma a ritroso 
				{
					// riparte dalle coordinate newROI e le carica sulle ROI_pts in senso inverso 
					for (i=0; i<k; i++)
					{
						ROI_ptsX[i] = newROIx[k-i-1];
						ROI_ptsY[i] = newROIy[k-i-1];
					}
				}
			}
			run("Select None");
			makeSelection("freehand", ROI_ptsX, ROI_ptsY); 
			if (SalvaCoordinate == true) saveAs("XY Coordinates", LongName + "-Contour.txt");
			
			after_the_contour();
			fourcalc();
		}
	}			
	setBatchMode("exit and display");
	run("Close All");
	File.close(f);
}

function after_the_contour()
{
	NumOrigPoints = ROI_ptsX.length;
	// check the ROI
	if (NumOrigPoints <= 5)
	{ 
		waitForUser("Error - contour coordinates less than 5");
		exit();
	}

	//SubName = " N=" + toString(NumEquiPoints) + " H=" + toString(NumHarm);
	
	// print il the Log window
	// print("LongName:", LongName);
	// print("image:", _fileName);
	// print("image subname:", SubName);
	// print("aquired coordinates:", NumOrigPoints);
	// print("coordinates to calculate:", NumEquiPoints);
	// print("harmonics:", NumHarm);
	
	// remove duplicate points
	NumOrigPoints = RemoveDuplicatePts(ROI_ptsX, ROI_ptsY, NumOrigPoints, 0);

	// calcolo min e max
	minX = 10000; maxX = 0; minY = 10000; maxY = 0;
	for (i=0; i<ROI_ptsX.length; i++)
	{
	     if (ROI_ptsX[i]<minX) minX=ROI_ptsX[i]; 
	     if (ROI_ptsY[i]<minY) minY=ROI_ptsY[i]; 
	     if (ROI_ptsX[i]>maxX) maxX=ROI_ptsX[i]; 
	     if (ROI_ptsY[i]>maxY) maxY=ROI_ptsY[i];
	}
	ImageWidth = round((maxX-minX) * 1.1);
	ImageHeight = round((maxY-minY) * 1.1);

	makeRectangle(minX-ImageWidth*0.05, minY-ImageHeight*0.05, ImageWidth, ImageHeight);
	run("Crop");
	
	for (i=0; i<ROI_ptsX.length; i++)
	{
	     ROI_ptsX[i]=ROI_ptsX[i]-(minX-ImageWidth*0.05); 
	     ROI_ptsY[i]=ROI_ptsY[i]-(minY-ImageHeight*0.05); 
	}

	// crea l'immagine per la verifica delle coordinate
	newImage(LongName + "-" + SubName + "-Orig=RED Equisp=BLUE Synth=GREEN", "RGB", ImageWidth, ImageHeight, 3);	
	ImCOMPARE = getImageID();
	
	selectImage(ImCOMPARE);	
	setSlice(3);	
	setForegroundColor(255,255,255);
	run("Select All");
	run("Fill", "slice");
	run("Select None");	
	setForegroundColor(0,0,0);
	moveTo(ROI_ptsX[0], ROI_ptsY[0]);
	for (i=0; i<ROI_ptsX.length; i++)
	     lineTo(ROI_ptsX[i], ROI_ptsY[i]);
	// to close the contour
	lineTo(ROI_ptsX[0], ROI_ptsY[0]);
		
	// create floating point outline with equidistant points
	NumEquiPoints = Equispac(ROI_ptsX, ROI_ptsY, NumOrigPoints, NumEquiPoints);

	// print("numero di equispaziate: " + toString(NumEquiPoints));
	NumEquiPoints = RemoveDuplicatePts(EQUI_fptsX, EQUI_fptsY, NumEquiPoints, 0);
	
	//print("numero di equispaziate: " + toString(NumEquiPoints));
	selectImage(ImCOMPARE);	
	setSlice(2);	
	setForegroundColor(255,255,255);
	run("Select All");
	run("Fill", "slice");
	run("Select None");	
	setForegroundColor(0,0,0);
	moveTo(EQUI_ptsX[0], EQUI_ptsY[0]);
	for (i=1; i < NumEquiPoints; i++)     // modifica 27 sett 2017: for (i=1 anziché i = 0...
	     lineTo(EQUI_ptsX[i],EQUI_ptsY[i]);
	// to close the contour
	lineTo(EQUI_ptsX[0], EQUI_ptsY[0]);
}

// Remove adjacent points that have the same coordinates
function RemoveDuplicatePts(arrayX, arrayY, N, base) //===================================================================
{
	if (base == 0)
	{
		j = 0;
		for (i = 1; i < N; i++)
		{
			if ((arrayX[i] != arrayX[j]) || (arrayY[i] != arrayY[j]))
			{
				j = j + 1;
				arrayX[j] = arrayX[i];
				arrayY[j] = arrayY[i];
			}
		}
		N = j + 1;
	}
	if (base == 1)
	{
		j = 1;
		for (i = 2; i <= N; i++)
		{
			if ((arrayX[i] != arrayX[j]) || (arrayY[i] != arrayY[j]))
			{
				j = j + 1;
				arrayX[j] = arrayX[i];
				arrayY[j] = arrayY[i];
			}
		}
		N = j + 1;
	}
	
	return N;
}

function Equispac(array1X, array1Y, NumOrigPoints, NumEquiPoints) //===================================================================
{
	RI = newArray(128); NI = newArray(128);
	G = newArray(maxpoints); H = newArray(maxpoints); X = newArray(maxpoints); Y = newArray(maxpoints);
	
	print("N° coordinate originali: " + toString(NumOrigPoints));
	print("N° equispaziate iniziale: " + toString(NumEquiPoints));

	// X e Y partono da 1
	// array1X e array1Y - che sono le ROI, partono da zero
	for (i = 1; i <= NumOrigPoints; i++)
	{
		// per partire dall'elemento 1
		X[i]=array1X[i-1];
		Y[i]=array1Y[i-1];
	}

	X[0]=NumOrigPoints;
	Y[0]=NumOrigPoints;	
	
	rm = 0; lm = 0; t=0;
	
	G[0] = NumEquiPoints;	
	RI[0] = 0;
	NI[0] = 0;

	t = perimeter(X,Y, NumOrigPoints, 1);
	print("perimeter(X,Y, NumOrigPoints, 1)" + toString(t));
	
	rri = t/NumEquiPoints*1.3;
	rr = 0;
	flag = 0;
	
	while (flag != 1)
	{
		k = 1;
		G[1] = X[1];
		H[1] = Y[1];
		sx = 0;
		sy = 0;
		i = 0;
		
		// piazzare cursore in G[1] e H[1]
		for (j = 2; j <= NumOrigPoints; j++)
		{
			i = i+1;
	 		sx = sx+X[j]*i;
			sy = sy+Y[j]*i;
			x1 = G[k];
			x2 = sx/((i*(i+1))/2);
			y1 = H[k];
			y2 = sy/((i*(i+1))/2);
			d = sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
	 		if (d >=  rri)
	 		{ 
				k = k+1; 
				G[k] = rri/d*(x2-x1)+x1;
				H[k] = rri/d*(y2-y1)+y1;
				sx = 0;
				sy = 0;
				i = 0;
				j = j-1;
	 		}
		}
		
		d = sqrt((G[k]-X[1])*(G[k]-X[1])+(H[k]-Y[1])*(H[k]-Y[1]));

		while (d >= (rri*1.5))
		{
			x1 = G[k];
			x2 = X[1];
			y1 = H[k];
			y2 = Y[1];
	 		k = k+1;
			G[k] = rri/d*(x2-x1)+x1;
			H[k] = rri/d*(y2-y1)+y1;
			d = sqrt((G[k]-X[1])*(G[k]-X[1])+(H[k]-Y[1])*(H[k]-Y[1]));
		}
		
		if(d<rri*0.5) k = k-1;
		
		if(abs(k-NumEquiPoints)>1)
		{
			rr = rr+1;
			RI[rr] = rri;
			NI[rr] = k;
			if(rr == 1)
			{
				kn = k/NumEquiPoints;
				if (1 > kn) 
					rri = rri*(kn+kn/100);
				else
					rri = rri*(kn-kn/100);
			}
		 	else
			{  // regressione 
	 			r1 = 0; r2 = 0; c1 = 0;	c2 = 0; cr = 0; n = 0;
				if (rr >= 5) 
					gg = rr-4;
				else
					gg = 1;
	
				for (j = gg; j <= rr; j++)
				{
		 			r1 = r1+RI[j];
					r2 = r2+RI[j]*RI[j];						// v. dipendente y - lungh. 
					c1 = c1+NI[j];
					c2 = c2+NI[j]*NI[j];						// v. indipend.	x - numero
					cr = cr+RI[j]*NI[j];
					n = n+1;
				}
				
				xy = cr-(c1*r1/n);
				xx = c2-(c1*c1/n);				 					
		 		if (xx <=  0.0001)
		 		{
		 			kn = NI[rr]/NumEquiPoints;		
		 			random("seed", 48);					
					if (1 > kn)
			 			rri = rri*(kn+(kn/(3+random())));
					else
			 			rri = rri*(kn-(kn/(3+random())));
		 		}
				else
				{
			 		b = xy/xx;
					a = (r1-b*c1)/n;
					rri = a+b*NumEquiPoints;
				}
			}
		 	
		 	/*	
	 	 	For j = 1 To rr
			Debug.Print NI[j]; " segmenti di Lunghezza"; RI[j];
			Debug.Print "Nuova Lunghezza"; rri;
			Next j
			chiedi nuova lunghezza
			rri;
			*/		
		}
		if (abs(k-NumEquiPoints) <= 1) flag = 1;  // esce dal ciclo 
	}

	NumEquiPoints = k;

	// EQUI... partono da zero
	for (j = 0; j < NumEquiPoints; j++)
	{
		EQUI_fptsX[j] = G[j+1];
		EQUI_fptsY[j] = H[j+1];
	}
		
	for (j = 0; j < NumEquiPoints; j++)
	{
		EQUI_ptsX[j] = round(EQUI_fptsX[j]);
		EQUI_ptsY[j] = round(EQUI_fptsY[j]);
	}

	perim_equi = perimeter(EQUI_fptsX,EQUI_fptsY, NumEquiPoints, 0);
	//SubName = SubName + " PE=" + toString(perim_equi) + " IW=" + toString(ImageWidth) + " IH=" + toString(ImageHeight);
	
	//print("numero di equispaziate: " + toString(NumEquiPoints));
	//print("coordinate floating point");
	//print("start   x =:" + toString(EQUI_fptsX[0]) +  "   y =:" + toString(EQUI_fptsY[0]));
	//print("end     x =:" + toString(EQUI_fptsX[NumEquiPoints-1]) +  "   y =:" + toString(EQUI_fptsY[NumEquiPoints-1]));	
	//print("coordinate intere");	
	//print("start   x =:" + toString(EQUI_ptsX[0]) +  "   y =:" + toString(EQUI_ptsY[0]));
	//print("end     x =:" + toString(EQUI_ptsX[NumEquiPoints-1]) +  "   y =:" + toString(EQUI_ptsY[NumEquiPoints-1]));	
	//print("perimetro equispaziate =" + toString(perim_equi));
	
	return NumEquiPoints;
}

function Atn2(deltay, deltax) //===================================================================
// Returns angles from 0 (vertical pointing up) to
// 2*pi (360deg.), clockwise
{
	if (deltax == 0)
	{
		if (deltay < 0) 
			aatn2 = 0;
		else
			aatn2 = PI;
	}
	else
	{
		angle = atan(deltay / deltax) + PI/2;
		if (deltax < 0) angle = angle + PI;
		aatn2 = angle;
	}
	// aggiunto da me, per riportare l'angolo 0 in orizzontale
	return (aatn2 + PI/2);
}

function fourcalc() //===================================================================
{
	//Dim xx#, yy#, dx#, dy#, tm#, tp#, dt#, si#, de#, hh#, sa#, sb#, sc#, sd#, e1#, e2#, s1#, s2#, tth#, sth#, cth#
	//Dim av#, bv#, cv#, dv#, xxa#, xxb#, ffa#, ffb#, zz#, perim_equi#, HX#, HY#, difcos#, difsin# 
	//Dim Z#(3,3), FZ#(3,3), TT#(3,3), K#(3,3) ' tutti da azzerare
	//Dim NORMR$,NORMG$
	//Dim i#, j#, kk%, flag%, flig%, aa%, DocId%
	//Dim y%, c1$, c2$, shortname$, longname$, Iname As String *255
	
	// A, B, C, D coefficienti armoniche da calcolare 
	// XA, XB semiassi
	// FA flag rotazione
	// TH angolo
	// G, H  coordinate equispaziate (input), con base = 1 !!!
			

	Z1 = newArray(3); Z2 = newArray(3); Z3 = newArray(3);
	F1 = newArray(3); F2 = newArray(3); F3 = newArray(3);	
	T1 = newArray(3); T2 = newArray(3); T3 = newArray(3);
	K1 = newArray(3); K2 = newArray(3); K3 = newArray(3);	
	
	A[0] = NumHarm; 	

	// G e H base 1
	for (i = 1; i <= NumEquiPoints; i++)
	{
		G[i] = EQUI_fptsX[i-1];
		H[i] = EQUI_fptsY[i-1];
	}

	for (i=0; i <= 2; i++) 
	{
		Z1[i]=0; Z2[i]=0; 
		F1[i]=0; F2[i]=0; 
		T1[i]=0; T2[i]=0;
		K1[i]=0; K2[i]=0;
	}
		
	for (i=1; i <= NumHarm; i++)
	{
		PER_ALLIN[i]=0;
		PER_NORM_ALLIN[i]=0;
	}
	
	G[0]=G[NumEquiPoints]; 
	H[0]=H[NumEquiPoints];
	
	xx=G[0]; yy=H[0];
	HX=0; HY=0; dx=0; dy=0; dt=0; tm=0; tp=0;
	
	for (i=1; i <= NumEquiPoints; i++)
	{
		xx=xx+dx; yy=yy+dy; tm=tm+dt;
		dx=G[i]-G[i-1]; dy=H[i]-H[i-1]; dt=sqrt(dx*dx+dy*dy); tp=tp+dt;
		si=(xx-dx/dt*tm)*dt; de=(yy-dy/dt*tm)*dt;
		HX=HX+dx/(2*dt)*(tp*tp-tm*tm)+si; HY=HY+dy/(2*dt)*(tp*tp-tm*tm)+de;
	}
	HX=HX/perim_equi; HY=HY/perim_equi;
	print("centro ellissi:  x = " + toString(HX) + "   y = " + toString(HY));
	
	R[1]=HX; R[2]=HY; R[3]=perim_equi;

	// Coefficienti Armoniche 
	print("Calcolo armoniche");   
	for (i=1; i <= NumHarm; i++)
	{
		hh=perim_equi/(2*i*i*PI*PI); zz=2*i*PI/perim_equi;
		sa=0; sb=0; sc=0; sd=0; tp=0;
		for (j=1; j <= NumEquiPoints; j++)
		{
			dx=G[j]-G[j-1]; dy=H[j]-H[j-1]; dt=sqrt(dx*dx+dy*dy); tp=tp+dt;
			e1=zz*tp; e2=zz*(tp-dt);
	 		difcos=(cos(e1)-cos(e2))/dt; difsin=(sin(e1)-sin(e2))/dt;
			sa=sa+dx*difcos; sb=sb+dx*difsin;
			sc=sc+dy*difcos; sd=sd+dy*difsin;
		}
		A[i]=hh*sa; B[i]=hh*sb; C[i]=hh*sc; D[i]=hh*sd;
	}

	print("Semiassi, Inclinazione e Starting Point");
	for (i=1; i <= NumHarm; i++)
	{
		print("arm  " + toString(i));
		s1=2*(A[i]*B[i]+C[i]*D[i]);
		s2=(pow(A[i],2)+pow(C[i],2)-pow(B[i],2)-pow(D[i],2));
		tth = 0.5*Atn2(s1,s2);  
		
		while(tth<0) 
		{
			tth=tth+2*PI; 
		}
		sth=sin(tth); cth=cos(tth); TH[i]=tth;
		av=A[i]*cth+B[i]*sth; bv=A[i]*(-sth)+B[i]*cth;
		cv=C[i]*cth+D[i]*sth; dv=C[i]*(-sth)+D[i]*cth;
		xxa=av*av+cv*cv; xxb=bv*bv+dv*dv;
		xxa=sqrt(xxa); xxb=sqrt(xxb);
		ffa=Atn2(cv,av); ffb=Atn2(dv,bv);
	
		while(abs(ffa-ffb)>(PI/2+PI/50))
		{
			if(ffa > ffb)
		 		ffa=ffa-(2*PI);
			else 
				ffb=ffb-(2*PI);
		}
 
 		if (ffa>ffb)
 			flig=1;	     	// rotazione oraria 
 		else 
 			flig=0;			// rotazione antioraria 

		
 		ffa=ffa-(10*PI);	    // angolo ffa riportato entro il range 0 - 2PI 
 		while(ffa<0)
 		{ 
 			ffa=ffa+(2*PI);
 		}

 		if (flig==0) ffa=ffa*(-1); // flag di rotazione 

		XA[i]=xxa; XB[i]=xxb; FA[i]=ffa;
		PER[i]=2*PI*sqrt(0.5*(pow(2*XA[i],2)+pow(2*XB[i],2)));

		// perimetro * n° armonica ^ 1.5
		PER_NORM[i]=PER[i] * pow(i, 1.5); // perimetro * n° armonica ^ 1.5

		if (i == 1)
		{
			PER_ALLIN[1]=PER[1];
			PER_NORM_ALLIN[1]=PER_NORM[1];
		}
		else
		{
			if (((FA[i] * FA[1]) > 0) && (i > 1))                  PER_ALLIN[i-1]=PER_ALLIN[i-1]+PER[i];
			if (((FA[i] * FA[1]) < 0) && (i > 1) && (i < NumHarm)) PER_ALLIN[i+1]=PER_ALLIN[i+1]+PER[i];
			if (((FA[i] * FA[1]) > 0) && (i > 1))                  PER_NORM_ALLIN[i-1]=PER_NORM_ALLIN[i-1]+PER_NORM[i];
			if (((FA[i] * FA[1]) < 0) && (i > 1) && (i < NumHarm)) PER_NORM_ALLIN[i+1]=PER_NORM_ALLIN[i+1]+PER_NORM[i];
		}
	}

	G[0]=NumEquiPoints; 
	H[0]=NumEquiPoints;
	
	A[0]=NumHarm; B[0]=NumHarm; C[0]=NumHarm; D[0]=NumHarm;
	XA[0]=NumHarm; XB[0]=NumHarm; FA[0]=NumHarm; TH[0]=NumHarm;
	R[0]=3;

	//////////////////// stack del contorno cumulato
	//SubName = SubName + " HX=" + toString(HX) + " HY=" + toString(HY); 
		
	//newImage(LongName + "-" + SubName, "8-bit white", ImageWidth*1.2, ImageHeight*1.2, NumHarm);
	//ImFILLED_SLICEScumul = getImageID();

	//newImage(LongName + "-" + SubName, "8-bit white", ImageWidth*1.2, ImageHeight*1.2, NumHarm);
	//ImCONTOUR_SLICEScumul = getImageID();

	for (j=1; j <= NumEquiPoints; j++) 
	{
		S[j]=0;
		T[j]=0;
	}
	S[0]=NumEquiPoints;
	T[0]=NumEquiPoints;	
	
	for (h=1; h <= NumHarm; h++)
	{
		tp=0; zz=2*h*PI/perim_equi; 
		// dt = perim_equi/ NumEquiPoints;  (? dovrebbe essere cosi')
		for (j=1; j <= NumEquiPoints; j++)
		{
			tp=tp+dt; 
			S[j]=S[j]+A[h]*cos(zz*tp)+B[h]*sin(zz*tp);
			T[j]=T[j]+C[h]*cos(zz*tp)+D[h]*sin(zz*tp);
			OUT_ptsX[j-1] = S[j]+HX;
			OUT_ptsY[j-1] = T[j]+HY;
		}

		// OUT... vanno dall'elemento 0 all'elemento NumEquiPoints-1
		// qui ho aggiunto l'elemento [NumEquiPoints] per chiudere la selezione
		OUT_ptsX[NumEquiPoints] = OUT_ptsX[0]; OUT_ptsY[NumEquiPoints] = OUT_ptsY[0];
		
		//selectImage(ImCONTOUR_SLICEScumul);
		//setSlice(h);
		//moveTo(OUT_ptsX[0], OUT_ptsY[0]);
		//for (i=1; i <= NumEquiPoints; i++)   // modifica 27 sett 2017: for (i=1 anziché i = 0...
			//lineTo(OUT_ptsX[i], OUT_ptsY[i]);
		// to close the contour
		//lineTo(OUT_ptsX[0], OUT_ptsY[0]);	
		
		//selectImage(ImFILLED_SLICEScumul);
		//setSlice(h);
		//makeSelection("freehand", OUT_ptsX, OUT_ptsY); // con questo disegna!!!
		//run("Fill", "slice");
		//run("Select None");
	}

	//////////////////// stack delle singole ellissi (non cumulativa)
	// l'immagine delle singole ellissi deve essere ingrandita
	ImSLICESsingleWidth = ImageWidth * SingleHarmSizeFactor;
	ImSLICESsingleCentrX = ImSLICESsingleWidth / 2;
	ImSLICESsingleHeight = ImageHeight * SingleHarmSizeFactor;
	ImSLICESsingleCentrY = ImSLICESsingleHeight / 2;
	newImage(LongName + "-" + SubName, "RGB", ImSLICESsingleWidth, ImSLICESsingleHeight, NumHarm);
	ImSLICESsingle = getImageID();
	selectImage(ImSLICESsingle);
	
	for (h=1; h <= NumHarm; h++)
	{
		setSlice(h);
		if((FA[1] * FA[h]) > 0) setForegroundColor(250,128,214);
		else setForegroundColor(30,144,255);
		
		tp=0; zz=2*h*PI/perim_equi; // tp=tp+dt;
		for (j=1; j <= NumEquiPoints; j++)
		{
			tp=tp+dt; 
			xxx = (A[h]*cos(zz*tp)+B[h]*sin(zz*tp)) * pow(h, 1.2);
			yyy = (C[h]*cos(zz*tp)+D[h]*sin(zz*tp)) * pow(h, 1.2);
			if (j == 1) moveTo(xxx + ImSLICESsingleCentrX, yyy + ImSLICESsingleCentrY);			
			else
			lineTo(xxx + ImSLICESsingleCentrX, yyy + ImSLICESsingleCentrY);
		}
		tp = dt; // per chiudere
		xxx = (A[h]*cos(zz*tp)+B[h]*sin(zz*tp)) * pow(h, 1.2);
		yyy = (C[h]*cos(zz*tp)+D[h]*sin(zz*tp)) * pow(h, 1.2);
		lineTo(xxx + ImSLICESsingleCentrX, yyy + ImSLICESsingleCentrY);		
		if(XA[h] * XB[h]  > 0.0001)
		{
			run("Select None");	
			doWand(ImSLICESsingleCentrX, ImSLICESsingleCentrY);	
			run("Fill", "slice");
			run("Select None");
		}	
	}
	
	// draw the synthesized contour over the original contour
	// now OUT_ptsX[] and OUT_ptsY[] show the full-synthesized contour 
	selectImage(ImCOMPARE);	
	setSlice(1);	
	setForegroundColor(255,255,255);
	run("Select All");
	run("Fill", "slice");
	run("Select None");	
	setForegroundColor(0,0,0);
	moveTo(OUT_ptsX[0], OUT_ptsY[0]);
	for (i=0; i < NumEquiPoints; i++)
		lineTo(OUT_ptsX[i], OUT_ptsY[i]);
	// to close the contour
	lineTo(OUT_ptsX[0], OUT_ptsY[0]);	

 	run("Clear Results");
	
	stringa = FileNameSemplice + "\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(A[i],5)+"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(B[i],5)+"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(C[i],5)+"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(D[i],5)+"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(XA[i],5)+"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(XB[i],5)+"\t";
	for (i = 1; i <= NumHarm; i++) stringa = stringa + d2s(PER_NORM_ALLIN[i],5)+"\t";
	File.append(stringa, FileRisultati);
	
	selectImage(ImCOMPARE);
	saveAs("TIF", LongName + "-" + SubName + "-Comparative.tif");
	// close();
}

function perimeter(G, H, N, base)  //===================================================================
{
	perim=0;	

	if (base == 1)
	// sino all'elemento N
	{	
		for (j=1; j <= (N-1); j++)
		{
			perim=perim+sqrt((G[j]-G[j+1])*(G[j]-G[j+1])+(H[j]-H[j+1])*(H[j]-H[j+1]));
		}
		perim=perim+sqrt((G[N]-G[1])*(G[N]-G[1])+(H[N]-H[1])*(H[N]-H[1]));
	}

	if (base == 0)
	// sino all'elemento N-1
	{	
		for (j=0; j < (N-1); j++)
		{
			perim=perim+sqrt((G[j]-G[j+1])*(G[j]-G[j+1])+(H[j]-H[j+1])*(H[j]-H[j+1]));
		}
		perim=perim+sqrt((G[N-1]-G[0])*(G[N-1]-G[0])+(H[N-1]-H[0])*(H[N-1]-H[0]));
	}
	return perim;
}

function svuota_Roi_manager()
{
	n = roiManager("count");
	// print(n);
	if(n > 0) 
	{
		for(j = n-1; j >=0; j--) 
		{
			roiManager("select", j);
			roiManager("Delete");
		}
	}
}


