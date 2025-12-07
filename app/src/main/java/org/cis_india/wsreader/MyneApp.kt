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


package org.cis_india.wsreader

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import cat.ereza.customactivityoncrash.config.CaocConfig
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import okhttp3.OkHttpClient
import org.cis_india.wsreader.api.BookAPI
import java.io.File
import java.util.concurrent.TimeUnit


import java.util.Properties
import org.cis_india.wsreader.data.BookRepository
import org.cis_india.wsreader.data.db.AppDatabase
import org.cis_india.wsreader.domain.Bookshelf
import org.cis_india.wsreader.domain.CoverStorage
import org.cis_india.wsreader.domain.PublicationRetriever
import org.cis_india.wsreader.reader.ReaderRepository
import org.cis_india.wsreader.utils.tryOrLog


@HiltAndroidApp
class MyneApp : Application(), ImageLoaderFactory {

    lateinit var readium: Readium
        private set

    lateinit var storageDir: File

    lateinit var bookRepository: BookRepository
        private set

    lateinit var bookApi: BookAPI
        private set

    lateinit var bookshelf: Bookshelf
        private set

    lateinit var readerRepository: ReaderRepository
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val Context.navigatorPreferences: DataStore<Preferences>
            by preferencesDataStore(name = "navigator-preferences")

    override fun onCreate() {
        super.onCreate()
        CaocConfig.Builder.create().restartActivity(MainActivity::class.java).apply()
        readium = Readium(this)

        storageDir = computeStorageDir()

        val database = AppDatabase.getDatabase(this)

        bookRepository = BookRepository(database.booksDao())

        bookApi = BookAPI(this)

        val downloadsDir = File(cacheDir, "downloads")

        // Cleans the download dir.
        tryOrLog { downloadsDir.delete() }

        val publicationRetriever =
            PublicationRetriever(
                context = applicationContext,
                assetRetriever = readium.assetRetriever,
                bookshelfDir = storageDir,
                tempDir = downloadsDir,
                httpClient = readium.httpClient,
            )

        bookshelf =
            Bookshelf(
                bookRepository,
                CoverStorage(storageDir, httpClient = readium.httpClient),
                readium.publicationOpener,
                readium.assetRetriever,
                publicationRetriever
            )

        readerRepository = ReaderRepository(
            this@MyneApp,
            readium,
            bookRepository,
            navigatorPreferences,
            bookApi
        )
    }

    override fun newImageLoader(): ImageLoader {
        val coilOkhttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Wikisource Reader App (https://meta.wikimedia.org/wiki/Wikisource_reader_app; android)"
                    )
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        return ImageLoader(this).newBuilder()
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizePercent(0.05)
                    .directory(cacheDir)
                    .build()
            }
            .okHttpClient(coilOkhttpClient)
            .logger(DebugLogger())
            .build()
    }

    private fun computeStorageDir(): File {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        return File(
            if (useExternalFileDir) {
                getExternalFilesDir(null)?.path + "/"
            } else {
                filesDir?.path + "/"
            }
        )
    }
}