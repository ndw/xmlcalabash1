package com.xmlcalabash.drivers;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.piperack.PiperackApplication;
import com.xmlcalabash.util.ParseArgs;
import com.xmlcalabash.util.UserArgs;
import net.sf.saxon.s9api.SaxonApiException;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * Ths file is part of XMLCalabash.
 * Created by ndw on 10/25/13.
 */
public class Piperack {
    public static void main(String[] args) throws Exception {
        UserArgs userArgs = null;
        try {
            userArgs = new ParseArgs().parsePiperack(args);
        } catch (XProcException xe) {
            System.err.println(xe.getMessage());
            System.exit(-1);
        }

        XProcConfiguration config = null;
        XProcRuntime runtime = null;

        try {
            config = userArgs.createConfiguration();
            runtime = new XProcRuntime(config);
        } catch (SaxonApiException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        if (userArgs.isShowVersion()) {
            System.out.println("Piperack, a web server for running XProc pipelines.");
            XProcConfiguration.showVersion(runtime);
        }

        // Create a component
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, config.piperackPort);

        // Create an application
        PiperackApplication application = new PiperackApplication(config, runtime);

        // Attach the application to the component and start it
        component.getDefaultHost().attach(application);
        component.start();

        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // nop
            }

            // It doesn't seem like this should be necessary!
            if (application.stopped()) {
                System.exit(0);
            }

            application.expirePipelines();
        }
    }
}
