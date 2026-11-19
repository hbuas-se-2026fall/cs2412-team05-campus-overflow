package com.campusoverflow.qa.application.fake;

import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.qa.application.query.AnswerRow;
import com.campusoverflow.qa.application.query.CommentRow;
import com.campusoverflow.qa.application.query.QuestionRow;
import com.campusoverflow.qa.application.query.QuestionSummaryView;
import com.campusoverflow.qa.application.query.TagView;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 应用层测试夹具（第二批）：补齐 {@link QaTestFixtures} 未覆盖的端口。
 *
 * <p>与第一批的分工：第一批提供仓储 / 事件 / 审计 / 缓存的假实现，供写路径使用；
 * 本批提供标签、评论、读模型、渲染器、浏览计数，供问题与评论的用例测试使用。</p>
 */
public final class QaApplicationTestFixtures {

    private QaApplicationTestFixtures() {
    }

    /** 记录标签使用量的增减，便于断言「编辑时只调整差集」。 */
    public static class InMemoryTags implements TagRepository {
        public final Map<String, Integer> usage = new LinkedHashMap<>();
        public final List<Integer> deltas = new ArrayList<>();

        @Override
        public void adjustUsage(Collection<String> tagNames, int delta) {
            deltas.add(delta);
            for (String name : tagNames) {
                usage.merge(name, delta, Integer::sum);
            }
        }
    }

    /** 内存评论仓储。 */
    public static class InMemoryComments implements CommentRepository {
        private final Map<Long, Comment> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<Comment> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Comment save(Comment comment) {
            if (comment.id() == null) {
                comment.assignId(sequence.incrementAndGet());
            }
            store.put(comment.id(), comment);
            return comment;
        }

        public int size() {
            return store.size();
        }
    }

    /** 固定返回值的渲染器：不引入 commonmark，只验证调用点传参与长度截断。 */
    public static class StubRenderer implements ContentRenderer {
        public final List<Integer> plainTextLimits = new ArrayList<>();

        @Override
        public String toSafeHtml(String markdown) {
            return "<p>" + markdown + "</p>";
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            plainTextLimits.add(maxLength);
            return markdown.length() <= maxLength ? markdown : markdown.substring(0, maxLength);
        }
    }

    /** 可编排的读模型：测试按需塞行，验证查询服务的装配与排序逻辑。 */
    public static class StubReadModel implements QaReadModel {
        public PageResult<QuestionRow> questionPage = PageResult.empty(1, 20);
        public QuestionRow question;
        public List<AnswerRow> answers = List.of();
        public List<CommentRow> comments = List.of();
        public Map<String, Integer> myVotes = Map.of();
        public List<TagView> tags = List.of();
        public int listCalls;
        public int popularTagsArg = -1;

        @Override
        public PageResult<QuestionRow> listQuestions(Long courseId, PageRequest page) {
            listCalls++;
            return questionPage;
        }

        @Override
        public Optional<QuestionRow> findQuestion(long questionId) {
            return Optional.ofNullable(question);
        }

        @Override
        public List<AnswerRow> findAnswers(long questionId) {
            return answers;
        }

        @Override
        public List<CommentRow> findComments(long questionId) {
            return comments;
        }

        @Override
        public Map<String, Integer> findMyVotes(long voterId, long questionId, Collection<Long> answerIds) {
            return myVotes;
        }

        @Override
        public List<TagView> popularTags(int limit) {
            popularTagsArg = limit;
            return tags;
        }
    }

    /** 记录浏览计数调用次数。 */
    public static class CountingViewCounter implements ViewCounter {
        public int increments;

        @Override
        public void increment(long questionId) {
            increments++;
        }
    }

    /** 真正存取的缓存：用于验证「命中即返回、未命中才查读模型」的分支。 */
    public static class StoringCache implements QuestionListCache {
        private PageResult<QuestionSummaryView> stored;
        public int puts;

        public void prime(PageResult<QuestionSummaryView> value) {
            stored = value;
        }

        @Override
        public Optional<PageResult<QuestionSummaryView>> get(Long courseId, PageRequest page) {
            return Optional.ofNullable(stored);
        }

        @Override
        public void put(Long courseId, PageRequest page, PageResult<QuestionSummaryView> value) {
            puts++;
            stored = value;
        }

        @Override
        public void invalidateAll() {
            stored = null;
        }
    }
}
