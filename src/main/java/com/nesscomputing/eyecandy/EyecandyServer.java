package com.nesscomputing.eyecandy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.servlet.ServletModule;
import com.nesscomputing.config.Config;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.jms.JmsEventModule;
import com.nesscomputing.eyecandy.consumer.Consumer;
import com.nesscomputing.eyecandy.consumer.GeoConverter;
import com.nesscomputing.eyecandy.servlet.GeoDataServlet;
import com.nesscomputing.galaxy.GalaxyConfigModule;
import com.nesscomputing.httpserver.HttpServerHandlerBinder;
import com.nesscomputing.httpserver.HttpServerModule;
import com.nesscomputing.httpserver.jetty.StaticResourceHandlerProvider;
import com.nesscomputing.httpserver.standalone.StandaloneServer;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.jmx.starter.guice.JmxStarterModule;

public class EyecandyServer extends StandaloneServer
{
    public static void main(String[] args)
    {
        final EyecandyServer server = new EyecandyServer();
        server.startServer();
    }

    @Override
    protected Module getMainModule(final Config config)
    {
        return new AbstractModule() {
            @Override
            public void configure() {
                binder().requireExplicitBindings();
                binder().disableCircularProxies();

                install(new JmxStarterModule(config));

                install(new GalaxyConfigModule());

                install(new NessJacksonModule());

                install(new NessEventModule());
                install(new JmsEventModule(config));

                NessEventModule.bindEventReceiver(binder()).to(EyecandyReceiver.class).asEagerSingleton();

                bind(new TypeLiteral<BlockingQueue<NessEvent>>() {}).toInstance( new LinkedBlockingQueue<NessEvent>(200));
                bind(Consumer.class).asEagerSingleton();
                bind(GeoConverter.class).asEagerSingleton();
                bind(GeoDataServlet.class).in(Scopes.SINGLETON);


                final ContextHandler ch = new ContextHandler();
                ch.setContextPath("");
                ch.setResourceBase("/home/henning/trumpet/source/henning/eyecandy/src/main/webapp");
                ch.setHandler(new ResourceHandler());

                HttpServerHandlerBinder.bindHandler(binder()).toProvider(new StaticResourceHandlerProvider("")).in(Scopes.SINGLETON);

                install (new ServletModule() {
                    @Override
                    public void configureServlets() {
                        serve("/eyecandy/geo").with(GeoDataServlet.class);
                    }
                });

                install (new HttpServerModule(config));
            }
        };
    }
}
