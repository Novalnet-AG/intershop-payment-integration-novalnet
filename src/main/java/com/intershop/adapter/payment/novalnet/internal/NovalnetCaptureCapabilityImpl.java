package com.intershop.adapter.payment.novalnet.internal;

import java.util.Arrays;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.intershop.api.data.common.v1.Money;
import com.intershop.api.data.common.v1.changeable.StringAttribute;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.Capture;
import com.intershop.api.service.payment.v1.result.CaptureResult;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.localization.context.LocalizationContext;

public class NovalnetCaptureCapabilityImpl implements Capture
{
    @Inject
    public LocalizationProvider localizationProvider;
    
    @Override
    public Result<CaptureResult> capture(PaymentContext context, Payable payable, Money captureAmount)
    {
        Result<CaptureResult> result = new Result<>(new CaptureResult());
        result.setState(Result.FAILURE);
        return result; 
    }
    
    @Override
    public Result<CaptureResult> onCaptureNotification(PaymentContext context, Payable payable,
                    Map<String, Object> parameters)
    {
        CaptureResult captureResult = new CaptureResult();
        Result<CaptureResult> result = new Result<>(captureResult);
        result.setState(Result.SUCCESS);
        
        String postData = parameters.get("novalnetWebhookData").toString();
        JsonObject convertedObject = new Gson().fromJson(postData, JsonObject.class);
        JsonObject transactionJsonObject = convertedObject.get("transaction").getAsJsonObject();
        String paymentType = transactionJsonObject.get("payment_type").getAsString();
        
        LocalizationContext localizationContext = LocalizationContext.create();
        String comment = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.confirmed", NovalnetWebhookUtil.getCurrentDate());
        if(transactionJsonObject.get("bank_details") != null)
            comment += NovalnetUtil.getBankDetails(transactionJsonObject, localizationProvider);
        
        
        
        captureResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_COMMENTS, comment));
        captureResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_STATUS, transactionJsonObject.get("status").getAsString()));
        captureResult.setCapturedTotalAmount(payable.getTotals().getGrandTotalGross());
        captureResult.setTransactionAmount(payable.getTotals().getGrandTotalGross());
        
        String[] pendingPayments = {"INVOICE", "PREPAYMENT"}; 
        if(Arrays.asList(pendingPayments).contains(paymentType)) {
            result.setState(Result.PENDING);
        }
        
        return result;
    }

}
