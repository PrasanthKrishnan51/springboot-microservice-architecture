package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dao.Order;
import com.ecommerce.orderservice.dao.ShippingAddress;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ShippingAddressRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {
    OrderResponse toResponse(Order order);

    ShippingAddress toShippingAddress(ShippingAddressRequest req);
}
