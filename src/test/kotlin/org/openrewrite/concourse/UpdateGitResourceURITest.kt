/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.concourse

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.yaml.YamlRecipeTest

class UpdateGitResourceURITest : YamlRecipeTest {
    @Test
    fun updateGitURI() = assertChanged(
        recipe = UpdateGitResourceURI(
            "https://github.com/openrewrite/rewrite0",
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        before = """
            resources:
            - name: git-repo
              type: git
              source:
                uri: https://github.com/openrewrite/rewrite0
            - name: custom
              type: custom-type
              source:
                uri: https://github.com/openrewrite/rewrite0
        """,
        after = """
            resources:
            - name: git-repo
              type: git
              source:
                uri: git@github.com:openrewrite/rewrite1.git
            - name: custom
              type: custom-type
              source:
                uri: https://github.com/openrewrite/rewrite0
        """
    )

    @Test
    fun oldURIAsEmpty() = assertChanged(
        recipe = UpdateGitResourceURI(
            null,
            "git@github.com/openrewrite/rewrite1.git",
            null
        ),
        before = """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: https://github.com/someOrg/someRepo0
            - name: git-repo1
              type: git
              source:
                uri: https://github.com/someOrg/someRepo1
            - name: custom
              type: custom-type
              source:
                uri: https://github.com/openrewrite/rewrite0
        """,
        after = """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: git@github.com/openrewrite/rewrite1.git
            - name: git-repo1
              type: git
              source:
                uri: git@github.com/openrewrite/rewrite1.git
            - name: custom
              type: custom-type
              source:
                uri: https://github.com/openrewrite/rewrite0
        """
    )

    @Test
    fun oldURIAsRegex() = assertChanged(
        recipe = UpdateGitResourceURI(
            ".*@.*",
            "https://github.com/openrewrite/rewrite",
            null
        ),
        before = """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: git@github.com:openrewrite/rewrite0.git
            - name: git-repo1
              type: git
              source:
                uri: https://github.com/openrewrite/rewrite1
            - name: custom
              type: custom-type
              source:
                uri: https://github.com/openrewrite/rewrite0
        """,
        after = """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: https://github.com/openrewrite/rewrite
            - name: git-repo1
              type: git
              source:
                uri: https://github.com/openrewrite/rewrite1
            - name: custom
              type: custom-type
              source:
                uri: https://github.com/openrewrite/rewrite0
        """
    )

    @Test
    fun checkValidation() {
        val recipe = UpdateGitResourceURI("*unmatched*", "newURI", null)
        val valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("oldURIPattern")
    }

}
