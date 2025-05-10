package org.mendeley;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mendeley.controller.PredictHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import java.io.File;

public class WebApp {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        // Serve static files from resources/public
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        String resourceBase = System.getProperty("user.dir") + "/app/src/main/resources/public";
        File indexFile = new File(resourceBase, "index.html");
        if (!indexFile.exists()) {
            System.err.println("Error: index.html not found at " + indexFile.getAbsolutePath());
        } else {
            System.out.println("index.html found at " + indexFile.getAbsolutePath());
        }
        resourceHandler.setResourceBase(resourceBase);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});

        ContextHandler staticContext = new ContextHandler("/");
        staticContext.setHandler(resourceHandler);

        // Servlet context for /api/predict
        ServletContextHandler apiContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        apiContext.setContextPath("/api");
        apiContext.addServlet(new ServletHolder(new PredictHandler()), "/predict");

        // Combine static and API
        ContextHandlerCollection handlers = new ContextHandlerCollection();
        handlers.addHandler(staticContext);
        handlers.addHandler(apiContext);

        server.setHandler(handlers);
        server.start();
        System.out.println("Server started at http://localhost:8080");
        server.join();
    }
}
