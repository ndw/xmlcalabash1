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

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.transform.URIResolver;

import com.xmlcalabash.util.Input.Type;
import com.xmlcalabash.util.UserArgs;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;
import org.xml.sax.EntityResolver;

import static com.xmlcalabash.util.Input.Type.DATA;
import static com.xmlcalabash.util.Input.Type.XML;
import static java.lang.Long.MAX_VALUE;
import static java.util.Arrays.asList;

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

    private UserArgs userArgs = new UserArgs();

    /**
     * Input ports and the resources associated with each.
     */
    private Map<String, List<TypedResource>> inputResources = new HashMap<String, List<TypedResource>>();

    /**
     * Input ports and the mapper associated with each.
     */
    private Map<String, TypedFileNameMapper> inputMappers = new HashMap<String, TypedFileNameMapper>();

    /**
     * Where to find the source XML file, default is the project's basedir
     */
    private File baseDir = null;

    /**
     * Port of the pipeline input. As attribute.
     */
    private String inPort = null;

    /**
     * URI of the input XML. As attribute.
     */
    private Resource inResource = null;

    /**
     * Type of the input resource.
     */
    private Type inType = XML;

    /**
     * Whether the build should fail if the nested resource collection is empty.
     */
    private boolean failOnNoResources = true;

    /**
     * The pipeline to run as a {@link org.apache.tools.ant.types.Resource}
     */
    private Resource pipelineResource = null;

    /**
     * destination directory
     */
    private File destDir = null;

    /**
     * Port of the pipeline output. As attribute.
     */
    private String outPort = null;

    /**
     * Resource of the output XML. As attribute.
     */
    private Resource outResource = null;

    /**
     * Output ports and the resources associated with each.
     */
    private HashMap<String, Union> outputResources = new HashMap<String, Union>();

    /**
     * Output ports and the mapper associated with each.
     */
    private Map<String, FileNameMapper> outputMappers = new HashMap<String, FileNameMapper>();

    /**
     * extension of the files produced by pipeline processing
     */
    private String targetExtension = "-out.xml";

    /**
     * whether target extension has been set from build file
     */
    private boolean isTargetExtensionSet = false;

    /**
     * Whether to fail the build if an error occurs.
     */
    private boolean failOnError = true;

    /**
     * Additional resource collections to process.
     */
    private Union resources = new Union();

    /**
     * Whether to use the implicit fileset.
     */
    private boolean useImplicitFileset = true;

    /**
     * Whether to process all files in the included directories as well.
     */
    private boolean performDirectoryScan = true;

    /**
     * Mapper to use when a set of files gets processed.
     */
    private FileNameMapper mapper = null;

    /**
     * force output of target files even if they already exist
     */
    private boolean force = false;

    /**
     * System properties to set during transformation.
     */
    private CommandlineJava.SysProperties sysProperties =
            new CommandlineJava.SysProperties();

    /**
     * The list of parameters.
     */
    private List<Parameter> parameters = new ArrayList<Parameter>();

    /**
     * The list of options.
     */
    private List<Option> options = new ArrayList<Option>();

    /**
     * The list of steps.
     */
    private List<Step> steps = new ArrayList<Step>();

    /* End of fields to reset at end of execute(). */

    /**
     * Set the base directory; optional, default is the project's basedir.
     *
     * @param dir the base directory
     */
    public void setBasedir(File dir) {
        baseDir = dir;
    }

    /**
     * Set the input port name. optional, default is the first unmatched pipeline input port.
     *
     * @param port the port name
     */
    public void setInPort(String port) {
        inPort = port;
    }

    /**
     * Set the input resource. optional, implicit and/or explicit fileset will be used if this and outResource are not
     * set.
     *
     * @param inResource the {@link org.apache.tools.ant.types.Resource}
     */
    public void setIn(Resource inResource) {
        this.inResource = inResource;
    }

    /**
     * Set the input type. optional, default is XML. This is used for the implicit and / or explicit fileset or the
     * {@code in} attribute value.
     *
     * @param inType the input type
     */
    public void setInType(Type inType) {
        this.inType = inType;
    }

    /**
     * Work with an instance of an {@code <input>} element already configured by Ant.
     *
     * @param input the configured input Port
     */
    public void addConfiguredInput(Input input) {
        if (!input.shouldUse()) {
            log("Skipping input '" + input.getPort() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        String port = input.getPort();
        FileNameMapper inputMapper = input.getMapper();
        Union resources = input.getResources();

        if (port == null) {
            port = inPort;
        }

        if (inputMapper != null) {
            if (resources.size() != 0) {
                handleError("Both mapper and fileset on input port: " + port);
                return;
            }

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

            inputMappers.put(port, new TypedFileNameMapper(inputMapper, input.getType(), input.getContentType()));
        } else {
            if (inputMappers.containsKey(port)) {
                handleError("Resources used on input port that already has a mapper: " + port);
                return;
            }

            if ((resources.size() != 0) && !inputResources.containsKey(port)) {
                inputResources.put(port, new ArrayList<TypedResource>());
            }

            for (Resource resource : resources.listResources()) {
                inputResources.get(port).add(new TypedResource(resource, input.getType(), input.getContentType()));
            }
        }
    }

    /**
     * Whether the build should fail if the nested resource collection is empty.
     */
    public void setFailOnNoResources(boolean b) {
        failOnNoResources = b;
    }

    /**
     * Set the pipeline. optional, nested &lt;pipeline> will be used if not set.
     *
     * @param pipeline pipeline location
     */
    public void setPipeline(Resource pipeline) {
        try {
            userArgs.setPipeline(pipeline.getInputStream(), pipeline.toString());
            this.pipelineResource = pipeline;
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Add a nested &lt;pipeline&gt; element.
     *
     * @param pipeline the configured Resources object represented as &lt;pipeline&gt;.
     */
    public void addConfiguredPipeline(UseableResources pipeline) {
        if (!pipeline.shouldUse()) {
            log("Skipping pipeline as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        if (pipeline.size() == 0) {
            return;
        }

        if (pipeline.size() > 1) {
            handleError("The pipeline element must be specified with at most one nested resource.");
        }

        setPipeline((Resource) pipeline.iterator().next());
    }

    /**
     * Set the destination directory into which the XSL result
     * files should be copied to;
     * required, unless <tt>in</tt> and <tt>out</tt> are
     * specified.
     *
     * @param dir the name of the destination directory
     */
    public void setDestdir(File dir) {
        destDir = dir;
    }

    /**
     * Set the output port name.
     * optional, default is the first unmatched pipeline output port.
     *
     * @param port the port name
     */
    public void setOutPort(String port) {
        outPort = port;
    }

    /**
     * Set the output resource.
     * optional, implicit and/or explicit fileset will be used if this
     * and inResource are not set.
     *
     * @param outResource the {@link org.apache.tools.ant.types.Resource}
     */
    public void setOut(Resource outResource) {
        this.outResource = outResource;
    }

    /**
     * Work with an instance of an <output> element already configured by Ant.
     *
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
            handleError("Both mapper and fileset on output port: " + port);
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
                outputResources.put(port, new Union());
            }
            outputResources.get(port).add(resources);
        }
    }

    /**
     * Set the desired file extension to be used for the target;
     * optional, default is '-out.xml'.
     *
     * @param name the extension to use
     */
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
    }

    /**
     * Whether to use the implicit fileset.
     *
     * <p>Set this to false if you want explicit control with nested resource collections.</p>
     *
     * @param useimplicitfileset set to true if you want to use implicit fileset
     */
    public void setUseImplicitFileset(boolean useimplicitfileset) {
        useImplicitFileset = useimplicitfileset;
    }

    /**
     * Whether to process all files in the included directories as well;
     * optional, default is true.
     *
     * @param b true if files in included directories are processed.
     */
    public void setScanIncludedDirectories(boolean b) {
        performDirectoryScan = b;
    }

    /**
     * Defines the mapper to map source to destination files.
     *
     * @param mapper the mapper to use
     * @throws BuildException if more than one mapper is defined
     */
    public void addMapper(Mapper mapper) throws BuildException {
        add(mapper.getImplementation());
    }

    /**
     * Adds a nested FileNameMapper.
     *
     * @param fileNameMapper the mapper to add
     * @throws BuildException if more than one mapper is defined
     */
    public void add(FileNameMapper fileNameMapper) throws BuildException {
        if (mapper != null) {
            handleError("Cannot define more than one mapper");
            return;
        }

        mapper = fileNameMapper;
    }

    /**
     * Set whether to check dependencies, or always generate; optional, default is false.
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
     * Work with an instance of a <binding> element already configured by Ant.
     *
     * @param namespace the configured Namespace
     */
    public void addConfiguredNamespace(Namespace namespace) {
        if (!namespace.shouldUse()) {
            log("Skipping namespace '" + namespace.getPrefix() + "=" + namespace.getURI() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        // prefix and/or uri may have been omitted in build file
        // without Ant complaining
        if (namespace.getPrefix() == null) {
            handleError("<namespace> prefix cannot be null");
            return;
        }

        if (namespace.getURI() == null) {
            handleError("<namespace> URI cannot be null");
            return;
        }

        try {
            userArgs.addBinding(namespace.getPrefix(), namespace.getURI());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Work with an instance of a <option> element already configured by Ant.
     *
     * @param option the configured Option
     */
    public void addConfiguredOption(Option option) {
        if (!option.shouldUse()) {
            log("Skipping option '" + option.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        options.add(option);
    }

    /**
     * Use an {@code <option>} element.
     *
     * @param option the Option
     */
    public void useOption(Option option) {
        if (!option.shouldUse()) {
            log("Skipping option '" + option.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        try {
            userArgs.addOption(option.getName(), option.getValue());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Work with an instance of a <parameter> element already configured by Ant.
     *
     * @param parameter the configured Parameter
     */
    public void addConfiguredParameter(Parameter parameter) {
        if (!parameter.shouldUse()) {
            log("Skipping parameter '" + parameter.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        parameters.add(parameter);
    }

    /**
     * Use a {@code <parameter>} element.
     *
     * @param parameter the Parameter
     */
    public void useParameter(Parameter parameter) {
        if (!parameter.shouldUse()) {
            log("Skipping parameter '" + parameter.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        try {
            userArgs.addParam(parameter.getPort(), parameter.getName(), parameter.getValue());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set whether to enable debugging output;
     * optional, default is false.
     *
     * @param debug true if enable debug output
     */
    public void setDebug(boolean debug) {
        try {
            userArgs.setDebug(debug);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set whether to enable general values;
     * optional, default is false.
     *
     * @see <a href="http://xmlcalabash.com/docs/reference/extensions.html#ext.general-values">General values extension</a>
     * @param generalValues true if enable general values
     */
    public void setGeneralValues(boolean generalValues) {
        try {
            userArgs.setExtensionValues(generalValues);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set whether xpointer attribute on an XInclude element can be used when parse="text";
     * optional, default is false.
     *
     * @param xPointerOnText true if enable XPointer on text
     */
    public void setXPointerOnText(boolean xPointerOnText) {
        try {
            userArgs.setAllowXPointerOnText(xPointerOnText);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set whether to enable use of XSLT 1.0;
     * optional, default is false.
     *
     * @param useXslt10 true if enable XSLT 1.0 support
     */
    public void setUseXslt10(boolean useXslt10) {
        try {
            userArgs.setUseXslt10(useXslt10);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set whether to automatically translate between JSON and XML;
     * optional, default is false.
     *
     * @param transparentJSON true if enable translation
     */
    public void setTransparentJSON(boolean transparentJSON) {
        try {
            userArgs.setTransparentJSON(transparentJSON);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set the desired JSON flavor.
     *
     * @param jsonFlavor the flavor of JSON/XML transformation to use
     */
    public void setJSONFlavor(String jsonFlavor) {
        try {
            userArgs.setJsonFlavor(jsonFlavor);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Set the path to the file where profile information should be written to, or {@code -} for stdout.
     *
     * @param profileFile the path to the file where profile information should be written to, or {@code -} for stdout
     */
    public void setProfileFile(Resource profileFile) {
        try {
            userArgs.setProfile(profileFile.getOutputStream());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Add a nested {@code <profile>} element.
     *
     * @param profile the configured Resources object represented as {@code <profile>}
     */
    public void addConfiguredProfile(UseableResources profile) {
        if (!profile.shouldUse()) {
            log("Skipping profile as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        if (profile.size() == 0) {
            return;
        }

        if (profile.size() > 1) {
            handleError("The profile element must be specified with at most one nested resource.");
        }

        setProfileFile((Resource) profile.iterator().next());
    }

    /**
     * Request a specific edition of Saxon. Must be {@code he} (default), {@code pe} or {@code ee}.
     *
     * @param saxonProcessor the edition of Saxon that should be used, must be {@code he} (default), {@code pe} or {@code ee}
     */
    public void setSaxonProcessor(String saxonProcessor) {
        try {
            userArgs.setSaxonProcessor(saxonProcessor);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Load the specified Saxon configuration file.
     *
     * @param saxonConfigFile the path to the Saxon configuration file to be loaded
     */
    public void setSaxonConfigFile(Resource saxonConfigFile) {
        try {
            userArgs.setSaxonConfig(saxonConfigFile.getInputStream(), saxonConfigFile.toString());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Add a nested {@code <saxonConfig>} element.
     *
     * @param saxonConfig the configured Resources object represented as {@code <saxonConfig>}
     */
    public void addConfiguredSaxonConfig(UseableResources saxonConfig) {
        if (!saxonConfig.shouldUse()) {
            log("Skipping saxonConfig as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        if (saxonConfig.size() == 0) {
            return;
        }

        if (saxonConfig.size() > 1) {
            handleError("The saxonConfig element must be specified with at most one nested resource.");
        }

        setSaxonConfigFile((Resource) saxonConfig.iterator().next());
    }

    /**
     * Specify whether schema-aware processing should be done.
     *
     * @param schemaAware whether schema-aware processing should be done
     */
    public void setSchemaAware(boolean schemaAware) {
        try {
            userArgs.setSchemaAware(schemaAware);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Specify whether "safe" execution should be done.
     *
     * @param safeMode whether "safe" execution should be done
     */
    public void setSafeMode(boolean safeMode) {
        try {
            userArgs.setSafeMode(safeMode);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Specify a particular configuration file to be loaded.
     *
     * @param configFile the path to a particular configuration file to be loaded
     */
    public void setConfigFile(Resource configFile) {
        try {
            userArgs.setConfig(configFile.getInputStream(), configFile.toString());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Add a nested {@code <config>} element.
     *
     * @param config the configured Resources object represented as {@code <config>}
     */
    public void addConfiguredConfig(UseableResources config) {
        if (!config.shouldUse()) {
            log("Skipping config as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        if (config.size() == 0) {
            return;
        }

        if (config.size() > 1) {
            handleError("The config element must be specified with at most one nested resource.");
        }

        setConfigFile((Resource) config.iterator().next());
    }

    /**
     * Specify the default style for p:log output. Must be {@code off}, {@code plain}, {@code wrapped} (default), or {@code directory}.
     *
     * @param logStyle the default style for p:log output, must be {@code off}, {@code plain}, {@code wrapped} (default), or {@code directory}
     */
    public void setLogStyle(String logStyle) {
        try {
            userArgs.setLogStyle(logStyle);
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Specify a resolver class for entity resolution.
     *
     * @param entityResolver the resolver class for entity resolution
     */
    public void setEntityResolver(Class<? extends EntityResolver> entityResolver) {
        try {
            userArgs.setEntityResolverClass(entityResolver.getName());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Specify a resolver class for URI resolution.
     *
     * @param uriResolver the resolver class for URI resolution
     */
    public void setURIResolver(Class<? extends URIResolver> uriResolver) {
        try {
            userArgs.setUriResolverClass(uriResolver.getName());
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Add a nested {@code <library>} element.
     *
     * @param libraries the configured Resources object represented as {@code <library>}.
     */
    public void addConfiguredLibrary(UseableResources libraries) {
        if (!libraries.shouldUse()) {
            log("Skipping library as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        try {
            for (Iterator<Resource> iterator = libraries.iterator(); iterator.hasNext(); ) {
                Resource library = iterator.next();
                userArgs.addLibrary(library.getInputStream(), library.toString());
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Add a nested {@code <step>} element.
     *
     * @param step the configured Step object represented as {@code <step>}.
     */
    public void addConfiguredStep(Step step) {
        if (!step.shouldUse()) {
            log("Skipping step '" + step.getName() + "' as it is configured to be unused.", Project.MSG_DEBUG);
            return;
        }

        if (step.getName() == null) {
            handleError("Steps must have their 'name' attribute set");
        }

        steps.add(step);
    }

    /**
     * Do the work.
     */
    public void execute() {
        if (((inResource != null) || (resources.size() != 0) || !inputResources.isEmpty() || !inputMappers.isEmpty())
            && !steps.isEmpty()) {

            handleError("if steps are given, only active inputs nested in these steps are supported");
        }

        if (!parameters.isEmpty() && !steps.isEmpty()) {
            handleError("if steps are given, only active parameters nested in these steps are supported");
        }

        if (!options.isEmpty() && !steps.isEmpty()) {
            handleError("if steps are given, only active options nested in these steps are supported");
        }

        if ((pipelineResource != null) && !pipelineResource.isExists()) {
            handleError("pipeline '" + pipelineResource.getName() + "' does not exist");
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

        if (!steps.isEmpty() && useImplicitFileset) {
            log("steps cannot be used with implicit fileset: ignoring implicit fileset.", Project.MSG_VERBOSE);
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

        try {
            if (baseDir == null) {
                baseDir = getProject().getBaseDir();
            }

            if (sysProperties.size() > 0) {
                sysProperties.setSystem();
            }

            //-- make sure destination directory exists...
            checkDest();

            // if we have an in file or out file then process them
            if (inResource != null) {
                Input i = new Input();
                i.setPort(inPort);
                i.add(inResource);
                i.setType(inType);
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
                handleError("Either 'out' or <output> corresponding to default output port and either 'extension' or nested <mapper> for naming output cannot be used together.");
                return;
            }

            for (Parameter parameter : parameters) {
                useParameter(parameter);
            }

            for (Option option : options) {
                useOption(option);
            }

            // If neither implicit or explicit fileset, assume user
            // knows what they're doing even though there may be no
            // input or output ports.
            if (!useImplicitFileset && resources.size() == 0) {
                Map<String, List<TypedResource>> useInputResources = new HashMap<String, List<TypedResource>>();
                // Any fixed resources on any input ports.
                useInputResources.putAll(inputResources);
                // Any mapped resources on non-inPort input ports.
                for (String port : inputMappers.keySet()) {
                    TypedFileNameMapper inputMapper = inputMappers.get(port);
                    for (TypedResource typedResource : inputResources.get(inPort)) {
                        String[] inputFileNames = inputMapper.mapFileName(typedResource.getResource().getName());
                        // Mapper may produce zero or more filenames,
                        // which may or may not be what was wanted but
                        // only the user will know that.
                        if (inputFileNames != null) {
                            List<TypedResource> mappedResources = new ArrayList<TypedResource>();
                            for (String fileName : inputFileNames) {
                                FileResource mappedResource = new FileResource(baseDir, fileName);
                                if (mappedResource.isExists()) {
                                    mappedResources.add(new TypedResource(mappedResource, inputMapper.getType(), inputMapper.getContentType()));
                                } else {
                                    log("Skipping non-exstent mapped resource: " + mappedResource.toString(), Project.MSG_DEBUG);
                                }
                            }
                            useInputResources.put(port, mappedResources);
                        }
                    }
                }
                HashMap<String, Union> useOutputResources = new HashMap<String, Union>();
                useOutputResources.putAll(outputResources);

                if (outputMappers.size() != 0) {
                    for (TypedResource typedResource : inputResources.get(inPort)) {
                        // Add any mapped resources on output ports.
                        for (String port : outputMappers.keySet()) {
                            FileNameMapper outputMapper = outputMappers.get(port);

                            String[] outputFileNames = outputMapper.mapFileName(typedResource.getResource().getName());
                            // Mapper may produce zero or more filenames,
                            // which may or may not be what was wanted but
                            // only the user will know that.
                            if (outputFileNames != null) {
                                Union outputResources = new Union();
                                for (String fileName : outputFileNames) {
                                    outputResources.add(new FileResource(destDir, fileName));
                                }
                                useOutputResources.put(port, outputResources);
                            }
                        }
                    }
                }
                process(useInputResources, useOutputResources);
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
                    Map<String, List<TypedResource>> useInputResources = new HashMap<String, List<TypedResource>>();

                    // Any fixed resources on other input ports.
                    useInputResources.putAll(inputResources);
                    // The resource.
                    useInputResources.put(inPort, asList(new TypedResource(resource, inType)));
                    // Any mapped resources on other input ports.
                    for (String port : inputMappers.keySet()) {
                        TypedFileNameMapper inputMapper = inputMappers.get(port);

                        String[] inputFileNames = inputMapper.mapFileName(resource.getName());
                        // Mapper may produce zero or more filenames,
                        // which may or may not be what was wanted but
                        // only the user will know that.
                        if (inputFileNames != null) {
                            List<TypedResource> mappedResources = new ArrayList<TypedResource>();
                            for (String fileName : inputFileNames) {
                                FileResource mappedResource = new FileResource(baseDir, fileName);
                                mappedResources.add(new TypedResource(mappedResource, inputMapper.getType(), inputMapper.getContentType()));
                            }
                            useInputResources.put(port, mappedResources);
                        }
                    }

                    HashMap<String, Union> useOutputResources = new HashMap<String, Union>();
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

                        String[] outputFileNames = outputMapper.mapFileName(resource.getName());
                        // Mapper may produce zero or more filenames,
                        // which may or may not be what was wanted but
                        // only the user will know that.
                        if (outputFileNames != null) {
                            Union outputResources = new Union();
                            for (String fileName : outputFileNames) {
                                outputResources.add(new FileResource(destDir, fileName));
                            }
                            useOutputResources.put(port, outputResources);
                        }
                    }
                    process(useInputResources, useOutputResources);
                }
            }
        } finally {
            // Same instance is reused when Ant runs this task
            // again, so reset everything.
            userArgs = new UserArgs();
            inputResources.clear();
            inputMappers.clear();
            baseDir = null;
            inPort = null;
            inResource = null;
            inType = XML;
            failOnNoResources = true;
            pipelineResource = null;
            destDir = null;
            outPort = null;
            outResource = null;
            outputResources.clear();
            outputMappers.clear();
            targetExtension = "-out.xml";
            isTargetExtensionSet = false;
            failOnError = true;
            resources = new Union();
            useImplicitFileset = true;
            performDirectoryScan = true;
            mapper = null;
            force = false;
            if (sysProperties.size() > 0) {
                sysProperties.restoreSystem();
                // No way to clear CommandlineJava.SysProperties
                sysProperties = new CommandlineJava.SysProperties();
            }
            parameters.clear();
            options.clear();
            steps.clear();
        }
    }

    /**
     * Process the input file to the output file with the given pipeline.
     *
     * @param inputResources   the map of input ports to resources
     * @param outputResources  the map of output ports to resources
     * @throws BuildException if the processing fails.
     */
    private void process(Map<String, List<TypedResource>> inputResources, Map<String, Union> outputResources) throws BuildException {
        if (!force && (pipelineResource != null)) {
            long pipelineLastModified = pipelineResource.getLastModified();
            pipelineLastModified = (pipelineLastModified == 0) ? MAX_VALUE : pipelineLastModified;
            Collection<Long> inputsLastModified = new Vector<Long>();
            for (String port : inputResources.keySet()) {
                for (TypedResource typedResource : inputResources.get(port)) {
                    long lastModified = typedResource.getResource().getLastModified();
                    inputsLastModified.add((lastModified == 0) ? MAX_VALUE : lastModified);
                }
            }
            long newestInputLastModified = inputsLastModified.isEmpty() ? MAX_VALUE : Collections.max(inputsLastModified);

            Collection<Long> outputsLastModified = new Vector<Long>();
            for (String port : outputResources.keySet()) {
                for (Resource resource : outputResources.get(port).listResources()) {
                    outputsLastModified.add(resource.getLastModified());
                }
            }
            long oldestOutputLastModified = outputsLastModified.isEmpty() ? 0 : Collections.min(outputsLastModified);

            log("Newest input time: " + newestInputLastModified, Project.MSG_DEBUG);
            log("Oldest output time: " + oldestOutputLastModified, Project.MSG_DEBUG);
            log("Pipeline file " + pipelineResource + " time: " + pipelineLastModified, Project.MSG_DEBUG);

            if (newestInputLastModified <= oldestOutputLastModified &&
                pipelineLastModified <= oldestOutputLastModified) {
                log("Skipping because all outputs are newer than inputs and newer than pipeline", Project.MSG_DEBUG);
                return;
            }
        }

        try {
            for (String port : outputResources.keySet()) {
                Union resources = outputResources.get(port);
                for (Iterator<Resource> iterator = resources.iterator(); iterator.hasNext(); ) {
                    Resource resource = iterator.next();
                    userArgs.addOutput(port, resource.getOutputStream());
                }
            }
            for (String port : inputResources.keySet()) {
                for (TypedResource typedResource : inputResources.get(port)) {
                    Resource resource = typedResource.getResource();
                    userArgs.addInput(port, resource.getInputStream(), resource.toString(), typedResource.getType(), typedResource.getContentType());
                }
            }
            for (Step step : steps) {
                for (Input input : step.getInputs()) {
                    for (Resource resource : input.getResources().listResources()) {
                        userArgs.addInput(input.getPort(), resource.getInputStream(), resource.toString(), input.getType(), input.getContentType());
                    }
                }
                for (Parameter parameter : step.getParameters()) {
                    useParameter(parameter);
                }
                userArgs.setCurStepName(step.getName());
                for (Option option : step.getOptions()) {
                    useOption(option);
                }
            }

            new Main().run(userArgs, userArgs.createConfiguration());
        } catch (Exception e) {
            handleError(e);
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
            return new String[] { xmlFile + targetExtension };
        }
    }

    /**
     * Sets the destination directory to the base directory if it is not set explicitly.
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
     * failOnError is true, otherwise logs the message using the WARN level.
     */
    protected void handleError(Throwable ex) {
        if (failOnError) {
            throw new BuildException(ex);
        } else {
            log("Caught an exception: " + ex, Project.MSG_WARN);
            ex.printStackTrace(new PrintStream(new LogOutputStream(this, Project.MSG_VERBOSE)));
        }
    }

    /**
     * The {@code Useable} inner class used to represent something which usage
     * can be controlled by {@code if} and {@code unless} properties.
     */
    private static class Useable {
        private Project project;
        private Object ifCond;
        private Object unlessCond;

        /**
         * Set the current project
         *
         * @param project the current project
         */
        public void setProject(Project project) {
            this.project = project;
        }

        /**
         * Set whether this {@code Useable} should be used. It will be used if
         * the expression evaluates to {@code true} or the name of a property
         * which has been set, otherwise it won't.
         *
         * @param ifCond evaluated expression
         */
        public void setIf(Object ifCond) {
            this.ifCond = ifCond;
        }

        /**
         * Set whether this {@code Useable} should NOT be used. It will not be
         * used if the expression evaluates to {@code true} or the name of a
         * property which has been set, otherwise it will be used.
         *
         * @param unlessCond evaluated expression
         */
        public void setUnless(Object unlessCond) {
            this.unlessCond = unlessCond;
        }

        /**
         * Ensures that the {@code Useable} passes the conditions placed
         * on it with {@code if} and {@code unless} properties.
         *
         * @return {@code true} if the task passes the {@code if} and {@code unless} parameters
         */
        public boolean shouldUse() {
            PropertyHelper ph = PropertyHelper.getPropertyHelper(project);
            return ph.testIfCondition(ifCond) && ph.testUnlessCondition(unlessCond);
        }
    }

    /**
     * The {@code Port} inner class used to represent output on a port.
     */
    public static class Port extends Useable {
        /**
         * The input port
         */
        private String port = null;

        /**
         * The input's resources
         */
        private Union resources = new Union();

        /**
         * Mapper to inPort files.
         */
        private FileNameMapper mapper = null;

        /**
         * Set the input port.
         *
         * @param port the name of the port.
         */
        public void setPort(String port) {
            this.port = port;
        }

        /**
         * Adds a collection of resources to process in addition to the given file or the implicit fileset.
         *
         * @param rc the collection of resources to use
         */
        public void add(ResourceCollection rc) {
            resources.add(rc);
        }

        /**
         * Defines the mapper to map source to destination files.
         *
         * @param mapper the mapper to use
         * @throws BuildException if more than one mapper is defined
         */
        public void addConfiguredMapper(Mapper mapper) throws BuildException {
            add(mapper.getImplementation());
        }

        /**
         * Adds a nested FileNameMapper.
         *
         * @param fileNameMapper the mapper to add
         * @throws BuildException if more than one mapper is defined
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
         * @return the ports' mapper
         */
        public FileNameMapper getMapper() {
            return mapper;
        }
    } // Port

    /**
     * The {@code Input} inner class used to represent input on a port.
     */
    public static class Input extends Port {
        /**
         * The input type
         */
        private Type type = XML;

        /**
         * The content type
         */
        private String contentType;

        /**
         * Get the input type
         *
         * @return the input type
         */
        public Type getType() {
            return type;
        }

        /**
         * Set the input type
         *
         * @param type the input type
         */
        public void setType(Type type) {
            this.type = type;
        }

        /**
         * Get the content type
         *
         * @return the content type
         */
        public String getContentType() {
            if ((contentType != null) && (type != DATA)) {
                throw new IllegalStateException("contentType of input can only be set if type is DATA");
            }
            return contentType;
        }

        /**
         * Set the content type
         *
         * @param contentType the content type
         */
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    } // Input

    /**
     * The {@code Namespace} inner class represents a namespace binding.
     */
    public static class Namespace extends Useable {
        /**
         * The prefix
         */
        private String prefix = null;
        /**
         * The URI
         */
        private String uri = null;

        /**
         * Set the prefix.
         *
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
         *
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
     * The {@code Option} inner class represents a pipeline option.
     */
    public static class Option extends Useable {
        /**
         * The name
         */
        private String name = null;

        /**
         * The parameter value
         */
        private String value = null;

        /**
         * Set the name.
         *
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
         *
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
    } // Option

    /**
     * The {@code Parameter} inner class represents a pipeline parameter,
     * which looks a lot like an option sent to a parameter port (or ports).
     */
    public static class Parameter extends Option {
        /**
         * The port
         */
        private String port = "*";

        /**
         * Set the port.
         *
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

    /**
     * The {@code Step} inner class represents a pipeline step.
     */
    public static class Step extends Useable {
        private String name = null;
        private List<Input> inputs = new ArrayList<Input>();
        private List<Parameter> parameters = new ArrayList<Parameter>();
        private List<Option> options = new ArrayList<Option>();

        /**
         * Set the name.
         *
         * @param name the name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Add a nested {@code <input>}.
         *
         * @param input the {@code Input} object
         */
        public void addConfiguredInput(Input input) {
            if (!input.shouldUse()) {
                super.project.log("Skipping input '" + input.getPort() + "' as it is configured to be unused.", Project.MSG_DEBUG);
                return;
            }

            inputs.add(input);
            if (input.getMapper() != null) {
                throw new BuildException("Mappers are not supported for inputs that are nested in steps");
            }
        }

        /**
         * Get the inputs.
         *
         * @return the inputs
         */
        public List<Input> getInputs() {
            return inputs;
        }

        /**
         * Add a nested {@code <parameter>}.
         *
         * @param parameter the {@code Parameter} object
         */
        public void addConfiguredParameter(Parameter parameter) {
            parameters.add(parameter);
        }

        /**
         * Get the parameters.
         *
         * @return the parameters
         */
        public List<Parameter> getParameters() {
            return parameters;
        }

        /**
         * Add a nested {@code <option>}.
         *
         * @param option the {@code Option} object
         */
        public void addConfiguredOption(Option option) {
            options.add(option);
        }

        /**
         * Get the options.
         *
         * @return the options
         */
        public List<Option> getOptions() {
            return options;
        }
    } // Step

    public static class UseableResources extends Resources {
        private Useable useable = new Useable();

        public void setIf(Object ifCond) {
            useable.setIf(ifCond);
        }

        public void setUnless(Object unlessCond) {
            useable.setUnless(unlessCond);
        }

        public boolean shouldUse() {
            return useable.shouldUse();
        }
    }

    private static class TypedResource {
        private Resource resource = null;
        private Type type = null;
        private String contentType = null;

        private TypedResource(Resource resource, Type type) {
            this(resource, type, null);
        }

        private TypedResource(Resource resource, Type type, String contentType) {
            if (resource == null) {
                throw new IllegalArgumentException("resource must not be null");
            }
            this.resource = resource;
            if (type == null) {
                throw new IllegalArgumentException("type must not be null");
            }
            this.type = type;
            this.contentType = contentType;
        }

        public Resource getResource() {
            return resource;
        }

        public Type getType() {
            return type;
        }

        public String getContentType() {
            return contentType;
        }
    } // TypedResource

    private static class TypedFileNameMapper implements FileNameMapper {
        private FileNameMapper fileNameMapper;
        private Type type;
        private String contentType;

        private TypedFileNameMapper(FileNameMapper fileNameMapper, Type type) {
            this(fileNameMapper, type, null);
        }

        private TypedFileNameMapper(FileNameMapper fileNameMapper, Type type, String contentType) {
            if (fileNameMapper == null) {
                throw new IllegalArgumentException("fileNameMapper must not be null");
            }
            this.fileNameMapper = fileNameMapper;
            if (type == null) {
                throw new IllegalArgumentException("type must not be null");
            }
            this.type = type;
            this.contentType = contentType;
        }

        public Type getType() {
            return type;
        }

        public String getContentType() {
            return contentType;
        }

        @Override
        public void setFrom(String s) {
            fileNameMapper.setFrom(s);
        }

        @Override
        public void setTo(String s) {
            fileNameMapper.setTo(s);
        }

        @Override
        public String[] mapFileName(String s) {
            return fileNameMapper.mapFileName(s);
        }
    } // TypedFileNameMapper
}
