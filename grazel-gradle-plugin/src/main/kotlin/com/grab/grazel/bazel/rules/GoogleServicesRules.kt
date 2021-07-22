/*
 * Copyright 2021 Grabtaxi Holdings PTE LTD (GRAB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grab.grazel.bazel.rules

import com.grab.grazel.bazel.starlark.BazelDependency
import com.grab.grazel.bazel.starlark.StatementsBuilder
import com.grab.grazel.bazel.starlark.StringStatement
import com.grab.grazel.bazel.starlark.asString
import com.grab.grazel.bazel.starlark.function
import com.grab.grazel.bazel.starlark.load
import com.grab.grazel.bazel.starlark.quote
import com.grab.grazel.bazel.starlark.statements


internal const val TOOLS_ANDROID = "tools_android"
internal const val TOOLS_ANDROID_COMMIT = "58d67fd54a3b7f5f1e6ddfa865442db23a60e1b6"
internal const val TOOLS_ANDROID_SHA = "a192553d52a42df306437a8166fc6b5ec043282ac4f72e96999ae845ece6812f"
internal const val GOOGLE_SERVICES_XML = "GOOGLE_SERVICES_XML"

/**
 * Imports and configures [tools_android](https://github.com/bazelbuild/tools_android) repo needed for crashlytics
 * and google services support.
 */
fun StatementsBuilder.googleServicesWorkspaceDependencies(
    toolsAndroidCommit: String = TOOLS_ANDROID_COMMIT,
    toolsAndroidSha: String = TOOLS_ANDROID_SHA
) {
    val toolsAndroidCommitVar = "TOOLS_ANDROID_COMMIT"
    toolsAndroidCommitVar eq toolsAndroidCommit.quote()
    newLine()
    httpArchive(
        name = TOOLS_ANDROID,
        sha256 = toolsAndroidSha.quote(),
        stripPrefix = """"$TOOLS_ANDROID-" + $toolsAndroidCommitVar""",
        url = """"https://github.com/bazelbuild/tools_android/archive/%s.tar.gz" % $toolsAndroidCommitVar"""
    )
    load("@$TOOLS_ANDROID//tools/googleservices:defs.bzl", "google_services_workspace_dependencies")
    add("google_services_workspace_dependencies()")
}

/**
 * Adds a google services XML target required by crashlytics and other google services
 *
 * @param packageName The package name for the generated target
 * @param googleServicesJson The path to google_services.json relative to module.
 *
 * @return `StringStatement` instance containing reference to generated google_services.xml
 */
fun StatementsBuilder.googleServicesXml(
    packageName: String,
    googleServicesJson: String
): StringStatement {
    load("@tools_android//tools/googleservices:defs.bzl", "google_services_xml")
    GOOGLE_SERVICES_XML eq statements { // Create new statements scope so as to not add to current scope
        function("google_services_xml") {
            "package_name" eq packageName.quote()
            "google_services_json" eq googleServicesJson.quote()
        }
    }.asString()
    return StringStatement(GOOGLE_SERVICES_XML)
}

/**
 * Add crashlytics_android_library target from tools_android repo.
 *
 * @param name The name for this target
 * @param packageName The package name of the android binary target
 * @param buildId The build id generated from crashlytics
 * @param resourceFiles The path to resource XML generated by google services xml target
 * @return The BazelDependency instance of the written target that can be used in android_binary.deps
 */
fun StatementsBuilder.crashlyticsAndroidLibrary(
    name: String = "crashlytics_lib",
    packageName: String,
    buildId: String,
    resourceFiles: String
): BazelDependency {
    load("@tools_android//tools/crashlytics:defs.bzl", "crashlytics_android_library")
    rule("crashlytics_android_library") {
        "name" eq name.quote()
        "package_name" eq packageName.quote()
        "build_id" eq buildId.quote()
        "resource_files" eq resourceFiles
    }
    return BazelDependency.StringDependency(":$name")
}