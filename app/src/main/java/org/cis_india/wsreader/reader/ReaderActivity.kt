/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.reader

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.toUri
import org.cis_india.wsreader.MyneApp
import org.cis_india.wsreader.R
import org.cis_india.wsreader.databinding.ActivityReaderBinding
import org.cis_india.wsreader.outline.OutlineContract
import org.cis_india.wsreader.outline.OutlineFragment
import org.cis_india.wsreader.utils.launchWebBrowser

/*
 * An activity to read a publication
 *
 * This class can be used as it is or be inherited from.
 */
open class ReaderActivity : AppCompatActivity() {

    private val model: ReaderViewModel by viewModels()

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = ReaderViewModel.createFactory(
            application as MyneApp,
            ReaderActivityContract.parseIntent(this)
        )

    private lateinit var binding: ActivityReaderBinding
    private lateinit var readerFragment: BaseReaderFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.binding = binding

        val readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG)
            ?.let { it as BaseReaderFragment }
            ?: run { createReaderFragment(model.readerInitData) }

        if (readerFragment is VisualReaderFragment) {
            val fullscreenDelegate = FullscreenReaderActivityDelegate(this, readerFragment, binding)
            lifecycle.addObserver(fullscreenDelegate)
        }

        readerFragment?.let { this.readerFragment = it }

        model.activityChannel.receive(this) { handleReaderFragmentEvent(it) }

        reconfigureActionBar()

        supportFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                val locator = OutlineContract.parseResult(result).destination
                closeOutlineFragment(locator)
            }
        )

        supportFragmentManager.addOnBackStackChangedListener {
            reconfigureActionBar()
        }

        // Add support for display cutout.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun createReaderFragment(readerData: ReaderInitData): BaseReaderFragment? {
        val readerClass: Class<out Fragment>? = when (readerData) {
            is EpubReaderInitData -> EpubReaderFragment::class.java
            is ImageReaderInitData -> org.cis_india.wsreader.reader.ImageReaderFragment::class.java
            is MediaReaderInitData -> AudioReaderFragment::class.java
            is PdfReaderInitData -> PdfReaderFragment::class.java
            is DummyReaderInitData -> null
        }

        readerClass?.let { it ->
            supportFragmentManager.commitNow {
                replace(R.id.activity_container, it, Bundle(), READER_FRAGMENT_TAG)
            }
        }

        return supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as BaseReaderFragment?
    }

    override fun onStart() {
        super.onStart()
        reconfigureActionBar()
    }
/*
    private fun reconfigureActionBar() {
        val currentFragment = supportFragmentManager.fragments.lastOrNull()

        title = when (currentFragment) {
            is OutlineFragment -> model.publication.metadata.title
            else -> null
        }

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true
                /*when (currentFragment) {
                    is OutlineFragment -> true
                    else -> false
                }*/
            )
        } else {
            Log.e("ReaderActivity", "ActionBar is null")
        }

    }*/
    private fun reconfigureActionBar() {
        val currentFragment = supportFragmentManager.fragments.lastOrNull()

        title = when (currentFragment) {
            is OutlineFragment -> model.publication.metadata.title
            else -> null
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(
            when (currentFragment) {
                is OutlineFragment -> true
                else -> false
            }
        )
    }

    private fun handleReaderFragmentEvent(command: ReaderViewModel.ActivityCommand) {
        when (command) {
            is ReaderViewModel.ActivityCommand.OpenOutlineRequested ->
                showOutlineFragment()
            is ReaderViewModel.ActivityCommand.OpenExternalLink ->
                launchWebBrowser(this, command.url.toUri())
            is ReaderViewModel.ActivityCommand.ToastError ->
                command.error.show(this)
        }
    }

    private fun showOutlineFragment() {
        val outlineFragment = supportFragmentManager.findFragmentByTag(OUTLINE_FRAGMENT_TAG)
        if(outlineFragment == null) {
            supportFragmentManager.commit {
                add(
                    R.id.activity_container,
                    OutlineFragment::class.java,
                    Bundle(),
                    OUTLINE_FRAGMENT_TAG
                )
                hide(readerFragment)
                addToBackStack(null)
            }
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        readerFragment.go(locator, true)
        supportFragmentManager.popBackStack()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFragmentManager.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
        const val OUTLINE_FRAGMENT_TAG = "outline"
    }
}
