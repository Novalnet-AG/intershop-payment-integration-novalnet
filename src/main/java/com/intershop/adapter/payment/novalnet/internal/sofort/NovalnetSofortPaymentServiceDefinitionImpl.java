package com.intershop.adapter.payment.novalnet.internal.sofort;

import com.intershop.adapter.payment.novalnet.internal.NovalnetPaymentServiceDefinitionImpl;
import com.intershop.component.service.capi.assignment.ServiceProvider;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetSofortPaymentServiceDefinitionImpl extends NovalnetPaymentServiceDefinitionImpl
{
    @Override
    public ServiceProvider getServiceProvider(ServiceConfigurationBO serviceConfigurationBO)
    {
        return new PaymentServiceProvider(new NovalnetSofortPaymentServiceImpl(serviceConfigurationBO));
    }
}