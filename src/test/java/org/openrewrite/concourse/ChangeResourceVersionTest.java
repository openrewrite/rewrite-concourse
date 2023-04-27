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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class ChangeResourceVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void pinVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeResourceVersion("git", "2.0")),
          //language=yaml
          yaml(
            """
              resources:
                - name: git-repo
                  type: git
                  version: 1.0
                - name: git-repo2
                  type: git
                  version: 2.0
                - name: git-repo3
                  type: git
              """,
            """
              resources:
                - name: git-repo
                  type: git
                  version: 2.0
                - name: git-repo2
                  type: git
                  version: 2.0
                - name: git-repo3
                  type: git
                  version: 2.0
              """
          )
        );
    }

    @Test
    void unpinVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeResourceVersion("git", null)),
          //language=yaml
          yaml(
            """
              resources:
                - name: git-repo
                  type: git
                  version: 1.0
                - name: git-repo2
                  type: git
                  version: 2.0
                - name: git-repo3
                  type: git
              """,
            """
              resources:
                - name: git-repo
                  type: git
                - name: git-repo2
                  type: git
                - name: git-repo3
                  type: git
              """
          )
        );
    }
}
