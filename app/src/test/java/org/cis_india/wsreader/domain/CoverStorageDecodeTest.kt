package org.cis_india.wsreader.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverStorageDecodeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `large cover file is stored at the cover size`() = runTest {
        val source = tempFolder.newFile("large-cover.png")
        source.outputStream().use {
            Bitmap.createBitmap(1200, 2000, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val storage = CoverStorage(tempFolder.newFolder("app"), DefaultHttpClient())
        val publication = Publication(Manifest(metadata = Metadata(localizedTitle = null)))

        val stored: File = storage
            .storeCover(publication, source.toUrl(isDirectory = false) as AbsoluteUrl)
            .getOrNull()!!

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stored.path, options)
        assertThat(options.outWidth).isEqualTo(120)
        assertThat(options.outHeight).isEqualTo(200)
    }
}
