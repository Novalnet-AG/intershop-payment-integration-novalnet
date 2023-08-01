package com.intershop.adapter.payment.novalnet.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intershop.api.data.common.v1.Attribute;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.data.payment.v1.PaymentHistoryEntry;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.Cancel;
import com.intershop.api.service.payment.v1.capability.Capture;
import com.intershop.api.service.payment.v1.capability.NotificationRouting;
import com.intershop.api.service.payment.v1.capability.RedirectAfterCheckout;
import com.intershop.api.service.payment.v1.capability.Refund;
import com.intershop.api.service.payment.v1.result.NotificationRoutingResult;
import com.intershop.beehive.core.capi.log.Logger;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetPaymentNotificationRoutingImpl implements NotificationRouting
{
    private ServiceConfigurationBO serviceConfigurationBO;
    private String serviceID;
    
    public NovalnetPaymentNotificationRoutingImpl(ServiceConfigurationBO serviceConfigurationBO, String serviceID)
    {
        this.serviceConfigurationBO = serviceConfigurationBO;
        this.serviceID = serviceID;
    }

    @Override
    public Result<NotificationRoutingResult> routeNotification(PaymentContext context, Payable payable,
                    Map<String, Object> paramter)
    {
        NotificationRoutingResult routingResult = new NotificationRoutingResult();
        Result<NotificationRoutingResult> result = new Result<>(routingResult);
        result.setState(Result.SUCCESS);
        
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, "MERCHANT_CONFIG");
        if(NovalnetUtil.nnIsEmpty(paramter.get("novalnetWebhookData")) == false) {
            String postData = paramter.get("novalnetWebhookData").toString();    
            Boolean isRequestValid = NovalnetWebhookUtil.authenticateRequest(paymentConfiguration.get("webhookTestMode"), paramter.get("remoteAddress"));
            if(isRequestValid == false) {
                result.setState(Result.FAILURE);
                routingResult.setValid(false);
                return result;
            }
            
            JsonObject convertedObject = new Gson().fromJson(postData, JsonObject.class);
            JsonObject resultJsonObject = convertedObject.get("result").getAsJsonObject();
            JsonObject eventJsonObject = convertedObject.get("event").getAsJsonObject();
            JsonObject transactionJsonObject = convertedObject.get("transaction").getAsJsonObject();
            
            Map<String, Object> requiredParams = new HashMap<>();
            requiredParams.put("type", eventJsonObject.get("type"));
            requiredParams.put("checksum", eventJsonObject.get("checksum"));
            requiredParams.put("tid", eventJsonObject.get("tid"));
            requiredParams.put("result_status", resultJsonObject.get("status"));
            requiredParams.put("transaction_status", transactionJsonObject.get("status"));
            requiredParams.put("transaction_tid", transactionJsonObject.get("tid"));
            requiredParams.put("payment_type", transactionJsonObject.get("payment_type"));
            
            for (Map.Entry<String,Object> entry : requiredParams.entrySet()) {
                if(NovalnetUtil.nnIsEmpty(entry.getValue())) {
                    Logger.error("NovalnetWebhook", "Required Parameter" + entry.getKey() + "is missing");
                    result.setState(Result.FAILURE);
                    routingResult.setValid(false);
                    return result;
                }
            }
            
            Boolean isChecksumVaild = NovalnetWebhookUtil.validateChecksum(postData, paymentConfiguration.get("paymentAccessKey").toString());
            if(isChecksumVaild == false) {
                Logger.error("NovalnetWebhook", "While notifying some data has been changed. The hash check failed");
                result.setState(Result.FAILURE);
                routingResult.setValid(false);
                return result;
            }
            
            String nnEventType = eventJsonObject.get("type").getAsString();
            String paymentType = transactionJsonObject.get("payment_type").getAsString();
            String transactionStatus = transactionJsonObject.get("status").getAsString();
            String[] redirectNotificationEvent = {"TRANSACTION_UPDATE", "CREDIT", "CHARGEBACK", "PAYMENT_REMINDER_1", "PAYMENT_REMINDER_2", "SUBMISSION_TO_COLLECTION_AGENCY"};
            String parentTID = transactionJsonObject.get("tid").getAsString();
            PaymentHistoryEntry paymentHistory = context.getPaymentTransaction().getLatestPaymentHistoryEntry("RedirectAfterCheckout");
            
            if(!NovalnetUtil.nnIsEmpty(eventJsonObject.get("parent_tid"))) {
                parentTID = eventJsonObject.get("parent_tid").getAsString();
            }
            Attribute transactionIDAttribute =  paymentHistory.getAttributes().get(NovalnetUtil.NOVALNET_TRANSACTION_ID);
            if(transactionIDAttribute != null && transactionIDAttribute.getValue() != null) {
                String shopTID = transactionIDAttribute.getValue().toString();
                if(!parentTID.equals(shopTID)) {
                    Logger.error("NovalnetWebhook", "Recevied TID: " + parentTID + " shopTID: "+ shopTID +" mismatched");
                    result.setState(Result.FAILURE);
                    routingResult.setValid(false);
                    return result;
                }
            }
            else {
                PaymentHistoryEntry failurePaymentHistory = context.getPaymentTransaction().getLatestPaymentHistoryEntry("RedirectAfterCheckoutNotification");
                if(failurePaymentHistory == null && nnEventType.equals("PAYMENT")) {
                    routingResult.setCapability(RedirectAfterCheckout.class);
                    routingResult.setValid(true);
                    return result;
                }
            }
            
            if(nnEventType.equals("TRANSACTION_CAPTURE")) {
                routingResult.setCapability(Capture.class);
                routingResult.setValid(true);
            }
            else if(nnEventType.equals("TRANSACTION_CANCEL")) {
                routingResult.setCapability(Cancel.class);
                routingResult.setValid(true);
            }
            else if(nnEventType.equals("TRANSACTION_REFUND")) {
                routingResult.setCapability(Refund.class);
                routingResult.setValid(true);
            }
            else if(Arrays.asList(redirectNotificationEvent).contains(nnEventType)) {
                routingResult.setCapability(RedirectAfterCheckout.class);
                routingResult.setValid(true);
            }
            else {
                Logger.error("NovalnetWebhook", "Unhandled eventType is received" + nnEventType);
                routingResult.setValid(false);
            }
        }
        return result;
    }

}
