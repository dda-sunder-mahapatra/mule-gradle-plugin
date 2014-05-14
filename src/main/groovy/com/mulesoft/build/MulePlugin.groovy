/*
 * Copyright 2014 juancavallotti.
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
package com.mulesoft.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by juancavallotti on 02/01/14.
 */
class MulePlugin implements Plugin<Project> {

    Logger logger = LoggerFactory.getLogger(MulePlugin.class)

    void apply(Project project) {

        //apply the java plugin.
        project.apply(plugin: 'java')

        //add the mule extension.
        project.extensions.create("mule", MulePluginExtension)

        //apply plugins that also read the config

        //add the tasks related to deployment
        project.apply(plugin: 'mule-deploy')

        //add the tasks related to execution
        project.apply(plugin: 'mule-run')


        //add providedCompile and providedRuntime for dependency management.
        //this is needed because we'll be generating a container - based archive.
        project.configurations {

            providedCompile {
                description = 'Compile time dependencies that should not be part of the final zip file.'
                visible = false
            }

            providedRuntime {
                description = 'Runtime dependencies that should not be part of the final zip file.'
                visible = false
                extendsFrom providedCompile
            }

            providedTestCompile {
                description = 'Compile time test dependencies that are already provided by tooling I.E. MuleStudio.'
                visible = false
            }

            providedTestRuntime {
                description = 'Runtime time test dependencies that are already provided by tooling I.E. MuleStudio.'
                visible = false
                extendsFrom providedTestCompile

            }

            compile {
                extendsFrom providedCompile
            }

            runtime {
                extendsFrom providedRuntime
            }

            testCompile {
                extendsFrom providedTestCompile
            }

            testRuntime {
                extendsFrom providedTestRuntime
            }

        }

        project.afterEvaluate {
            proj ->
            MuleDependenciesConfigurer configurer = new MuleDependenciesConfigurer();
                configurer.addDependenciesToProject(proj)

                project.repositories {

                    //local maven repository
                    mavenLocal()

                    //central maven repository
                    mavenCentral()

                    //the CE mule repository.
                    maven {
                        url "http://repository.mulesoft.org/releases/"
                    }

                    if (proj.mule.muleEnterprise) {

                        if (proj.mule.enterpriseRepoUsername.length() == 0) {
                            logger.warn("muleEnterprise is enabled but no enterprise repository credentials are configured.")
                            logger.warn("Please set the enterpriseRepoUsername and enterpriseRepoPassword variables.")
                        }

                        maven {
                            credentials {
                                username proj.mule.enterpriseRepoUsername
                                password proj.mule.enterpriseRepoPassword
                            }
                            url "https://repository.mulesoft.org/nexus-ee/content/repositories/releases-ee/"
                        }
                    }

                    //jboss repository, always useful.
                    maven {
                        url "https://repository.jboss.org/nexus/content/repositories/"
                    }
                }

        }

        Task ziptask = addZipDistributionTask(project)

        ArchivePublishArtifact zipArtifact = new ArchivePublishArtifact(ziptask)
        //make it believe it is a war
        zipArtifact.setType("war")

        project.extensions.getByType(DefaultArtifactPublicationSet.class).addCandidate(zipArtifact)

    }

    private Task addZipDistributionTask(Project project) {
        //the packaging logic.
        Task ziptask = project.tasks.create("mulezip", MuleZip.class)

        ziptask.dependsOn project.check

        //add the app directory to the root of the zip file.
        ziptask.from {
            return 'src/main/app'
        }

        //add the data-mapper mappings
        ziptask.from {
            return 'mappings'
        }

        //add the APIKit specific files.
        ziptask.from {
            return 'src/main/api'
        }

        ziptask.classpath {

            FileCollection runtimeClasspath = project.convention.getPlugin(JavaPluginConvention.class)
                    .sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath

            Configuration providedRuntime = project.configurations.getByName(
                    'providedRuntime');

            runtimeClasspath -= providedRuntime;

        }

        ziptask.description = "Generate a deployable zip archive for this Mule APP"
        ziptask.group = BasePlugin.BUILD_GROUP

        return ziptask
    }





}