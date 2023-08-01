package com.intershop.adapter.payment.novalnet.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intershop.api.data.address.v1.Address;
import com.intershop.api.data.address.v1.Contact;
import com.intershop.api.data.common.v1.Money;
import com.intershop.api.data.common.v1.MoneyImpl;
import com.intershop.api.data.payment.v1.PaymentContext;
import com.intershop.api.data.shipping.v1.Bucket;
import com.intershop.api.service.payment.v1.Payable;
import com.intershop.beehive.core.capi.currency.CurrencyMgr;
import com.intershop.beehive.core.capi.domain.Domain;
import com.intershop.beehive.core.capi.localization.LocaleMgr;
import com.intershop.beehive.core.capi.localization.LocalizationProvider;
import com.intershop.beehive.core.capi.localization.context.LocalizationContext;
import com.intershop.beehive.core.capi.log.Logger;
import com.intershop.beehive.core.capi.request.Request;
import com.intershop.beehive.core.capi.request.RequestInformation;
import com.intershop.beehive.core.capi.url.Action;
import com.intershop.beehive.core.capi.url.Parameters;
import com.intershop.beehive.core.capi.url.URLComposition;
import com.intershop.beehive.core.capi.url.URLUtils;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetUtil
{
    public static String NOVALNET_TRANSACTION_COMMENTS = "Novalnet Transaction Details";
    public static String NOVALNET_TRANSACTION_STATUS = "Novalnet Transaction Status";
    public static String NOVALNET_TRANSACTION_ID = "Novalnet TransactionID";
    
    public static Map<String, Object> getMerchantParameters(ServiceConfigurationBO serviceConfigurationBO) {
        Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, "MERCHANT_CONFIG");
        Map<String, Object> merchantParameters = new HashMap<>();
        merchantParameters.put("signature", paymentConfiguration.get("productActivationKey"));
        merchantParameters.put("tariff", paymentConfiguration.get("tariff"));
        return merchantParameters;
    }
    
    public static Boolean nnIsEmpty(String data) {
        if(data == null || data.isEmpty()) {
            return true;
        }
        return false;
    }
    
    public static Boolean nnIsEmpty(Object data) {
        if(data == null) {
            return true;
        }
        else if(data.toString().isEmpty()){
            return true;
        }
        return false;
    }
    
    public static Map<String, Object> getCustomerParameters(Payable payable, PaymentContext context) {
        Map<String, Object> customerParameters    = new HashMap<>();
        
        customerParameters.put("customer_no", payable.getCustomer().getCustomerNo());
        customerParameters.put("customer_ip", context.getSessionInfo().getClientIPAddress());
        
        Address billingAddress = payable.getInvoiceToAddress();
        Contact contact = billingAddress.getContact();
        customerParameters.put("first_name", contact.getFirstName());
        customerParameters.put("last_name", contact.getLastName());
        customerParameters.put("email", contact.getEMail());
        if(!(nnIsEmpty(contact.getPhoneMobile()))) {
            customerParameters.put("mobile", contact.getPhoneMobile());
        }
        
        Map<String, Object> billingParameters  = getBillingParameters(payable);
        customerParameters.put("billing", billingParameters);
        
        Map<String, Object> shippingParameters  = getShippingParameters(payable);
        
        if(billingParameters.equals(shippingParameters)) {
            shippingParameters.clear();
            shippingParameters.put("same_as_billing", "1");
        }
        
        customerParameters.put("shipping", shippingParameters);
        
        return customerParameters;
    }
    
    public static Map<String, Object> getShippingParameters(Payable payable)
    {
        Map<String, Object> shippingParameters  = new HashMap<>();
        Bucket bucket = payable.getBuckets().iterator().next();
        Address shippingAddress = bucket.getShipToAddress();
        String shippingStreet = shippingAddress.getLine1();
        if(!nnIsEmpty(shippingAddress.getLine2())) {
            shippingStreet += shippingAddress.getLine2();
        }
        
        if(!(nnIsEmpty(shippingAddress.getLine3()))) {
            shippingStreet += shippingAddress.getLine3();
        }
        
        shippingParameters.put("street", shippingStreet);
        shippingParameters.put("city", shippingAddress.getCity());
        shippingParameters.put("zip", shippingAddress.getPostalCode());
        shippingParameters.put("country_code", shippingAddress.getCountryCode());
        
        return shippingParameters;
    }

    public static Map<String, Object> getBillingParameters(Payable payable) {
        Address billingAddress = payable.getInvoiceToAddress();
        
        Map<String, Object> billingParameters     = new HashMap<>();
        String street = billingAddress.getLine1();
        if(!(nnIsEmpty(billingAddress.getLine2()))) {
            street += billingAddress.getLine2();
        }
        
        if(!(nnIsEmpty(billingAddress.getLine3()))) {
            street += billingAddress.getLine3();
        }
        
        billingParameters.put("street", street);
        billingParameters.put("city", billingAddress.getCity());
        billingParameters.put("zip", billingAddress.getPostalCode());
        billingParameters.put("country_code", billingAddress.getCountryCode());
        
        if(!(nnIsEmpty(billingAddress.getMainDivision()))) {
            billingParameters.put("state", billingAddress.getMainDivision());
        }
        return billingParameters;
    }
    
    public static Map<String, Object> getHostedPaymentPageParameters(String paymentType) {
        Map<String, Object> hostedPaymentPageParameters  = new HashMap<>();
        Set<String> hideBlocks = new HashSet<>(Arrays.asList("HEADER", "LANGUAGE_MENU", "SHOP_INFO", "TARIFF"));
        Set<String> skipPages = new HashSet<>(Arrays.asList("CONFIRMATION_PAGE", "SUCCESS_PAGE"));
        String[] displayPayments = {paymentType};
        
        String[] guaranteedPayments = {"GUARANTEED_INVOICE", "GUARANTEED_DIRECT_DEBIT_SEPA"};
        if(!(Arrays.asList(guaranteedPayments).contains(paymentType))) {
            hideBlocks.add("ADDRESS_FORM");
            skipPages.add("PAYMENT_PAGE");
        }
        
        hostedPaymentPageParameters.put("hide_blocks", hideBlocks);
        hostedPaymentPageParameters.put("skip_pages", skipPages);
        hostedPaymentPageParameters.put("display_payments", displayPayments);
        
        return hostedPaymentPageParameters;
    }
    
    public static String getEndPoint(String paymentAction) {
        Map<String, String> endPoints  = new HashMap<>();
        endPoints.put("payment", "https://payport.novalnet.de/v2/seamless/payment");
        endPoints.put("authorize", "https://payport.novalnet.de/v2/seamless/authorize");
        endPoints.put("paymentDetails", "https://payport.novalnet.de/v2/transaction/details");
        return endPoints.get(paymentAction);
    }
    
    public static Integer getOrderAmount(Payable payable) {
        Money grandTotalGross = payable.getTotals().getGrandTotalGross();
        Integer amount = grandTotalGross.getValue().movePointRight(2).intValue();
        return amount;
    }
    
    public static String BuildRequestParameters(PaymentContext context, Payable payable, ServiceConfigurationBO serviceConfigurationBO, String paymentType, URI successURL, URI failureURL, String hookURL) {
      Map<String, Object> paymentConfiguration = NovalnetConfig.getPaymentConfiguration(serviceConfigurationBO, paymentType);
      
      Map<String, Object> merchantParameters = getMerchantParameters(serviceConfigurationBO);
      Map<String, Object> customerParameters    = getCustomerParameters(payable, context);
                      
      Map<String, Object> transactionParameters = new HashMap<>();
      Integer testMode = 0;
      if(paymentConfiguration.get("testMode") != null && (Boolean)paymentConfiguration.get("testMode") == true) {
          testMode = 1;
      }
      transactionParameters.put("test_mode", testMode);
      transactionParameters.put("payment_type", paymentType);
      transactionParameters.put("amount", getOrderAmount(payable));
      transactionParameters.put("currency", payable.getTotals().getGrandTotalGross().getCurrency());
      transactionParameters.put("order_no", payable.getHeader().getDocumentInfo().getDocumentNo());
      transactionParameters.put("return_url", successURL);
      transactionParameters.put("error_return_url", failureURL);
      transactionParameters.put("hook_url", hookURL);
      transactionParameters.put("system_name", "Intershop");
      
      String[] dueDatePayments = {"INVOICE", "CASHPAYMENT", "DIRECT_DEBIT_SEPA"};
      Object dueDate =  paymentConfiguration.get("dueDate");
      if(Arrays.asList(dueDatePayments).contains(paymentType) && !nnIsEmpty(dueDate)) {
          Calendar cal = Calendar.getInstance();
          cal.setTime(new Date());
          
          cal.add(Calendar.DATE, (Integer)dueDate); 
          SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
          String formattedDueDate = formatter.format(cal.getTime());
          
          Map<String, Object> dueDateParameters = new HashMap<>();
          dueDateParameters.put(paymentType, formattedDueDate);
          transactionParameters.put("due_dates", dueDateParameters);
      }
      
      if(paymentType == "CREDITCARD") {
          if(paymentConfiguration.get("enforce3d") != null && (Boolean)paymentConfiguration.get("enforce3d") == true) {
              transactionParameters.put("enforce_3d", 1);
          }
      }

      Map<String, Object> hostedpageParameters  = getHostedPaymentPageParameters(paymentType);
      
      Map<String, Object> customParameters = new HashMap<>();
      customParameters.put("lang", getLanguage(payable));
      
      Map<String, Object> paymentRequestParameters = new HashMap<>();
      paymentRequestParameters.put("merchant", merchantParameters);
      paymentRequestParameters.put("customer", customerParameters);
      paymentRequestParameters.put("transaction", transactionParameters);
      paymentRequestParameters.put("hosted_page", hostedpageParameters);
      paymentRequestParameters.put("custom", customParameters);

      String jsonString =  new Gson().toJson(paymentRequestParameters);
      return jsonString;
  }
  
    public static String createURL(LocaleMgr localeMgr, CurrencyMgr currencyMgr, URLComposition urlComposition,
                    Request request, Domain domain, String callThisPipeline, Parameters params)
    {
        String host = (String) request.getObject(RequestInformation.HOST);
        if (host == null)
        {
            Logger.debug("Novalnet", "RequestInformation.HOST is null, using default.");
        }
    
        String protocol = (String) request.getObject(RequestInformation.SCHEME);
        if (protocol == null)
        {
            protocol = URLUtils.HTTPS;
            Logger.debug("Novalnet", "RequestInformation.SCHEME is null! use default: {}", protocol);
        }
    
        String port = (String) request.getObject(RequestInformation.PORT);
        if (port == null)
        {
            port = "";
            Logger.debug("Novalnet", "RequestInformation.PORT is null!");
        }
    
        String servergroup = (String) request.getObject(RequestInformation.SERVER_GROUP);
        if (servergroup == null)
        {
            Logger.debug("Novalnet", "RequestInformation.SERVER_GROUP is null, using default.");
        }
    
        String localeId = (String) request.getObject(RequestInformation.REQUESTED_LOCALE);
        if (localeId == null)
        {
            localeId = localeMgr.getLeadLocale().getLocaleID();
            Logger.debug("Novalnet", "RequestInformation.REQUESTED_LOCALE is null! Use lead locale: {}", localeId);
        }
    
        String currency = (String) request.getObject(RequestInformation.REQUESTED_CURRENCY);
        if (currency == null)
        {
            currency = currencyMgr.getLeadCurrencyMnemonic();
            Logger.debug("Novalnet", "RequestInformation.REQUESTED_CURRENCY is null! use default: {}", currency);
        }
    
        // create action
        Action action = new Action(callThisPipeline, servergroup, domain.getDomainName(), localeId, currency);
        // return created returnURL
        return new URLComposition().createURL(action, params);
        
    }
    

private static String getLanguage(Payable payable)
  {
      if(payable.getHeader().getDocumentInfo().getLocale() != null && payable.getHeader().getDocumentInfo().getLocale().getLanguage() != null) {
          return payable.getHeader().getDocumentInfo().getLocale().getLanguage().toUpperCase();
      }
      return "EN";
  }

public static String generateChecksum(String tokenString) {
      String checkSum = "";
      try {
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          byte[] hashes = digest.digest(tokenString.getBytes(StandardCharsets.UTF_8));
          StringBuilder hexString = new StringBuilder();
          for (byte hash : hashes) {
              String hex = Integer.toHexString(0xff & hash);
              if (hex.length() == 1) {
                  hexString.append('0');
              }
              hexString.append(hex);
          }
          checkSum = hexString.toString();
      } catch (NoSuchAlgorithmException e) {
          Logger.error("Novalnet", "NoSuchAlgorithmException " + e.getMessage());
      }
      return checkSum;
  }
  
  public static StringBuilder sendAPIRequest(String jsonString, String paymentAccessKey, String endpoint) {
      StringBuilder response = new StringBuilder();
      try {
          URL obj = new URL(endpoint);
          HttpURLConnection con = (HttpURLConnection) obj.openConnection();
          byte[] postData = jsonString.getBytes(StandardCharsets.UTF_8);
          con.setRequestMethod("POST");
          con.setRequestProperty("Content-Type", "application/json");
          con.setRequestProperty("Charset", "utf-8");
          con.setRequestProperty("Accept", "application/json");
          con.setRequestProperty("X-NN-Access-Key", Base64.getEncoder().encodeToString(paymentAccessKey.getBytes()));
    
          con.setDoOutput(true);
          DataOutputStream wr = new DataOutputStream(con.getOutputStream());
          wr.write(postData);
          wr.flush();
          wr.close();
    
          int responseCode = con.getResponseCode();
          BufferedReader iny = new BufferedReader(
          new InputStreamReader(con.getInputStream()));
          String output;
          
          while ((output = iny.readLine()) != null) {
              response.append(output);
          }
          iny.close();
      } catch (MalformedURLException ex) {
          Logger.error("Novalnet", "Novalnet LOG MalformedURLException Composition of Redirect URL failed.");
      } catch (IOException ex) {
          Logger.error("Novalnet", "Novalnet LOG IOException Composition of Redirect URL failed.");
      }
      Logger.error("Novalnet", "Novalnet LOG API response. dsd" + response);
    return response;
  }

public static String getRetriveTransactionDetailsParam(Payable payable, String tid)
{
    Map<String, Object> parameters    = new HashMap<>();
    
    Map<String, String> transactionParameters    = new HashMap<>();
    transactionParameters.put("tid", tid);
    
    Map<String, String> customParameters = new HashMap<>();
    customParameters.put("lang", getLanguage(payable));
    
    parameters.put("transaction", transactionParameters);
    parameters.put("custom", customParameters);

    String jsonString =  new Gson().toJson(parameters);
    return jsonString;
}

public static String getTransactionNotes(JsonObject response, LocalizationProvider localizationProvider)
{
    LocalizationContext localizationContext = LocalizationContext.create();
    String comment = "";
    String newLine = "\n";
    
    if(response.get("transaction") != null) {
        JsonObject transaction = response.get("transaction").getAsJsonObject();
        if (!(nnIsEmpty(transaction.get("tid"))))
        comment += localizationProvider.getText(localizationContext, "novalnet.transactionid", transaction.get("tid").getAsString()) + newLine;
        
        if (transaction.get("test_mode") != null && transaction.get("test_mode").getAsInt() == 1)
        comment += localizationProvider.getText(localizationContext, "novalnet.testorder") + newLine;
        
        String[] failureStatus = {"FAILURE", "DEACTIVATED"};

        if(!(nnIsEmpty(transaction.get("status")))) {
           if(!(Arrays.asList(failureStatus).contains(transaction.get("status").getAsString()))) {
                String paymentType = transaction.get("payment_type").getAsString();
                
                if(!(nnIsEmpty(transaction.get("bank_details")))) {
                    comment += getBankDetails(transaction, localizationProvider);
                }
                else if(!(nnIsEmpty(transaction.get("nearest_stores")))){
                    comment += getNearestStoreDetails(transaction, localizationProvider);
                }
                else if("MULTIBANCO".equals(paymentType)) {
                    Integer amount = transaction.get("amount").getAsInt();
                    String currency = transaction.get("currency").getAsString();
                    Money formattedAmount =  getFormattedAmount(amount, currency);
                    
                    comment += localizationProvider.getText(localizationContext, "novalnet.multibanco_paymentreference_note", formattedAmount.toString()) + newLine;
                    comment += localizationProvider.getText(localizationContext, "novalnet.multibanco_paymentreference", transaction.get("partner_payment_reference").toString()) + newLine;
                    comment += localizationProvider.getText(localizationContext, "novalnet.multibanco_entity", transaction.get("service_supplier_id").toString()) + newLine;
                }
           }
           else if(response.get("result") != null){
               if(response.get("result").getAsJsonObject().get("status_text") != null)
                   comment += response.get("result").getAsJsonObject().get("status_text").getAsString();
           }
        }
    }
    return comment;
}

public static String getNearestStoreDetails(JsonObject transactionDetails, LocalizationProvider localizationProvider) {
    String comment = "";
    String newLine = "\n";
    LocalizationContext localizationContext = LocalizationContext.create();
    
    comment += localizationProvider.getText(localizationContext, "novalnet.cashpayment_slipexiprydate", transactionDetails.get("due_date").getAsString()) + newLine;
    
    if(!nnIsEmpty(transactionDetails.get("nearest_stores"))) {
        comment += localizationProvider.getText(localizationContext, "novalnet.cashpayment_stores") + newLine;
        
        JsonObject nearestStores = transactionDetails.get("nearest_stores").getAsJsonObject();
        Iterator<String> nearestStoreKeys = nearestStores.keySet().iterator(); 
        
        while(nearestStoreKeys.hasNext()){
            String nearestStoreKey = nearestStoreKeys.next();
            JsonObject storeDetails = nearestStores.get(nearestStoreKey).getAsJsonObject();
            comment += storeDetails.get("store_name").getAsString() + newLine;
            comment += storeDetails.get("street").getAsString() + newLine;
            comment += storeDetails.get("city").getAsString() + newLine;
            comment += storeDetails.get("zip").getAsString() + newLine;
            comment += storeDetails.get("country_code").getAsString() + newLine + newLine;
        }
    }
    
    return comment;
}

public static String getBankDetails(JsonObject transactionDetails, LocalizationProvider localizationProvider) {
    String comment = "";
    String newLine = "\n";
    LocalizationContext localizationContext = LocalizationContext.create();
    
    String paymentType = transactionDetails.get("payment_type").getAsString();
    String status = transactionDetails.get("status").getAsString();
    String tid = transactionDetails.get("tid").getAsString();
    
    String[] guaranteedPayments = {"GUARANTEED_INVOICE", "GUARANTEED_DIRECT_DEBIT_SEPA"};
    if((Arrays.asList(guaranteedPayments).contains(paymentType)) && status.equalsIgnoreCase("PENDING")) {
        comment = localizationProvider.getText(localizationContext, "novalnet.guaranteed_pending") + newLine;
    }
    else {
        JsonObject bankDetails = transactionDetails.get("bank_details").getAsJsonObject();
        Integer amount = transactionDetails.get("amount").getAsInt();
        String currency = transactionDetails.get("currency").getAsString();
        Money formattedAmount =  getFormattedAmount(amount, currency);
        
        comment = localizationProvider.getText(localizationContext, "novalnet.amount_transfer", formattedAmount.toString()) + newLine;
        if(status.equalsIgnoreCase("ON_HOLD") == false && !(nnIsEmpty(transactionDetails.get("due_date")))) {
            comment = localizationProvider.getText(localizationContext, "novalnet.amount_transfer_with_duedate", formattedAmount.toString(), transactionDetails.get("due_date").getAsString()) + newLine;
        }
        
        if(!(nnIsEmpty(bankDetails.get("account_holder"))))
            comment += localizationProvider.getText(localizationContext, "novalnet.accountholder", bankDetails.get("account_holder").getAsString()) + newLine;
        
        if(!(nnIsEmpty(bankDetails.get("bank_name"))))
            comment += localizationProvider.getText(localizationContext, "novalnet.bankname", bankDetails.get("bank_name").getAsString()) + newLine;
        
        if(!(nnIsEmpty(bankDetails.get("bank_place"))))
            comment += localizationProvider.getText(localizationContext, "novalnet.place", bankDetails.get("bank_place").getAsString()) + newLine;
        
        if(!(nnIsEmpty(bankDetails.get("iban"))))
            comment += localizationProvider.getText(localizationContext, "novalnet.iban", bankDetails.get("iban").getAsString()) + newLine;
        
        if(!(nnIsEmpty(bankDetails.get("bic"))))
            comment += localizationProvider.getText(localizationContext, "novalnet.bic", bankDetails.get("bic").getAsString()) + newLine;
        
        comment += localizationProvider.getText(localizationContext, "novalnet.payment_reference_note") + newLine;
        comment += localizationProvider.getText(localizationContext, "novalnet.payment_reference", 1, tid) + newLine;
        if(!(nnIsEmpty(transactionDetails.get("invoice_ref")))) {
            comment += localizationProvider.getText(localizationContext, "novalnet.payment_reference", 2, transactionDetails.get("invoice_ref").getAsString()) + newLine;
        }
    }
    
    return comment;
}

public static Money getFormattedAmount(Integer amount, String currency) {
    BigDecimal decimalAmount = new BigDecimal(amount).movePointLeft(2);
    Money amountMoney = new MoneyImpl(decimalAmount, currency);
    return amountMoney;
}

public static Integer getValidInteger(String data) {
    Integer validNumber = 0;
    if(!nnIsEmpty(data)) {
        data = data.replaceAll("[^0-9]", "");
        validNumber = Integer.parseInt(data);
    }
    return validNumber;
}

public static String getNovalnetPaymentType(String paymentName)
{
    Map<String, String> paymentTypes    = new HashMap<>();
    paymentTypes.put("NOVALNET_INVOICE", "INVOICE");
    paymentTypes.put("NOVALNET_IDEAL", "IDEAL");
    paymentTypes.put("NOVALNET_CREDITCARD", "CREDITCARD");
    paymentTypes.put("NOVALNET_PREPAYMENT", "PREPAYMENT");
    paymentTypes.put("NOVALNET_CASHPAYMENT", "CASHPAYMENT");
    paymentTypes.put("NOVALNET_SEPA", "DIRECT_DEBIT_SEPA");
    paymentTypes.put("NOVALNET_GUARANTEEDSEPA", "GUARANTEED_DIRECT_DEBIT_SEPA");
    paymentTypes.put("NOVALNET_GUARANTEEDINVOICE", "GUARANTEED_INVOICE");
    paymentTypes.put("NOVALNET_SOFORT", "ONLINE_TRANSFER");
    paymentTypes.put("NOVALNET_PAYPAL", "PAYPAL");
    paymentTypes.put("NOVALNET_EPS", "EPS");
    paymentTypes.put("NOVALNET_GIROPAY", "GIROPAY");
    paymentTypes.put("NOVALNET_PRZELEWY", "PRZELEWY24");
    paymentTypes.put("NOVALNET_BANKTRANSFER", "ONLINE_BANK_TRANSFER");
    paymentTypes.put("NOVALNET_POSTFINANCE", "POSTFINANCE");
    paymentTypes.put("NOVALNET_POSTFINANCECARD", "POSTFINANCE_CARD");
    paymentTypes.put("NOVALNET_TRUSTLY", "TRUSTLY");
    paymentTypes.put("NOVALNET_MULTIBANCO", "MULTIBANCO");
    paymentTypes.put("NOVALNET_BANCONTACT", "BANCONTACT");
    return paymentTypes.get(paymentName);
}

public static String getPaymentAction(Payable payable, Map<String, Object> paymentConfiguration)
{
    String paymentAction = "payment";
    if(paymentConfiguration.get("paymentAction") != null) {
        String configuredPaymentAction = paymentConfiguration.get("paymentAction").toString();
        if(configuredPaymentAction.equalsIgnoreCase("authorize")) {
            paymentAction = "authorize";
            if(paymentConfiguration.get("authorizationMinAmount") != null && (NovalnetUtil.getOrderAmount(payable) < (int)paymentConfiguration.get("authorizationMinAmount"))) {
                paymentAction = "payment";
            }
        }
    }
    return paymentAction;
}
}
