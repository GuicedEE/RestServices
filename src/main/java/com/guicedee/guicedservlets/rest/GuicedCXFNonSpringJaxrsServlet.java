package com.guicedee.guicedservlets.rest;

import com.google.inject.*;
import com.guicedee.guicedinjection.*;
import com.guicedee.guicedinjection.interfaces.IDefaultService;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.*;
import org.apache.cxf.jaxrs.servlet.*;
import org.apache.cxf.message.Message;

import javax.servlet.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import static com.guicedee.guicedservlets.rest.RESTContext.*;
import static com.guicedee.guicedservlets.rest.services.JaxRsPreStartup.*;

@Singleton
public class GuicedCXFNonSpringJaxrsServlet
		extends CXFNonSpringJaxrsServlet {
	private static final Logger log = com.guicedee.logger.LogFactory.getLog("GuicedCXFNonSpringJaxrsServlet");

	protected List<?> getProviders(ServletConfig servletConfig, String splitChar) throws ServletException {
		String providersList = servletConfig.getServletContext().getInitParameter(providersString);
		if (providersList == null) {
			return Collections.EMPTY_LIST;
		}
		String[] classNames = providersList.split(splitChar);
		List<Object> providers = new ArrayList<>();
		for (String cName : classNames) {
			Map<String, List<String>> props = new HashMap<>();
			String theName = getClassNameAndProperties(cName, props);
			if (theName.length() != 0) {
				Class<?> cls = loadClass(theName);
				providers.add(createSingletonInstance(cls, props, servletConfig));
			}
		}
		return providers;
	}

	protected void setInterceptors(JAXRSServerFactoryBean bean, ServletConfig servletConfig,
								   String paramName,
								   String splitChar) throws ServletException {
		String value = servletConfig.getServletContext().getInitParameter(paramName);
		if (value == null) {
			return;
		}
		String[] values = value.split(splitChar);
		List<Interceptor<? extends Message>> list = new ArrayList<>();
		for (String interceptorVal : values) {
			Map<String, List<String>> props = new HashMap<>();
			String theValue = getClassNameAndProperties(interceptorVal, props);
			if (theValue.length() != 0) {
				try {
					Class<?> intClass = loadClass(theValue, "Interceptor");
					Object object = GuiceContext.get(intClass);
					list.add((Interceptor<? extends Message>) object);
				} catch (ServletException ex) {
					throw ex;
				} catch (Exception ex) {
					log.warning("Interceptor class " + theValue + " can not be created");
					throw new ServletException(ex);
				}
			}
		}
		if (list.size() > 0) {
			if (outInterceptorsString.equals(paramName)) {
				bean.setOutInterceptors(list);
			} else if (outFaultInterceptorsString.equals(paramName)) {
				bean.setOutFaultInterceptors(list);
			} else {
				bean.setInInterceptors(list);
			}
		}
	}


	protected Map<Class<?>, java.util.Map<String, List<String>>> getServiceClasses(ServletConfig servletConfig, boolean modelAvailable, String splitChar) throws
			ServletException {
		String serviceBeans = RESTContext.renderServices(getPathServices());
		String[] classNames = serviceBeans.split(splitChar);
		Map<Class<?>, Map<String, List<String>>> map = new HashMap<>();
		int len$ = classNames.length;
		for (int i$ = 0; i$ < len$; ++i$) {
			String cName = classNames[i$];
			Map<String, List<String>> props = new HashMap<>();
			String theName = this.getClassNameAndProperties(cName, props);
			if (theName.length() != 0) {
				Class<?> cls = this.loadClass(theName);
				map.put(cls, props);
			}
		}
		if (map.isEmpty()) {
			log.warning("No JaxRS Resource Class was found");

		}
		return map;

	}


	@SuppressWarnings("unchecked")

	protected Map<Class<?>, ResourceProvider> getResourceProviders(ServletConfig servletConfig, Map<Class<?>, Map<String, List<String>>> resourceClasses) throws ServletException {
		String scope = servletConfig.getServletContext()
				.getInitParameter("jaxrs.scope");
		if (scope != null && !"singleton".equals(scope) && !"prototype".equals(scope)) {
			throw new ServletException("Only singleton and prototype scopes are supported");
		} else {
			boolean isPrototype = "prototype".equals(scope);
			Map<Class<?>, ResourceProvider> map = new HashMap();

			Set<RestProvidersFilter> filters = IDefaultService.loaderToSet(ServiceLoader.load(RestProvidersFilter.class));
			Map<Class<?>, Map<String, List<String>>> activeResources = new ConcurrentHashMap<>();
			activeResources.putAll(resourceClasses);
			for (RestProvidersFilter<?> filter : filters) {
				activeResources = filter.processResourceList(activeResources);
			}

			for (Map.Entry<Class<?>, Map<String, List<String>>> classMapEntry : activeResources.entrySet()) {
				Map.Entry<Class<?>, Map<String, List<String>>> entry = classMapEntry;
				Class<?> c = entry.getKey();
				map.put(c, isPrototype
						? new PerRequestResourceProvider(c)
						: new SingletonResourceProvider(this.createSingletonInstance(c, entry.getValue(), servletConfig), true));
			}
			return map;
		}
	}


	protected Object createSingletonInstance(Class<?> cls, Map<String, List<String>> props, ServletConfig sc) throws ServletException {
		try {
			return GuiceContext.get(cls);
		} catch (Throwable T) {
			log.log(Level.SEVERE, "Unable to construct instance for cxf", T);
			Object o = null;
			try {
				o = cls.getDeclaredConstructor().newInstance();
				GuiceContext.inject().injectMembers(o);
				return o;
			} catch (Throwable T1) {
				if (o != null) {
					return o;
				}
				log.log(Level.FINE, "Unable to create instance for rest :", T1);
			}
			return null;
		}
	}

	private String getClassNameAndProperties(String cName, Map<String, List<String>> props) {
		String theName = cName.trim();
		int ind = theName.indexOf("(");
		if (ind != -1 && theName.endsWith(")")) {
			props.putAll(parseMapListSequence(theName.substring(ind + 1, theName.length() - 1)));
			theName = theName.substring(0, ind)
					.trim();
		}
		return theName;
	}

	protected Class<?> loadClass(String cName, String classType) throws ServletException {
		if (cName == null)
			return null;
		try {
			Class<?> clazz = GuiceContext.instance()
					.getScanResult()
					.loadClass(cName, true);
			if (clazz == null || Modifier.isAbstract(clazz.getModifiers())) {
				return null;
			}
			return clazz;
		} catch (Throwable e) {
			try {
				Class<?> clazz = Class.forName(cName);
				if (Modifier.isAbstract(clazz.getModifiers())) {
					return null;
				}
				return clazz;
			} catch (Throwable e1) {
				log("Unable to load class - " + cName);
			}
		}
		return null;
	}
}
