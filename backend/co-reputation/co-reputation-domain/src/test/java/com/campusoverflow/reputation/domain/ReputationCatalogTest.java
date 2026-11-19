package com.campusoverflow.reputation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;

/** 徽章目录文案、高声誉阈值与投票值域的兜底分支。 */
class ReputationCatalogTest {

    @Test
    void everyBadgeCarriesChineseNameAndDescription() {
        for (BadgeType badge : BadgeType.values()) {
            assertThat(badge.displayName()).isNotBlank();
            assertThat(badge.description()).isNotBlank();
        }
    }

    @Test
    void reputationBadgesUnlockAtHundredAndThousand() {
        assertThat(BadgePolicy.forReputation(99)).isEmpty();
        assertThat(BadgePolicy.forReputation(100)).containsExactly(BadgeType.REPUTATION_100);
        assertThat(BadgePolicy.forReputation(1000))
                .containsExactly(BadgeType.REPUTATION_100, BadgeType.REPUTATION_1000);
    }

    @Test
    void voteEffectRejectsValuesOutsideDomain() {
        assertThatThrownBy(() -> ReputationRulebook.voteEffect(2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1、0 或 1");
        assertThat(ReputationRulebook.voteEffect(0)).isZero();
    }

    @Test
    void onlyTeacherGetsInitialBountyPoints() {
        assertThat(ReputationRulebook.initialPoints(Role.TEACHER)).isEqualTo(1000);
        assertThat(ReputationRulebook.initialPoints(Role.TA)).isZero();
    }
}
