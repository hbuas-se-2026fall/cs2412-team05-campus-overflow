package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.application.fake.QaApplicationTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
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

/** 评论写路径用例测试：验证目标解析（问题/回答）、二级回复父评论校验与事件发布。 */
class CommentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T02:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryComments comments = new InMemoryComments();
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);

    private CommentService service;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, questions, answers, events, clock);
        Question question = questions.save(Question.post(ming.userId(), 100L,
                new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("插入后黑高不平衡，需要旋转恢复。"), Tags.of(List.of("数据结构")), NOW));
        questionId = question.id();
        Answer answer = answers.save(Answer.reconstitute(null, questionId, hong.userId(),
                MarkdownBody.ofAnswer("关键在于黑高这个不变量。"), 0, null, NOW, NOW, null, 0));
        answerId = answer.id();
    }

    @Test
    @DisplayName("评论问题：questionId 取自目标问题，发布 CommentPostedEvent")
    void postOnQuestionResolvesQuestionId() {
        long id = service.post(hong, new PostCommentCommand(TargetType.QUESTION, questionId, null, "补充一句。"));

        Comment saved = comments.findById(id).orElseThrow();
        assertThat(saved.questionId()).isEqualTo(questionId);
        assertThat(saved.authorId()).isEqualTo(hong.userId());

        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).commentId()).isEqualTo(id);
        assertThat(posted.get(0).questionId()).isEqualTo(questionId);
        assertThat(posted.get(0).body()).isEqualTo("补充一句。");
    }

    @Test
    @DisplayName("评论回答：questionId 从回答反查，而不是用 targetId")
    void postOnAnswerResolvesQuestionIdFromAnswer() {
        long id = service.post(ming, new PostCommentCommand(TargetType.ANSWER, answerId, null, "受教了。"));

        assertThat(comments.findById(id).orElseThrow().questionId()).isEqualTo(questionId);
        assertThat(events.ofType(CommentPostedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("二级回复：父评论存在时挂上 parentId")
    void postReplyKeepsParent() {
        long parentId = service.post(hong, new PostCommentCommand(TargetType.QUESTION, questionId, null, "第一层。"));

        long replyId = service.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId, parentId,
                "第二层回复。"));

        Comment reply = comments.findById(replyId).orElseThrow();
        assertThat(reply.parentId()).isEqualTo(parentId);
        assertThat(comments.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("目标问题不存在：抛 QA-4001 且不落库")
    void postOnMissingQuestionThrowsNotFound() {
        assertThatThrownBy(() -> service.post(hong, new PostCommentCommand(TargetType.QUESTION, 9999L, null, "内容")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("QA-4001"));
        assertThat(comments.size()).isZero();
    }

    @Test
    @DisplayName("目标回答不存在：抛 QA-4002")
    void postOnMissingAnswerThrowsNotFound() {
        assertThatThrownBy(() -> service.post(hong, new PostCommentCommand(TargetType.ANSWER, 9999L, null, "内容")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("QA-4002"));
    }

    @Test
    @DisplayName("父评论不存在：抛 QA-4004")
    void postWithMissingParentThrowsNotFound() {
        assertThatThrownBy(() -> service.post(hong, new PostCommentCommand(TargetType.QUESTION, questionId, 9999L,
                "内容")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        ex -> assertThat(ex.code()).isEqualTo("QA-4004"));
    }
}
