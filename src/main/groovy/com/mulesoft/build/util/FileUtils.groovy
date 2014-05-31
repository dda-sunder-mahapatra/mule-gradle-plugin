/*
 * Copyright 2012 the original author or authors.
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
package com.mulesoft.build.util

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by juancavallotti on 31/05/14.
 */
class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils)

    /**
     * Utility method for writing contents of a file inside a given project.
     * @param project the project affected by this writing
     * @param fileName the filename to write, if is part of a sub dir structure, this structure must exist.
     * @param contents an input stream representing the contents of this file.
     */
    static void writeFile(Project project, String fileName, InputStream contents) {

        logger.debug("Trying to create: $fileName")

        if (!contents) {
            throw new TaskExecutionException(this, new IllegalArgumentException("Invalid contents $fileName"))
        }

        File f = project.file(fileName)

        if (f.exists()) {
            logger.warn("Will not create $fileName, file already exists")
            return
        }

        boolean created = f.createNewFile()
        if (!created) {
            throw new TaskExecutionException(this, new IOException("Could not create $fileName"))
        }

        logger.debug('Writing file contents...')

        f.append(contents)
        logger.debug('Done!')
    }
}
