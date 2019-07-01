da_quali_immagini = newArray("dalle due originali a colori", "dall'immagine binarizzata");
Dialog.create(""); 
Dialog.addRadioButtonGroup("Menu", da_quali_immagini, 2, 2, da_quali_immagini[0]); 
Dialog.addNumber("Lato delle immagini ritagliate: ", 150);
Dialog.show();

opzione1 = Dialog.getRadioButton();
opzione2 = 0;
smallSize = Dialog.getNumber();

run("Close All");
run("Labels...", "color=black font=14 show draw bold");

NewMaskNames = newArray(500);
NewRGBNames = newArray(500);

//if(opzione1 == "dalle due originali a colori")
//{
	showMessage("dopo OK, selezionare nell'ordine l'immagine a sfondo chiaro e poi quella a sfondo scuro");
	open("");
	run("Set Scale...", "distance=60.5347 known=6 pixel=1 unit=mm");
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
	ImSubtracted = getImageID();	
	selectImage(Im1);
	run("Close");
	selectImage(Im2);
	run("Close");
	selectImage(ImSubtracted);	
	setAutoThreshold("Default");
	run("Convert to Mask");
	ImMask = getImageID();
	//run("Invert LUT");
	run("Make Binary");
  run("Fill Holes");
  run("Analyze Particles...", "size=3-Infinity show=Masks display clear");  
  string = fn1 + " & " + fn2 + extension;
  rename(string);
  selectImage(ImMask);
	run("Close"); 
	answ = getBoolean("salvare?");
	if (answ == 1) run("Save");
  //run("Close");
//}
