package tk.sciwhiz12.gradle.license

import groovy.text.SimpleTemplateEngine
import groovy.transform.PackageScope
import org.cadixdev.gradle.licenser.LicenseExtension
import org.cadixdev.gradle.licenser.LicenseProperties
import org.cadixdev.gradle.licenser.header.Header
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.resources.ResourceException
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.Actions

import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.util.function.BiConsumer

final class LicenserExtensions {
    static void configure(Project project) {
        configure(project, Actions.doNothing())
    }

    static void configure(Project project, Action<LicenseProperties> propertiesConfigure) {
        project.getExtensions().configure(LicenseExtension.class, extension -> {
            final Logger logger = project.getLogger()
            logger.debug("Found org.cadixdev.licenser extension, configuring")

            Map<String, Instant> fileLastModifiedTimes

            try (Repository repository = new FileRepositoryBuilder()
                    .findGitDir(project.getProjectDir())
                    .readEnvironment()
                    .build()) {

                fileLastModifiedTimes = RepositoryWalker.findLastModifiedTimes(repository)

            } catch (RepositoryNotFoundException ignored) {
                logger.lifecycle("No git repository found, skipping configuring Licenser")
                return
            } catch (IOException e) {
                logger.warn("Error while fetch git repository info, skipping configuring Licenser", e)
                return
            }

            final Map<String, Set<String>> yearsToFiles = [:]

            for (Map.Entry<String, Instant> entry : fileLastModifiedTimes.entrySet()) {
                int year = entry.getValue().atOffset(ZoneOffset.UTC).getYear()
                Path projectDir = project.projectDir.toPath()
                yearsToFiles.computeIfAbsent(String.valueOf(year), it -> []).add(projectDir.resolve(entry.key).toString())
            }

            final IdentityHashMap<LicenseProperties, String> propertiesToYears = new IdentityHashMap<>()

            LicenserMetaClass.interceptPrepareHeader((licenseProps, extProps) -> {
                final String year = propertiesToYears.get(licenseProps)
                if (year != null) {
                    extProps.put("year", year)
                    extProps.put("lastModifiedYear", year)
                }
            })

            for (String currentYear : yearsToFiles.keySet()) {
                final Set<String> filesForCurrentYear = yearsToFiles.get(currentYear)

                final PatternSet pattern = new PatternSet()
                pattern.include { FileTreeElement it ->
                    filesForCurrentYear.contains(it.file.toString())
                }
                extension.matching(pattern, { LicenseProperties props ->
                    // Forces the tasks to recognize this rather than skipping over it because of missing custom header
                    props.getHeader().convention(extension.getHeader())
                    propertiesToYears.put(props, currentYear)
                    propertiesConfigure.execute(props)
                })
            }
        })
    }

    /*
     * The MIT License (MIT)
     *
     * Copyright (c) 2015, Minecrell <https://github.com/Minecrell>
     *
     * Permission is hereby granted, free of charge, to any person obtaining a copy
     * of this software and associated documentation files (the "Software"), to deal
     * in the Software without restriction, including without limitation the rights
     * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     * copies of the Software, and to permit persons to whom the Software is
     * furnished to do so, subject to the following conditions:
     *
     * The above copyright notice and this permission notice shall be included in
     * all copies or substantial portions of the Software.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
     * THE SOFTWARE.
     *
     * Modification(s): added a way to add to or modify the properties for the text substitution
     */

    @PackageScope
    static Header prepareHeader(BiConsumer<LicenseProperties, Map<String, String>> propertiesEditor,
                                LicenseExtension extension, LicenseProperties properties) {
        def headerResource = properties.header.orElse(extension.header)
        def extraProperties = extension.ext
        extension.keywords.disallowChanges()
        properties.newLine.disallowChanges()
        return new Header(extension.style, extension.keywords, extension.providers.provider {
            TextResource header = headerResource.getOrNull()
            if (header != null) {
                def text
                try {
                    text = header.asString()
                } catch (ResourceException ignored) {
                    return ""
                }

                Map<String, String> props = extraProperties.properties ?: [:]
                propertiesEditor.accept(properties, props)
                if (!props?.isEmpty()) {
                    def engine = new SimpleTemplateEngine()
                    def template = engine.createTemplate(text).make(props)
                    text = template.toString()
                }

                return text
            }

            return ""
        }, (PatternSet) properties.filter, properties.newLine.orElse(extension.newLine))
    }
}
