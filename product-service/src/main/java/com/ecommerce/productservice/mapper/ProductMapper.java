package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.dto.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    ProductResponse toResponse(Product p);

    Product toEntity(CreateProductRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(UpdateProductRequest req, @MappingTarget Product p);
}
