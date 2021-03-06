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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.yaml.YamlRecipeTest

class ChangeValueTest : YamlRecipeTest {
    @Test
    fun updateGitURI() = assertChanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
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
    fun updateProperty() = assertChanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        dependsOn = arrayOf(
            """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: ((git-uri))
        """
        ),
        before = """
            git-uri: https://github.com/openrewrite/rewrite0
        """,
        after = """
            git-uri: git@github.com:openrewrite/rewrite1.git
        """
    )

    @Test
    fun updatePropertyWithNestedKey() = assertChanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        dependsOn = arrayOf(
            """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: ((git.source_code.uri))
        """
        ),
        before = """
            git:
              source_code:
                uri: https://github.com/openrewrite/rewrite0
        """,
        after = """
            git:
              source_code:
                uri: git@github.com:openrewrite/rewrite1.git
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-concourse/issues/2")
    fun doNotReplaceOriginalParameterReferenceWhenParameterized() = assertUnchanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        dependsOn = arrayOf(
            """
            git_uri: https://github.com/openrewrite/rewrite0
        """
        ),
        before = """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: ((git_uri))
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-concourse/issues/2")
    fun handleMultipleLayersOfParameterRedirection() = assertChanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        dependsOn = arrayOf(
            """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: ((git_uri_0))
            """,
            """
            git_uri_0: ((git.git_uri_1))
        """
        ),
        before = """
            git:
              git_uri_1: https://github.com/openrewrite/rewrite0
        """,
        after = """
            git:
              git_uri_1: git@github.com:openrewrite/rewrite1.git
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-concourse/issues/2")
    fun breakInfiniteRecursion() = assertUnchanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        dependsOn = arrayOf(
            """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: ((git_uri_0))
            """,
            """
            git_uri_0: ((git_uri_1))
        """,
        ),
        before = """
            git_uri_1: ((git_uri_0))
        """
    )

    @Test
    fun doNothingIfPropertyKeyNotFound() = assertUnchanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
        ),
        dependsOn = arrayOf(
            """
            resources:
            - name: git-repo0
              type: git
              source:
                uri: ((git-uri))
        """
        ),
        before = """
            some-other-key: https://github.com/openrewrite/rewrite0
        """
    )

    @Test
    fun oldURIAsEmpty() = assertChanged(
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
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
        recipe = ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
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
}
