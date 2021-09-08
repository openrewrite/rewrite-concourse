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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindResource extends Recipe {
    @Option(displayName = "Type",
        description = "Resource type",
        example = "git")
    String type;

    @Override
    public String getDisplayName() {
        return "Find resource";
    }

    @Override
    public String getDescription() {
        return "Find a Concourse resource by name";
    }

    @Override
    protected YamlVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher resource = new JsonPathMatcher("$.resources[*].type[@ == ' + " + type + "']");
        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                if(resource.matches(getCursor())) {
                    return entry.withMarkers(entry.getMarkers().addIfAbsent(new YamlSearchResult(Tree.randomId(), FindResource.this, null)));
                }
                return super.visitMappingEntry(entry, executionContext);
            }
        };
    }
}
