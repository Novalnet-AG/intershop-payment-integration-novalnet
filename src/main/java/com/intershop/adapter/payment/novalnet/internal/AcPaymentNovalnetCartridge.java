package com.intershop.adapter.payment.novalnet.internal;

import com.intershop.beehive.core.capi.cartridge.Cartridge;
import com.intershop.beehive.core.capi.environment.LifecycleListenerException;

public class AcPaymentNovalnetCartridge extends Cartridge
{
    /**
     * The constructor.
     */
    public AcPaymentNovalnetCartridge()
    { 
        super();
    }

    /**
     * Returns the unique name of a cartridge (for internal lookup).
     *
     * @return the unique name
     */
    @Override
    public String getName()
    {
        return "ac_payment_novalnet";
    }

    /**
     * Inside this method all-important startups must be performed.
     *
     * @exception LifecycleListenerException
     *              This exception is thrown to report severe errors to the
     *              cartridge processor.
     */
    @Override
    public void onStartupHook() throws LifecycleListenerException
    {        
        super.onStartupHook();
    }

}
