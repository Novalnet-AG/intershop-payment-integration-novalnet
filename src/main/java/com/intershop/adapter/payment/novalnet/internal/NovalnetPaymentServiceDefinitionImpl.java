package com.intershop.adapter.payment.novalnet.internal;

import java.util.ArrayList;
import java.util.Collection;

import com.intershop.api.service.payment.v1.PaymentService;
import com.intershop.component.service.capi.assignment.ServiceProvider;
import com.intershop.component.service.capi.service.ServiceDefinition;

public abstract class NovalnetPaymentServiceDefinitionImpl implements ServiceDefinition
{
    
    private final Collection<Class<?>> serviceInterfaces = new ArrayList<>(1);
    
    public NovalnetPaymentServiceDefinitionImpl() {
        serviceInterfaces.add(PaymentService.class);
    }

    @Override
    public Collection<Class<?>> getServiceInterfaces()
    {
        return this.serviceInterfaces;
    }

    
    protected class PaymentServiceProvider implements ServiceProvider
    {
        private final PaymentService paymentService;
        
        public PaymentServiceProvider(PaymentService paymentService) {
          this.paymentService = paymentService;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getServiceAdapter(Class<T> serviceInterface) {
          if (!serviceInterface.isAssignableFrom(PaymentService.class))
            throw new IllegalArgumentException("Can't provide an implementation for requested interface: " + serviceInterface
                .getName()); 
          return (T)this.paymentService;
        }
    }

}
