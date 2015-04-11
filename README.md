# XML Calabash (1.1.x)

This project contains the "1.1" version of XML Calabash.

[![Build Status](https://travis-ci.org/ndw/xmlcalabash1.svg?branch=saxon95)](https://travis-ci.org/ndw/xmlcalabash1.svg?branch=saxon95)

XML Calabash depends on Saxon and the sources vary a bit depending on
the version of Saxon that you want to use.

* The `saxon96` branch contains the sources for the Saxon 9.6 version of XML Calabash.
  This is also the default branch as of 9 March 2015.
* The `saxon95` branch contains the sources for the Saxon 9.5 version of XML Calabash.
* The `saxon94` branch contains the sources for the Saxon 9.4 version of XML Calabash.
I'm no longer attempting to maintain the Saxon 9.4 version.
* Saxon 9.3 is no longer supported. This is the "master" branch for historical reasons.
* The `docs` branch is moribund, see [the docs repo](http://github.com/ndw/xmlcalabash1-docs)
  instead.

You can download compiled versions from [the releases page](https://github.com/ndw/xmlcalabash1/releases).

## Modularity

As of version 1.1.0, XML Calabash is distributed in modules. This
repository contains the core processor. The jar files from additional
repositories are needed for some functionality:

| Module   | Functionality |
| -------- | ------------- |
| [deltaxml](http://github.com/ndw/xmlcalabash1-deltaxml) | XML comparison with [Delta XML](http://www.deltaxml.com/) |
| [ditaa](http://github.com/ndw/xmlcalabash1-ditaa) | ASCII diagrams with [ditaa](http://sourceforge.net/projects/ditaa/) |
| [mathml-to-svg](http://github.com/ndw/xmlcalabash1-mathml-to-svg) | MathML to SVG conversion with [JEuclid](http://sourceforge.net/projects/jeuclid/) |
| [metadata-extractor](http://github.com/ndw/xmlcalabash1-metadata-extractor) | Image [Metadata Extractor](https://drewnoakes.com/code/exif/) |
| [plantuml](http://github.com/ndw/xmlcalabash1-plantuml) | ASCII diagrams with [PlantUML](http://sourceforge.net/projects/plantuml/) |
| [print](http://github.com/ndw/xmlcalabash1-print) | Printing with [XSL FO](http://www.w3.org/standards/techs/xsl#w3c_all) or [CSS](http://www.w3.org/Style/CSS/) |
| [rdf](http://github.com/ndw/xmlcalabash1-rdf) | Read/write/query [RDF](http://www.w3.org/RDF/) |
| [sendmail](http://github.com/ndw/xmlcalabash1-sendmail) | Sending email |
| [xcc](http://github.com/ndw/xmlcalabash1-xcc) | [MarkLogic](http://www.marklogic.com/) XCC steps |
| [xmlunit](http://github.com/ndw/xmlcalabash1-xmlunit) | XML comparison with [XMLUnit](http://www.xmlunit.org/) |

Simply place the appropriate jar files in your classpath, there's no
additional configuration required. Note that you will also need commercial
libraries and licenses for some steps.

These steps (and XML Calabash itself) are also available
[through Maven](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.xmlcalabash%22).
