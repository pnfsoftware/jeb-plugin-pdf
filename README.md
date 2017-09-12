# PDF Document Analyzer Plugin for JEB

- Requires JEB 2.3+ Pro
- Demo: https://www.youtube.com/watch?v=PD40exjToDU
- Improvements/fixes/suggestions are encouraged!


# Usage

- Copy the JAR plugin (out/JebPdfPlugin-x.y.z.jar) to your JEB's coreplugins/ folder
- Copy the libs/ folder to your JEB's coreplugins/ folder
- Start JEB. This line should appear in the console log: `Plugin loaded: class com.pnf.plugin.pdf.PdfPlugin`
- Open a PDF file. The PDF plugin will detect it and analyze it as such

More information at https://www.pnfsoftware.com/jeb2/pdfplugin


# Source

## How to Build

Make sure to create an environment variable `JEB_HOME` pointing to your JEB folder.

Use the provided ANT build file to build the PDF JAR plugin:

`$ ant -Dversion=x.y.z`

where `x.y.z` is the plugin version, as defined in PdfPlugin.java.

## Third Party Software

- com.sun.pdfview by Pirion Systems Pty Ltd (license: GNU LGPL)
- org.apache.pdfbox.filter by ASF (license: Apache)
- JAI ImageIO (see JAR files)
- Levigo JBIG2 ImageIO (see JAR file)


# License

Copyright 2017 PNF Software, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
