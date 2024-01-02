package com.guicedee.guicedservlets.rest.implementations;

import com.google.inject.Singleton;
import com.guicedee.guicedinjection.GuiceContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.core.Application;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ClassLoaderUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Invoker;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Log
@Singleton
public class GuicedCXFNonSpringJaxrsServlet
				extends CXFNonSpringJaxrsServlet
{
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		//System.out.println("init");
		Enumeration<String> initParameterNames = config.getInitParameterNames();
		//this.key1 = config.getInitParameter("key1");
		//this.key2 = config.getInitParameter("key2");
	}
	
	
	private static final Logger LOG = Logger.getLogger("GuicedCXFJaxRS");
	
	private static final String USER_MODEL_PARAM = "user.model";
	private static final String SERVICE_ADDRESS_PARAM = "jaxrs.address";
	private static final String IGNORE_APP_PATH_PARAM = "jaxrs.application.address.ignore";
	private static final String SERVICE_CLASSES_PARAM = "jaxrs.serviceClasses";
	private static final String PROVIDERS_PARAM = "jaxrs.providers";
	private static final String FEATURES_PARAM = "jaxrs.features";
	private static final String OUT_INTERCEPTORS_PARAM = "jaxrs.outInterceptors";
	private static final String OUT_FAULT_INTERCEPTORS_PARAM = "jaxrs.outFaultInterceptors";
	private static final String IN_INTERCEPTORS_PARAM = "jaxrs.inInterceptors";
	private static final String INVOKER_PARAM = "jaxrs.invoker";
	private static final String SERVICE_SCOPE_PARAM = "jaxrs.scope";
	private static final String EXTENSIONS_PARAM = "jaxrs.extensions";
	private static final String LANGUAGES_PARAM = "jaxrs.languages";
	private static final String PROPERTIES_PARAM = "jaxrs.properties";
	private static final String SCHEMAS_PARAM = "jaxrs.schemaLocations";
	private static final String DOC_LOCATION_PARAM = "jaxrs.documentLocation";
	private static final String STATIC_SUB_RESOLUTION_PARAM = "jaxrs.static.subresources";
	private static final String SERVICE_SCOPE_SINGLETON = "singleton";
	private static final String SERVICE_SCOPE_REQUEST = "prototype";
	
	private static final String PARAMETER_SPLIT_CHAR = "class.parameter.split.char";
	private static final String DEFAULT_PARAMETER_SPLIT_CHAR = ",";
	private static final String SPACE_PARAMETER_SPLIT_CHAR = "space";
	
	private static final String JAXRS_APPLICATION_PARAM = "jakarta.ws.rs.Application";
	
	private ClassLoader classLoader;
	private Application application;
	
	public GuicedCXFNonSpringJaxrsServlet()
	{
	
	}
	
	public GuicedCXFNonSpringJaxrsServlet(Application app)
	{
		this.application = app;
	}
	
	public GuicedCXFNonSpringJaxrsServlet(Object singletonService)
	{
		super(Collections.singleton(singletonService));
	}
	
	private String getClassNameAndProperties(String cName, Map<String, List<String>> props)
	{
		String theName = cName.trim();
		int ind = theName.indexOf('(');
		if (ind != -1 && theName.endsWith(")"))
		{
			props.putAll(parseMapListSequence(theName.substring(ind + 1, theName.length() - 1)));
			theName = theName.substring(0, ind).trim();
		}
		return theName;
	}
	
	private void injectProperties(Object instance, Map<String, List<String>> props)
	{
		if (props == null || props.isEmpty())
		{
			return;
		}
		Method[] methods = instance.getClass().getMethods();
		Map<String, Method> methodsMap = new HashMap<>();
		for (Method m : methods)
		{
			methodsMap.put(m.getName(), m);
		}
		for (Map.Entry<String, List<String>> entry : props.entrySet())
		{
			Method m = methodsMap.get("set" + StringUtils.capitalize(entry.getKey()));
			if (m != null)
			{
				Class<?> type = m.getParameterTypes()[0];
				Object value;
				if (InjectionUtils.isPrimitive(type))
				{
					value = PrimitiveUtils.read(entry.getValue().get(0), type);
				} else
				{
					value = entry.getValue();
				}
				InjectionUtils.injectThroughMethod(instance, m, value);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void setInterceptors(JAXRSServerFactoryBean bean, ServletConfig servletConfig,
	                               String paramName,
	                               String splitChar) throws ServletException
	{
		String value = servletConfig.getInitParameter(paramName);
		if (value == null)
		{
			return;
		}
		String[] values = value.split(splitChar);
		List<Interceptor<? extends Message>> list = new ArrayList<>();
		for (String interceptorVal : values)
		{
			Map<String, List<String>> props = new HashMap<>();
			String theValue = getClassNameAndProperties(interceptorVal, props);
			if (!theValue.isEmpty())
			{
				try
				{
					Class<?> intClass = loadClass(theValue, "Interceptor");
					Object object = GuiceContext.get(intClass);// intClass.getDeclaredConstructor().newInstance();
					injectProperties(object, props);
					list.add((Interceptor<? extends Message>) object);
				} catch (ServletException ex)
				{
					throw ex;
				} catch (Exception ex)
				{
					LOG.warning("Interceptor class " + theValue + " can not be created");
					throw new ServletException(ex);
				}
			}
		}
		if (!list.isEmpty())
		{
			if (OUT_INTERCEPTORS_PARAM.equals(paramName))
			{
				bean.setOutInterceptors(list);
			} else if (OUT_FAULT_INTERCEPTORS_PARAM.equals(paramName))
			{
				bean.setOutFaultInterceptors(list);
			} else
			{
				bean.setInInterceptors(list);
			}
		}
	}
	
	protected void setInvoker(JAXRSServerFactoryBean bean, ServletConfig servletConfig)
					throws ServletException
	{
		String value = servletConfig.getInitParameter(INVOKER_PARAM);
		if (value == null)
		{
			return;
		}
		Map<String, List<String>> props = new HashMap<>();
		String theValue = getClassNameAndProperties(value, props);
		if (!theValue.isEmpty())
		{
			try
			{
				Class<?> intClass = loadClass(theValue, "Invoker");
				Object object = GuiceContext.get(intClass);// intClass.getDeclaredConstructor().newInstance();
				injectProperties(object, props);
				bean.setInvoker((Invoker) object);
			} catch (ServletException ex)
			{
				throw ex;
			} catch (Exception ex)
			{
				LOG.warning("Invoker class " + theValue + " can not be created");
				throw new ServletException(ex);
			}
		}
	}
	
	protected Object createSingletonInstance(Class<?> cls, Map<String, List<String>> props, ServletConfig sc)
					throws ServletException
	{
		boolean isApplication = Application.class.isAssignableFrom(cls);
		try
		{
			Object injectedInstance = GuiceContext.get(cls);
			final ProviderInfo<? extends Object> provider;
			if (isApplication)
			{
				provider = new ApplicationInfo((Application) injectedInstance, getBus());
			} else
			{
				provider = new ProviderInfo<>(injectedInstance, getBus(), false, true);
			}
			Object instance = provider.getProvider();
			injectProperties(instance, props);
			configureSingleton(instance);
			return isApplication ? provider : instance;
		} catch (RuntimeException ex)
		{
			try
			{
				return super.createSingletonInstance(cls, props, sc);
			}catch (ServletException se)
			{
				log.log(Level.WARNING,"Unable to create instance of singleton - ",se);
				return null;
			}
		}
	}
	
	
	protected Class<?> loadApplicationClass(String appClassName) throws ServletException
	{
		return loadClass(appClassName, "Application");
	}
	
	protected Class<?> loadClass(String cName) throws ServletException
	{
		return loadClass(cName, "Resource");
	}
	
	protected Class<?> loadClass(String cName, String classType) throws ServletException
	{
		try
		{
			final Class<?> cls;
			if (classLoader == null)
			{
				try
				{
					cls = GuiceContext.instance().getScanResult().loadClass(cName, false);// ClassLoaderUtils.loadClass(cName, CXFNonSpringJaxrsServlet.class);
				} catch (IllegalArgumentException aer)
				{
					return super.loadClass(cName, classType);
				}
			} else
			{
				cls = classLoader.loadClass(cName);
			}
			return cls;
		} catch (ClassNotFoundException ex)
		{
			throw new ServletException("No " + classType + " class " + cName.trim() + " can be found", ex);
		}
	}
}
