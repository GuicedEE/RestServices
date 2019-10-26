package com.guicedee.guiced.rest;

import com.google.inject.AbstractModule;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;

@SuppressWarnings("PointlessBinding")
public class RestTestBinding
		extends AbstractModule
		implements IGuiceModule<RestTestBinding>
{

	@Override
	protected void configure()
	{
		bind(HelloResource.class);
		bind(Greeter.class).to(DefaultGreeter.class);
	}
}
