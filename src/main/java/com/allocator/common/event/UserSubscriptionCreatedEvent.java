package com.allocator.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserSubscriptionCreatedEvent extends BaseEvent {
    private String userId;
    private String email;
    private String name;
    private String planTier;
    private String planName;
    private String renewalDate;
    private String subscriptionId;
    private String stripeSubscriptionId;
}
