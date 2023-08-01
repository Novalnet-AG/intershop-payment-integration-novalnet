package com.intershop.adapter.payment.novalnet.internal.postfinancecard;

import java.util.Collection;
import java.util.Collections;

import com.intershop.adapter.payment.novalnet.internal.NovalnetPaymentHandler;
import com.intershop.adapter.payment.novalnet.internal.NovalnetPaymentHostedPaymentPageImpl;
import com.intershop.adapter.payment.novalnet.internal.NovalnetPaymentNotificationRoutingImpl;
import com.intershop.adapter.payment.novalnet.internal.NovalnetRefundCapabilityImpl;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.PaymentService;
import com.intershop.api.service.payment.v1.capability.HostedPaymentPage;
import com.intershop.api.service.payment.v1.capability.NotificationRouting;
import com.intershop.api.service.payment.v1.capability.PaymentCapability;
import com.intershop.api.service.payment.v1.capability.RedirectAfterCheckout;
import com.intershop.api.service.payment.v1.capability.Refund;
import com.intershop.api.service.payment.v1.result.ApplicabilityResult;
import com.intershop.beehive.core.capi.naming.NamingMgr;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetPostfinancecardPaymentServiceImpl implements PaymentService
{
    
    private final static String SERVICE_ID = "NOVALNET_POSTFINANCECARD";
    private RedirectAfterCheckout redirectAfterCheckoutCapability;
    private NotificationRouting notificationRouting;
    private ServiceConfigurationBO serviceConfigurationBO;
    private NovalnetPaymentHostedPaymentPageImpl hostedPaymentPage;
    private Refund refundcapability;
    
    public NovalnetPostfinancecardPaymentServiceImpl(ServiceConfigurationBO serviceConfigurationBO)
    {
        redirectAfterCheckoutCapability = new NovalnetPostfinancecardRedirectAfterCheckoutCapabilityImpl(serviceConfigurationBO, this.SERVICE_ID);
        NamingMgr.injectMembers(redirectAfterCheckoutCapability);
        
        notificationRouting = new NovalnetPaymentNotificationRoutingImpl(serviceConfigurationBO, this.SERVICE_ID);
        NamingMgr.injectMembers(notificationRouting);
        
        hostedPaymentPage = new NovalnetPaymentHostedPaymentPageImpl(serviceConfigurationBO, this.SERVICE_ID);
        NamingMgr.injectMembers(hostedPaymentPage);
        
        refundcapability = new NovalnetRefundCapabilityImpl();
        NamingMgr.injectMembers(refundcapability);
        
        this.serviceConfigurationBO = serviceConfigurationBO;
    
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PaymentCapability> T getCapability(Class<T> capability)
    {
        if (capability.isAssignableFrom(RedirectAfterCheckout.class))
        {
            return (T)redirectAfterCheckoutCapability;
        }
        if (capability.isAssignableFrom(NotificationRouting.class))
        {
            return (T)notificationRouting;
        }
        if (capability.isAssignableFrom(HostedPaymentPage.class))
        {
            return (T)hostedPaymentPage;
        }
        if (capability.isAssignableFrom(Refund.class))
        {
            return (T)refundcapability;
        }
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
        result.setState(ApplicabilityResult.APPLICABLE);
        if(!(NovalnetPaymentHandler.displayPaymentMethod(this.serviceConfigurationBO))) {
            result.setState(ApplicabilityResult.NOT_APPLICABLE);            
        }
        return result;
    }

    @Override
    public Collection<Class<?>> getPaymentParameterDescriptors(PaymentContext arg0)
    {
        return Collections.emptyList();
    }

}