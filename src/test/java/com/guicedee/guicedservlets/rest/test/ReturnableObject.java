package com.guicedee.guicedservlets.rest.test;

public class ReturnableObject
{
	private String name;
	public ReturnableObject()
	{
	}

	public String getName()
	{
		return name;
	}

	public ReturnableObject setName(String name)
	{
		this.name = name;
		return this;
	}
}
