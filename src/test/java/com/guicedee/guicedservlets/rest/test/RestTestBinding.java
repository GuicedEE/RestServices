package com.guicedee.guicedservlets.rest.test;

import com.google.inject.AbstractModule;
import com.guicedee.client.services.lifecycle.IGuiceModule;

@SuppressWarnings("PointlessBinding")
public class RestTestBinding
		extends AbstractModule
		implements IGuiceModule<RestTestBinding>
{

	@Override
	protected void configure()
	{
		bind(Greeter.class).to(DefaultGreeter.class);
	}
}
