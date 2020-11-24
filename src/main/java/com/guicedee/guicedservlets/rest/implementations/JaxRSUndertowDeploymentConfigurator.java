package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.guicedservlets.rest.RESTContext;
import com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator;
import io.undertow.servlet.api.DeploymentInfo;

import java.util.Set;

import static com.guicedee.guicedservlets.rest.RESTContext.*;
import static com.guicedee.guicedservlets.rest.services.JaxRsPreStartup.*;

public class JaxRSUndertowDeploymentConfigurator
		implements UndertowDeploymentConfigurator
{

	@Override
	public DeploymentInfo configure(DeploymentInfo deploymentInfo)
	{
		deploymentInfo.addServletContextAttribute(serviceClassesString, renderServices(getPathServices()));
		deploymentInfo.addServletContextAttribute(providersString, renderServices(getProviders()));
		deploymentInfo.addServletContextAttribute(inInterceptorsString, renderServices(getInInterceptors()));
		deploymentInfo.addServletContextAttribute(outInterceptorsString, renderServices(getOutInterceptors()));
		deploymentInfo.addServletContextAttribute(outFaultInterceptorsString, renderServices(getOutFaultInterceptors()));
		deploymentInfo.addServletContextAttribute(propertiesString, renderServices(getProperties()));
		deploymentInfo.addServletContextAttribute(applicationsString, renderServices(getApplications()));
		deploymentInfo.addServletContextAttribute(featuresString, renderServices(getFeatures()));

		deploymentInfo.addInitParameter(serviceClassesString, renderServices(getPathServices()));
		deploymentInfo.addInitParameter(providersString, renderServices(getProviders()));
		deploymentInfo.addInitParameter(inInterceptorsString, renderServices(getInInterceptors()));
		deploymentInfo.addInitParameter(outInterceptorsString, renderServices(getOutInterceptors()));
		deploymentInfo.addInitParameter(outFaultInterceptorsString, renderServices(getOutFaultInterceptors()));
		deploymentInfo.addInitParameter(propertiesString, renderServices(getProperties()));
		deploymentInfo.addInitParameter(applicationsString, renderServices(getApplications()));
		deploymentInfo.addInitParameter(featuresString, renderServices(getFeatures()));

		return deploymentInfo;
	}
}
