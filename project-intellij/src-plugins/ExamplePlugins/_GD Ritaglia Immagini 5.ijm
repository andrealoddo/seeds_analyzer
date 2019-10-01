da_quali_immagini = newArray("dalle due originali a colori", "dall'immagine binarizzata");
Dialog.create(""); 
Dialog.addRadioButtonGroup("Menu", da_quali_immagini, 2, 2, da_quali_immagini[1]); 
Dialog.addNumber("Lato delle immagini ritagliate: ", 150);
Dialog.show();

opzione1 = Dialog.getRadioButton();
opzione2 = 0;
smallSize = Dialog.getNumber();

run("Close All");
run("Labels...", "color=black font=14 show draw bold");

NewMaskNames = newArray(500);
NewRGBNames = newArray(500);

if(opzione1 == "dalle due originali a colori")
{
	showMessage("dopo OK, selezionare nell'ordine l'immagine a sfondo chiaro e poi quella a sfondo scuro");
	open("");
	run("Set Scale...", "distance=0 known=0 pixel=1 unit=pixel");
	ImRGB = getImageID();  
	run("Duplicate...", " ");
	run("8-bit");
	Im1 = getImageID();
	fn1 = File.nameWithoutExtension;
	
	open("");
	run("8-bit");
	Im2 = getImageID();
	fn2 = File.nameWithoutExtension;
	
	lfn = lengthOf(File.name);
	extension = substring(File.name, lfn-4, lfn);
	print(extension);
	fd = File.directory;
	
	imageCalculator("Subtract create", Im1, Im2);
	setAutoThreshold("Default");
	run("Convert to Mask");
	//run("Invert LUT");
	ImMask = getImageID();
	string = fn1 + fn2 + extension;
	rename(string);

	opzione2 = getBoolean("crea ritagli anche dall'immagine a colori?");
	print("opzione 2 =", opzione2);
}
if(opzione1 == "dall'immagine binarizzata")
{
	open("");
	run("Set Scale...", "distance=0 known=0 pixel=1 unit=pixel");
	lfn = lengthOf(File.name);
	fn1 = File.nameWithoutExtension;
	extension = substring(File.name, lfn-4, lfn);
	ImMask = getImageID();
}

setBatchMode("hide");

run("Colors...", "foreground=white background=black selection=red");

run("Set Measurements...", "area centroid bounding fit display redirect=None decimal=3");
run("Analyze Particles...", "size=50-Infinity show=[Overlay Masks] display clear include in_situ");

n = nResults();
// waitForUser;
// nb: anche la numerazione delle righe della finestra Results inizia da zero
if (n > 1)
{ 
	setForegroundColor(193, 209, 221);
	for (j = 0; j <n; j+=1)  
	{
		selectImage(ImMask);	
		centrX = getResult("X", j); // centroid
		centrY = getResult("Y", j);
		bX = getResult("BX", j); // origin of the bounding rectangle
		bY = getResult("BY", j);
		bW = getResult("Width", j); // width of the bounding rectangle
		bH = getResult("Height", j); // height of the bounding rectangle
		majE = getResult("Major", j); // major axis of the fit ellipsis
		minE = getResult("Minor", j); // minor axis of the fit ellipsis
		anglE = getResult("Angle", j); // angle of the major axis of the fit ellipsis

		// allargo il bounding box
		bX = bX - 2;
		bY = bY - 2;
		bW = bW + 4;
		bH = bH + 4;

		addstring = "";
		u = j + 1;
		if (u < 100) addstring = "0";
		if (u < 10) addstring = addstring + "0";
		addstring = addstring + toString(u);
		print (addstring);

		newImageMaskName = File.nameWithoutExtension + "-Mask" + addstring + extension;
		print (newImageMaskName);
		newImage(newImageMaskName, "8-bit  black", smallSize, smallSize, 1);
		ImMaskSmall = getImageID();

		if(opzione1 == "dalle due originali a colori" && opzione2 == 1)
		{
			newImageRGBName = File.nameWithoutExtension + "-RGB" + addstring + extension;
			print (newImageRGBName);
			newImage(newImageRGBName, "RGB white", smallSize, smallSize, 1);
			fill();
			ImRGBSmall = getImageID();
		}
				
		//setTool("rectangle");	
		selectImage(ImMask);	
		makeRectangle(bX, bY, bW, bH);
		run("Copy");
		selectImage(ImMaskSmall);
		run("Paste");
		run("Select None");	
		stringa = "angle=" + toString(anglE - 90) + " grid=1 interpolation=Bicubic fill";
		setBackgroundColor(0, 0,0);
		run("Rotate... ", stringa);
		wholePath = File.directory + newImageMaskName;
		saveAs("Tiff", wholePath);
		NewMaskNames[j] = wholePath;
		print ("NewMaskNames[] =", NewMaskNames[j]);

		if(opzione1 == "dalle due originali a colori" && opzione2 == 1)
		{
			selectImage(ImRGB);	
			makeRectangle(bX, bY, bW, bH);
			run("Copy");
			selectImage(ImRGBSmall);
			run("Paste");
			run("Select None");	
			stringa = "angle=" + toString(anglE - 90) + " grid=1 interpolation=Bicubic fill";
			setBackgroundColor(193, 209, 221);
			run("Rotate... ", stringa);
			wholePath = File.directory + newImageRGBName;
			saveAs("Tiff", wholePath);
			NewRGBNames[j] = wholePath;
			print ("NewRGBNames[] =", NewRGBNames[j]);
		}
	}					
	run("Close All"); 

	// verifica ed eventuale rotazione 180°
	IJ.deleteRows(0, n);
	run("Set Measurements...", "mean redirect=None decimal=3");
	r = -1;
	for (j = 0; j <n; j+=1)  
	{	
		open(NewMaskNames[j]);
		makeRectangle(20, (smallSize/2)-40, smallSize-40, 30);		
		run("Measure");
		r = r + 1;
		meanL = getResult("Mean", r);

		makeRectangle(20, (smallSize/2)+10, smallSize-40, 30);		
		run("Measure");
		r = r + 1;
		meanR = getResult("Mean", r);

		if(meanL > meanR) 
		{
			run("Select None");	
			print("seme: ", n, "     media sinistra: ", meanL, "      media destra:", meanR, "       RUOTATA");
			run("Rotate... ", "angle=180 grid=1 interpolation=Bicubic fill");
			run("Save");
			run("Close");

			if(opzione1 == "dalle due originali a colori" && opzione2 == 1)
			{
				open(NewRGBNames[j]);
				run("Rotate... ", "angle=180 grid=1 interpolation=Bicubic fill");
				run("Save");
				run("Close");
			}
		}

		else 
		{
			print("seme: ", n, "     media sinistra: ", meanL, "      media destra:", meanR);
			run("Close");
		}
	}
}

setBatchMode("exit and display");

//// closes all images

