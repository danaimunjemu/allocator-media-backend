package com.allocator.notificationservice.provider;

import java.util.Optional;

public interface MailchimpAudienceProvider {

    /**
     * Adds or updates a member in the Mailchimp audience.
     * Will only subscribe (set status=subscribed) when consentGiven is true — GDPR requirement.
     *
     * @param email        subscriber email
     * @param brandId      tag applied for segmentation
     * @param consentGiven GDPR consent flag; if false, member is NOT added
     * @return Mailchimp subscriber hash (MD5 of lowercase email), or empty if skipped/failed
     */
    Optional<String> addMember(String email, String brandId, boolean consentGiven);

    /**
     * Archives (unsubscribes) a member in Mailchimp. Does not permanently delete.
     *
     * @param email subscriber email to archive
     */
    void archiveMember(String email);
}
