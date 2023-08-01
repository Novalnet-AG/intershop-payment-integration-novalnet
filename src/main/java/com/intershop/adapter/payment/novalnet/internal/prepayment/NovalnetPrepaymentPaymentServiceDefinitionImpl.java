package com.intershop.adapter.payment.novalnet.internal.prepayment;

import com.intershop.adapter.payment.novalnet.internal.NovalnetPaymentServiceDefinitionImpl;
import com.intershop.component.service.capi.assignment.ServiceProvider;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetPrepaymentPaymentServiceDefinitionImpl extends NovalnetPaymentServiceDefinitionImpl
{
    @Override
    public ServiceProvider getServiceProvider(ServiceConfigurationBO serviceConfigurationBO)
    {
        return new PaymentServiceProvider(new NovalnetPrepaymentPaymentServiceImpl(serviceConfigurationBO));
    }
}