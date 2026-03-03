/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.reader

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SearchView
import androidx.core.os.BundleCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.*
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.toCss
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.epub.pageList
import org.cis_india.wsreader.LITERATA
import org.cis_india.wsreader.R
import org.cis_india.wsreader.reader.preferences.UserPreferencesViewModel
import org.cis_india.wsreader.search.SearchFragment
import android.widget.SeekBar
import org.readium.r2.shared.publication.services.positions
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import android.widget.FrameLayout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.cis_india.wsreader.helpers.PreferenceUtil
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.shared.util.Url
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import org.cis_india.wsreader.reader.preferences.MainPreferencesBottomSheetDialogFragment

@OptIn(ExperimentalReadiumApi::class)
class EpubReaderFragment : VisualReaderFragment() {

    override lateinit var navigator: EpubNavigatorFragment

    private lateinit var menuSearch: MenuItem
    lateinit var menuSearchView: SearchView

    private var isSearchViewIconified = true

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            isSearchViewIconified = savedInstanceState.getBoolean(IS_SEARCH_VIEW_ICONIFIED)
        }

        val readerData = model.readerInitData as? EpubReaderInitData ?: run {
            // We provide a dummy fragment factory  if the ReaderActivity is restored after the
            // app process was killed because the ReaderRepository is empty. In that case, finish
            // the activity as soon as possible and go back to the previous one.
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            requireActivity().finish()
            return
        }

        childFragmentManager.fragmentFactory =
            readerData.navigatorFactory.createFragmentFactory(
                initialLocator = readerData.initialLocation,
                initialPreferences = readerData.preferencesManager.preferences.value,
                listener = model,
                configuration = EpubNavigatorFragment.Configuration {
                    // To customize the text selection menu.
                    // text selection
                    selectionActionModeCallback = customSelectionActionModeCallback

                    // App assets which will be accessible from the EPUB resources.
                    // You can use simple glob patterns, such as "images/.*" to allow several
                    // assets in one go.
                    servedAssets = listOf(
                        // For the custom font Literata.
                        "fonts/.*",
                        // Icon for the annotation side mark, see [annotationMarkTemplate].
                        "annotation-icon.svg"
                    )

                    // Register the HTML templates for our custom decoration styles.
                    decorationTemplates[DecorationStyleAnnotationMark::class] =
                        annotationMarkTemplate()
                    decorationTemplates[DecorationStylePageNumber::class] = pageNumberTemplate()

                    // Declare a custom font family for reflowable EPUBs.
                    addFontFamilyDeclaration(FontFamily.LITERATA) {
                        addFontFace {
                            addSource("fonts/Literata-VariableFont_opsz,wght.ttf")
                            setFontStyle(FontStyle.NORMAL)
                            // Literata is a variable font family, so we can provide a font weight range.
                            setFontWeight(200..900)
                        }
                        addFontFace {
                            addSource("fonts/Literata-Italic-VariableFont_opsz,wght.ttf")
                            setFontStyle(FontStyle.ITALIC)
                            setFontWeight(200..900)
                        }
                    }
                }
            )

        childFragmentManager.setFragmentResultListener(
            SearchFragment::class.java.name,
            this,
            FragmentResultListener { _, result ->
                menuSearch.collapseActionView()
                BundleCompat.getParcelable(
                    result,
                    SearchFragment::class.java.name,
                    Locator::class.java
                )?.let {
                    navigator.go(it)
                }
            }
        )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(
                    R.id.fragment_reader_container,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    NAVIGATOR_FRAGMENT_TAG
                )
            }
        }
        navigator =
            childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG) as EpubNavigatorFragment

        return view
    }

    // Save seekbar snap points from book positions start locator
    private var booksPositionsStartProgression: List<Int> = emptyList()

    // save all book positions
    // to help navigate to these positions
    private var allBookPositions: List<Locator> = emptyList()

    fun getNextChapterPosition(
        positionCache: Map<String, PositionInfo>,
        locator: Locator?
    ): ChapterNavigation {

        val currentHref = locator?.href ?: return ChapterNavigation()
        val currentProgression: Double = locator.locations.totalProgression ?: 0.0
        val chapterHrefsInOrder = positionCache.keys.toList()
        val currentIndex = chapterHrefsInOrder.indexOf(currentHref.toString())
        val previousChapterHref: String?
        val nextChapterHref: String?
        val currentChapterHref: String?

        if (currentIndex != -1) {
            previousChapterHref = chapterHrefsInOrder.getOrNull(currentIndex - 1)
            nextChapterHref = chapterHrefsInOrder.getOrNull(currentIndex + 1)
            currentChapterHref = chapterHrefsInOrder.getOrNull(currentIndex)

        } else {
            nextChapterHref = chapterHrefsInOrder
                .firstOrNull { href ->
                    positionCache[href]?.StartChapterProgression ?: Double.MAX_VALUE > currentProgression
                }

            val chapterBelongingToIndex = chapterHrefsInOrder.indexOf(nextChapterHref) - 1

            previousChapterHref = chapterHrefsInOrder.getOrNull(chapterBelongingToIndex - 1)
            currentChapterHref = chapterHrefsInOrder.getOrNull(chapterBelongingToIndex)
        }

        val previousChapterData = previousChapterHref?.let { href ->
            positionCache[href]?.let { info ->
                PositionInfo(
                    info.position,
                    info.StartChapterProgression,
                    info.EndChapterProgression,
                    info.StartChapterLink,
                    info.EndChapterLink
                )
            }
        }

        val nextChapterData = nextChapterHref?.let { href ->
            positionCache[href]?.let { info ->
                PositionInfo(
                    info.position,
                    info.StartChapterProgression,
                    info.EndChapterProgression,
                    info.StartChapterLink,
                    info.EndChapterLink
                )
            }
        }

        val currentChapterData = currentChapterHref?.let { href ->
            positionCache[href]?.let { info ->
                PositionInfo(
                    info.position,
                    info.StartChapterProgression,
                    info.EndChapterProgression,
                    info.StartChapterLink,
                    info.EndChapterLink
                )
            }
        }

        return ChapterNavigation(
            previousChapter = previousChapterData,
            nextChapter = nextChapterData,
            currentChapterHref = currentChapterData
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<SeekBar>(R.id.readingProgressBar)
        val positionCache: MutableMap<String, PositionInfo> = mutableMapOf()

        lifecycleScope.launch {
            val allBookPositions: List<Locator> = publication.positions()
            val chapterBoundaryLocators = mutableMapOf<Url, Pair<Locator, Locator>>()

            val tocHrefs = publication.manifest.tableOfContents
                .map { it.url().removeFragment().removeQuery().normalize() }
                .toSet()

            for (locator in allBookPositions) {
                val chaptersHref = locator.href.removeFragment().removeQuery().normalize()

                if (tocHrefs.contains(chaptersHref)) {
                    if (!chapterBoundaryLocators.containsKey(chaptersHref)) {
                        chapterBoundaryLocators[chaptersHref] = Pair(locator, locator)
                    } else {
                        val currentPair = chapterBoundaryLocators.getValue(chaptersHref)
                        chapterBoundaryLocators[chaptersHref] = currentPair.copy(second = locator)
                    }
                }
            }

            chapterBoundaryLocators.forEach { (_, pair) ->
                val startLocator = pair.first
                val endLocator = pair.second

                val startHrefString = startLocator.href.toString()
                val startPosition = startLocator.locations.position
                val startTotalProgression = startLocator.locations.totalProgression

                if (startTotalProgression != null) {
                    val endOfChapter = endLocator.locations.totalProgression ?: 1.0
                    positionCache[startHrefString] = PositionInfo(
                        startPosition,
                        startTotalProgression,
                        endOfChapter,
                        startLocator,
                        endLocator
                    )
                }
            }
        }

        val marginInPx = (12 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(progressBar) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = systemBars.bottom + marginInPx
            }

            windowInsets
        }

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, thumbSwiped: Boolean) {
                // Handle progress changes
                if (thumbSwiped && seekBar != null && booksPositionsStartProgression.isNotEmpty()) {

                    val closestPoint =
                        booksPositionsStartProgression.minByOrNull { Math.abs(it - progress) }
                            ?: progress

                    if (progress != closestPoint) {
                        seekBar.progress = closestPoint
                    }
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // handle start
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                // Navigate to selected progress
                // When you release your thumb.

                val progressOnThumbRelease = seekBar?.progress ?: return

                val targetPositionLocator = allBookPositions.minByOrNull {
                    val point = ((it.locations.totalProgression ?: 0.0) * 100).toInt()
                    Math.abs(point - progressOnThumbRelease)
                }

                targetPositionLocator?.let {
                    navigator.go(it)
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            // update book positions
            allBookPositions = publication.positions()

            // update seekbar snap points from book positions
            booksPositionsStartProgression = allBookPositions.map {
                ((it.locations.totalProgression ?: 0.0) * 100).toInt()
            }.distinct().sorted()

            // add seekbar max as the last position total progression
            // This enables seekbar to show accurate complete status
            // As progression does not get to 100 percent fully
            if (booksPositionsStartProgression.isNotEmpty()) {
                progressBar.max = booksPositionsStartProgression.last()
            }

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigator.currentLocator.collect { locator ->
                    val progress = locator?.locations?.totalProgression ?: 0.0
                    progressBar.progress = (progress * 100).toInt()
                }
            }
        }

        @Suppress("Unchecked_cast")
        val userPreferencesViewModel =
            (model.settings as UserPreferencesViewModel<EpubSettings, EpubPreferences>)

        userPreferencesViewModel.bind(navigator, viewLifecycleOwner)

        val prefUtil = PreferenceUtil(requireContext())
        val continuousChaptersFlow = kotlinx.coroutines.flow.MutableStateFlow(
            prefUtil.getString(
                "chapter_scroll_pref",
                "Left/Right"
            ) == "Up/Down"
        )

        val prefs =
            requireContext().getSharedPreferences("wikisource_settings", Context.MODE_PRIVATE)
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "chapter_scroll_pref") {
                    continuousChaptersFlow.value =
                        prefUtil.getString("chapter_scroll_pref", "Left/Right") == "Up/Down"
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        viewLifecycleOwner.lifecycle.addObserver(object :
            androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        })

        val horizontalSwipeDisableLayout =
            view.findViewById<HorizontalSwipeDisableLayout>(R.id.horizontal_swipe_disable_layout)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val scrollInputListener = createChapterInputListener(positionCache)

                continuousChaptersFlow
                    .combine(userPreferencesViewModel.editor.filterNotNull()) { chapterScrolling, editor ->
                        Pair(chapterScrolling, editor)
                    }
                    .collect { (chapterScrolling, editor) ->
                        val scrollSettings = editor.preferences.scroll ?: false

                        if (scrollSettings && chapterScrolling) {
                            navigator.addInputListener(scrollInputListener)
                            horizontalSwipeDisableLayout?.disableHorizontalSwipe = true
                        } else {
                            navigator.removeInputListener(scrollInputListener)
                            horizontalSwipeDisableLayout?.disableHorizontalSwipe = false
                        }
                    }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Display page number labels if the book contains a `page-list` navigation document.
                (navigator as? DecorableNavigator)?.applyPageNumberDecorations()
            }
        }

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuSearch = menu.findItem(R.id.search).apply {
                        isVisible = true
                        menuSearchView = actionView as SearchView
                    }

                    if (!isTutorialStarted.value) {
                        view.post {
                            startMenuTutorial()
                        }
                    }

                    connectSearch()
                    if (!isSearchViewIconified) menuSearch.expandActionView()
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.search -> {
                            return true
                        }

                        android.R.id.home -> {
                            menuSearch.collapseActionView()
                            return true
                        }
                    }
                    return false
                }
            },
            viewLifecycleOwner
        )
    }

    private fun getActiveWebView(): android.webkit.WebView? {
        var activeWebView: android.webkit.WebView? = null
        val root = navigator.view ?: return null
        fun traverse(view: View) {
            if (view is android.webkit.WebView) {
                val rect = android.graphics.Rect()
                // A WebView is genuinely active if it's visible and occupying screen space
                if (view.getGlobalVisibleRect(rect) && rect.width() > 100 && rect.height() > 100) {
                    activeWebView = view
                }
                return
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) traverse(view.getChildAt(i))
            }
        }
        traverse(root)
        return activeWebView
    }

    private fun createChapterInputListener(positionCache: Map<String, PositionInfo>): InputListener {
        return object : InputListener {
            override fun onDrag(event: DragEvent): Boolean {

                if (event.type != DragEvent.Type.End) {
                    return false
                }

                val distanceDragged: Double = event.offset.y.toDouble()
                val locator = navigator.currentLocator.value
                val currentTotalProgression = locator?.locations?.totalProgression ?: 0.0
                val navigation = getNextChapterPosition(positionCache, locator)
                val nextChapterInfo = navigation.nextChapter
                val previousChapterInfo = navigation.previousChapter
                val currentChaperInfo = navigation.currentChapterHref

                val webView = getActiveWebView()
                val isAtBottom = webView?.canScrollVertically(1) == false
                val isAtTop = webView?.canScrollVertically(-1) == false

                // if scrolling down and next chapter is available
                if (nextChapterInfo != null && distanceDragged < 0) {
                    val currentChapterEndProgression: Double =
                        currentChaperInfo?.EndChapterProgression ?: 1.0
                    if (isAtBottom || Math.abs(currentChapterEndProgression - currentTotalProgression) < 0.0001) {
                        val nextChapterStartLocator: Locator = nextChapterInfo.StartChapterLink
                        navigator.go(nextChapterStartLocator)
                        return true
                    }

                    // if scrolling up and previous chapter is available
                } else if (previousChapterInfo != null && distanceDragged > 0) {
                    val currentChapterStartProgression: Double =
                        currentChaperInfo?.StartChapterProgression ?: 0.0

                    if (isAtTop || currentTotalProgression <= currentChapterStartProgression + 0.000001) {
                        val previousChapterEndLocator: Locator = previousChapterInfo.EndChapterLink
                        navigator.go(previousChapterEndLocator)
                        return true
                    }
                }

                return false
            }
        }
    }

    // start menu tap target
    // show onboarding guide for reader menu
    private fun startMenuTutorial() {
        val activity = requireActivity()
        val targets = mutableListOf<TapTarget>()

        activity.findViewById<View>(R.id.search)?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    getString(R.string.tutorial_search_title),
                    getString(R.string.tutorial_search_desc)
                )
            )
        }

        activity.findViewById<View>(R.id.tts)?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    getString(R.string.tutorial_tts_title),
                    getString(R.string.tutorial_tts_desc)
                )
            )
        }

        activity.findViewById<View>(R.id.bookmark)?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    getString(R.string.tutorial_bookmark_title),
                    getString(R.string.tutorial_bookmark_desc)
                )
            )
        }

        activity.findViewById<View>(R.id.settings)?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    getString(R.string.tutorial_settings_title),
                    getString(R.string.tutorial_settings_desc)
                )
            )
        }

        activity.findViewById<View>(R.id.toc)?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    getString(R.string.tutorial_toc_title),
                    getString(R.string.tutorial_toc_desc)
                )
            )
        }

        if (targets.isNotEmpty()) {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        onMenuTutorialComplete()
                    }

                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                        // Optional: Runs after every single step
                        if (targetClicked && lastTarget?.id() == R.id.settings) {
                            // open settings form more guides
//                            openSettings()
                        }
                    }

                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        onReaderMenuboardingComplete()
                    }
                })
                .start()
        }
    }


    // show tap target for navigation i.e
    // navigate next page, navigate previous page and toggle the menu.
    private fun startTapTutorial() {
        val activity = requireActivity()
        val targets = mutableListOf<TapTarget>()

        activity.findViewById<View>(R.id.touch_center)?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    getString(R.string.tutorial_menu_toggle_title),
                    getString(R.string.tutorial_menu_toggle_desc)
                )
            )
        }

        if (targets.isNotEmpty()) {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        onTapTutorialComplete()
                    }

                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                        // Optional: Runs after every single step
                    }

                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        onReaderMenuboardingComplete()
                    }
                })
                .start()
        }
    }

//    private fun openSettings() {
//        MainPreferencesBottomSheetDialogFragment()
//            .show(childFragmentManager, "Settings")
//    }

    private fun onMenuTutorialComplete() {
        updateSystemUiVisibility()
        startTapTutorial()
    }

    private fun onTapTutorialComplete() {
        onReaderMenuboardingComplete()
        startSelectionPromptTutorial()
    }

    private fun startSelectionPromptTutorial() {
        val activity = requireActivity()
        val centerView = activity.findViewById<View>(R.id.touch_center) ?: view

        TapTargetView.showFor(activity,
            TapTarget.forView(
                centerView,
                getString(R.string.tutorial_select_text_title),
                getString(R.string.tutorial_select_text_desc)
            )
                .cancelable(true)
                .transparentTarget(true),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                }
            }
        )
    }

    /**
     * Will display margin labels next to page numbers in an EPUB publication with a `page-list`
     * navigation document.
     *
     * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
     */
    private suspend fun DecorableNavigator.applyPageNumberDecorations() {
        val decorations = publication.pageList
            .mapIndexedNotNull { index, link ->
                val label = link.title ?: return@mapIndexedNotNull null
                val locator = publication.locatorFromLink(link) ?: return@mapIndexedNotNull null

                Decoration(
                    id = "page-$index",
                    locator = locator,
                    style = DecorationStylePageNumber(label = label)
                )
            }

        applyDecorations(decorations, "pageNumbers")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SEARCH_VIEW_ICONIFIED, isSearchViewIconified)
    }

    private fun connectSearch() {
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (isSearchViewIconified) { // It is not a state restoration.
                    showSearchFragment()
                }

                isSearchViewIconified = false
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                isSearchViewIconified = true
                childFragmentManager.popBackStack()
                menuSearchView.clearFocus()

                return true
            }
        })

        menuSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                model.search(query)
                menuSearchView.clearFocus()

                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })

        menuSearchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            .setOnClickListener {
                menuSearchView.requestFocus()
                model.cancelSearch()
                menuSearchView.setQuery("", false)

                (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(
                    this.view,
                    0
                )
            }
    }

    private fun showSearchFragment() {
        childFragmentManager.commit {
            childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)?.let { remove(it) }
            add(
                R.id.fragment_reader_container,
                SearchFragment::class.java,
                Bundle(),
                SEARCH_FRAGMENT_TAG
            )
            hide(navigator)
            addToBackStack(SEARCH_FRAGMENT_TAG)
        }
    }

    companion object {
        private const val SEARCH_FRAGMENT_TAG = "search"
        private const val NAVIGATOR_FRAGMENT_TAG = "navigator"
        private const val IS_SEARCH_VIEW_ICONIFIED = "isSearchViewIconified"
    }
// Examples of HTML templates for custom Decoration Styles.

    /**
     * This Decorator Style will display a tinted "pen" icon in the page margin to show that a highlight
     * has an associated note.
     *
     * Note that the icon is served from the app assets folder.
     */
    private fun annotationMarkTemplate(@ColorInt defaultTint: Int = Color.YELLOW): HtmlDecorationTemplate {
        val className = "testapp-annotation-mark"
        val iconUrl = checkNotNull(EpubNavigatorFragment.assetUrl("annotation-icon.svg"))
        return HtmlDecorationTemplate(
            layout = HtmlDecorationTemplate.Layout.BOUNDS,
            width = HtmlDecorationTemplate.Width.PAGE,
            element = { decoration ->
                val style = decoration.style as? DecorationStyleAnnotationMark
                val tint = style?.tint ?: defaultTint
                // Using `data-activable=1` prevents the whole decoration container from being
                // clickable. Only the icon will respond to activation events.
                """
            <div><div data-activable="1" class="$className" style="background-color: ${tint.toCss()} !important"/></div>"
            """
            },
            stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background: url('$iconUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
        )
    }

    /**
     * This Decoration Style is used to display the page number labels in the margins, when a book
     * provides a `page-list`. The label is stored in the [DecorationStylePageNumber] itself.
     *
     * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
     */
    private fun pageNumberTemplate(): HtmlDecorationTemplate {
        val className = "testapp-page-number"
        return HtmlDecorationTemplate(
            layout = HtmlDecorationTemplate.Layout.BOUNDS,
            width = HtmlDecorationTemplate.Width.PAGE,
            element = { decoration ->
                val style = decoration.style as? DecorationStylePageNumber

                // Using `var(--RS__backgroundColor)` is a trick to use the same background color as
                // the Readium theme. If we don't set it directly inline in the HTML, it might be
                // forced transparent by Readium CSS.
                """
            <div><span class="$className" style="background-color: var(--RS__backgroundColor) !important">${style?.label}</span></div>"
            """
            },
            stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                padding: 0px 4px 0px 4px;
                border: 1px solid;
                border-radius: 20%;
                box-shadow: rgba(50, 50, 93, 0.25) 0px 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
                opacity: 0.8;
            }
            """
        )
    }
}

data class PositionInfo(
    val position: Int?,
    val StartChapterProgression: Double?,
    val EndChapterProgression: Double?,
    val StartChapterLink: Locator,
    val EndChapterLink: Locator,
)

data class ChapterNavigation(
    val previousChapter: PositionInfo? = null,
    val nextChapter: PositionInfo? = null,
    val currentChapterHref: PositionInfo? = null
)