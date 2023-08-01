package com.intershop.adapter.payment.novalnet.internal;

import java.util.HashMap;
import java.util.Map;

import com.intershop.beehive.configuration.capi.common.Configuration;
import com.intershop.component.service.capi.service.ConfigurationProvider;
import com.intershop.component.service.capi.service.ServiceConfigurationBO;

public class NovalnetConfig
{
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPaymentConfiguration(ServiceConfigurationBO serviceConfigurationBO, String paymentType) {
        Map<String, Object> paymentConfigurations  = new HashMap<>();
        
        ConfigurationProvider configProviderExtension = serviceConfigurationBO.getExtension(ConfigurationProvider.class);
        Configuration configuration = configProviderExtension.getConfiguration();
        switch(paymentType) {
            case "MERCHANT_CONFIG":
                ServiceConfigurationBO nnMerchantConfigServiceConfigurationBO = serviceConfigurationBO.getRepository().getServiceConfigurationBOByName("Novalnet Merchant Configuration");
                if(nnMerchantConfigServiceConfigurationBO != null ) {
                    ConfigurationProvider nnMerchantConfigProviderExtension = nnMerchantConfigServiceConfigurationBO.getExtension(ConfigurationProvider.class);
                    Configuration nnMerchantConfig = nnMerchantConfigProviderExtension.getConfiguration();
                    
                    paymentConfigurations.put("productActivationKey", nnMerchantConfig.getString("NovalnetGlobalConfigPaymentServiceDefinition.ProductActivationKey"));
                    paymentConfigurations.put("paymentAccessKey", nnMerchantConfig.getString("NovalnetGlobalConfigPaymentServiceDefinition.PaymentAccessKey"));
                    paymentConfigurations.put("tariff", nnMerchantConfig.getString("NovalnetGlobalConfigPaymentServiceDefinition.TariffID"));
                    paymentConfigurations.put("webhookTestMode", nnMerchantConfig.getBoolean("NovalnetGlobalConfigPaymentServiceDefinition.WebhookTestMode"));
                }
                break;
            case "INVOICE":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetInvoicePaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("paymentAction", configuration.getString("NovalnetInvoicePaymentServiceDefinition.PaymentAction"));
                paymentConfigurations.put("dueDate", configuration.getInteger("NovalnetInvoicePaymentServiceDefinition.DueDate"));
                paymentConfigurations.put("authorizationMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetInvoicePaymentServiceDefinition.AuthorizationMinAmount")));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetInvoicePaymentServiceDefinition.PaymentDescription"));
                break;
            case "PREPAYMENT":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetPrepaymentPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("dueDate", configuration.getInteger("NovalnetPrepaymentPaymentServiceDefinition.DueDate"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetPrepaymentPaymentServiceDefinition.PaymentDescription"));
                break;
            case "CASHPAYMENT":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetCashpaymentPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("dueDate", configuration.getInteger("NovalnetCashpaymentPaymentServiceDefinition.DueDate"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetCashpaymentPaymentServiceDefinition.PaymentDescription"));
                break;
            case "DIRECT_DEBIT_SEPA":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetSepaPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("paymentAction", configuration.getString("NovalnetSepaPaymentServiceDefinition.PaymentAction"));
                paymentConfigurations.put("dueDate", configuration.getInteger("NovalnetSepaPaymentServiceDefinition.DueDate"));
                paymentConfigurations.put("authorizationMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetSepaPaymentServiceDefinition.AuthorizationMinAmount")));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetSepaPaymentServiceDefinition.PaymentDescription"));
                break;
            case "GUARANTEED_DIRECT_DEBIT_SEPA":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetGuaranteedsepaPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("paymentAction", configuration.getString("NovalnetGuaranteedsepaPaymentServiceDefinition.PaymentAction"));
                paymentConfigurations.put("authorizationMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetGuaranteedsepaPaymentServiceDefinition.AuthorizationMinAmount")));
                paymentConfigurations.put("guaranteedMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetGuaranteedsepaPaymentServiceDefinition.GuaranteedMinAmount")));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetGuaranteedsepaPaymentServiceDefinition.PaymentDescription"));
                break;
            case "GUARANTEED_INVOICE":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetGuaranteedinvoicePaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("paymentAction", configuration.getString("NovalnetGuaranteedinvoicePaymentServiceDefinition.PaymentAction"));
                paymentConfigurations.put("authorizationMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetGuaranteedinvoicePaymentServiceDefinition.AuthorizationMinAmount")));
                paymentConfigurations.put("guaranteedMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetGuaranteedinvoicePaymentServiceDefinition.GuaranteedMinAmount")));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetGuaranteedinvoicePaymentServiceDefinition.PaymentDescription"));
                break;
            case "CREDITCARD":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetCreditcardPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("paymentAction", configuration.getString("NovalnetCreditcardPaymentServiceDefinition.PaymentAction"));
                paymentConfigurations.put("authorizationMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetCreditcardPaymentServiceDefinition.AuthorizationMinAmount")));
                paymentConfigurations.put("enforce3d", configuration.getBoolean("NovalnetCreditcardPaymentServiceDefinition.Enforce3D"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetCreditcardPaymentServiceDefinition.PaymentDescription"));
                break;
            case "IDEAL":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetIdealPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetIdealPaymentServiceDefinition.PaymentDescription"));
                break;
            case "ONLINE_TRANSFER":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetSofortPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetSofortPaymentServiceDefinition.PaymentDescription"));
                break;
            case "PAYPAL":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetPaypalPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("paymentAction", configuration.getString("NovalnetPaypalPaymentServiceDefinition.PaymentAction"));
                paymentConfigurations.put("authorizationMinAmount", NovalnetUtil.getValidInteger(configuration.getString("NovalnetPaypalPaymentServiceDefinition.AuthorizationMinAmount")));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetPaypalPaymentServiceDefinition.PaymentDescription"));
                break;
            case "EPS":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetEpsPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetEpsPaymentServiceDefinition.PaymentDescription"));
                break;
            case "GIROPAY":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetGiropayPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetGiropayPaymentServiceDefinition.PaymentDescription"));
                break;
            case "PRZELEWY24":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetPrzelewyPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetPrzelewyPaymentServiceDefinition.PaymentDescription"));
                break;
            case "ONLINE_BANK_TRANSFER":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetBanktransferPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetBanktransferPaymentServiceDefinition.PaymentDescription"));
                break;
            case "POSTFINANCE":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetPostfinancePaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetPostfinancePaymentServiceDefinition.PaymentDescription"));
                break;
            case "POSTFINANCE_CARD":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetPostfinancecardPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetPostfinancecardPaymentServiceDefinition.PaymentDescription"));
                break;
            case "TRUSTLY":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetTrustlyPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetTrustlyPaymentServiceDefinition.PaymentDescription"));
                break;
            case "MULTIBANCO":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetMultibancoPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetMultibancoPaymentServiceDefinition.PaymentDescription"));
                break;
            case "BANCONTACT":
                paymentConfigurations.put("testMode", configuration.getBoolean("NovalnetBancontactPaymentServiceDefinition.TestMode"));
                paymentConfigurations.put("additionalNote", configuration.getString("NovalnetBancontactPaymentServiceDefinition.PaymentDescription"));
                break;
        }
        
        return paymentConfigurations;
    }
}
