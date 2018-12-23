package com.jwebmp.guiced.rest.implementations;

import com.jwebmp.guicedinjection.GuiceContext;
import com.jwebmp.logger.LogFactory;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import javax.servlet.ServletContext;

public class RestEasyUndertowServletExtension
		implements ServletExtension
{
	@SuppressWarnings("unchecked")
	@Override
	public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext)
	{
		LogFactory.getLog("RestEasyUndertowRegistration")
		          .config("Registering Rest Easy Servlet Context Listener");
		InstanceFactory guiceInstanceFactory = new ImmediateInstanceFactory<>(GuiceContext.get(GuiceResteasyBootstrapServletContextListener.class));
		deploymentInfo.addListener(new ListenerInfo(GuiceResteasyBootstrapServletContextListener.class, guiceInstanceFactory));
	}
}
