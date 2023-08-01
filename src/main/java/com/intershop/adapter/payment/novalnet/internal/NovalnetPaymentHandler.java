package com.intershop.adapter.payment.novalnet.internal;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intershop.api.data.common.v1.changeable.StringAttribute;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.data.payment.v1.PaymentHistoryEntry;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.EnumRedirectResultStatus;
import com.intershop.api.service.payment.v1.result.CallbackResult;
import com.intershop.api.service.payment.v1.result.RedirectAfterCheckoutCallbackResult;
import com.intershop.beehive.core.capi.currency.CurrencyMgr;
import com.intershop.beehive.core.capi.localization.LocaleMgr;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.request.Request;
import com.intershop.beehive.core.capi.url.Parameters;
import com.intershop.beehive.core.capi.url.URLComposition;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetPaymentHandler
{    
    public static Map<String, String> getRedirectUrl(PaymentContext context, Payable payable, ServiceConfigurationBO serviceConfigurationBO, LocaleMgr localeMgr, CurrencyMgr currencyMgr, URLComposition urlComposition,
                    Request request, String serviceID, URI successURL, URI failureURL) {
        Map<String, String> result = new HashMap<>();
        
        String paymentType = NovalnetUtil.getNovalnetPaymentType(serviceID);
        
        Parameters params = new Parameters();
        params.addParameter("PaymentID", context.getPayment().getId());
        params.addParameter("OrderID", payable.getHeader().getDocumentInfo().getId());
        String hookURL = NovalnetUtil.createURL(localeMgr, currencyMgr, urlComposition,
                        request, request.getExecutionSite(), "NovalnetWebhook-Start", params);
        
        String requestParamString = NovalnetUtil.BuildRequestParameters(context, payable, serviceConfigurationBO, paymentType, successURL, failureURL, hookURL);
        
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, paymentType);
        Map<String, Object> merchantConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, "MERCHANT_CONFIG");

        String novalnetPaymentAction = NovalnetUtil.getPaymentAction(payable, paymentConfiguration);
        String endPoint = NovalnetUtil.getEndPoint(novalnetPaymentAction);
        String paymentAccessKey= merchantConfiguration.get("paymentAccessKey").toString();
        StringBuilder response = NovalnetUtil.sendAPIRequest(requestParamString, paymentAccessKey, endPoint);
        
        if(NovalnetUtil.nnIsEmpty(response)) {
            result.put("status", "FAILURE");
            return result;
        }
        
        JsonObject convertedObject = new Gson().fromJson(response.toString(), JsonObject.class);
        JsonObject nnResponseResult = convertedObject.get("result").getAsJsonObject();
        String status = nnResponseResult.get("status").getAsString();
        
        result.put("status", status);
        
        if(status.equalsIgnoreCase("FAILURE")) {
            result.put("status_text", nnResponseResult.get("status_text").getAsString());
            return result;
        }
        
        result.put("redirect_url", nnResponseResult.get("redirect_url").getAsString());
        result.put("txn_secret", convertedObject.get("transaction").getAsJsonObject().get("txn_secret").getAsString());
        return result;
    }

    public static Map<String, String> handleNovalnetResponse(PaymentContext context, Payable payable, ServiceConfigurationBO serviceConfigurationBO, LocalizationProvider localizationProvider, EnumRedirectResultStatus status,
                    Map<String, Object> paramters)
    {
        Map<String, String> result = new HashMap<>();
        result.put("redirectStatus", "SUCCESS");
        result.put("status", "SUCCESS");
        
        if(NovalnetUtil.nnIsEmpty(paramters.get("tid"))) {
            result.put("status", "FAILURE");
            return result;
        }
        String tid = paramters.get("tid").toString();
        result.put("tid", tid);
        
        String novalnetTransactionStatus = paramters.get("status").toString();
        String transactionComments = "";
        
        Boolean isValidChecksum = isValidChecksum(context, serviceConfigurationBO, paramters);
        if(isValidChecksum == true) {
            Map<String, Object> merchantConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, "MERCHANT_CONFIG");
            String endPoint = NovalnetUtil.getEndPoint("paymentDetails");
            
            String paymentAccessKey = merchantConfiguration.get("paymentAccessKey").toString();
            String requestParamString = NovalnetUtil.getRetriveTransactionDetailsParam(payable, tid);
            
            StringBuilder response = NovalnetUtil.sendAPIRequest(requestParamString, paymentAccessKey, endPoint);
            
            if(response != null) {
                result.put("novalnetAPIResponse", response.toString());
                JsonObject convertedObject = new Gson().fromJson(response.toString(), JsonObject.class);
                transactionComments = NovalnetUtil.getTransactionNotes(convertedObject, localizationProvider);
                
                if(convertedObject.get("transaction") != null && !NovalnetUtil.nnIsEmpty(convertedObject.get("transaction").getAsJsonObject().get("status"))) {
                    novalnetTransactionStatus = convertedObject.get("transaction").getAsJsonObject().get("status").getAsString();
                    
                    result.put("novalnetPaymentType", convertedObject.get("transaction").getAsJsonObject().get("payment_type").getAsString());
                }
                
                if(novalnetTransactionStatus.equals("FAILURE")) {
                    result.put("status", "FAILURE");
                }
            }
            else {
                result.put("redirectStatus", "FAILURE");
            }
        }
        else {
            result.put("status", "FAILURE");
        }
        result.put("transactionStatus", novalnetTransactionStatus);
        result.put("transactionComments", transactionComments);
        return result;
    }
    
    private static Boolean isValidChecksum(PaymentContext context, ServiceConfigurationBO serviceConfigurationBO, Map<String, Object> paramters) {
        PaymentHistoryEntry paymentHistory = context.getPaymentTransaction().getLatestPaymentHistoryEntry("RedirectAfterCheckout");
        
        if(paramters.get("status") != null && paramters.get("txn_secret") != null && paramters.get("checksum") != null && paymentHistory.getAttributes().get("TransactionID") != null) {
            String txnSecret = paramters.get("txn_secret").toString();
            String checksum = paramters.get("checksum").toString();
            String storedTxnSecret = paymentHistory.getAttributes().get("TransactionID").getValue().toString();
            
            Map<String, Object> merchantConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, "MERCHANT_CONFIG");
            String nnStatus = paramters.get("status").toString();
            String tid = paramters.get("tid").toString();
            
            if(!(nnStatus.isEmpty()) && !(txnSecret.isEmpty()) && !(checksum.isEmpty())) {
                String paymentAccessKey = merchantConfiguration.get("paymentAccessKey").toString();
                String tokenString = tid + storedTxnSecret + nnStatus + new StringBuilder(paymentAccessKey).reverse().toString();
                String generatedChecksum = NovalnetUtil.generateChecksum(tokenString);
                
                if(!(generatedChecksum.equals(checksum))) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static Boolean nnIsCapturingAuthorization(String serviceID, ServiceConfigurationBO serviceConfigurationBO, Payable payable) {
        String paymentType = NovalnetUtil.getNovalnetPaymentType(serviceID);
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, paymentType);
        String novalnetPaymentAction = NovalnetUtil.getPaymentAction(payable, paymentConfiguration);
        if(novalnetPaymentAction.equals("payment")) {
            return true;
        }
        return false;
    }
    
    public static Boolean displayPaymentMethod(ServiceConfigurationBO serviceConfigurationBO) {
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, "MERCHANT_CONFIG");
        if(!NovalnetUtil.nnIsEmpty(paymentConfiguration.get("productActivationKey")) && !NovalnetUtil.nnIsEmpty(paymentConfiguration.get("paymentAccessKey")) && !NovalnetUtil.nnIsEmpty(paymentConfiguration.get("tariff"))) {
            return true;
        }
        return false;
    }
    
    public static Boolean validateGuaranteedPayment(String serviceID, Payable payable, ServiceConfigurationBO serviceConfigurationBO) {
        Map<String, Object> billingParameters  = NovalnetUtil.getBillingParameters(payable);
        Map<String, Object> shippingParameters  = NovalnetUtil.getShippingParameters(payable);
        
        String paymentType = NovalnetUtil.getNovalnetPaymentType(serviceID);
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, paymentType);
        
        Boolean isValid = true;
        Integer guaranteedMinAmount = 999;
        
        if(!NovalnetUtil.nnIsEmpty(paymentConfiguration.get("guaranteedMinAmount")))
            guaranteedMinAmount = (int)paymentConfiguration.get("guaranteedMinAmount");
        
        
        Integer orderAmount = NovalnetUtil.getOrderAmount(payable);
        if(orderAmount < guaranteedMinAmount) {
            isValid = false;
        }
        
        if(!(billingParameters.equals(shippingParameters))) {
            isValid = false;
        }
        
        if(!(payable.getTotals().getGrandTotalGross().getCurrency().equals("EUR"))) {
            isValid = false;
        }
        return isValid;
    }
    
    public static RedirectAfterCheckoutCallbackResult setRedirectCallbackResult(Map<String, String> novalnetResponse, EnumRedirectResultStatus status) {
        RedirectAfterCheckoutCallbackResult callbackResult = new RedirectAfterCheckoutCallbackResult();
        
        callbackResult.setRedirectStatus(status);
        callbackResult.setTransactionProcessed(true);
        
        if(novalnetResponse.get("redirectStatus").equals("FAILURE")) {
            callbackResult.setRedirectStatus(EnumRedirectResultStatus.FAILURE);
        }
        
        if(NovalnetUtil.nnIsEmpty(novalnetResponse.get("tid")) == false) {
            callbackResult.setTransactionID(novalnetResponse.get("tid"));
            callbackResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_ID, novalnetResponse.get("tid")));
        }
        
        
        if(NovalnetUtil.nnIsEmpty(novalnetResponse.get("transactionComments")) == false)
        callbackResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_COMMENTS, novalnetResponse.get("transactionComments")));
        
        if(NovalnetUtil.nnIsEmpty(novalnetResponse.get("transactionStatus")) == false)
        callbackResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_STATUS, novalnetResponse.get("transactionStatus"))); 
        return callbackResult;
    }
    
    public static Result<? extends CallbackResult> setRedirectTransactionStatus(Map<String, String> novalnetResponse, RedirectAfterCheckoutCallbackResult callbackResult) {
        Result<? extends CallbackResult> result = new Result<>(callbackResult);
        result.setState(Result.SUCCESS);
        String transactionStatus = novalnetResponse.get("transactionStatus");
        String redirectStatus = novalnetResponse.get("status");
        
        if("FAILURE".equals(transactionStatus) || "FAILURE".equals(redirectStatus) || "FAILURE".equals(novalnetResponse.get("redirectStatus"))) {
            result.setState(Result.FAILURE);
        }
        if("PENDING".equals(transactionStatus)) {
            result.setState(Result.PENDING);
        }
        return result;
    }
}

