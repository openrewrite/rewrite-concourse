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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.time.Duration;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeResourceVersion extends Recipe {
    @Option(displayName = "Resource type",
            description = "Update any resources of this type",
            example = "git")
    String resourceType;

    @Option(displayName = "Version",
            description = "If less than this version, update. If not provided, clears pins.",
            example = "2.0",
            required = false)
    @Nullable
    String version;

    @Override
    public String getDisplayName() {
        return "Change resource version";
    }

    @Override
    public String getDescription() {
        return "Pin or unpin a resource to a particular version.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public YamlVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher resourceMatcher = new JsonPathMatcher("$.resources[?(@.type == '" + resourceType + "')]");
        JsonPathMatcher versionMatcher = new JsonPathMatcher("$.resources[?(@.type == '" + resourceType + "')].version");
        return new YamlVisitor<>() {
            @Override
            public Yaml visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                if (resourceMatcher.matches(getCursor())) {
                    if (version != null && mapping.getEntries().stream().noneMatch(e -> "version".equals(e.getKey().getValue()))) {
                        //noinspection OptionalGetWithoutIsPresent
                        Yaml.Mapping versionMapping = (Yaml.Mapping) new YamlParser()
                                .parse("version: " + version)
                                .map(Yaml.Documents.class::cast)
                                .findFirst()
                                .get()
                                .getDocuments().get(0).getBlock();
                        Yaml.Mapping.Entry versionEntry = versionMapping.getEntries().get(0);
                        versionEntry = autoFormat(versionEntry, ctx, getCursor());
                        return mapping.withEntries(ListUtils.concat(mapping.getEntries(), versionEntry));
                    }
                }
                return super.visitMapping(mapping, ctx);
            }

            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (versionMatcher.matches(getCursor())) {
                    if (version == null) {
                        //noinspection ConstantConditions
                        return null; // unpin
                    } else if (entry.getValue() instanceof Yaml.Scalar) {
                        if (!((Yaml.Scalar) entry.getValue()).getValue().equals(version)) {
                            return entry.withValue(((Yaml.Scalar) entry.getValue()).withValue(version));
                        }
                    }
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
    }
}
