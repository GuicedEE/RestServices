package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator;
import io.undertow.servlet.api.DeploymentInfo;

import java.util.Set;

import static com.guicedee.guicedservlets.rest.RESTContext.*;
import static com.guicedee.guicedservlets.rest.services.JaxRsPreStartup.*;

public class JaxRSUndertowDeploymentConfigurator implements UndertowDeploymentConfigurator {

	@Override
	public DeploymentInfo configure(DeploymentInfo deploymentInfo) {

		if (!runSeparately) {
			deploymentInfo.addServletContextAttribute(serviceClassesString, buildInitParamFromSet(getPathServices()));
			deploymentInfo.addServletContextAttribute(providersString, buildInitParamFromSet(getProviders()));
			deploymentInfo.addServletContextAttribute(inInterceptorsString, buildInitParamFromSet(getInInterceptors()));
			deploymentInfo.addServletContextAttribute(outInterceptorsString, buildInitParamFromSet(getOutInterceptors()));
			deploymentInfo.addServletContextAttribute(propertiesString, buildInitParamFromSet(getProperties()));
			deploymentInfo.addServletContextAttribute(applicationsString, buildInitParamFromSet(getApplications()));

			deploymentInfo.addInitParameter(serviceClassesString, buildInitParamFromSet(getPathServices()));
			deploymentInfo.addInitParameter(providersString, buildInitParamFromSet(getProviders()));
			deploymentInfo.addInitParameter(inInterceptorsString, buildInitParamFromSet(getInInterceptors()));
			deploymentInfo.addInitParameter(outInterceptorsString, buildInitParamFromSet(getOutInterceptors()));
			deploymentInfo.addInitParameter(propertiesString, buildInitParamFromSet(getProperties()));
			deploymentInfo.addInitParameter(applicationsString, buildInitParamFromSet(getApplications()));
		}
		return deploymentInfo;
	}

	private String buildInitParamFromSet(Set<String> values) {
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			sb.append(value)
			  .append(",");
		}
		if (!values.isEmpty()) { sb.deleteCharAt(sb.length() - 1); }

		return sb.toString();
	}

}
