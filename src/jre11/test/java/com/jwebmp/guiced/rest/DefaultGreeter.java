package com.guicedee.jpms.guiced.rest;

public class DefaultGreeter implements Greeter
{
	public String greet(final String name)
	{
		return "Hello " + name;
	}
}
