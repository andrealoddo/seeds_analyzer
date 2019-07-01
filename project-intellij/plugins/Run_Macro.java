import ij.IJ;
import ij.plugin.PlugIn;

public class Run_Macro implements PlugIn {
    @Override
    public void run(final String arg) {
			/*
				final String macro =
            "run(\"Clown (14K)\");\n" +
            "run(\"Make Binary\");\n";
        IJ.runMacro(macro);
			*/
			String status = IJ.runMacroFile("_Test.ijm");
    }
}
