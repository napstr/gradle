/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal

import org.gradle.test.fixtures.concurrent.ConcurrentSpec
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule

class SystemPropertiesIntegrationTest extends ConcurrentSpec {
    @Rule TestNameTestDirectoryProvider temporaryFolder

    def "creates Java compiler for mismatching Java home directory for multiple threads concurrently"() {
        final int threadCount = 100
        Factory<String> factory = Mock()
        String factoryCreationResult = 'test'
        File originalJavaHomeDir = SystemProperties.instance.javaHomeDir
        File providedJavaHomeDir = temporaryFolder.file('my/test/java/home/toolprovider')

        when:
        async {
            threadCount.times {
                start {
                    String expectedString = SystemProperties.instance.withJavaHome(providedJavaHomeDir, factory)
                    assert factoryCreationResult == expectedString
                }
            }
        }

        then:
        threadCount * factory.create() >> {
            assert SystemProperties.instance.javaHomeDir == providedJavaHomeDir
            factoryCreationResult
        }
        assert SystemProperties.instance.javaHomeDir == originalJavaHomeDir
    }

    def "sets a system property for the duration of a Runnable"() {
        final int threadCount = 100
        final String propertyWithoutValue = "org.gradle.test.sysprop.without.value"
        final String propertyWithOriginalValue = "org.gradle.test.sysprop.with.original"
        System.setProperty(propertyWithOriginalValue, "original")

        when:
        async {
            threadCount.times { i ->
                start {
                    SystemProperties.instance.withSystemProperty(propertyWithOriginalValue, i.toString(), new Runnable() {
                        @Override
                        void run() {
                            assert System.getProperty(propertyWithOriginalValue) == i.toString()
                        }
                    })
                    SystemProperties.instance.withSystemProperty(propertyWithoutValue, i.toString(), new Runnable() {
                        @Override
                        void run() {
                            assert System.getProperty(propertyWithoutValue) == i.toString()
                        }
                    })
                }
            }
        }

        then:
        assert System.getProperty(propertyWithOriginalValue) == "original"
        assert System.getProperty(propertyWithoutValue) == null

        cleanup:
        System.clearProperty(propertyWithOriginalValue)
    }
}
