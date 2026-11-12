package com.campusoverflow.qa.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.CountingViewCounter;
import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.StoringCache;
import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.StubReadModel;
import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.StubRenderer;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 问题读路径用例测试：验证缓存分支、视图装配、答案排序与权限视图。 */
class QuestionQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final StubReadModel readModel = new StubReadModel();
    private final StubRenderer renderer = new StubRenderer();
    private final CountingViewCounter viewCounter = new CountingViewCounter();
    private final StoringCache cache = new StoringCache();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT)
            .withUser(2, "小红", Role.STUDENT)
            .withUser(9, "王老师", Role.TEACHER)
            .withCourseManager(9);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);

    private QuestionQueryService service;

    @BeforeEach
    void setUp() {
        service = new QuestionQueryService(readModel, identity, renderer, viewCounter, cache);
    }

    private static QuestionRow row(long id, long authorId, Long courseId, int answerCount, Long accepted) {
        return new QuestionRow(id, authorId, courseId, "红黑树插入后为什么必须旋转？", "插入后黑高不平衡，需要旋转恢复。",
                List.of("数据结构"), accepted, 3, answerCount, 12, NOW, NOW);
    }

    @Test
    @DisplayName("列表未命中缓存：查读模型、拼作者信息、回填缓存")
    void listOnCacheMissMapsRowsAndFillsCache() {
        readModel.questionPage = new PageResult<>(List.of(row(10, 1, 100L, 0, null)), 1, 1, 20);

        PageResult<QuestionSummaryView> result = service.list(100L, new PageRequest(1, 20));

        assertThat(result.items()).hasSize(1);
        QuestionSummaryView view = result.items().get(0);
        assertThat(view.id()).isEqualTo(10);
        assertThat(view.author().displayName()).isEqualTo("小明");
        assertThat(view.excerpt()).isEqualTo("插入后黑高不平衡，需要旋转恢复。");
        assertThat(view.accepted()).isFalse();
        assertThat(cache.puts).isEqualTo(1);
        assertThat(renderer.plainTextLimits).containsExactly(160);
    }

    @Test
    @DisplayName("列表命中缓存：不再查读模型")
    void listOnCacheHitSkipsReadModel() {
        QuestionSummaryView cached = new QuestionSummaryView(10, "缓存中的标题", "摘要", List.of("标签"), 100L,
                new AuthorView(1, "小明", "STUDENT", false), 0, 0, 0, false, NOW);
        cache.prime(new PageResult<>(List.of(cached), 1, 1, 20));

        PageResult<QuestionSummaryView> result = service.list(100L, new PageRequest(1, 20));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("缓存中的标题");
        assertThat(readModel.listCalls).isZero();
    }

    @Test
    @DisplayName("详情不存在：抛 QA-4001")
    void detailMissingQuestionThrowsNotFound() {
        readModel.question = null;

        assertThatThrownBy(() -> service.detail(9999L, ming))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("QA-4001"));
    }

    @Test
    @DisplayName("匿名查看详情：权限全关、我的投票为 0、浏览数 +1")
    void detailAnonymousHasNoPermissions() {
        readModel.question = row(10, 1, null, 0, null);

        QuestionDetailView view = service.detail(10L, null);

        assertThat(view.permissions()).isEqualTo(PermissionsView.NONE);
        assertThat(view.myVote()).isZero();
        assertThat(view.course()).isNull();
        assertThat(viewCounter.increments).isEqualTo(1);
        assertThat(view.bodyHtml()).isEqualTo("<p>插入后黑高不平衡，需要旋转恢复。</p>");
    }

    @Test
    @DisplayName("作者查看详情：可编辑/可删/可采纳，采纳答案排在最前，我的投票按 key 装配")
    void detailAsAuthorExposesPermissionsAndSortsAcceptedFirst() {
        readModel.question = row(10, 1, 100L, 1, 21L);
        readModel.answers = List.of(
                new AnswerRow(20, 2, "未被采纳的回答，分数更高。", 9, null, NOW, NOW),
                new AnswerRow(21, 2, "被采纳的回答。", 1, 9L, NOW, NOW));
        readModel.comments = List.of(
                new CommentRow(30, "QUESTION", 10, 2, null, "评论一下", NOW),
                new CommentRow(31, "ANSWER", 21, 1, 30L, "二级回复", NOW));
        readModel.myVotes = Map.of("QUESTION:10", 1, "ANSWER:20", -1);

        QuestionDetailView view = service.detail(10L, ming);

        assertThat(view.permissions().canEdit()).isTrue();
        assertThat(view.permissions().canAccept()).isFalse();
        assertThat(view.permissions().canDelete()).isFalse();
        assertThat(view.myVote()).isEqualTo(1);
        assertThat(view.answers()).extracting(AnswerView::id).containsExactly(21L, 20L);
        assertThat(view.answers().get(0).accepted()).isTrue();
        assertThat(view.answers().get(0).comments()).hasSize(1);
        assertThat(view.answers().get(0).endorsedBy().displayName()).isEqualTo("王老师");
        assertThat(view.answers().get(1).myVote()).isEqualTo(-1);
        assertThat(view.comments()).hasSize(1);
    }

    @Test
    @DisplayName("作者在尚无回答时可删可采纳；课管可管理")
    void authorCanDeleteAndAcceptBeforeAnyAnswer() {
        readModel.question = row(10, 1, 100L, 0, null);

        QuestionDetailView view = service.detail(10L, ming);

        assertThat(view.permissions().canDelete()).isTrue();
        assertThat(view.permissions().canAccept()).isTrue();
        assertThat(view.permissions().canManage()).isFalse();
    }

    @Test
    @DisplayName("热门标签：负数 limit 归零后再查")
    void popularTagsClampsNegativeLimit() {
        service.popularTags(-5);

        assertThat(readModel.popularTagsArg).isZero();
    }
}
