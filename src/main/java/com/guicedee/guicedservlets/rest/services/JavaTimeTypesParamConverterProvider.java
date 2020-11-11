package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.json.LocalDateDeserializer;
import com.guicedee.guicedinjection.json.LocalDateTimeDeserializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * ParamConverterProvider for Java 8 JSR 310 Date Time API
 */
@Provider
public class JavaTimeTypesParamConverterProvider
		implements ParamConverterProvider
{

	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations)
	{

		if (rawType.equals(LocalDateTime.class))
		{
			return (ParamConverter<T>) new LocalDateTimeConverter();
		}
		else if (rawType.equals(LocalDate.class))
		{
			return (ParamConverter<T>) new LocalDateConverter();
		}
		else if (rawType.equals(LocalTime.class))
		{
			return (ParamConverter<T>) new LocalTimeConverter();
		}
		else if (rawType.equals(OffsetDateTime.class))
		{
			return (ParamConverter<T>) new OffsetDateTimeConverter();
		}
		else if (rawType.equals(OffsetTime.class))
		{
			return (ParamConverter<T>) new OffsetTimeConverter();
		}
		else if (rawType.equals(ZonedDateTime.class))
		{
			return (ParamConverter<T>) new ZonedDateTimeConverter();
		}
		else
		{
			return null;
		}
	}

	public static class LocalDateTimeConverter
			implements ParamConverter<LocalDateTime>
	{
		@Override
		public LocalDateTime fromString(String value)
		{
			try
			{
				return new LocalDateTimeDeserializer().convert(value);
			}
			catch (Exception parseException)
			{
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(LocalDateTime localDateTime)
		{
			return getFormatter().format(localDateTime);
		}

		protected DateTimeFormatter getFormatter()
		{
			return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		}
	}

	public static class LocalDateConverter
			implements ParamConverter<LocalDate>
	{
		@Override
		public LocalDate fromString(String value)
		{
			try
			{
				return new LocalDateDeserializer().convert(value);
			}
			catch (Exception parseException)
			{
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(LocalDate localDate)
		{
			return getFormatter().format(localDate);
		}

		protected DateTimeFormatter getFormatter()
		{
			return DateTimeFormatter.ISO_LOCAL_DATE;
		}
	}

	public static class LocalTimeConverter
			implements ParamConverter<LocalTime>
	{
		@Override
		public LocalTime fromString(String value)
		{
			try
			{
				return LocalTime.parse(value, getFormatter());
			}
			catch (DateTimeParseException parseException)
			{
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(LocalTime localTime)
		{
			return getFormatter().format(localTime);
		}

		protected DateTimeFormatter getFormatter()
		{
			return DateTimeFormatter.ISO_LOCAL_TIME;
		}
	}

	public static class OffsetDateTimeConverter
			implements ParamConverter<OffsetDateTime>
	{
		@Override
		public OffsetDateTime fromString(String value)
		{
			try
			{
				return OffsetDateTime.parse(value, getFormatter());
			}
			catch (DateTimeParseException parseException)
			{
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(OffsetDateTime offsetDateTime)
		{
			return getFormatter().format(offsetDateTime);
		}

		protected DateTimeFormatter getFormatter()
		{
			return DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		}
	}

	public static class OffsetTimeConverter
			implements ParamConverter<OffsetTime>
	{
		@Override
		public OffsetTime fromString(String value)
		{
			try
			{
				return OffsetTime.parse(value, getFormatter());
			}
			catch (DateTimeParseException parseException)
			{
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(OffsetTime offsetTime)
		{
			return getFormatter().format(offsetTime);
		}

		protected DateTimeFormatter getFormatter()
		{
			return DateTimeFormatter.ISO_OFFSET_TIME;
		}
	}

	public static class ZonedDateTimeConverter
			implements ParamConverter<ZonedDateTime>
	{
		@Override
		public ZonedDateTime fromString(String value)
		{
			try
			{
				return ZonedDateTime.parse(value, getFormatter());
			}
			catch (DateTimeParseException parseException)
			{
				throw new IllegalArgumentException(parseException);
			}
		}

		@Override
		public String toString(ZonedDateTime zonedDateTime)
		{
			return getFormatter().format(zonedDateTime);
		}

		protected DateTimeFormatter getFormatter()
		{
			return DateTimeFormatter.ISO_ZONED_DATE_TIME;
		}
	}
}
