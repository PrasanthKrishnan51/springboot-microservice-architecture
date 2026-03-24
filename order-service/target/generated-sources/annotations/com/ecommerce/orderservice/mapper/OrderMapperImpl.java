package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dao.Order;
import com.ecommerce.orderservice.dao.OrderItem;
import com.ecommerce.orderservice.dao.ShippingAddress;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ShippingAddressRequest;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-23T22:08:54+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Homebrew)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Override
    public OrderResponse toResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponse.OrderResponseBuilder orderResponse = OrderResponse.builder();

        orderResponse.id( order.getId() );
        orderResponse.userId( order.getUserId() );
        orderResponse.correlationId( order.getCorrelationId() );
        List<OrderItem> list = order.getItems();
        if ( list != null ) {
            orderResponse.items( new ArrayList<OrderItem>( list ) );
        }
        orderResponse.totalAmount( order.getTotalAmount() );
        orderResponse.status( order.getStatus() );
        orderResponse.shippingAddress( order.getShippingAddress() );
        orderResponse.paymentId( order.getPaymentId() );
        orderResponse.trackingNumber( order.getTrackingNumber() );
        orderResponse.createdAt( order.getCreatedAt() );
        orderResponse.updatedAt( order.getUpdatedAt() );

        return orderResponse.build();
    }

    @Override
    public ShippingAddress toShippingAddress(ShippingAddressRequest req) {
        if ( req == null ) {
            return null;
        }

        ShippingAddress.ShippingAddressBuilder shippingAddress = ShippingAddress.builder();

        shippingAddress.street( req.getStreet() );
        shippingAddress.city( req.getCity() );
        shippingAddress.state( req.getState() );
        shippingAddress.zipCode( req.getZipCode() );
        shippingAddress.country( req.getCountry() );
        shippingAddress.recipientName( req.getRecipientName() );
        shippingAddress.phone( req.getPhone() );

        return shippingAddress.build();
    }
}
