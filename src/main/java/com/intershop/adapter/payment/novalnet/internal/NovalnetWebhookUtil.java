package com.intershop.adapter.payment.novalnet.internal;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intershop.api.data.common.v1.Attribute;
import com.intershop.api.data.common.v1.Money;
import com.intershop.api.data.common.v1.MoneyImpl;
import com.intershop.api.data.common.v1.changeable.MoneyAttribute;
import com.intershop.api.data.common.v1.changeable.StringAttribute;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.data.payment.v1.PaymentHistoryEntry;
import com.intershop.api.service.common.v1.Result;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.api.service.payment.v1.result.AuthorizationResult;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.localization.context.LocalizationContext;
import com.intershop.beehive.core.capi.log.Logger;

public class NovalnetWebhookUtil
{
    protected static String tid;
    protected static String parentTid;
    protected static String currentDate;
    protected static String paymentType;
    protected static JsonObject transactionObject;
    protected static JsonObject postData;
    protected static String eventType;
    protected static LocalizationProvider localizationProvider;
    protected static LocalizationContext localizationContext;
    
    public static Boolean authenticateRequest(Object testMode, Object remoteIp)
    {
        // NOVALNET IP ADDRESS FROM HOST
        String vendorScriptHostIpAddress = "";
        try {
            InetAddress address = InetAddress.getByName("pay-nn.de"); //Novalnet vendor script host
            vendorScriptHostIpAddress = address.getHostAddress();
        } catch (UnknownHostException e) {
            vendorScriptHostIpAddress = "";
        }
        if ("".equals(vendorScriptHostIpAddress)) {
            Logger.error("NovalnetWebhookUtil", "Novalnet HOST IP missing");
            return false;
        }
        
        if(NovalnetUtil.nnIsEmpty(remoteIp) == true) {
            Logger.error("NovalnetWebhookUtil", "Novalnet RemoteIP missing");
            return false;
        }
        // Get remote IP address
        String callerIp = remoteIp.toString();
        
        if(testMode == null || (Boolean)testMode == false) {
           // Check for IP and testmode validation
            if (!vendorScriptHostIpAddress.equals(callerIp)) {
                Logger.error("NovalnetWebhookUtil", "Novalnet webhook received. Unauthorised access from the IP " + callerIp);
                return false;
            }
        }
        
        return true;
    }

    public static Boolean validateChecksum(String postData, String paymentAccessKey)
    {
        JsonObject convertedObject = new Gson().fromJson(postData, JsonObject.class);
        JsonObject resultJsonObject = convertedObject.get("result").getAsJsonObject();
        JsonObject eventJsonObject = convertedObject.get("event").getAsJsonObject();
        JsonObject transactionJsonObject = convertedObject.get("transaction").getAsJsonObject();
        
        String tokenString = eventJsonObject.get("tid").getAsString() + eventJsonObject.get("type").getAsString() + resultJsonObject.get("status").getAsString();

        if (transactionJsonObject.get("amount") != null && !"".equals(transactionJsonObject.get("amount").getAsString())) {
            tokenString += transactionJsonObject.get("amount").getAsString();
        }
        if (transactionJsonObject.get("currency") != null && !"".equals(transactionJsonObject.get("currency").getAsString()) ) {
            tokenString += transactionJsonObject.get("currency").getAsString();
        }
        if (!NovalnetUtil.nnIsEmpty(paymentAccessKey)) {
            tokenString += new StringBuilder(paymentAccessKey.trim()).reverse().toString();
        }
        String generatedChecksum = NovalnetUtil.generateChecksum(tokenString);
        
        if (eventJsonObject.get("checksum").getAsString().equals(generatedChecksum) ) {
            return true;
        }
        return false;
    }
    
    public static String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDueDate = formatter.format(cal.getTime());
        return formattedDueDate;
    }

    public static Result<AuthorizationResult> handleCallbackEvents(PaymentContext context, Payable payable, Map<String, Object> parameters, String serviceID, LocalizationProvider nnLocalizationProvider)
    {
        AuthorizationResult authResult = new AuthorizationResult();
        Result<AuthorizationResult> result = new Result<>(authResult);
        result.setState(Result.SUCCESS);
        
        Map<String, String> callbackResult = new HashMap<>();
        postData = new Gson().fromJson(parameters.get("novalnetWebhookData").toString(), JsonObject.class);
        JsonObject eventData = postData.get("event").getAsJsonObject();
        eventType = eventData.get("type").getAsString();
        transactionObject = postData.get("transaction").getAsJsonObject();
        tid = transactionObject.get("tid").getAsString();
        parentTid = tid; 
        if(eventData.get("parent_tid") != null) {
            parentTid = eventData.get("parent_tid").getAsString(); 
        }
        paymentType = transactionObject.get("payment_type").getAsString();
        currentDate = NovalnetWebhookUtil.getCurrentDate();
        localizationProvider = nnLocalizationProvider;
        localizationContext = LocalizationContext.create();
        String currency = transactionObject.get("currency").getAsString();
        String comment = "";
        switch(eventType) {
            case "TRANSACTION_UPDATE":
                callbackResult = handleTransactionUpdate(context, payable);
                break;
            case "CREDIT":
                callbackResult = handleCredit(context, payable);
                if(!NovalnetUtil.nnIsEmpty(callbackResult.get("creditedAmount"))) {
                    Integer amount = Integer.parseInt(callbackResult.get("creditedAmount"));
                    Money creditedAmount = NovalnetUtil.getFormattedAmount(amount, currency);
                    authResult.put(new MoneyAttribute("Novalnet Credited Amount", creditedAmount));
                }
                break;
            case "CHARGEBACK":
                Money chargebackAmount = NovalnetUtil.getFormattedAmount(transactionObject.get("amount").getAsInt(), currency);
                callbackResult.put("comments", localizationProvider.getText(localizationContext, "novalnet.webhook.chargeback", parentTid, chargebackAmount.toString(), currentDate, tid));
                callbackResult.put("status", "PENDING");
                break;
            case "PAYMENT_REMINDER_1":
            case "PAYMENT_REMINDER_2":
                String reminderCount = eventType.replaceAll("[^0-9]", "");
                callbackResult.put("comments", localizationProvider.getText(localizationContext, "novalnet.webhook.payment_reminder", reminderCount));
                callbackResult.put("status", "PENDING");
                break;
            case "PAYMENT":
                callbackResult = handleCommunicationFailure(context, payable);
                authResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_ID, tid));
                authResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_STATUS, transactionObject.get("status").getAsString()));
                authResult.setTransactionID(tid);
                break;
        }
        
        if(!NovalnetUtil.nnIsEmpty(callbackResult.get("comments")))
            authResult.put(new StringAttribute(NovalnetUtil.NOVALNET_TRANSACTION_COMMENTS, callbackResult.get("comments")));
        if(!NovalnetUtil.nnIsEmpty(callbackResult.get("status"))) {
            if(callbackResult.get("status").equals("PENDING"))
                result.setState(Result.PENDING);
            if(callbackResult.get("status").equals("FAILURE"))
                result.setState(Result.FAILURE);
        }
            
        return result;
    }

    private static Map<String, String> handleCommunicationFailure(PaymentContext context, Payable payable)
    {
        Map<String, String> result = new HashMap<>();
        String comments = NovalnetUtil.getTransactionNotes(postData, localizationProvider);
        result.put("comments", comments);
        result.put("status", "SUCCESS");
        
        String status = transactionObject.get("status").getAsString();
        String[] failureStatus = {"FAILURE", "DEACTIVATED"};
        
        if(Arrays.asList(failureStatus).contains(status)) 
            result.put("status", "FAILURE");
        
        if(status.equals("PENDING"))
            result.put("status", "PENDING");
        
        return result;
    }

    private static Map<String, String> handleCredit(PaymentContext context, Payable payable)
    {
        Map<String, String> result = new HashMap<>();
        String comments = "";
        String currency = transactionObject.get("currency").getAsString();
        Money formattedAmount = NovalnetUtil.getFormattedAmount(transactionObject.get("amount").getAsInt(), currency);
        comments = localizationProvider.getText(localizationContext, "novalnet.webhook.credit_executed", parentTid, formattedAmount.toString(), currentDate, tid);
        String statusToUpdate = "SUCCESS";
                        
        String[] creditPayments = {"INVOICE_CREDIT", "CASHPAYMENT_CREDIT", "MULTIBANCO_CREDIT"};
        if(Arrays.asList(creditPayments).contains(paymentType)) {
            statusToUpdate = "PENDING";
            PaymentHistoryEntry paymentHistory = context.getPaymentTransaction().getLatestPaymentHistoryEntry("RedirectAfterCheckoutNotification");
            Money creditedAmount = new MoneyImpl(new BigDecimal(0), currency);
            if(paymentHistory != null) {
                Attribute amountAttribute = paymentHistory.getAttributes().get("Novalnet Credited Amount");
                if(amountAttribute != null && amountAttribute.getValue() != null) {
                    creditedAmount = (Money)amountAttribute.getValue();
                }
            }
            
            Integer creditedAmountInt = creditedAmount.getValue().movePointRight(2).intValue();
            Integer orderAmount = 0;
            Money orderGrandTotal = payable.getTotals().getGrandTotalGross();
            if(orderGrandTotal.getValue() != null) {
                orderAmount = orderGrandTotal.getValue().movePointRight(2).intValue();
            }
            if(creditedAmountInt < orderAmount) {
                creditedAmountInt += transactionObject.get("amount").getAsInt();
                if(creditedAmountInt >= orderAmount) {
                    statusToUpdate = "SUCCESS";
                }
            }
            result.put("creditedAmount", creditedAmountInt.toString());
        }
        result.put("comments", comments);
        result.put("status", statusToUpdate);
        return result;
    }
    
    private static String getOrderPaymentStatus(PaymentContext context) {
        PaymentHistoryEntry paymentHistory = context.getPaymentTransaction().getLatestPaymentHistoryEntry("RedirectAfterCheckout");
        Attribute statusAttribute = paymentHistory.getAttributes().get(NovalnetUtil.NOVALNET_TRANSACTION_STATUS);
        String status = ""; 
        if(statusAttribute != null && statusAttribute.getValue() != null) {
            status = statusAttribute.getValue().toString();
        }
        return status;
    }

    private static Map<String, String> handleTransactionUpdate(PaymentContext context, Payable payable)
    {
        Map<String, String> result = new HashMap<>();
        String status = transactionObject.get("status").getAsString();
        String[] allowedStatus = {"DEACTIVATED", "PENDING", "ON_HOLD", "CONFIRMED"};
        if(Arrays.asList(allowedStatus).contains(status)) {
            String[] pendingStatus = {"PENDING", "ON_HOLD"};
            String paymentType = transactionObject.get("payment_type").getAsString();
            String[] pendingPayments = {"INVOICE", "PREPAYMENT", "CASHPAYMENT"};
            String previousStatus = getOrderPaymentStatus(context);
            String comments = "";
            String statusToUpdate = previousStatus;
            if("DEACTIVATED".equals(status)) {
                comments = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.cancelled", currentDate);
                statusToUpdate = "FAILURE";
            }
            else if(Arrays.asList(pendingStatus).contains(previousStatus)) {
                statusToUpdate = "PENDING";
                if("PENDING".equals(previousStatus) && "ON_HOLD".equals(status)) {
                    comments = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.status_changed", tid, currentDate);
                    statusToUpdate = "SUCCESS";
                }
                else {
                    Money formattedAmount = NovalnetUtil.getFormattedAmount(transactionObject.get("amount").getAsInt(), transactionObject.get("currency").getAsString());
                    
                    comments = localizationProvider.getText(localizationContext, "novalnet.webhook.transaction.updated", tid, formattedAmount.toString(), currentDate);
                    
                    if(transactionObject.get("due_date") != null && Arrays.asList(pendingPayments).contains(paymentType)) {
                        String dueDateLang = "novalnet.webhook.transaction.updated_with_duedate";
                        if(paymentType.equals("CASHPAYMENT")) {
                            dueDateLang = "novalnet.webhook.transaction.updated_with_slipdate";
                        }
                        comments = localizationProvider.getText(localizationContext, dueDateLang, formattedAmount.toString(), transactionObject.get("due_date").getAsString());
                        
                        if(transactionObject.get("bank_details") != null)
                            comments += NovalnetUtil.getBankDetails(transactionObject, localizationProvider);
                    }
                    if(!Arrays.asList(pendingPayments).contains(paymentType))
                        statusToUpdate = "SUCCESS";
                }
            }
            result.put("comments", comments);
            result.put("status", statusToUpdate);
        }
        
        return result;
    }
}
