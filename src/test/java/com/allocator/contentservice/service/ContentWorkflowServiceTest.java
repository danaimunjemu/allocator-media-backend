package com.allocator.contentservice.service;

import com.allocator.contentservice.model.BrandAssignment;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.model.WorkflowAuditLog;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContentWorkflowService")
class ContentWorkflowServiceTest {

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
    UUID editorId;
    UUID authorId;
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
        editorId  = UUID.randomUUID();
        authorId  = UUID.randomUUID();
        brandId   = UUID.randomUUID();

        when(contentRevisionRepository.findMaxRevisionNumber(any())).thenReturn(null);
        when(contentRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventPublisher.publishContentCreated(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentUpdated(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentSubmitted(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentRejected(any(), any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentApproved(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentScheduled(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentPublished(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishArticlePublished(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentArchived(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentRestored(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(eventPublisher.publishContentUnpublished(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Content contentInStatus(ContentStatus status) {
        Content c = new Content();
        c.setId(contentId);
        c.setBrandId(brandId);
        c.setStatus(status);
        c.setTitle("Test Article");
        c.setSlug("test-article");
        c.setCreatedBy(authorId);
        c.setContentType(ContentType.ARTICLE);
        c.setAuthors(List.of());
        return c;
    }

    // ─── Workflow: submit for review ──────────────────────────────────────────

    @Nested
    @DisplayName("submitForReview")
    class SubmitForReview {

        @Test
        @DisplayName("DRAFT → REVIEW succeeds for EDITOR")
        void draftToReview_editor() {
            Content content = contentInStatus(ContentStatus.DRAFT);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.submitForReview(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.REVIEW);
            assertThat(result.getSubmittedBy()).isEqualTo(editorId);
            assertThat(result.getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("REVIEW content cannot be re-submitted")
        void reviewContentRejected() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.submitForReview(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("READER cannot submit")
        void readerCannotSubmit() {
            Content content = contentInStatus(ContentStatus.DRAFT);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.submitForReview(contentId, editorId, ContentWorkflowService.UserRole.READER))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not authorised");
        }
    }

    // ─── Workflow: approve ────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveContent")
    class ApproveContent {

        @Test
        @DisplayName("REVIEW → APPROVED succeeds when approver ≠ creator")
        void reviewToApproved() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.approveContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.APPROVED);
            assertThat(result.getApprovedBy()).isEqualTo(editorId);
            assertThat(result.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("Self-approval is rejected")
        void selfApprovalBlocked() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            // Creator = same as approver
            content.setCreatedBy(editorId);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.approveContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Four-eyes");
        }

        @Test
        @DisplayName("Cannot approve DRAFT content")
        void cannotApproveDraft() {
            Content content = contentInStatus(ContentStatus.DRAFT);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.approveContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REVIEW");
        }

        @Test
        @DisplayName("SUPER_ADMIN is exempt from four-eyes and may self-approve")
        void superAdminSelfApprovalAllowed() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            // Creator = same as approver
            content.setCreatedBy(editorId);
            content.setSubmittedBy(editorId);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.approveContent(contentId, editorId, ContentWorkflowService.UserRole.SUPER_ADMIN);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.APPROVED);
            assertThat(result.getApprovedBy()).isEqualTo(editorId);
        }
    }

    // ─── Workflow: reject ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectContent")
    class RejectContent {

        @Test
        @DisplayName("REVIEW → DRAFT with reason")
        void reviewToDraft() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.rejectContent(contentId, editorId,
                    ContentWorkflowService.UserRole.EDITOR, "Missing disclosure statement");

            assertThat(result.getStatus()).isEqualTo(ContentStatus.DRAFT);
            assertThat(result.getRejectionReason()).isEqualTo("Missing disclosure statement");
            assertThat(result.getRejectedBy()).isEqualTo(editorId);
        }

        @Test
        @DisplayName("Rejection without reason throws")
        void rejectionRequiresReason() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.rejectContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason");
        }

        @Test
        @DisplayName("Cannot reject APPROVED content")
        void cannotRejectApproved() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.rejectContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR, "Reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REVIEW");
        }
    }

    // ─── Workflow: publish ────────────────────────────────────────────────────

    @Nested
    @DisplayName("publishContent")
    class PublishContent {

        @Test
        @DisplayName("APPROVED → PUBLISHED succeeds")
        void approvedToPublished() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
            assertThat(result.getPublishedAt()).isNotNull();
            assertThat(result.getPublishedBy()).isEqualTo(editorId);
        }

        @Test
        @DisplayName("DRAFT cannot be published directly")
        void draftCannotBePublished() {
            Content content = contentInStatus(ContentStatus.DRAFT);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("REVIEW cannot be published directly (must go through APPROVED)")
        void reviewCannotBePublishedDirectly() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("Self-publish by non-admin creator/approver is a four-eyes violation")
        void selfPublishBlockedForEditor() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            content.setCreatedBy(editorId);
            content.setApprovedBy(editorId);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Four-eyes");
        }

        @Test
        @DisplayName("SUPER_ADMIN is exempt from four-eyes and may self-publish own approved content")
        void superAdminSelfPublishAllowed() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            // Same user created, approved, and compliance-approved the content
            content.setCreatedBy(editorId);
            content.setApprovedBy(editorId);
            content.setComplianceApprovedBy(editorId);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.publishContent(contentId, editorId, ContentWorkflowService.UserRole.SUPER_ADMIN);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
            assertThat(result.getPublishedBy()).isEqualTo(editorId);
        }
    }

    // ─── Workflow: archive and restore ───────────────────────────────────────

    @Nested
    @DisplayName("archiveContent and restoreFromArchive")
    class ArchiveRestore {

        @Test
        @DisplayName("PUBLISHED → ARCHIVED succeeds for ADMIN")
        void publishedToArchived() {
            Content content = contentInStatus(ContentStatus.PUBLISHED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.archiveContent(contentId, editorId, ContentWorkflowService.UserRole.ADMIN);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Sub-EDITOR role cannot archive")
        void editorCannotArchive() {
            Content content = contentInStatus(ContentStatus.PUBLISHED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.archiveContent(contentId, editorId, ContentWorkflowService.UserRole.ANALYST))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not authorised");
        }

        @Test
        @DisplayName("ARCHIVED → DRAFT (restore) succeeds for ADMIN")
        void archivedToDraft() {
            Content content = contentInStatus(ContentStatus.ARCHIVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            Content result = service.restoreFromArchive(contentId, editorId, ContentWorkflowService.UserRole.ADMIN);

            assertThat(result.getStatus()).isEqualTo(ContentStatus.DRAFT);
        }

        @Test
        @DisplayName("Cannot restore content that is not archived")
        void cannotRestoreNonArchived() {
            Content content = contentInStatus(ContentStatus.PUBLISHED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.restoreFromArchive(contentId, editorId, ContentWorkflowService.UserRole.ADMIN))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ARCHIVED");
        }
    }

    // ─── Workflow: scheduling as metadata ─────────────────────────────────────

    @Nested
    @DisplayName("scheduleContent")
    class ScheduleContent {

        @Test
        @DisplayName("APPROVED content can receive a future publish time")
        void setPublishTime() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
            Instant future = Instant.now().plusSeconds(3600);

            Content result = service.scheduleContent(contentId, future, editorId, ContentWorkflowService.UserRole.EDITOR);

            // Status stays APPROVED — scheduling is metadata, not a state
            assertThat(result.getStatus()).isEqualTo(ContentStatus.APPROVED);
            assertThat(result.getScheduledAt()).isEqualTo(future);
        }

        @Test
        @DisplayName("Past scheduledAt is rejected")
        void pastDateRejected() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.scheduleContent(contentId, Instant.now().minusSeconds(60),
                            editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("DRAFT content cannot be scheduled")
        void draftCannotBeScheduled() {
            Content content = contentInStatus(ContentStatus.DRAFT);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            assertThatThrownBy(() ->
                    service.scheduleContent(contentId, Instant.now().plusSeconds(3600),
                            editorId, ContentWorkflowService.UserRole.EDITOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APPROVED");
        }
    }

    // ─── Auto-publisher ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("publishScheduledContent (auto-publisher)")
    class AutoPublish {

        @Test
        @DisplayName("Auto-publishes APPROVED content with elapsed scheduledAt")
        void autoPublishesElapsedContent() {
            Content content = contentInStatus(ContentStatus.APPROVED);
            content.setScheduledAt(Instant.now().minusSeconds(60));
            when(contentRepository.findScheduledContentToPublish(any())).thenReturn(List.of(content));

            List<Content> published = service.publishScheduledContent();

            assertThat(published).hasSize(1);
            assertThat(published.get(0).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
            assertThat(published.get(0).getPublishedAt()).isNotNull();
        }
    }

    // ─── Audit trail ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Audit trail recording")
    class AuditTrail {

        @Test
        @DisplayName("Approval records an audit entry with actor and status")
        void approvalRecordsAuditEntry() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

            service.approveContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR);

            ArgumentCaptor<WorkflowAuditLog> captor = ArgumentCaptor.forClass(WorkflowAuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(captor.capture());

            WorkflowAuditLog log = captor.getValue();
            assertThat(log.getEventType()).isEqualTo(WorkflowEventType.APPROVED);
            assertThat(log.getActorId()).isEqualTo(editorId);
            assertThat(log.getFromStatus()).isEqualTo("REVIEW");
            assertThat(log.getToStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Rejection records reason in audit entry")
        void rejectionRecordsReason() {
            Content content = contentInStatus(ContentStatus.REVIEW);
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
            String reason = "Missing analyst disclosure";

            service.rejectContent(contentId, editorId, ContentWorkflowService.UserRole.EDITOR, reason);

            ArgumentCaptor<WorkflowAuditLog> captor = ArgumentCaptor.forClass(WorkflowAuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(captor.capture());

            WorkflowAuditLog log = captor.getValue();
            assertThat(log.getEventType()).isEqualTo(WorkflowEventType.RETURNED);
            assertThat(log.getReason()).isEqualTo(reason);
        }
    }

    // ─── updateDraft — single revision ───────────────────────────────────────

    @Nested
    @DisplayName("updateDraft")
    class UpdateDraft {

        @Test
        @DisplayName("Produces exactly one revision per update")
        void singleRevisionPerUpdate() {
            Content existing = contentInStatus(ContentStatus.DRAFT);
            Content updated  = contentInStatus(ContentStatus.DRAFT);
            updated.setTitle("Updated Title");
            when(contentRepository.findById(contentId)).thenReturn(Optional.of(existing));

            service.updateDraft(contentId, updated, editorId, ContentWorkflowService.UserRole.EDITOR);

            verify(contentRevisionRepository, times(1)).save(any());
        }
    }
}
