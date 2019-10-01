id = getImageID();
w = getWidth();
h = getHeight();

doWand(1, h/2);
run("Fit Spline");
run("EllipticFD ", "number=10 results reconstruction");

selectImage(id);
run("Select None");
run("Duplicate...", "title=rotated");
id2 = getImageID();
run("Rotate... ", "angle=15 grid=1 interpolation=None enlarge");
doWand(1, h/2);
run("Fit Spline");
run("EllipticFD ", "number=10 results reconstruction");

selectImage(id);
run("Duplicate...", "title=scaled");
id3 = getImageID();
run("Size...", "width="+(w/2)+" height=" + (h/2) +" constrain interpolation=None");
doWand(1, h/4);

run("EllipticFD ", "number=10 results reconstruction");
