/**
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
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

package com.cis.wsreader.api.models

import androidx.annotation.Keep

/** Extra info from google books API */
@Keep
data class ExtraInfo(
    val coverImage: String = "",
    val pageCount: Int = 0,
    val description: String = "",
    // Not part of the API response.
    // Used to check if extra info is cached.
    val isCached: Boolean = false
)
