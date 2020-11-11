package com.guicedee.guicedservlets.rest.implementations;

import com.google.common.net.MediaType;
import com.guicedee.guicedinjection.pairing.Pair;
import com.guicedee.logger.LogFactory;
import org.apache.commons.io.IOUtils;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.logging.Level;

import static java.nio.charset.StandardCharsets.*;

@Provider
@Produces("application/xml")
@Consumes("application/xml")
public class JAXBMarshaller
		implements MessageBodyWriter, MessageBodyReader
{
	public static boolean pretty = true;
	public static boolean fragment = true;

	@Override
	public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType)
	{
		return true;
	}

	@Override
	public void writeTo(Object o, Class type, Type genericType, Annotation[] annotations,
	                    jakarta.ws.rs.core.MediaType mediaType,
	                    MultivaluedMap httpHeaders,
	                    OutputStream entityStream) throws IOException, WebApplicationException
	{
		entityStream.write(toXml(o).getBytes());
	}

	/**
	 * Writes any object as XML
	 *
	 * @return The XML Generated
	 */
	@SuppressWarnings("unchecked")
	public String toXml(Object requestObject)
	{
		StringWriter stringWriter = new StringWriter();
		try
		{
			JAXBContext context = null;
			context = JAXBContext.newInstance(requestObject.getClass());
			if (requestObject instanceof Pair)
			{
				Pair p = (Pair) requestObject;
				Class<?> keyType = p.getKey()
				                    .getClass();
				Class<?> valueType = p.getValue()
				                      .getClass();
				context = JAXBContext.newInstance(requestObject.getClass(), keyType, valueType);
			}
			JAXBIntrospector introspector = context.createJAXBIntrospector();
			Marshaller marshaller = context.createMarshaller();
			if (null == introspector.getElementName(requestObject))
			{

				JAXBElement jaxbElement = new JAXBElement(new QName(requestObject.getClass()
				                                                                 .getSimpleName()),
				                                          requestObject.getClass(), requestObject);
				marshaller.setProperty("com.sun.xml.bind.xmlDeclaration", Boolean.FALSE);
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
				marshaller.marshal(jaxbElement, stringWriter);
			}
			else
			{
				marshaller.marshal(requestObject, stringWriter);
			}
			return stringWriter.toString();
		}
		catch (Exception e)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to marshal string writer from log intercepter", e);
			return "";
		}
		finally
		{
			try
			{
				stringWriter.close();
			}
			catch (IOException e)
			{
				LogFactory.getLog("IXmlRepresentation")
				          .log(Level.SEVERE, "Unable to close marshalling string writer from log intercepter", e);
			}
		}
	}

	public long getSize(Object obj, Class<?> type, Type genericType,
	                    Annotation[] annotations, MediaType mediaType)
	{
		return -1;
	}

	@Override
	public boolean isReadable(Class type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType)
	{
		return true;
	}

	@Override
	public Object readFrom(Class type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
	{
		String xml = IOUtils.toString(entityStream, UTF_8);
		return fromXml(xml, type);
	}

	/**
	 * Reads any XML into any Object.
	 * Returns null if error occured
	 *
	 * @param xml
	 * 		The XML String to transform
	 * @param type
	 * 		The type to transform into
	 *
	 * @return The object or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T fromXml(String xml, Class<T> type)
	{
		try
		{
			T instance = type.getDeclaredConstructor()
			                 .newInstance();
			JAXBContext context = null;
			context = JAXBContext.newInstance(type);
			JAXBIntrospector introspector = context.createJAXBIntrospector();
			Unmarshaller unmarshaller = context.createUnmarshaller();
			if (null == introspector.getElementName(instance))
			{
				XMLInputFactory factory = XMLInputFactory.newFactory();
				factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
				factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

				XMLStreamReader streamReader = factory.createXMLStreamReader(
						new StringReader(xml));
				JAXBElement<T> customer = unmarshaller.unmarshal(streamReader, type);
				instance = customer.getValue();
			}
			else
			{
				instance = (T) unmarshaller.unmarshal(new StringReader(xml));
			}
			return instance;
		}
		catch (IllegalAccessException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to IllegalAccessException ", T);
		}
		catch (IllegalArgumentException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to IllegalArgumentException ", T);
		}
		catch (InstantiationException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to InstantiationException ", T);
		}
		catch (NoSuchMethodException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to NoSuchMethodException ", T);
		}
		catch (SecurityException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to SecurityException ", T);
		}
		catch (InvocationTargetException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to InvocationTargetException ", T);
		}
		catch (JAXBException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to JAXBException ", T);
		}
		catch (XMLStreamException T)
		{
			LogFactory.getLog("IXmlRepresentation")
			          .log(Level.SEVERE, "Unable to XMLStreamException ", T);
		}
		return null;
	}
}