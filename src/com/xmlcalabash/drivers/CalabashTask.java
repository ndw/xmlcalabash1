package com.xmlcalabash.drivers;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.runtime.XPipeline;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PropertyHelper;
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
public class CalabashTask extends Task {

    private Vector<Input> inputs = new Vector<Input>();
    private HashMap<String,Vector<Input>> inputsMap = new HashMap<String,Vector<Input>> ();
    /** Port of the pipeline input. As attribute. */
    private String inPort;
    public void setinPort(String port) {
        inPort = port;
    }

    /** URI of the input XML. As attribute. */
    private Resource inResource;
    public void setIn(Resource inResource) {
        this.inResource = inResource;
    }

    /** URI of the pipeline to run. As attribute. */
    String pipelineURI;
    public void setPipeline(String uri) {
        pipelineURI = uri;
    }

    /** Pipeline as a {@link org.apache.tools.ant.types.Resource} */
    private Resource pipelineResource = null;
    /**
     * API method to set the pipeline Resource.
     * @param pipelineResource Resource to set as the pipeline.
     */
    public void setPipelineResource(Resource pipelineResource) {
 	this.pipelineResource = pipelineResource;
    }

    /**
     * Add a nested &lt;pipeline&gt; element.
     * @param rc the configured Resources object represented as &lt;pipeline&gt;.
     */
    public void addConfiguredPipeline(Resources rc) {
 	if (rc.size() != 1) {
	    throw new BuildException("The pipeline element must be specified with exactly one"
			+ " nested resource.");
 	} else {
	    setPipelineResource((Resource) rc.iterator().next());
 	}
    }

    /** Port of the pipeline output. As attribute. */
    String outPort;
    public void setOutPort(String port) {
        outPort = port;
    }

    /** Resource of the output XML. As attribute. */
    Resource outResource;
    public void setOut(Resource outResource) {
        this.outResource = outResource;
    }

    /** Whether to fail the build if an error occurs. */
    private boolean failOnError = true;
    /**
     * Whether any errors should make the build fail.
     */
    public void setFailOnError(boolean b) {
        failOnError = b;
    }

    /**
     * Additional resource collections to process.
     */
    private Union resources = new Union();

    /**
     * Whether to use the implicit fileset.
     */
    private boolean useImplicitFileset = true;
    /**
     * Whether to use the implicit fileset.
     *
     * <p>Set this to false if you want explicit control with nested
     * resource collections.</p>
     * @param useimplicitfileset set to true if you want to use implicit fileset
     */
    public void setUseImplicitFileset(boolean useimplicitfileset) {
        useImplicitFileset = useimplicitfileset;
    }

    /**
     * Adds a collection of resources to process in addition to the
     * given file or the implicit fileset.
     *
     * @param rc the collection of resources to style
     */
    public void add(ResourceCollection rc) {
        resources.add(rc);
    }

    /**
     * Mapper to use when a set of files gets processed.
     */
    private Mapper mapperElement = null;

    /**
     * Defines the mapper to map source to destination files.
     * @param mapper the mapper to use
     * @exception BuildException if more than one mapper is defined
     */
    public void addMapper(Mapper mapper) {
        if (mapperElement != null) {
            handleError("Cannot define more than one mapper");
        } else {
            mapperElement = mapper;
        }
    }

    /**
     * Adds a nested filenamemapper.
     * @param fileNameMapper the mapper to add
     * @exception BuildException if more than one mapper is defined
     */
    public void add(FileNameMapper fileNameMapper) throws BuildException {
       Mapper mapper = new Mapper(getProject());
       mapper.add(fileNameMapper);
       addMapper(mapper);
    }

    /** force output of target files even if they already exist */
    private boolean force = false;
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
     * System properties to set during transformation.
     */
    private CommandlineJava.SysProperties sysProperties =
        new CommandlineJava.SysProperties();

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



    /** Do the work. */
    public void execute() {
	Resource usePipelineResource;
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
	    handleError("pipeline file " + usePipelineResource.getName() + " does not exist");
	    return;
	}

	if (inResource != null && !inResource.isExists()) {
	    handleError("input file " + inResource.getName() + " does not exist");
	    return;
	}
	if (outResource == null) {
	    handleError("'out' must be specified.");
	    return;
	}

	try {
	    if (sysProperties.size() > 0) {
		sysProperties.setSystem();
	    }

	    // if we have an in file and out then process them
	    if (inResource != null && outResource != null) {
		Input i = new Input();
		i.setPort(inPort);
		i.add(inResource);
		xaddInput(i);
		process(inputsMap, outResource, usePipelineResource);
		return;
	    }
	} finally {
            if (sysProperties.size() > 0) {
                sysProperties.restoreSystem();
            }
	}
    }

    /**
     * Process the input file to the output file with the given pipeline.
     *
     * @param inResource the input file to process.
     * @param outFile the destination file.
     * @param pipeline the pipeline to use.
     * @exception BuildException if the processing fails.
     */
    private void process(HashMap inputsMap, Resource out, Resource pipelineResource) throws BuildException {

	long pipelineLastModified = pipelineResource.getLastModified();
	//log("In file " + in + " time: " + in.getLastModified(), Project.MSG_DEBUG);
	log("Out file " + out + " time: " + out.getLastModified(), Project.MSG_DEBUG);
	log("Style file " + pipelineResource + " time: " + pipelineLastModified, Project.MSG_DEBUG);
/*
	if (!force && in.getLastModified() < out.getLastModified()
                    && pipelineLastModified < out.getLastModified()) {
	    log("Skipping input file " + in + " because it is older than output file "
		+ out + " and so is the stylesheet " + pipelineResource, Project.MSG_DEBUG);
	    return;
	}
*/
	//log("Processing " + in + " to " + out, Project.MSG_INFO);
        XProcConfiguration config = new XProcConfiguration("he", false);
        XProcRuntime runtime = new XProcRuntime(config);

	try {
	    XPipeline pipeline =
		runtime.load(pipelineResource.getName());

            Set<String> wantPorts = pipeline.getInputs();
            Set<String> setPorts = inputsMap.keySet();
            String defaultPort = null;
	    for (String port : wantPorts) {
                if (pipeline.getInput(port).getParameters()) {
                    continue;
                }
		if (!setPorts.contains(port) && !pipeline.getInput(port).getParameters()) {
                    if (defaultPort == null) {
                        defaultPort = port;
                        log("Binding default input port to '" + port + "'.", Project.MSG_INFO);
                    } else {
                        log("You didn't specify any binding for the input port '" + port + "'.", Project.MSG_WARN);
                        continue;
                    }
                }
                Vector<Input> portInputs = (Vector<Input>) inputsMap.get(port.equals(defaultPort) ? null : port);
                for (Input in : portInputs) {
                    Iterator<Resource> element = in.getResources().iterator();
                    while (element.hasNext()) {
                        Resource resource = element.next();
                        log(resource.getName(), Project.MSG_INFO);
                        InputStream is = resource.getInputStream();
                        XdmNode doc = runtime.parse(new InputSource(resource.getInputStream()));
                        pipeline.writeTo(port, doc);
                    }
                }
            }
	    pipeline.run();

            // Look for primary output port
            for (String port : pipeline.getOutputs()) {
                if (outPort == null) {
                    outPort = port;
		} else if (!port.equals(outPort)) {
		    log("You didn't specify any binding for the output port '" + port + "', its output will be discarded.", Project.MSG_WARN);
                }
            }

            for (String port : pipeline.getOutputs()) {
                String uri = null;

                if (outPort.equals(port)) {
                    uri = outResource.getName();
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
     * Add an instance of an pipeline input.
     */
    protected void xaddInput(Input i) {
	String port = i.getPort();

	if (!inputsMap.containsKey(port)) {
	    inputsMap.put(port, new Vector<Input> ());
	}
        inputsMap.get(port).add(i);
    }

    /**
     * Create an instance of an pipeline input for configuration by Ant.
     *
     * @return an instance of the Input class to be configured.
     */
    public void addConfiguredInput(Input i) {
	xaddInput(i);
    }

    /**
     * The Input inner class used to store pipeline inputs
     */
    public static class Input {
        /** The input port */
        private String port = null;

        /** The input's resources */
        private Union resources = new Union();

        private Object ifCond;
        private Object unlessCond;
        private Project project;

        /**
         * Set the current project
         *
         * @input project the current project
         */
        public void setProject(Project project) {
            this.project = project;
        }

        /**
         * Set the input port.
         *
         * @input port the name of the port.
         */
        public void setPort(String port) {
            this.port = port;
        }

	/**
	 * Adds a collection of resources to process in addition to the
	 * given file or the implicit fileset.
	 *
	 * @param rc the collection of resources to style
	 */
	public void add(ResourceCollection rc) {
	    resources.add(rc);
	}

        /**
         * Get the input port
         *
         * @return the input port name
         */
        public String getPort() throws BuildException {
            return port;
        }

        /**
         * Get the input's resources
         *
         * @return the input's resources
         */
        public Union getResources() {
            return resources;
        }

        /**
         * Set whether this input should be used.  It will be used if
         * the expression evalutes to true or the name of a property
         * which has been set, otherwise it won't.
         * @input ifCond evaluated expression
         */
        public void setIf(Object ifCond) {
            this.ifCond = ifCond;
        }

        /**
         * Set whether this input should be used.  It will be used if
         * the expression evalutes to true or the name of a property
         * which has been set, otherwise it won't.
         * @input ifProperty evaluated expression
         */
        public void setIf(String ifProperty) {
            setIf((Object) ifProperty);
        }

        /**
         * Set whether this input should NOT be used. It will not be
         * used if the expression evaluates to true or the name of a
         * property which has been set, otherwise it will be used.
         * @input unlessCond evaluated expression
         */
        public void setUnless(Object unlessCond) {
            this.unlessCond = unlessCond;
        }

        /**
         * Set whether this input should NOT be used. It will not be
         * used if the expression evaluates to true or the name of a
         * property which has been set, otherwise it will be used.
         * @input unlessProperty evaluated expression
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
    } // Input

}