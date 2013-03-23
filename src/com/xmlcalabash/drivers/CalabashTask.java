/*
 * CalabashTask.java
 *
 * Copyright 2012 Mentea.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.drivers;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.JSONtoXML;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import org.xml.sax.InputSource;

/**
 * Ant task to run Calabash.
 *
 * <p>Owes a lot to Ant's &lt;xslt> task, but this task can't become
 * part of Ant because this task relies on Calabash, which is licensed
 * under LGPL.
 *
 * @author MenteaXML
 */
public class CalabashTask extends MatchingTask {
    /*
     * Ant can reuse a Task multiple times in the one build file, so
     * all of these fields need to be reset to their initial values
     * at the end of evaluate().
     *
     * Fields with null or false initial value are explicitly set so
     * it's obvious what to reset them to at the end of evaluate().
     *
     * Fields vaguely follow the sequence input, pipeline, output,
     * then the rest.
     */

    /** Input ports and the resources associated with each. */
    private HashMap<String,Union> inputResources = new HashMap<String,Union> ();

    /** Input ports and the mapper associated with each. */
    private Map<String,FileNameMapper> inputMappers = new HashMap<String,FileNameMapper> ();

    /** Where to find the source XML file, default is the project's
     * basedir */
    private File baseDir = null;

    /** Port of the pipeline input. As attribute. */
    private String inPort = null;

    /** URI of the input XML. As attribute. */
    private Resource inResource = null;

    /** Whether the build should fail if the nested resource
     * collection is empty. */
    private boolean failOnNoResources = true;

    /** URI of the pipeline to run. As attribute. */
    private String pipelineURI = null;

    /** Pipeline as a {@link org.apache.tools.ant.types.Resource} */
    private Resource pipelineResource = null;

    /** destination directory */
    private File destDir = null;

    /** Port of the pipeline output. As attribute. */
    private String outPort = null;

    /** Resource of the output XML. As attribute. */
    private  Resource outResource = null;

    /** Output ports and the resources associated with each. */
    private HashMap<String,Union> outputResources = new HashMap<String,Union> ();

    /** Output ports and the mapper associated with each. */
    private Map<String,FileNameMapper> outputMappers = new HashMap<String,FileNameMapper> ();

    /** extension of the files produced by pipeline processing */
    private String targetExtension = "-out.xml";

    /** whether target extension has been set from build file */
    private boolean isTargetExtensionSet = false;

    /** Whether to fail the build if an error occurs. */
    private boolean failOnError = true;

    /** Additional resource collections to process. */
    private Union resources = new Union();

    /** whether resources has been set from nested resource collection */
    private boolean isResourcesSet = false;

    /** Whether to use the implicit fileset. */
    private boolean useImplicitFileset = true;

    /** Whether to process all files in the included directories as
     * well. */
    private boolean performDirectoryScan = true;

    /** Mapper to use when a set of files gets processed. */
    private FileNameMapper mapper = null;

    /** force output of target files even if they already exist */
    private boolean force = false;

    /** System properties to set during transformation. */
    private CommandlineJava.SysProperties sysProperties =
        new CommandlineJava.SysProperties();

    /** Namespace prefix--URI bindings */
    private Hashtable<String, String> bindings =
	new Hashtable<String,String> ();

    /** The <option>s, ready for further processing */
    private Vector<Option> options = new Vector<Option> ();

    /** The processed options, ready for passing to Calabash */
    private Map<QName,RuntimeValue> optionsMap =
	new HashMap<QName,RuntimeValue> ();

    /** The <param>s, ready for further processing */
    private Vector<Parameter> parameters = new Vector<Parameter> ();

    /** The processed parameters, ready for passing to Calabash */
    private Map<String,Hashtable<QName,RuntimeValue>> parametersTable =
	new Hashtable<String,Hashtable<QName,RuntimeValue>> ();

    /** whether to enable debug output */
    private boolean debug = false;

    /** whether to enable general values.  Description is about
     * 'general values', but XProcConfiguration field is
     * 'extensionValues'. */
    private boolean extensionValues = false;

    /** whether the xpointer attribute on an XInclude element can be
     * used when parse="text" */
    private boolean allowXPointerOnText = false;

    /** whether to use XSLT 1.0 when ??? */
    private boolean useXslt10 = false;

    /** whether to automatically translate between JSON and XML */
    private boolean transparentJSON = false;

    /** flavor of JSON to use. As attribute. */
    String jsonFlavor = null;

    /* End of fields to reset at end of execute(). */

    /**
     * Set the base directory;
     * optional, default is the project's basedir.
     *
     * @param dir the base directory
     **/
    public void setBasedir(File dir) {
        baseDir = dir;
    }

    /**
     * Set the input port name.
     * optional, default is the first unmatched pipeline input port.
     *
     * @param port the port name
     **/
    public void setinPort(String port) {
        inPort = port;
    }

    /**
     * Set the input resource.
     * optional, implicit and/or explicit filest will be used if this
     * and outResource are not set.
     *
     * @param inResource the {@link org.apache.tools.ant.types.Resource}
     **/
    public void setIn(Resource inResource) {
        this.inResource = inResource;
    }

    /**
     * Work with an instance of an <input> element already configured
     * by Ant.
     * @param i the configured input Port
     */
    public void addConfiguredInput(Port i) {
	if (!i.shouldUse()) {
	    log("Skipping input '" + i.getPort() + "' as it is configured to be unused.", Project.MSG_DEBUG);
	    return;
	}

	String port = i.getPort();
	FileNameMapper inputMapper = i.getMapper();
	Union resources = i.getResources();

	if (port == null) {
            port = inPort;
        }

	if (inputMapper != null && resources.size() != 0) {
	    handleError("Both mapper and fileset on input port: " + port);
	    return;
	}

	if (inputMapper != null) {
	    if (port.equals(inPort)) {
		handleError("Cannot use mapper on main input port: " + port);
		return;
	    }
	    if (inputResources.containsKey(port)) {
		handleError("Mapper used on input port that already has resources: " + port);
		return;
	    }

	    if (inputMappers.containsKey(port)) {
		handleError("Mapper used on input port that already has a mapper: " + port);
		return;
	    }

	    inputMappers.put(port, inputMapper);
	} else {
	    if (inputMappers.containsKey(port)) {
		handleError("Resources used on input port that already has a mapper: " + port);
		return;
	    }

	    if (!inputResources.containsKey(port)) {
		inputResources.put(port, new Union ());
	    }

	    inputResources.get(port).add(resources);
	}
    }

    /**
     * Whether the build should fail if the nested resource collection
     * is empty.
     */
    public void setFailOnNoResources(boolean b) {
        failOnNoResources = b;
    }

    /**
     * Set the pipeline.
     * optional, nested &lt;pipeline> will be used if not set.
     *
     * @param uri pipeline location
     **/
    public void setPipeline(String uri) {
        pipelineURI = uri;
    }

    /**
     * API method to set the pipeline Resource.
     * @param pipelineResource Resource to set as the pipeline.
     */
    public void setPipelineResource(Resource pipelineResource) {
 	this.pipelineResource = pipelineResource;
    }

    /**
     * Add a nested &lt;pipeline&gt; element.
     * @param rc the configured Resources object represented as
     * &lt;pipeline&gt;.
     */
    public void addConfiguredPipeline(Resources rc) {
 	if (rc.size() != 1) {
	    throw new BuildException("The pipeline element must be specified with exactly one"
			+ " nested resource.");
 	} else {
	    setPipelineResource((Resource) rc.iterator().next());
 	}
    }

    /**
     * Set the destination directory into which the XSL result
     * files should be copied to;
     * required, unless <tt>in</tt> and <tt>out</tt> are
     * specified.
     * @param dir the name of the destination directory
     **/
    public void setDestdir(File dir) {
        destDir = dir;
    }

    /**
     * Set the output port name.
     * optional, default is the first unmatched pipeline output port.
     *
     * @param port the port name
     **/
    public void setOutPort(String port) {
        outPort = port;
    }

    /**
     * Set the output resource.
     * optional, implicit and/or explicit filest will be used if this
     * and inResource are not set.
     *
     * @param outResource the {@link org.apache.tools.ant.types.Resource}
     **/
    public void setOut(Resource outResource) {
        this.outResource = outResource;
    }

    /**
     * Work with an instance of an <output> element already configured
     * by Ant.
     * @param o the configured Port
     */
    public void addConfiguredOutput(Port o) {
	if (!o.shouldUse()) {
	    log("Skipping output '" + o.getPort() + "' as it is configured to be unused.", Project.MSG_DEBUG);
	    return;
	}

	String port = o.getPort();
 	FileNameMapper outputMapper = o.getMapper();
	Union resources = o.getResources();

	if (port == null) {
            port = outPort;
        }

	if (outputMapper != null && resources.size() != 0) {
	    handleError("Both mapper and fileset on input port: " + port);
	    return;
	}

	if (outputMapper != null) {
	    if (outputResources.containsKey(port)) {
		handleError("Mapper used on output port that already has resources: " + port);
		return;
	    }

	    if (outputMappers.containsKey(port)) {
		handleError("Mapper used on output port that already has a mapper: " + port);
		return;
	    }

	    outputMappers.put(port, outputMapper);
	} else {
	    if (outputMappers.containsKey(port)) {
		handleError("Resources used on output port that already has a mapper: " + port);
		return;
	    }

	    if (!outputResources.containsKey(port)) {
		outputResources.put(port, new Union ());
	    }
	    outputResources.get(port).add(o.getResources());
	}
    }

    /**
     * Set the desired file extension to be used for the target;
     * optional, default is '-out.xml'.
     * @param name the extension to use
     **/
    public void setExtension(String name) {
        targetExtension = name;
	isTargetExtensionSet = true;
    }

    /**
     * Whether any errors should make the build fail.
     */
    public void setFailOnError(boolean b) {
        failOnError = b;
    }

    /**
     * Adds a collection of resources to process in addition to the
     * given file or the implicit fileset.
     *
     * @param rc the collection of resources to style
     */
    public void add(ResourceCollection rc) {
        resources.add(rc);
	isResourcesSet = true;
    }

    /**
     * Whether to use the implicit fileset.
     *
     * <p>Set this to false if you want explicit control with nested
     * resource collections.</p>
     * @param useimplicitfileset set to true if you want to use
     * implicit fileset
     */
    public void setUseImplicitFileset(boolean useimplicitfileset) {
        useImplicitFileset = useimplicitfileset;
    }

    /**
     * Whether to process all files in the included directories as
     * well; optional, default is true.
     *
     * @param b true if files in included directories are processed.
     */
    public void setScanIncludedDirectories(boolean b) {
        performDirectoryScan = b;
    }

    /**
     * Defines the mapper to map source to destination files.
     * @param mapper the mapper to use
     * @exception BuildException if more than one mapper is defined
     */
    public void addMapper(Mapper mapper) throws BuildException {
	add(mapper.getImplementation());
    }

    /**
     * Adds a nested filenamemapper.
     * @param fileNameMapper the mapper to add
     * @exception BuildException if more than one mapper is defined
     */
    public void add(FileNameMapper fileNameMapper) throws BuildException {
        if (mapper != null) {
            handleError("Cannot define more than one mapper");
	    return;
        }

	mapper = fileNameMapper;
    }

    /**
     * Set whether to check dependencies, or always generate;
     * optional, default is false.
     *
     * @param force true if always generate.
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * A system property to set during transformation.
     */
    public void addSysproperty(Environment.Variable sysp) {
        sysProperties.addVariable(sysp);
    }

    /**
     * A set of system properties to set during transformation.
     */
    public void addSyspropertyset(PropertySet sysp) {
        sysProperties.addSyspropertyset(sysp);
    }

    /**
     * Work with an instance of a <binding> element already configured
     * by Ant.
     * @param n the configured Namespace
     */
    public void addConfiguredNamespace(Namespace n) {
	// prefix and/or uri may have been omitted in build file
	// without Ant complaining
	if (n.getPrefix() == null) {
	   handleError("<namespace> prefix cannot be null");
	   return;
	}

	if (n.getURI() == null) {
	   handleError("<namespace> URI cannot be null");
	   return;
	}

	if (bindings.containsKey(n.getPrefix())) {
	    handleError("Duplicated <namespace> prefix: " + n.getPrefix());
	   return;
	}

        bindings.put(n.getPrefix(), n.getURI());
    }

    /**
     * Work with an instance of a <option> element already configured
     * by Ant.
     * @param o the configured Option
     */
    public void addConfiguredOption(Option o) {
	if (!o.shouldUse()) {
	    log("Skipping option '" + o.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
	    return;
	}

	options.add(o);
    }

    /**
     * Work with an instance of a <parameter> element already
     * configured by Ant.
     * @param p the configured Parameter
     */
    public void addConfiguredParameter(Parameter p) {
	if (!p.shouldUse()) {
	    log("Skipping parameter '" + p.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
	    return;
	}

	parameters.add(p);
    }

    /**
     * Set whether to enable debugging output;
     * optional, default is false.
     *
     * @param debug true if enable debug output
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Set whether to enable general values;
     * optional, default is false.
     *
     * @see <a href="http://xmlcalabash.com/docs/reference/extensions.html#ext.general-values">General values extension</a>
     * @param generalValues true if enable general values
     */
    public void setGeneralValues(boolean generalValues) {
        this.extensionValues = generalValues;
    }

    /**
     * Set whether xpointer attribute on an XInclude element can be
     * used when parse="text";
     * optional, default is false.
     *
     * @param xpointerOnText true if enable XPointer on text
     */
    public void setXPointerOnText(boolean xpointerOnText) {
        this.allowXPointerOnText = xpointerOnText;
    }

    /**
     * Set whether to enable use of XSLT 1.0;
     * optional, default is false.
     *
     * @param useXslt10 true if enable XSLT 1.0 support
     */
    public void setUseXslt10(boolean useXslt10) {
        this.useXslt10 = useXslt10;
    }

    /**
     * Set whether to automatically translate between JSON and XML;
     * optional, default is false.
     *
     * @param transparentJSON true if enable translation
     */
    public void setTransparentJSON(boolean transparentJSON) {
        this.transparentJSON = transparentJSON;
    }

    /**
     * Set whether to automatically translate between JSON and XML;
     * optional, default is false.
     *
     * @param jsonFlavor the flavor of JSON/XML transformation to use
     */
    public void setJSONFlavor(String jsonFlavor) {
        this.jsonFlavor = jsonFlavor;
    }


    /** Do the work. */
    public void execute() {
	Resource usePipelineResource = null;
	if (pipelineURI != null) {
	    // If we enter here, it means that the pipeline is supplied
	    // via 'pipeline' attribute
	    File pipelineFile = getProject().resolveFile(pipelineURI);
	    FileResource fr = new FileResource();
	    fr.setProject(getProject());
	    fr.setFile(pipelineFile);
	    usePipelineResource = fr;
	} else {
	    usePipelineResource = pipelineResource;
	}

	if (!usePipelineResource.isExists()) {
	    handleError("pipeline '" + usePipelineResource.getName() + "' does not exist");
	    return;
	}

	if (inResource != null && !inResource.isExists()) {
	    handleError("input file '" + inResource.getName() + "' does not exist");
	    return;
	}

	if (inResource != null && resources.size() != 0) {
	    handleError("'in' and explicit filesets cannot be used together.");
	    return;
	}

	if ((inResource != null || outResource != null) && useImplicitFileset) {
	    log("'in' and/or 'out' cannot be used with implicit fileset: ignoring implicit fileset.", Project.MSG_VERBOSE);
	    useImplicitFileset = false;
	}

	if (outResource != null && mapper != null) {
	    handleError("Nested <mapper> for default output and 'out' cannot be used together.");
	    return;
	}

	if ((outputMappers.containsKey(outPort) ||
	     outputResources.containsKey(outPort)) &&
	    mapper != null) {
	    handleError("Nested <mapper> and port for default output cannot be used together.");
	    return;
	}

	if (outResource != null && isTargetExtensionSet) {
	    handleError("'extension' and 'out' cannot be used together.");
	    return;
	}

	if (isTargetExtensionSet && mapper != null) {
	    handleError("'extension' and nested <mapper> cannot be used together.");
	    return;
	}

	File savedBaseDir = baseDir;

        try {
	    if (baseDir == null) {
		baseDir = getProject().getBaseDir();
	    }

	    if (sysProperties.size() > 0) {
		sysProperties.setSystem();
	    }

	    // When prefix "p" not set, default to the XProc namespace
	    if (!bindings.containsKey("p")) {
		bindings.put("p", XProcConstants.NS_XPROC);
	    }

	    // Can only really work with options now bindings all present
	    for (Option o : options) {
		QName qname = makeQName(o.getName());
		String value = o.getValue();

		if (optionsMap.containsKey(qname)) {
		    handleError("Duplicated option QName: " + qname.getClarkName());
		    continue;
		}

		optionsMap.put(qname, new RuntimeValue(value));
	    }

	    // Can only really work with parameters now bindings all present
	    for (Parameter p : parameters) {
		String port = p.getPort();
		QName qname = makeQName(p.getName());
		String value = p.getValue();

		Hashtable<QName,RuntimeValue> portParams;
		if (!parametersTable.containsKey(port)) {
		    portParams = new Hashtable<QName,RuntimeValue> ();
		} else {
		    portParams = parametersTable.get(port);
		    if (portParams.containsKey(qname)) {
			handleError("Duplicated parameter QName: " + qname.getClarkName());
			continue;
		    }
		}

		portParams.put(qname, new RuntimeValue(value));
		parametersTable.put(port, portParams);
	    }

	    //-- make sure destination directory exists...
	    checkDest();

	    // if we have an in file or out file then process them
	    if (inResource != null) {
		Port i = new Port();
		i.setPort(inPort);
		i.add(inResource);
		addConfiguredInput(i);
	    }
	    // Since processing specified inputs, use outResource too
	    if (outResource != null) {
		Port o = new Port();
		o.setPort(outPort);
		o.add(outResource);
		addConfiguredOutput(o);
	    }

	    if (outputResources.containsKey(outPort)
		&& (isTargetExtensionSet || mapper != null)) {
		handleError("Either 'out' or <output> corresponding to default output port and either 'extension' and nested <mapper> for naming output cannot be used together.");
		return;
	    }

	    // Set up Calabash config.
	    XProcConfiguration config =
		new XProcConfiguration("he", false);
            config.extensionValues |= extensionValues;
            config.xpointerOnText |= allowXPointerOnText;
            config.transparentJSON |= transparentJSON;
            if (jsonFlavor != null) {
		if (!JSONtoXML.knownFlavor(jsonFlavor)) {
		    handleError("Can't parse JSON flavor '" + jsonFlavor + "' or unrecognized format: " + jsonFlavor);
		    return;
                    }
                config.jsonFlavor = jsonFlavor;
            }
            config.useXslt10 |= useXslt10;

            config.debug = debug;

	    // If neither implicit or explicit fileset, assume user
	    // knows what they're doing even though there may be no
	    // input or output ports.
	    if (!useImplicitFileset && resources.size() == 0) {
		HashMap<String, Union> useInputResources =
		    new HashMap<String, Union> ();
		// Any fixed resources on any input ports.
		useInputResources.putAll(inputResources);
		// Any mapped resources on non-inPort input ports.
		for (String port : inputMappers.keySet()) {
		    FileNameMapper inputMapper = inputMappers.get(port);
		    for (Resource resource : inputResources.get(inPort).listResources()) {
			String[] inputFileNames =
			    inputMapper.mapFileName(resource.getName());
			// Mapper may produce zero or more filenames,
			// which may or may not be what was wanted but
			// only the user will know that.
			Union mappedResources = new Union();
			for (String fileName : inputFileNames) {
			    FileResource mappedResource =
				new FileResource(baseDir, fileName);
			    if (mappedResource.isExists()) {
				mappedResources.add(mappedResource);
			    } else {
				log("Skipping non-exstent mapped resource: " + mappedResource.toString(), Project.MSG_DEBUG);
			    }
			}
			useInputResources.put(port, mappedResources);
		    }
		}
		HashMap<String, Union> useOutputResources =
		    new HashMap<String, Union> ();
		useOutputResources.putAll(outputResources);

		if (outputMappers.size() != 0) {
		    for (Resource resource : inputResources.get(inPort).listResources()) {
			// Aadd any mapped resources on output ports.
			for (String port : outputMappers.keySet()) {
			    FileNameMapper outputMapper = outputMappers.get(port);

			    String[] outputFileNames =
				outputMapper.mapFileName(resource.getName());
			    // Mapper may produce zero or more filenames,
			    // which may or may not be what was wanted but
			    // only the user will know that.
			    if (outputFileNames != null) {
				Union outputResources = new Union();
				for (String fileName : outputFileNames) {
				    outputResources.add(new FileResource(baseDir, fileName));
				}
				useOutputResources.put(port, outputResources);
			    }
			}
		    }
		}
		process(config,
			useInputResources,
			useOutputResources,
			usePipelineResource,
			optionsMap,
			parametersTable,
			force);
		//return;
	    } else { // Using implicit and/or explicit filesets
		if (useImplicitFileset) {
		    DirectoryScanner scanner = getDirectoryScanner(baseDir);
		    log("Pipelining into " + destDir, Project.MSG_INFO);

		    // Process all the files marked for styling
		    String[] includedFiles = scanner.getIncludedFiles();
		    for (int i = 0; i < includedFiles.length; ++i) {
			resources.add(new FileResource(baseDir, includedFiles[i]));
		    }
		    if (performDirectoryScan) {
			// Process all the directories marked for styling
			String[] includedDirs = scanner.getIncludedDirectories();
			for (int j = 0; j < includedDirs.length; ++j) {
			    includedFiles = new File(baseDir, includedDirs[j]).list();
			    for (int i = 0; i < includedFiles.length; ++i) {
				resources.add(new FileResource(baseDir, includedDirs[j] + File.separator + includedFiles[i]));
			    }
			}
		    }
		} else { // only resource collections, there better be some
		    if (resources.size() == 0) {
			if (failOnNoResources) {
			    handleError("no resources specified");
			}
			return;
		    }
		}

    		FileNameMapper useMapper = null;
		if (!outputResources.containsKey(outPort)) {
		    if (mapper != null) {
			useMapper = mapper;
		    } else {
			useMapper = new ExtensionMapper();
		    }
                }

		// Process implicit and/or explicit resources one at a
		// time.
                for (Resource resource : resources.listResources()) {
                    log("Resource: " + resource.getName(), Project.MSG_DEBUG);
                    HashMap<String, Union> useInputResources =
			new HashMap<String, Union> ();

		    // Any fixed resources on other input ports.
		    useInputResources.putAll(inputResources);
		    // The resource.
                    useInputResources.put(inPort, new Union(resource));
		    // Any mapped resources on other input ports.
		    for (String port : inputMappers.keySet()) {
			FileNameMapper inputMapper = inputMappers.get(port);

                        String[] inputFileNames =
			    inputMapper.mapFileName(resource.getName());
			// Mapper may produce zero or more filenames,
			// which may or may not be what was wanted but
			// only the user will know that.
			if (inputFileNames != null) {
			    Union mappedResources = new Union();
			    for (String fileName : inputFileNames) {
				mappedResources.add(new FileResource(baseDir, fileName));
			    }
			    useInputResources.put(port, mappedResources);
			}
		    }

                    HashMap<String, Union> useOutputResources =
			new HashMap<String, Union> ();
		    useOutputResources.putAll(outputResources);
		    // FIXME: Why is it necessary to check for null?
                    if (useMapper != null) {
                        String[] outFileName = useMapper.mapFileName(resource.getName());
			// Require exactly one output for each mapped
			// input.
                        if (outFileName == null || outFileName.length == 0) {
                            log("Skipping '" + resource.getName() + "' as it cannot be mapped to output.", Project.MSG_VERBOSE);
                            continue;
                        } else if (outFileName == null || outFileName.length > 1) {
                            log("Skipping " + resource.getName() + " as its mapping is ambiguous.", Project.MSG_VERBOSE);
                            continue;
                        }
                        useOutputResources.put(outPort, new Union(new FileResource(destDir, outFileName[0])));
                    }

		    // Any mapped resources on other output ports.
		    for (String port : outputMappers.keySet()) {
			FileNameMapper outputMapper = outputMappers.get(port);

                        String[] outputFileNames =
			    outputMapper.mapFileName(resource.getName());
			// Mapper may produce zero or more filenames,
			// which may or may not be what was wanted but
			// only the user will know that.
			if (outputFileNames != null) {
			    Union outputResources = new Union();
			    for (String fileName : outputFileNames) {
				outputResources.add(new FileResource(baseDir, fileName));
			    }
			    useOutputResources.put(port, outputResources);
			}
		    }
                    process(config, useInputResources, useOutputResources, usePipelineResource, optionsMap, parametersTable, force);
                }
	    }
	} finally {
	    // Same instance is reused when Ant runs this task
	    // again, so reset everything.
	    inputResources.clear();
	    inputMappers.clear();
	    outputResources.clear();
            baseDir = null;
	    inPort = null;
	    inResource = null;
	    failOnNoResources = true;
	    pipelineURI = null;
	    pipelineResource = null;
	    destDir = null;
	    outPort = null;
	    outResource = null;
	    targetExtension = "-out.xml";
	    isTargetExtensionSet = false;
	    failOnError = true;
	    resources = new Union();
	    isResourcesSet = false;
	    useImplicitFileset = true;
	    performDirectoryScan = true;
	    mapper = null;
	    force = false;
            if (sysProperties.size() > 0) {
                sysProperties.restoreSystem();
		// No way to clear CommandlineJava.SysProperties
		sysProperties = new CommandlineJava.SysProperties();
            }
	    bindings.clear();
	    options.clear();
	    optionsMap.clear();
	    parameters.clear();
	    parametersTable.clear();
	    debug = false;
	    extensionValues = false;
	    allowXPointerOnText = false;
	    useXslt10 = false;
	    transparentJSON = false;
	    jsonFlavor = null;
	}
    }

    /**
     * Process the input file to the output file with the given pipeline.
     *
     * @param inputResources the map of input ports to resources
     * @param outputResources the map of output ports to resources
     * @param pipelineResource the pipeline to use.
     * @exception BuildException if the processing fails.
     */
    private void process(XProcConfiguration config,
			 Map<String,Union> inputResources,
			 Map<String,Union> outputResources,
			 Resource pipelineResource,
			 Map<QName,RuntimeValue> optionsMap,
			 Map<String,Hashtable<QName,RuntimeValue>> parametersMap,
			 boolean force) throws BuildException {

	long pipelineLastModified = pipelineResource.getLastModified();
	Collection<Long> inputsLastModified = new Vector<Long> ();
	for (String port : inputResources.keySet()) {
	    for (Resource resource : inputResources.get(port).listResources()) {
		inputsLastModified.add(resource.getLastModified());
	    }
	}
	long newestInputLastModified =
	    inputsLastModified.isEmpty() ? 0 : Collections.max(inputsLastModified);

	Collection<Long> outputsLastModified = new Vector<Long> ();
	for (String port : outputResources.keySet()) {
	    for (Resource resource : outputResources.get(port).listResources()) {
		outputsLastModified.add(resource.getLastModified());
	    }
	}
	long oldestOutputLastModified =
	    outputsLastModified.isEmpty() ? 0 : Collections.min(outputsLastModified);

	log("Newest input time: " + newestInputLastModified, Project.MSG_DEBUG);
	log("Oldest output time: " + oldestOutputLastModified, Project.MSG_DEBUG);
	log("Pipeline file " + pipelineResource + " time: " + pipelineLastModified, Project.MSG_DEBUG);

	if (!force) {
	    if (newestInputLastModified < oldestOutputLastModified &&
		pipelineLastModified < oldestOutputLastModified) {
		log("Skipping because all outputs are newer than inputs and newer than pipeline", Project.MSG_DEBUG);
		return;
	    }
	}

	//log("Processing " + in + " to " + out, Project.MSG_INFO);
        XProcRuntime runtime = new XProcRuntime(config);

	XPipeline pipeline = null;
	try {
	    pipeline =
		runtime.load(pipelineResource.toString());

	    // The unnamed input is matched to one unmatched input
	    for (String port : pipeline.getInputs()) {
                if (pipeline.getInput(port).getParameters()) {
                    continue;
                }
		if (!inputResources.containsKey(port)) {
                    if (inputResources.containsKey(null)) {
                        inputResources.put(port, inputResources.remove(null));
                        log("Binding unnamed input port to '" + port + "'.", Project.MSG_INFO);
                    } else {
                        log("You didn't specify any binding for the input port '" + port + "'.", Project.MSG_WARN);
                        continue;
                    }
                }
            }

	    for (String port : pipeline.getInputs()) {
                if (inputResources.containsKey(port)) {
		    for (Resource resource : inputResources.get(port).listResources()) {
			log(resource.getName() + "::" + resource.isExists(), Project.MSG_INFO);
			if (!resource.isExists()) {
			    log("Skipping non-existent input: " + resource, Project.MSG_DEBUG);
			}

			InputStream is = resource.getInputStream();
			XdmNode doc = runtime.parse(new InputSource(resource.getInputStream()));
			pipeline.writeTo(port, doc);
		    }
                }
            }

	    // Pass any options to the pipeline
	    for (QName name : optionsMap.keySet()) {
		pipeline.passOption(name, optionsMap.get(name));
	    }

	    // Pass any parameters to the pipeline
	    for (String port : parametersMap.keySet()) {
		Hashtable<QName,RuntimeValue> useTable = parametersMap.get(port);
		if ("*".equals(port)) { // For primary parameter input port
		    for (QName name : useTable.keySet()) {
			pipeline.setParameter(name, useTable.get(name));
		    }
		} else { // For specified parameter input port
		    for (QName name : useTable.keySet()) {
			pipeline.setParameter(port, name, useTable.get(name));
		    }
		}
	    }

	    pipeline.run();

            // The unamed output is matched to one unmatched output
            for (String port : pipeline.getOutputs()) {
		if (!outputResources.containsKey(port)) {
		    if (outputResources.containsKey(null)) {
			outputResources.put(port, outputResources.remove(null));
			log("Binding unnamed output port to '" + port + "'.", Project.MSG_INFO);
		    } else {
			log("You didn't specify any binding for the output port '" + port + "': its output will be discarded.", Project.MSG_WARN);
		    }
		}
            }

            for (String port : pipeline.getOutputs()) {
                String uri = null;

                if (outputResources.containsKey(port)) {
                    Union resources = outputResources.get(port);
		    if (resources.size() != 1) {
			handleError("The '" + port + "' output port must be specified with exactly one"
                        + " nested resource.");
		    }
		    uri = ((Resource) resources.iterator().next()).toString();
                    log("Writing port '" + port + "' to '" + uri + "'.", Project.MSG_INFO);
               }

                if (uri == null) {
                    // You didn't bind it, and it isn't going to stdout, so it's going into the bit bucket.
                    continue;
                }

                Serialization serial = pipeline.getSerialization(port);

                if (serial == null) {
                    // Use the configuration options
                    // FIXME: should each of these be considered separately?
                    // FIXME: should there be command-line options to override these settings?
                    serial = new Serialization(runtime, pipeline.getNode()); // The node's a hack
                    for (String name : config.serializationOptions.keySet()) {
                        String value = config.serializationOptions.get(name);

                        if ("byte-order-mark".equals(name)) serial.setByteOrderMark("true".equals(value));
                        if ("escape-uri-attributes".equals(name)) serial.setEscapeURIAttributes("true".equals(value));
                        if ("include-content-type".equals(name)) serial.setIncludeContentType("true".equals(value));
                        if ("indent".equals(name)) serial.setIndent("true".equals(value));
                        if ("omit-xml-declaration".equals(name)) serial.setOmitXMLDeclaration("true".equals(value));
                        if ("undeclare-prefixes".equals(name)) serial.setUndeclarePrefixes("true".equals(value));
                        if ("method".equals(name)) serial.setMethod(new QName("", value));

                        // FIXME: if ("cdata-section-elements".equals(name)) serial.setCdataSectionElements();
                        if ("doctype-public".equals(name)) serial.setDoctypePublic(value);
                        if ("doctype-system".equals(name)) serial.setDoctypeSystem(value);
                        if ("encoding".equals(name)) serial.setEncoding(value);
                        if ("media-type".equals(name)) serial.setMediaType(value);
                        if ("normalization-form".equals(name)) serial.setNormalizationForm(value);
                        if ("standalone".equals(name)) serial.setStandalone(value);
                        if ("version".equals(name)) serial.setVersion(value);
                    }
                }

                // ndw wonders if there's a better way...
                WritableDocument wd = null;
                if (uri != null) {
                    URI furi = new URI(uri);
                    String filename = furi.getPath();
                    FileOutputStream outfile = new FileOutputStream(filename);
                    wd = new WritableDocument(runtime,filename,serial,outfile);
                } else {
                    wd = new WritableDocument(runtime,uri,serial);
                }

                ReadablePipe rpipe = pipeline.readFrom(port);
                while (rpipe.moreDocuments()) {
                    wd.write(rpipe.read());
                }

                if (uri!=null) {
		    wd.close();
                }
            }
	} catch (Exception err) {
	   handleError("Pipeline failed: " + err.toString());
	} finally {
	    pipeline = null;
	    runtime = null;
	    config = null;
	}
    }

    /**
     * Mapper implementation that replaces file's extension.
     *
     * <p>If the file has an extension, chop it off.  Append whatever
     * the user has specified as extension or "-out.xml".</p>
     */
    private class ExtensionMapper implements FileNameMapper {
        public void setFrom(String from) {
        }
        public void setTo(String to) {
        }
        public String[] mapFileName(String xmlFile) {
            int dotPos = xmlFile.lastIndexOf('.');
            if (dotPos > 0) {
                xmlFile = xmlFile.substring(0, dotPos);
            }
            return new String[] {xmlFile + targetExtension};
        }
    }

    /**
     * Makes a QName using the bindings defined on the task.
     * @param name possibly-prefixed name
     * @returns QName
     */
    private QName makeQName(String name) {
	String uri = null;
	QName qname;

	if (name.indexOf("{") == 0) {
	    qname = QName.fromClarkName(name);
	} else {
	    int cpos = name.indexOf(":");
	    if (cpos > 0) {
		String prefix = name.substring(0, cpos);
		if (!bindings.containsKey(prefix)) {
		    handleError("Unbound prefix \"" + prefix + "\" in: " + name);
		}
		uri = bindings.get(prefix);
		qname = new QName(prefix, uri, name.substring(cpos+1));
	    } else {
		qname = new QName("", name);
	    }
	}

	return qname;
    }

    /**
     * Throws a BuildException if the destination directory hasn't
     * been specified.
     */
    private void checkDest() {
        if (destDir == null) {
	    destDir = baseDir;
            log("destdir defaulting to basedir", Project.MSG_DEBUG);
        }
    }

    /**
     * Throws an exception with the given message if failOnError is
     * true, otherwise logs the message using the WARN level.
     */
    protected void handleError(String msg) {
        if (failOnError) {
            throw new BuildException(msg, getLocation());
        }
        log(msg, Project.MSG_WARN);
    }


    /**
     * Throws an exception with the given nested exception if
     * failOnError is true, otherwise logs the message using the WARN
     * level.
     */
    protected void handleError(Throwable ex) {
        if (failOnError) {
            throw new BuildException(ex);
        } else {
            log("Caught an exception: " + ex, Project.MSG_WARN);
        }
    }

    /**
     * The Port inner class used to represent input and output ports.
     */
    public static class Port {
        /** The input port */
        private String port = null;

        /** The input's resources */
        private Union resources = new Union();

	/** Mapper to inPort files. */
	private FileNameMapper mapper = null;

        private Object ifCond;
        private Object unlessCond;
        private Project project;

        /**
         * Set the current project
         *
         * @param project the current project
         */
        public void setProject(Project project) {
            this.project = project;
        }

        /**
         * Set the input port.
         *
         * @param port the name of the port.
         */
        public void setPort(String port) {
            this.port = port;
        }

	/**
	 * Adds a collection of resources to process in addition to the
	 * given file or the implicit fileset.
	 *
	 * @param rc the collection of resources to use
	 */
	public void add(ResourceCollection rc) {
	    resources.add(rc);
	}

	/**
	 * Defines the mapper to map source to destination files.
	 * @param mapper the mapper to use
	 * @exception BuildException if more than one mapper is defined
	 */
	public void addMapper(Mapper mapper) throws BuildException {
	    add(mapper.getImplementation());
	}

	/**
	 * Adds a nested filenamemapper.
	 * @param fileNameMapper the mapper to add
	 * @exception BuildException if more than one mapper is defined
	 */
	public void add(FileNameMapper fileNameMapper) throws BuildException {
	    if (mapper != null) {
		throw new BuildException("Cannot define more than one mapper");
	    }

	    mapper = fileNameMapper;
	}

        /**
         * Get the port name
         *
         * @return the port name
         */
        public String getPort() {
            return port;
        }

        /**
         * Get the port's resources
         *
         * @return the port's resources
         */
        public Union getResources() {
            return resources;
        }

        /**
         * Get the port's Mapper element, if any
         *
         * @return the ports's mapper
         */
        public FileNameMapper getMapper() {
            return mapper;
        }

        /**
         * Set whether this input should be used.  It will be used if
         * the expression evalutes to true or the name of a property
         * which has been set, otherwise it won't.
         * @param ifCond evaluated expression
         */
        public void setIf(Object ifCond) {
            this.ifCond = ifCond;
        }

        /**
         * Set whether this input should be used.  It will be used if
         * the expression evalutes to true or the name of a property
         * which has been set, otherwise it won't.
         * @param ifProperty evaluated expression
         */
        public void setIf(String ifProperty) {
            setIf((Object) ifProperty);
        }

        /**
         * Set whether this input should NOT be used. It will not be
         * used if the expression evaluates to true or the name of a
         * property which has been set, otherwise it will be used.
         * @param unlessCond evaluated expression
         */
        public void setUnless(Object unlessCond) {
            this.unlessCond = unlessCond;
        }

        /**
         * Set whether this input should NOT be used. It will not be
         * used if the expression evaluates to true or the name of a
         * property which has been set, otherwise it will be used.
         * @param unlessProperty evaluated expression
         */
        public void setUnless(String unlessProperty) {
            setUnless((Object) unlessProperty);
        }

        /**
         * Ensures that the input passes the conditions placed
         * on it with <code>if</code> and <code>unless</code> properties.
         * @return true if the task passes the "if" and "unless" parameters
         */
        public boolean shouldUse() {
            PropertyHelper ph = PropertyHelper.getPropertyHelper(project);
            return ph.testIfCondition(ifCond)
                && ph.testUnlessCondition(unlessCond);
        }
    } // Port

    /**
     * The Namespace inner class represents a namespace binding.
     */
    public static class Namespace {
        /** The prefix */
        private String prefix = null;
        /** The URI */
        private String uri = null;

        /**
         * Set the prefix.
         * @param prefix prefix to which to bind the namespace
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

	/**
         * Get the prefix
         *
         * @return the namespace prefix
         */
        public String getPrefix() {
            return prefix;
        }

       /**
         * Set the namespace URI.
         * @param uri the namespace URI
         */
        public void setURI(String uri) {
            this.uri = uri;
        }

	/**
         * Get the namespace URI
         *
         * @return the namespace URI
         */
        public String getURI() {
            return uri;
        }
    } // Namespace

    /**
     * The Option inner class represents a pipeline option.
     */
    public static class Option {
        /** The name */
        private String name = null;
        /** The parameter value */
        private String value = null;

        private Object ifCond;
        private Object unlessCond;
        private Project project;

        /**
         * Set the current project
         *
         * @param project the current project
         */
        public void setProject(Project project) {
            this.project = project;
        }

       /**
         * Set the name.
         * @param name the parameter name
         */
        public void setName(String name) {
            this.name = name;
        }

	/**
         * Get the parameter name
         *
         * @return the parameter name
         */
        public String getName() {
            return name;
        }

       /**
         * Set the value.
         * @param value the parameter value
         */
        public void setValue(String value) {
            this.value = value;
        }

	/**
         * Get the parameter value
         *
         * @return the parameter value
         */
        public String getValue() {
            return value;
        }

        /**
         * Set whether this input should NOT be used. It will not be
         * used if the expression evaluates to true or the name of a
         * property which has been set, otherwise it will be used.
         * @param unlessCond evaluated expression
         */
        public void setUnless(Object unlessCond) {
            this.unlessCond = unlessCond;
        }

        /**
         * Set whether this input should NOT be used. It will not be
         * used if the expression evaluates to true or the name of a
         * property which has been set, otherwise it will be used.
         * @param unlessProperty evaluated expression
         */
        public void setUnless(String unlessProperty) {
            setUnless((Object) unlessProperty);
        }

        /**
         * Ensures that the input passes the conditions placed
         * on it with <code>if</code> and <code>unless</code> properties.
         * @return true if the task passes the "if" and "unless" parameters
         */
        public boolean shouldUse() {
            PropertyHelper ph = PropertyHelper.getPropertyHelper(project);
            return ph.testIfCondition(ifCond)
                && ph.testUnlessCondition(unlessCond);
        }
    } // Option

    /**
     * The Parameter inner class represents a pipeline parameter,
     * which looks a lot like an option sent to a parameter port (or
     * ports).
     */
    public static class Parameter extends Option {
        /** The port */
        private String port = "*";

        /**
         * Set the port.
         * @param port port to which to bind the parameter
         */
        public void setPort(String port) {
            this.port = port;
        }

	/**
         * Get the port
         *
         * @return the parameter port
         */
        public String getPort() {
            return port;
        }
    } // Parameter
}