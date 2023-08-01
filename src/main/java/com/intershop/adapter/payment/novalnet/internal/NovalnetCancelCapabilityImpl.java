package com.intershop.adapter.payment.novalnet.internal;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.intershop.api.data.common.v1.changeable.StringAttribute;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.capability.Cancel;
import com.intershop.api.service.payment.v1.result.CancelResult;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.localization.context.LocalizationContext;

public class NovalnetCancelCapabilityImpl implements Cancel
{
    
    @Inject
    public LocalizationProvider localizationProvider;
        
    @Override
    public Result<CancelResult> cancel(PaymentContext context, Payable payable)
    {
        Result<CancelResult> result = new Result<>(new CancelResult());
        result.setState(Result.FAILURE);
        return result; 
    }
    
    @Override
    public Result<CancelResult> onCancelNotification(PaymentContext context, Payable payable,
                    Map<String, Object> parameters)
    {
        CancelResult cancelResult = new CancelResult();
        Result<CancelResult> result = new Result<>(cancelResult);
        
        String postData = parameters.get("novalnetWebhookData").toString();
        JsonObject convertedObject = new Gson().fromJson(postData, JsonObject.class);
        JsonObject transactionJsonObject = convertedObject.get("transaction").getAsJsonObject();
        
        LocalizationContext localizationContext = LocalizationContext.create();
        String comment = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.cancelled", NovalnetWebhookUtil.getCurrentDate());
        
        cancelResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_COMMENTS, comment));
        cancelResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_STATUS, transactionJsonObject.get("status").getAsString()));
        result.setState(Result.SUCCESS);

        return result;
    };

}
