package eu.exeris.caps.audittrail;

import eu.exeris.caps.audittrail.api.AuditEvent;
import eu.exeris.caps.audittrail.api.AuditLogSink;
import eu.exeris.caps.audittrail.api.AuditQuery;
import eu.exeris.caps.audittrail.internal.InMemoryAuditLogSink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("exeris-caps-audit-trail — Capability Unit Tests")
class AuditTrailTest {

    @Test
    @DisplayName("Emit and query audit events with filtering")
    void testEmitAndFilter() {
        AuditLogSink sink = new InMemoryAuditLogSink();
        Instant now = Instant.now();

        sink.emit(AuditEvent.builder()
                .action("CONTENT_CREATED")
                .resourceType("blog-post")
                .resourceId("post-1")
                .actorId("editor-alice")
                .timestamp(now.minusSeconds(30))
                .details(Map.of("title", "Post 1"))
                .build());

        sink.emit(AuditEvent.builder()
                .action("CONTENT_PUBLISHED")
                .resourceType("blog-post")
                .resourceId("post-1")
                .actorId("admin-bob")
                .timestamp(now.minusSeconds(20))
                .build());

        sink.emit(AuditEvent.builder()
                .action("ASSET_UPLOADED")
                .resourceType("asset")
                .resourceId("img-1")
                .actorId("editor-alice")
                .timestamp(now.minusSeconds(10))
                .build());

        // 1. Query all
        List<AuditEvent> all = sink.query(AuditQuery.builder().limit(10).build());
        assertThat(all).hasSize(3);
        // Verified sorted timestamp descending
        assertThat(all.get(0).action()).isEqualTo("ASSET_UPLOADED");
        assertThat(all.get(1).action()).isEqualTo("CONTENT_PUBLISHED");
        assertThat(all.get(2).action()).isEqualTo("CONTENT_CREATED");

        // 2. Filter by resourceType
        List<AuditEvent> blogEvents = sink.query(AuditQuery.builder().resourceType("blog-post").build());
        assertThat(blogEvents).hasSize(2);

        // 3. Filter by resourceId
        List<AuditEvent> post1Events = sink.query(AuditQuery.builder().resourceId("post-1").build());
        assertThat(post1Events).hasSize(2);

        // 4. Filter by action
        List<AuditEvent> uploadEvents = sink.query(AuditQuery.builder().action("ASSET_UPLOADED").build());
        assertThat(uploadEvents).hasSize(1);
        assertThat(uploadEvents.get(0).resourceId()).isEqualTo("img-1");

        // 5. Filter by actorId
        List<AuditEvent> aliceEvents = sink.query(AuditQuery.builder().actorId("editor-alice").build());
        assertThat(aliceEvents).hasSize(2);

        // 6. Filter by time range
        List<AuditEvent> rangeEvents = sink.query(AuditQuery.builder()
                .from(now.minusSeconds(25))
                .to(now.minusSeconds(15))
                .build());
        assertThat(rangeEvents).hasSize(1);
        assertThat(rangeEvents.get(0).action()).isEqualTo("CONTENT_PUBLISHED");
    }

    @Test
    @DisplayName("Pagination and count work accurately")
    void testPaginationAndCount() {
        AuditLogSink sink = new InMemoryAuditLogSink();
        Instant base = Instant.now().minusSeconds(100);

        for (int i = 0; i < 25; i++) {
            sink.emit(AuditEvent.builder()
                    .action("CONTENT_UPDATED")
                    .resourceType("page")
                    .resourceId("page-" + i)
                    .timestamp(base.plusSeconds(i))
                    .build());
        }

        AuditQuery queryPage1 = AuditQuery.builder().resourceType("page").limit(10).offset(0).build();
        List<AuditEvent> page1 = sink.query(queryPage1);
        assertThat(page1).hasSize(10);
        assertThat(page1.get(0).resourceId()).isEqualTo("page-24"); // newest

        AuditQuery queryPage2 = AuditQuery.builder().resourceType("page").limit(10).offset(10).build();
        List<AuditEvent> page2 = sink.query(queryPage2);
        assertThat(page2).hasSize(10);
        assertThat(page2.get(0).resourceId()).isEqualTo("page-14");

        AuditQuery queryPage3 = AuditQuery.builder().resourceType("page").limit(10).offset(20).build();
        List<AuditEvent> page3 = sink.query(queryPage3);
        assertThat(page3).hasSize(5);
        assertThat(page3.get(0).resourceId()).isEqualTo("page-4");

        long totalCount = sink.count(queryPage1);
        assertThat(totalCount).isEqualTo(25);
    }

    @Test
    @DisplayName("Concurrency test: multi-threaded emission and querying")
    void testConcurrency() throws Exception {
        AuditLogSink sink = new InMemoryAuditLogSink();
        int threads = 10;
        int eventsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threads);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                final String actor = "actor-" + t;
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < eventsPerThread; i++) {
                            sink.emit(AuditEvent.builder()
                                    .action("ITEM_CREATED")
                                    .resourceType("item")
                                    .resourceId(UUID.randomUUID().toString())
                                    .actorId(actor)
                                    .build());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        long total = sink.count(AuditQuery.builder().action("ITEM_CREATED").build());
        assertThat(total).isEqualTo(500);

        List<AuditEvent> actor0Events = sink.query(AuditQuery.builder().actorId("actor-0").limit(100).build());
        assertThat(actor0Events).hasSize(50);
    }

    @Test
    @DisplayName("AuditTrailModule lifecycle and service wiring")
    void testModule() throws Exception {
        AuditTrailModule module = new AuditTrailModule();
        assertThat(module.auditLogSink()).isNotNull();
        assertThat(module.isReady()).isFalse();

        module.initialize();
        module.ready();
        assertThat(module.isReady()).isTrue();

        module.drain(Duration.ofSeconds(1));
        assertThat(module.isReady()).isFalse();

        module.terminate();
        assertThat(module.isReady()).isFalse();

        // Also test internal.AuditTrailModule
        eu.exeris.caps.audittrail.internal.AuditTrailModule internalModule =
                new eu.exeris.caps.audittrail.internal.AuditTrailModule();
        assertThat(internalModule.auditLogSink()).isNotNull();
    }

    @Test
    @DisplayName("AuditEvent handles null values in details map without NullPointerException")
    void testNullToleranceInDetails() {
        Map<String, Object> mapWithNull = new java.util.HashMap<>();
        mapWithNull.put("title", "Test");
        mapWithNull.put("summary", null);
        mapWithNull.put("tags", null);

        AuditEvent event = AuditEvent.builder()
                .action("CONTENT_CREATED")
                .resourceType("post")
                .resourceId("post-1")
                .details(mapWithNull)
                .build();

        assertThat(event.details()).containsKey("summary");
        assertThat(event.details().get("summary")).isNull();
    }
}
