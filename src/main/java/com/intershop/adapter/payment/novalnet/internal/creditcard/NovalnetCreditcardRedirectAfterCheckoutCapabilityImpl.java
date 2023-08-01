package com.intershop.adapter.payment.novalnet.internal.creditcard;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.google.inject.Inject;
import com.intershop.adapter.payment.novalnet.internal.NovalnetPaymentHandler;
import com.intershop.adapter.payment.novalnet.internal.NovalnetUtil;
import com.intershop.adapter.payment.novalnet.internal.NovalnetWebhookUtil;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.EnumRedirectResultStatus;
import com.intershop.api.service.payment.v1.capability.RedirectAfterCheckout;
import com.intershop.api.service.payment.v1.result.AuthorizationResult;
import com.intershop.api.service.payment.v1.result.CallbackResult;
import com.intershop.api.service.payment.v1.result.RedirectAfterCheckoutCallbackResult;
import com.intershop.api.service.payment.v1.result.RedirectURLCreationResult;
import com.intershop.beehive.core.capi.currency.CurrencyMgr;
import com.intershop.beehive.core.capi.localization.LocaleMgr;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.request.Request;
import com.intershop.beehive.core.capi.url.URLComposition;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetCreditcardRedirectAfterCheckoutCapabilityImpl implements RedirectAfterCheckout 
{
    
    private ServiceConfigurationBO serviceConfigurationBO;
    private String serviceID;
    @Inject private LocaleMgr localeMgr;
    @Inject private CurrencyMgr currencyMgr;
    @Inject private URLComposition urlComposition;
    
    @Inject
    public LocalizationProvider localizationProvider;

    public NovalnetCreditcardRedirectAfterCheckoutCapabilityImpl(ServiceConfigurationBO serviceConfigurationBO, String serviceID)
    {
        this.serviceConfigurationBO = serviceConfigurationBO;
        this.serviceID = serviceID;
    }
    @Override
    public boolean isCapturingAuthorization(PaymentContext context, Payable payable)
    {
        return NovalnetPaymentHandler.nnIsCapturingAuthorization(serviceID, serviceConfigurationBO, payable);
    }
    
    @Override
    public Result<RedirectURLCreationResult> createRedirectURL(PaymentContext context, Payable payable, URI successURL, URI cancelURL, URI failureURL)
    { 
        RedirectURLCreationResult creationResult = new RedirectURLCreationResult();
        Result<RedirectURLCreationResult> result = new Result<>(creationResult);
        result.setState(Result.SUCCESS);
        
        Map<String, String> urlCreationResult = NovalnetPaymentHandler.getRedirectUrl(context, payable, serviceConfigurationBO, localeMgr, currencyMgr, urlComposition, Request.getCurrent(), this.serviceID, successURL, failureURL);
        String status = urlCreationResult.get("status");
        
        if(status == "FAILURE" || NovalnetUtil.nnIsEmpty(urlCreationResult.get("redirect_url"))) {
            result.setState(Result.FAILURE);
            return result;
        }
       
        try
        {
            creationResult.setRedirectURL(new URI(urlCreationResult.get("redirect_url")));
        }
        catch(URISyntaxException e)
        {
            result.setState(Result.FAILURE);
            result.addError("TechnicalError", e.getMessage());
        }
        
        if(!(NovalnetUtil.nnIsEmpty(urlCreationResult.get("txn_secret")))) {
            creationResult.setTransactionID(urlCreationResult.get("txn_secret"));
        }
        
        return result;
    }

    @Override
    public Result<? extends CallbackResult> onCallback(PaymentContext context, Payable payable, EnumRedirectResultStatus status,
                    Map<String, Object> paramters)
    {
        Map<String, String> novalnetResponse = NovalnetPaymentHandler.handleNovalnetResponse(context, payable, serviceConfigurationBO, localizationProvider, status, paramters);
        RedirectAfterCheckoutCallbackResult callbackResult = NovalnetPaymentHandler.setRedirectCallbackResult(novalnetResponse, status);
        Result<? extends CallbackResult> result = NovalnetPaymentHandler.setRedirectTransactionStatus(novalnetResponse, callbackResult);
        
        return result;
    }
    
    @Override
    public Result<AuthorizationResult> onRedirectAfterCheckoutNotification(PaymentContext context, Payable payable,
                    Map<String, Object> parameters)
    {
        Result<AuthorizationResult> result =  NovalnetWebhookUtil.handleCallbackEvents(context, payable, parameters, this.serviceID, localizationProvider);
        return result;
    }

    @Override
    public URI calculateRedirectURL(PaymentContext context, Payable payable, URI successURL, URI cancelURL,
                    URI failureURL)
    {
        return null;
    }
}
