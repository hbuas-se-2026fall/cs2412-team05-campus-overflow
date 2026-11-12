package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.InMemoryTags;
import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.StubRenderer;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 问题写路径用例测试：验证编排（标签计数差集、事件、审计、缓存失效）与权限入口。 */
class QuestionCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");
    private static final String TITLE = "红黑树插入后为什么必须旋转两次？";

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryTags tags = new InMemoryTags();
    private final RecordingEvents events = new RecordingEvents();
    private final StubRenderer renderer = new StubRenderer();
    private final CountingCache cache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT)
            .withUser(2, "小红", Role.STUDENT)
            .withCourseManager(9);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);

    private QuestionCommandService service;

    @BeforeEach
    void setUp() {
        service = new QuestionCommandService(questions, tags, identity, events, renderer, cache, audit, clock);
    }

    @Test
    @DisplayName("发布问题：标签 +1、发布事件、写审计、失效列表缓存")
    void postPublishesEventAndBumpsTagUsage() {
        long id = service.post(ming, new PostQuestionCommand(100L, TITLE, "插入后黑高不平衡，需要旋转恢复。",
                List.of("数据结构", "红黑树")));

        assertThat(questions.findById(id)).isPresent();
        assertThat(tags.usage).containsEntry("数据结构", 1).containsEntry("红黑树", 1);
        assertThat(tags.deltas).containsExactly(1);

        List<QuestionPostedEvent> posted = events.ofType(QuestionPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).questionId()).isEqualTo(id);
        assertThat(posted.get(0).authorId()).isEqualTo(ming.userId());
        assertThat(posted.get(0).courseId()).isEqualTo(100L);

        assertThat(audit.actions).containsExactly("QUESTION_POSTED");
        assertThat(cache.invalidations).isEqualTo(1);
        assertThat(renderer.plainTextLimits).containsExactly(QuestionCommandService.PLAIN_TEXT_FOR_INDEX);
    }

    @Test
    @DisplayName("编辑问题：标签只按差集调整，移除的 -1、新增的 +1")
    void editAdjustsOnlyTagDifference() {
        long id = service.post(ming, new PostQuestionCommand(100L, TITLE, "原始正文，描述现象与期望。",
                List.of("数据结构", "红黑树")));
        tags.deltas.clear();
        events.published.clear();
        cache.invalidations = 0;

        service.edit(ming, id, new EditQuestionCommand("红黑树插入后为什么必须旋转？补充：AVL 对比",
                "补充说明：AVL 与红黑树的旋转次数差异。", List.of("红黑树", "平衡树")));

        assertThat(tags.usage).containsEntry("数据结构", 0).containsEntry("红黑树", 1).containsEntry("平衡树", 1);
        assertThat(tags.deltas).containsExactlyInAnyOrder(-1, 1);
        assertThat(events.ofType(QuestionEditedEvent.class)).hasSize(1);
        assertThat(audit.actions).containsExactly("QUESTION_POSTED", "QUESTION_EDITED");
        assertThat(cache.invalidations).isEqualTo(1);
        assertThat(questions.findById(id).orElseThrow().title().value()).startsWith("红黑树插入后为什么必须旋转？");
    }

    @Test
    @DisplayName("删除问题：软删除、标签 -1、发布删除事件")
    void deleteSoftDeletesAndPublishesEvent() {
        long id = service.post(ming, new PostQuestionCommand(100L, TITLE, "待删除的正文，仅用于测试。",
                List.of("数据结构")));
        tags.deltas.clear();
        events.published.clear();

        service.delete(ming, id);

        assertThat(questions.findById(id).orElseThrow().deletedAt()).isNotNull();
        assertThat(tags.deltas).containsExactly(-1);
        assertThat(events.ofType(ContentDeletedEvent.class)).hasSize(1);
        assertThat(audit.actions).contains("QUESTION_DELETED");
        assertThat(cache.invalidations).isEqualTo(2);
    }

    @Test
    @DisplayName("编辑不存在的问题：抛 QA-4001")
    void editMissingQuestionThrowsNotFound() {
        assertThatThrownBy(() -> service.edit(ming, 9999L, new EditQuestionCommand(TITLE, "正文内容足够长。",
                List.of("标签"))))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("QA-4001"));
    }

    @Test
    @DisplayName("非作者且非课管编辑/删除：拒绝且不产生事件")
    void nonAuthorCannotEditOrDelete() {
        long id = service.post(ming, new PostQuestionCommand(100L, TITLE, "原始正文，描述现象与期望。",
                List.of("数据结构")));
        events.published.clear();

        assertThatThrownBy(() -> service.edit(hong, id, new EditQuestionCommand(TITLE + "（改）", "改后正文内容。",
                List.of("数据结构")))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.delete(hong, id)).isInstanceOf(ForbiddenException.class);

        assertThat(events.published).isEmpty();
    }
}
