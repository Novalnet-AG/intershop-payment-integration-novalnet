package com.intershop.adapter.payment.novalnet.pipelet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.intershop.beehive.core.capi.log.Logger;
import com.intershop.beehive.core.capi.pipeline.Pipelet;
import com.intershop.beehive.core.capi.pipeline.PipeletExecutionException;
import com.intershop.beehive.core.capi.pipeline.PipelineDictionary;
import com.intershop.beehive.core.capi.pipeline.PipelineInitializationException;
import com.intershop.beehive.core.capi.request.Request;

public class NovalnetWebhookHandler extends Pipelet
{
    @Override
    public void init() throws PipelineInitializationException
    {
        super.init();
    }

    @Override
    public int execute(PipelineDictionary dict) throws PipeletExecutionException
    {
        Request request = dict.get("CurrentRequest");
        StringBuffer jsonRequest = new StringBuffer();
        String line = null;
        try
        {
            BufferedReader reader = request.getServletRequest().getReader();
            while ((line = reader.readLine()) != null)
                jsonRequest.append(line);
        }
        catch(IOException e)
        {
            Logger.error("NovalnetwebhookRequestError", e.getMessage());
            return PIPELET_NEXT;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("novalnetWebhookData", jsonRequest);
        parameters.put("remoteAddress", request.getServletRequest().getRemoteAddr());
        dict.put("AdditionalParameters", parameters);
        dict.put("PaymentID", dict.get("PaymentID"));
        dict.put("OrderID", dict.get("OrderID"));
        return PIPELET_NEXT;
    }

}
