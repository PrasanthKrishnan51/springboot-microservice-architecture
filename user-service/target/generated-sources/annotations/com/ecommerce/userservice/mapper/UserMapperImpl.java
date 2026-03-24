package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.dao.Address;
import com.ecommerce.userservice.dao.User;
import com.ecommerce.userservice.dto.UpdateProfileRequest;
import com.ecommerce.userservice.dto.UserResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-23T22:08:56+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Homebrew)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( user.getId() );
        userResponse.email( user.getEmail() );
        userResponse.firstName( user.getFirstName() );
        userResponse.lastName( user.getLastName() );
        userResponse.phone( user.getPhone() );
        userResponse.role( user.getRole() );
        userResponse.enabled( user.isEnabled() );
        List<Address> list = user.getAddresses();
        if ( list != null ) {
            userResponse.addresses( new ArrayList<Address>( list ) );
        }
        userResponse.createdAt( user.getCreatedAt() );

        return userResponse.build();
    }

    @Override
    public void updateFromRequest(UpdateProfileRequest req, User user) {
        if ( req == null ) {
            return;
        }

        if ( req.getFirstName() != null ) {
            user.setFirstName( req.getFirstName() );
        }
        if ( req.getLastName() != null ) {
            user.setLastName( req.getLastName() );
        }
        if ( req.getPhone() != null ) {
            user.setPhone( req.getPhone() );
        }
    }
}
