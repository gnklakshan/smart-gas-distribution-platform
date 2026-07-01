package com.gastracker.user_service.service.transformer;

import com.gastracker.user_service.dao.entity.Dealer;
import com.gastracker.user_service.dao.entity.User;
import com.gastracker.user_service.dto.response.DealerInfo;
import com.gastracker.user_service.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserTransformer {

    public UserResponse toResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .nic(user.getNic())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt());

        if (user.getDealer() != null) {
            builder.dealer(toDealerInfo(user.getDealer()));
        }

        return builder.build();
    }

    public DealerInfo toDealerInfo(Dealer dealer) {
        return DealerInfo.builder()
                .dealerId(dealer.getId())
                .businessName(dealer.getBusinessName())
                .businessRegNo(dealer.getBusinessRegNo())
                .address(dealer.getAddress())
                .latitude(dealer.getLatitude())
                .longitude(dealer.getLongitude())
                .build();
    }
}
