package com.guicedee.guicedservlets.rest.annotations;

import org.apache.cxf.interceptor.Interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE,ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestInInterceptor
{
	Class<? extends Interceptor<?>> value();
}
