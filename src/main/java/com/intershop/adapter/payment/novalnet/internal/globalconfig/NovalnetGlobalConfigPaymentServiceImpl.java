package com.intershop.adapter.payment.novalnet.internal.globalconfig;

import java.util.Collection;
import java.util.Collections;

import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.PaymentService;
import com.intershop.api.service.payment.v1.capability.PaymentCapability;
import com.intershop.api.service.payment.v1.result.ApplicabilityResult;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetGlobalConfigPaymentServiceImpl implements PaymentService
{
    
    private final static String SERVICE_ID = "NOVALNET_MERCHANT_CONFIG";
    
    public NovalnetGlobalConfigPaymentServiceImpl(ServiceConfigurationBO serviceConfigurationBO)
    {
    
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PaymentCapability> T getCapability(Class<T> capability)
    {
        return null;
    }

    @Override
    public String getID()
    {
         return SERVICE_ID;
    }

    @Override
    public Result<ApplicabilityResult> getApplicability(Payable payable)
    {
        ApplicabilityResult applicability =  new ApplicabilityResult();
        Result<ApplicabilityResult> result = new Result<>(applicability);
        result.setState(ApplicabilityResult.NOT_APPLICABLE);
        return result;
    }

    @Override
    public Collection<Class<?>> getPaymentParameterDescriptors(PaymentContext arg0)
    {
        return Collections.emptyList();
    }

}