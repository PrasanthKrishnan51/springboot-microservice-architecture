package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T13:34:04+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Homebrew)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductResponse toResponse(Product p) {
        if ( p == null ) {
            return null;
        }

        ProductResponse.ProductResponseBuilder productResponse = ProductResponse.builder();

        productResponse.id( p.getId() );
        productResponse.name( p.getName() );
        productResponse.description( p.getDescription() );
        productResponse.category( p.getCategory() );
        productResponse.brand( p.getBrand() );
        productResponse.sellerId( p.getSellerId() );
        productResponse.price( p.getPrice() );
        productResponse.discountedPrice( p.getDiscountedPrice() );
        productResponse.stock( p.getStock() );
        productResponse.rating( p.getRating() );
        productResponse.reviewCount( p.getReviewCount() );
        List<String> list = p.getImageUrls();
        if ( list != null ) {
            productResponse.imageUrls( new ArrayList<String>( list ) );
        }
        List<String> list1 = p.getTags();
        if ( list1 != null ) {
            productResponse.tags( new ArrayList<String>( list1 ) );
        }
        productResponse.status( p.getStatus() );
        productResponse.createdAt( p.getCreatedAt() );
        productResponse.updatedAt( p.getUpdatedAt() );

        return productResponse.build();
    }

    @Override
    public Product toEntity(CreateProductRequest req) {
        if ( req == null ) {
            return null;
        }

        Product.ProductBuilder product = Product.builder();

        product.name( req.getName() );
        product.description( req.getDescription() );
        product.category( req.getCategory() );
        product.brand( req.getBrand() );
        product.price( req.getPrice() );
        product.stock( req.getStock() );
        List<String> list = req.getImageUrls();
        if ( list != null ) {
            product.imageUrls( new ArrayList<String>( list ) );
        }
        List<String> list1 = req.getTags();
        if ( list1 != null ) {
            product.tags( new ArrayList<String>( list1 ) );
        }

        return product.build();
    }

    @Override
    public void updateFromRequest(UpdateProductRequest req, Product p) {
        if ( req == null ) {
            return;
        }

        if ( req.getName() != null ) {
            p.setName( req.getName() );
        }
        if ( req.getDescription() != null ) {
            p.setDescription( req.getDescription() );
        }
        if ( req.getCategory() != null ) {
            p.setCategory( req.getCategory() );
        }
        if ( req.getBrand() != null ) {
            p.setBrand( req.getBrand() );
        }
        if ( req.getPrice() != null ) {
            p.setPrice( req.getPrice() );
        }
        if ( req.getDiscountedPrice() != null ) {
            p.setDiscountedPrice( req.getDiscountedPrice() );
        }
        if ( p.getImageUrls() != null ) {
            List<String> list = req.getImageUrls();
            if ( list != null ) {
                p.getImageUrls().clear();
                p.getImageUrls().addAll( list );
            }
        }
        else {
            List<String> list = req.getImageUrls();
            if ( list != null ) {
                p.setImageUrls( new ArrayList<String>( list ) );
            }
        }
        if ( p.getTags() != null ) {
            List<String> list1 = req.getTags();
            if ( list1 != null ) {
                p.getTags().clear();
                p.getTags().addAll( list1 );
            }
        }
        else {
            List<String> list1 = req.getTags();
            if ( list1 != null ) {
                p.setTags( new ArrayList<String>( list1 ) );
            }
        }
    }
}
