package com.guicedee.guicedservlets.rest.test;

public class DefaultGreeter implements Greeter
{
	public String greet(final String name)
	{
		return "Hello " + name;
	}
}
