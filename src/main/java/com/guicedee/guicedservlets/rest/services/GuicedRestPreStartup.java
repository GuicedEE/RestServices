package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import com.zandero.rest.RestRouter;

public class GuicedRestPreStartup implements IGuicePreStartup<GuicedRestPreStartup>
{
    @Override
    public void onStartup()
    {
        RestRouter.injectWith(new GuiceRestInjectionProvider());
    }
}
