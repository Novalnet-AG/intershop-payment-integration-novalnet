package com.intershop.adapter.payment.novalnet.internal;

import java.util.HashMap;
import java.util.Map;

import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.HostedPaymentPage;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetPaymentHostedPaymentPageImpl implements HostedPaymentPage
{
    private ServiceConfigurationBO serviceConfigurationBO;
    private String serviceID;
    
    public NovalnetPaymentHostedPaymentPageImpl(ServiceConfigurationBO serviceConfigurationBO, String serviceID)
    {
        this.serviceConfigurationBO = serviceConfigurationBO;
        this.serviceID = serviceID;
    }

    @Override
    public Map<String, Object> getContent(PaymentContext context, Payable payable)
    {
        Map<String, Object> paymentData = new HashMap<>();
        String paymentType = NovalnetUtil.getNovalnetPaymentType(serviceID);
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, paymentType);
        Integer testMode = 0;
        if(paymentConfiguration.get("testMode") != null && (Boolean)paymentConfiguration.get("testMode") == true) {
            testMode = 1;
        }

        paymentData.put(serviceID + ".testMode", testMode);
        if(!NovalnetUtil.nnIsEmpty(paymentConfiguration.get("additionalNote"))) {
            paymentData.put(serviceID + ".additionalNote", paymentConfiguration.get("additionalNote").toString());
        }
        
        return paymentData;
    }

}
