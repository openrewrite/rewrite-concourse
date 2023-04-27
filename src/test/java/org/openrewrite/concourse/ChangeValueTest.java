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
package org.openrewrite.concourse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.yaml.Assertions.yaml;

class ChangeValueTest implements RewriteTest {
    @DocumentExample
    @Test
    void updateGitURI() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            "https://github.com/openrewrite/rewrite0",
            "git@github.com:openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
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
            """
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
        );
    }

    @Test
    void updateProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
              "$.resources[?(@.type == 'git')].source.uri",
              null,
              "git@github.com:openrewrite/rewrite1.git",
              null
            )
          ),
          //language=yaml
          yaml(
            """
              resources:
              - name: git-repo0
                type: git
                source:
                  uri: ((git-uri))
              """
          ),
          //language=yaml
          yaml(
            """
              git-uri: https://github.com/openrewrite/rewrite0
              """,
            """
              git-uri: git@github.com:openrewrite/rewrite1.git
              """
          )
        );
    }

    @Test
    void updatePropertyWithNestedKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
              resources:
              - name: git-repo0
                type: git
                source:
                  uri: ((git.source_code.uri))
              """
          ),
          //language=yaml
          yaml(
            """
              git:
                source_code:
                  uri: https://github.com/openrewrite/rewrite0
              """,
            """
              git:
                source_code:
                  uri: git@github.com:openrewrite/rewrite1.git
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-concourse/issues/2")
    void doNotReplaceOriginalParameterReferenceWhenParameterized() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
              git_uri: https://github.com/openrewrite/rewrite0
              """,
            SourceSpec::skip
          ),
          //language=yaml
          yaml(
            """
              resources:
              - name: git-repo0
                type: git
                source:
                  uri: ((git_uri))
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-concourse/issues/2")
    void handleMultipleLayersOfParameterRedirection() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
              resources:
              - name: git-repo0
                type: git
                source:
                  uri: ((git_uri_0))
              """
          ),
          yaml(
            """
              git_uri_0: ((git.git_uri_1))
              """
          ),
          //language=yaml
          yaml(
            """
              git:
                git_uri_1: https://github.com/openrewrite/rewrite0
              """,
            """
              git:
                git_uri_1: git@github.com:openrewrite/rewrite1.git
              """
          )
        );
    }

    @Test
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-concourse/issues/2")
    void breakInfiniteRecursion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
              resources:
              - name: git-repo0
                type: git
                source:
                  uri: ((git_uri_0))
              """
          ),
          //language=yaml
          yaml(
            """
              git_uri_0: ((git_uri_1))
              """
          ),
          //language=yaml
          yaml(
            """
              git_uri_1: ((git_uri_0))
              """
          )
        );
    }

    @Test
    void doNothingIfPropertyKeyNotFound() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com:openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
              resources:
              - name: git-repo0
                type: git
                source:
                  uri: ((git-uri))
              """
          ),
          //language=yaml
          yaml(
            """
              some-other-key: https://github.com/openrewrite/rewrite0
              """
          )
        );
    }

    @Test
    void oldURIAsEmpty() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            null,
            "git@github.com/openrewrite/rewrite1.git",
            null
          )),
          //language=yaml
          yaml(
            """
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
            """
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
        );
    }

    @Test
    void oldURIAsRegex() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.resources[?(@.type == 'git')].source.uri",
            ".*@.*",
            "https://github.com/openrewrite/rewrite",
            null
          )),
          //language=yaml
          yaml(
            """
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
            """
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
        );
    }
}
