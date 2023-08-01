package com.intershop.adapter.payment.novalnet.internal;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.intershop.api.data.common.v1.Money;
import com.intershop.api.data.common.v1.changeable.StringAttribute;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.Refund;
import com.intershop.api.service.payment.v1.result.RefundResult;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.localization.context.LocalizationContext;

public class NovalnetRefundCapabilityImpl implements Refund
{
    @Inject
    public LocalizationProvider localizationProvider;

    @Override
    public Result<RefundResult> refund(PaymentContext context, Payable payable, Money refundAmount, String refundReason)
    {
        Result<RefundResult> result = new Result<>(new RefundResult());
        result.setState(Result.FAILURE);
        return result; 
    }
    
    @Override
    public Result<RefundResult> onRefundNotification(PaymentContext context, Payable payable,
                    Map<String, Object> parameters)
    {
        RefundResult refundResult = new RefundResult();
        Result<RefundResult> result = new Result<>(refundResult);
        result.setState(Result.SUCCESS);
        
        String postData = parameters.get("novalnetWebhookData").toString();
        JsonObject convertedObject = new Gson().fromJson(postData, JsonObject.class);
        JsonObject transactionJsonObject = convertedObject.get("transaction").getAsJsonObject();
        Money formattedAmount = NovalnetUtil.getFormattedAmount(transactionJsonObject.get("amount").getAsInt(), transactionJsonObject.get("currency").getAsString());
        String tid = transactionJsonObject.get("tid").getAsString();
        
        LocalizationContext localizationContext = LocalizationContext.create();
        String comment = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.refunded", tid, formattedAmount.toString());
        
        if(transactionJsonObject.get("refund") != null) {
            JsonObject refundObject = transactionJsonObject.get("refund").getAsJsonObject(); 
            if(refundObject.get("amount") != null)
                formattedAmount = NovalnetUtil.getFormattedAmount(refundObject.get("amount").getAsInt(), refundObject.get("currency").getAsString());
            
            
            comment = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.refunded", tid, formattedAmount.toString());
            if(refundObject.get("tid") != null) {
                String refundTID = refundObject.get("tid").getAsString();
                refundResult.setTransactionID(refundTID);
                comment += localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.refundedtid", refundTID);
            }
            
        }
        
        Integer refundedAmount = transactionJsonObject.get("amount").getAsInt();
        if(transactionJsonObject.get("refunded_amount") != null)
            refundedAmount = transactionJsonObject.get("refunded_amount").getAsInt();
        
        refundResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_COMMENTS, comment));
        refundResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_STATUS, transactionJsonObject.get("status").getAsString()));
        
        Money refundMoney = NovalnetUtil.getFormattedAmount(refundedAmount, transactionJsonObject.get("currency").getAsString());
        refundResult.setRefundedTotalAmount(refundMoney);
        return result;
    };
    
    

}
