package com.allocator.contentservice.service;

import com.allocator.contentservice.model.BrandAssignment;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.model.WorkflowEventType;
import com.allocator.contentservice.repository.BrandAssignmentRepository;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.contentservice.repository.ContentRevisionRepository;
import com.allocator.contentservice.repository.WorkflowAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContentWorkflowService — Phase 2")
class ContentWorkflowPhase2Test {

    @Mock ContentRepository contentRepository;
    @Mock ContentRevisionRepository contentRevisionRepository;
    @Mock WorkflowAuditLogRepository auditLogRepository;
    @Mock ContentEventPublisher eventPublisher;
    @Mock SlugService slugService;
    @Mock com.allocator.contentservice.repository.AuthorRepository authorRepository;
    @Mock Counter contentCreatedCounter;
    @Mock Counter contentPublishedCounter;
    @Mock Counter contentScheduledCounter;
    @Mock Counter contentFailedWorkflowCounter;
    @Mock BrandAssignmentRepository brandAssignmentRepository;
    WorkflowTransitionPolicy transitionPolicy = new WorkflowTransitionPolicy();
    CompliancePolicy compliancePolicy = new CompliancePolicy();

    ContentWorkflowService service;
    ObjectMapper objectMapper = new ObjectMapper();

    UUID contentId;
    UUID officerId;
    UUID editorId;
    UUID brandId;

    @BeforeEach
    void setUp() {
        service = new ContentWorkflowService(
                contentRepository, contentRevisionRepository, auditLogRepository,
                brandAssignmentRepository, eventPublisher, slugService, authorRepository, objectMapper,
                transitionPolicy, compliancePolicy,
                contentCreatedCounter, contentPublishedCounter,
                contentScheduledCounter, contentFailedWorkflowCounter
        );
        when(brandAssignmentRepository.findByUserIdAndBrandId(any(), any()))
                .thenReturn(Optional.of(BrandAssignment.builder().role("PUBLISHER").build()));
        when(contentRepository.findVersionGroup(any())).thenReturn(List.of());
        contentId = UUID.randomUUID();
        officerId = UUID.randomUUID();
        editorId  = UUID.randomUUID();
        brandId   = UUID.randomUUID();

        when(contentRevisionRepository.findMaxRevisionNumber(any())).thenReturn(null);
        when(contentRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventPublisher.publishContentCreated(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentPublished(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishArticlePublished(any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    private Content contentInStatus(ContentStatus status) {
        Content c = new Content();
        c.setId(contentId);
        c.setBrandId(brandId);
        c.setStatus(status);
        c.setTitle("Research Report");
        c.setSlug("research-report");
        c.setCreatedBy(UUID.randomUUID());
        c.setContentType(ContentType.RESEARCH);
        c.setAuthors(new ArrayList<>());
        c.setVersionNumber(1);
        c.setLatestVersion(true);
        return c;
    }

    // ─── Compliance ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Compliance approval")
    class ComplianceApprovalTests {

        @Test
        @DisplayName("COMPLIANCE_OFFICER can approve compliance for RESEARCH content")
        void complianceOfficerCanApprove() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.approveCompliance(contentId, officerId, ContentWorkflowService.UserRole.COMPLIANCE_OFFICER);

            assertThat(result.getComplianceApproved()).isTrue();
            assertThat(result.getComplianceApprovedBy()).isEqualTo(officerId);
            assertThat(result.getComplianceApprovedAt()).isNotNull();

            ArgumentCaptor<com.allocator.contentservice.model.WorkflowAuditLog> captor =
                    ArgumentCaptor.forClass(com.allocator.contentservice.model.WorkflowAuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(WorkflowEventType.COMPLIANCE_APPROVED);
        }

        @Test
        @DisplayName("ADMIN can also approve compliance")
        void adminCanApproveCompliance() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.approveCompliance(contentId, officerId, ContentWorkflowService.UserRole.ADMIN);
            assertThat(result.getComplianceApproved()).isTrue();
        }

        @Test
        @DisplayName("EDITOR cannot approve compliance")
        void editorCannotApproveCompliance() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.approveCompliance(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLIANCE_OFFICER");
        }

        @Test
        @DisplayName("Compliance approval is rejected for non-RESEARCH content")
        void nonResearchIsRejected() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            content.setContentType(ContentType.ARTICLE);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.approveCompliance(contentId, officerId, ContentWorkflowService.UserRole.COMPLIANCE_OFFICER))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RESEARCH");
        }

        @Test
        @DisplayName("Compliance rejection returns content to DRAFT with reason")
        void complianceRejectionReturnsToDraft() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.rejectCompliance(contentId, officerId,
                    ContentWorkflowService.UserRole.COMPLIANCE_OFFICER, "Missing disclosure statement");

            assertThat(result.getStatus()).isEqualTo(ContentStatus.DRAFT);
            assertThat(result.getComplianceApproved()).isFalse();

            ArgumentCaptor<com.allocator.contentservice.model.WorkflowAuditLog> captor =
                    ArgumentCaptor.forClass(com.allocator.contentservice.model.WorkflowAuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(WorkflowEventType.COMPLIANCE_REJECTED);
            assertThat(captor.getValue().getReason()).isEqualTo("Missing disclosure statement");
        }

        @Test
        @DisplayName("Compliance rejection requires a reason")
        void complianceRejectionRequiresReason() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.rejectCompliance(contentId, officerId,
                            ContentWorkflowService.UserRole.COMPLIANCE_OFFICER, "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason");
        }

        @Test
        @DisplayName("RESEARCH content without compliance approval cannot be published")
        void researchRequiresComplianceBeforePublish() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            content.setComplianceApproved(null);  // not yet reviewed
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("compliance approval");
        }

        @Test
        @DisplayName("RESEARCH content with compliance approval can be published")
        void researchWithComplianceCanBePublished() {
            Content content = contentInStatus(ContentStatus.COMPLIANCE_APPROVED);
            content.setComplianceApproved(true);
            content.setComplianceApprovedBy(officerId);
            content.setComplianceApprovedAt(Instant.now());
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);
            assertThat(result.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        }
    }

    // ─── Versioning ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Version creation")
    class VersioningTests {

        @Test
        @DisplayName("createContentRevision from PUBLISHED content creates DRAFT with incremented version")
        void createsRevisionFromPublished() {
            Content original = contentInStatus(ContentStatus.PUBLISHED);
            original.setVersionNumber(1);
            original.setParentContentId(null);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(original));
            when(contentRepository.findMaxVersionInGroup(contentId)).thenReturn(Optional.of(1));
            when(slugService.generateSlug(any())).thenReturn("research-report-v2");

            Content result = service.createContentRevision(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.DRAFT);
            assertThat(result.getVersionNumber()).isEqualTo(2);
            assertThat(result.getParentContentId()).isEqualTo(contentId);
            assertThat(result.getLatestVersion()).isTrue();
            // Original should have latestVersion set to false
            assertThat(original.getLatestVersion()).isFalse();
        }

        @Test
        @DisplayName("createContentRevision from APPROVED content is allowed")
        void createsRevisionFromApproved() {
            Content original = contentInStatus(ContentStatus.APPROVED);
            original.setVersionNumber(1);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(original));
            when(contentRepository.findMaxVersionInGroup(contentId)).thenReturn(Optional.of(1));
            when(slugService.generateSlug(any())).thenReturn("research-report-v2");

            Content result = service.createContentRevision(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);
            assertThat(result.getStatus()).isEqualTo(ContentStatus.DRAFT);
            assertThat(result.getVersionNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("createContentRevision from DRAFT content is rejected")
        void draftCannotBeRevised() {
            Content content = contentInStatus(ContentStatus.DRAFT);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.createContentRevision(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PUBLISHED or APPROVED");
        }

        @Test
        @DisplayName("READER cannot create content revision")
        void readerCannotCreateRevision() {
            Content content = contentInStatus(ContentStatus.PUBLISHED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.createContentRevision(contentId, editorId, ContentWorkflowService.UserRole.READER))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("authorised");
        }

        @Test
        @DisplayName("Revision version number uses max from group when group already has multiple versions")
        void usesMaxVersionInGroup() {
            UUID rootId = UUID.randomUUID();
            Content original = contentInStatus(ContentStatus.PUBLISHED);
            original.setParentContentId(rootId);  // this is v3; root is elsewhere
            original.setVersionNumber(3);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(original));
            when(contentRepository.findMaxVersionInGroup(rootId)).thenReturn(Optional.of(3));
            when(slugService.generateSlug(any())).thenReturn("slug-v4");

            Content result = service.createContentRevision(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);
            assertThat(result.getVersionNumber()).isEqualTo(4);
            assertThat(result.getParentContentId()).isEqualTo(rootId);
        }

        @Test
        @DisplayName("getVersionHistory returns empty list for content with no group")
        void versionHistoryForSingleContent() {
            Content content = contentInStatus(ContentStatus.PUBLISHED);
            content.setParentContentId(null);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
            when(contentRepository.findVersionGroup(contentId)).thenReturn(List.of(content));

            List<Content> history = service.getVersionHistory(contentId);
            assertThat(history).hasSize(1);
        }

        @Test
        @DisplayName("Audit event VERSION_CREATED is recorded when revision is created")
        void auditEventRecordedForRevision() {
            Content original = contentInStatus(ContentStatus.PUBLISHED);
            original.setVersionNumber(1);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(original));
            when(contentRepository.findMaxVersionInGroup(contentId)).thenReturn(Optional.of(1));
            when(slugService.generateSlug(any())).thenReturn("slug-v2");

            service.createContentRevision(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);

            ArgumentCaptor<com.allocator.contentservice.model.WorkflowAuditLog> captor =
                    ArgumentCaptor.forClass(com.allocator.contentservice.model.WorkflowAuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(captor.capture());
            boolean hasVersionCreated = captor.getAllValues().stream()
                    .anyMatch(a -> a.getEventType() == WorkflowEventType.VERSION_CREATED);
            assertThat(hasVersionCreated).isTrue();
        }
    }

    // ─── Brand assignment service ─────────────────────────────────────────────

    @Nested
    @DisplayName("BrandAssignmentService")
    class BrandAssignmentServiceTests {

        @Mock BrandAssignmentRepository brandAssignmentRepository;
        BrandAssignmentService brandAssignmentService;

        @BeforeEach
        void setUpBrandService() {
            brandAssignmentService = new BrandAssignmentService(brandAssignmentRepository);
            when(brandAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("assign creates a new BrandAssignment when none exists")
        void assignCreatesNew() {
            when(brandAssignmentRepository.existsByUserIdAndBrandId(any(), any())).thenReturn(false);

            BrandAssignment result = brandAssignmentService.assign(editorId, brandId, "EDITOR", officerId);

            assertThat(result.getUserId()).isEqualTo(editorId);
            assertThat(result.getBrandId()).isEqualTo(brandId);
            assertThat(result.getRole()).isEqualTo("EDITOR");
            assertThat(result.getAssignedBy()).isEqualTo(officerId);
        }

        @Test
        @DisplayName("assign updates role when assignment already exists")
        void assignUpdatesExisting() {
            BrandAssignment existing = BrandAssignment.builder()
                    .userId(editorId).brandId(brandId).role("EDITOR").assignedBy(officerId).build();
            when(brandAssignmentRepository.existsByUserIdAndBrandId(editorId, brandId)).thenReturn(true);
            when(brandAssignmentRepository.findByUserIdAndBrandId(editorId, brandId)).thenReturn(Optional.of(existing));

            BrandAssignment result = brandAssignmentService.assign(editorId, brandId, "ADMIN", officerId);
            assertThat(result.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("isAssigned returns true when assignment exists")
        void isAssignedReturnsTrue() {
            when(brandAssignmentRepository.existsByUserIdAndBrandId(editorId, brandId)).thenReturn(true);
            assertThat(brandAssignmentService.isAssigned(editorId, brandId)).isTrue();
        }

        @Test
        @DisplayName("revoke calls deleteByUserIdAndBrandId")
        void revokeDeletesAssignment() {
            brandAssignmentService.revoke(editorId, brandId);
            verify(brandAssignmentRepository).deleteByUserIdAndBrandId(editorId, brandId);
        }
    }
}
