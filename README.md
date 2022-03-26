# Seeds Analyzer
Here, you can find the SeedsAnalyzer plugin for ImageJ.

Authors, releasers and maintainers: Dr. Andrea Loddo, Prof. Cecilia Di Ruberto - University of Cagliari

NOTE: please cite one of the following pubblications if you have found this tool useful for your studies:

@article{Loddo2022,
  author    = {Andrea Loddo and
               Cecilia Di Ruberto and
               A. M. P. G. Vale and
               Mariano Ucchesu and
               J. M. Soares and
               Gianluigi Bacchetta},
  title     = {An effective and friendly tool for seed image analysis},
  journal   = {CoRR},
  volume    = {abs/2103.17213},
  year      = {2021},
  url       = {https://arxiv.org/abs/2103.17213},
  eprinttype = {arXiv},
  eprint    = {2103.17213},
  timestamp = {Wed, 07 Apr 2021 15:31:46 +0200},
  biburl    = {https://dblp.org/rec/journals/corr/abs-2103-17213.bib},
  bibsource = {dblp computer science bibliography, https://dblp.org}
}


@Article{Loddo2021JI,
AUTHOR = {Loddo, Andrea and Di Ruberto, Cecilia},
TITLE = {On the Efficacy of Handcrafted and Deep Features for Seed Image Classification},
JOURNAL = {Journal of Imaging},
VOLUME = {7},
YEAR = {2021},
NUMBER = {9},
ARTICLE-NUMBER = {171},
URL = {https://www.mdpi.com/2313-433X/7/9/171},
PubMedID = {34564097},
ISSN = {2313-433X},
DOI = {10.3390/jimaging7090171}
}

@article{Loddo2021CEA,
title = {A novel deep learning based approach for seed image classification and retrieval},
journal = {Computers and Electronics in Agriculture},
volume = {187},
pages = {106269},
year = {2021},
issn = {0168-1699},
doi = {https://doi.org/10.1016/j.compag.2021.106269},
url = {https://www.sciencedirect.com/science/article/pii/S0168169921002866},
author = {Andrea Loddo and Mauro Loddo and Cecilia {Di Ruberto}}
}


## Setup for writing ImageJ Plugins with Eclipse, IntelliJ or NetBeans
This repo contains a minimal setup for writing ImageJ (1) plugins with the [Eclipse](https://www.eclipse.org/), [IntelliJ](https://www.jetbrains.com/idea/) or [NetBeans](https://netbeans.org/), respectively.
The projects are set up with ``<project-root>/plugins/`` as the default output folder (for generated ``.class`` files).

This repository is part of the [**imagingbook**](http://imagingbook.com) support suite.
See [www.imagingbook.com](http://imagingbook.com) for additional resources.


## Setup
Clone this repository. It contains separate folders ``project-eclipse/``, ``project-intellij/`` and ``project-netbeans``, each containing a self-contained project for Eclipse, IntelliJ and NetBeans, respectively. 
Choose whichever you want to use and delete (or keep) the others.

### Eclipse:
Start the **Eclipse Java IDE** and use ``Open Projects from File System...`` to navigate to the ``project-eclipse/`` folder.
Editing and saving plugin source files should update the associated class files (in ``plugins/``) automatically.

### IntelliJ:
Start the **IntelliJ IDEA** and use ``Open`` in the *Welcome screen* to navigate to the ``project-intellij/`` folder.
Editing and saving plugin source files should update the associated class files (in ``plugins/``) automatically.

### NetBeans:
Start the **NetBeans IDE** and use ``File`` -> ``Open Project`` to navigate to the ``project-netbeans/`` folder.
After editing plugin source files, use ``Build Project`` to update the associated class files (in ``plugins/``).


## Starting ImageJ
The ImageJ runtime can be launched in various ways:
- **Windows**: Execute ``ImageJ.exe`` (by double-clicking on the file).
When ImageJ starts up, it may ask for the ``javaw.exe`` executable, typically located in ``C:\Program Files\java\jre1.8xxx\bin\``. In case of problems, simply delete the ``ImageJ.cfg`` file and start anew.
- **MacOS**: Launch ``ij.jar``.
- **Java**: Run the ``ij.ImageJ.main()`` method within Eclipse.

The entire ImageJ functionality is contained in the single archive ``ij.jar``. To **update** to the most recent version, simply select ``Help`` -> ``Update ImageJ...`` from the ImageJ main menu.

## Adding/editing your plugin code
Code for ImageJ plugins is contained in the ``<project-root>/src-plugins/`` directory. Plugins may be contained in Java packages (such as ``my_plugins`` in this example). **Note** that packages with plugins may only be **one level deep**, otherwise ImageJ will not find them! It is recommended to use at least one underscore (``_``) in a plugin name to make ImageJ automatically install the plugin into the ``Plugins`` menu at startup.

## Executing plugins
At startup, ImageJ automatically installs existing plugins (under the above conditions) into the ``Plugins`` menu. To execute, simply select the listed plugin from the menu.

When the plugin's source code is **edited** in the IDE, the associated ``.class`` file in ``plugins/`` is updated (by Eclipse/IntelliJ), but **not** automatically reloaded by the ImageJ runtime. To **exectute an edited plugin** in ImageJ, use ``Plugins`` -> ``Compile and Run...`` and select the associated ``.class`` file (no compiler is needed).

## Adding other libraries (jars)
This project uses **no dependency management** (such as *Maven*) to keep things simple. If any external libraries are required, just do the following:
- Copy the associated JAR file ``xxx.jar`` into ``jars/``.
- In your IDE, add the JAR file to Java build path.
- Restart ImageJ.


## Additional ImageJ resources

- [ImageJ Home](https://imagej.nih.gov/ij/index.html)
- [ImageJ Plugins](http://rsbweb.nih.gov/ij/plugins/index.html)
- [ImageJ API (JavaDoc)](http://rsbweb.nih.gov/ij/developer/api/index.html)
- [ImagingBook (book and source code)](http://imagingbook.com)


## LICENSE
MIT License

Copyright (c) 2021 Andrea Loddo, Cecilia Di Ruberto

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
