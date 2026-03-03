# Wikisource-Reader

## Overview
Wikisource Reader is an Android app to read public domain and freely licensed works from the [Wikisource project](https://en.wikipedia.org/wiki/Wikisource). Catalogue of books is fetched from an [API](https://meta.wikimedia.org/wiki/Wikisource_reader_app#The_API). The app is built upon the [Myne app](https://github.com/Pool-Of-Tears/Myne/) with the Reader interface from [Readium](https://github.com/readium/kotlin-toolkit). Development is supported by the Centre for Internet and Society.

## Features
The following features are currently functional:

- Clean and beautiful user interface with Dark and Light themes
- Browse, filter, search, and download free e-books in multiple languages and literary forms
- Manage local library with indicator of book completion and option to jump through chapters
- In-built e-book reader with LTR and RTL support, horizontal and vertical scrolling
- Customization options: font color, size, weight, typefaces (including OpenDyslexic), and margins
- Reading modes: Light, Dark, Sepia, and customized color mode
- Text customization: line height, paragraph indent, paragraph spacing, word spacing, and letter spacing
- Options to highlight, underline, annotate texts, lock orientation, and bookmark
- Text to Speech in different languages with customizable speed and pitch

## Documentation

Welcome to Wikisource Reader app documentation!


### Wikisource Reader App
This is the mobile client for the Wikisource Reader, providing an optimized interface for reading validated public domain texts. It consumes data from the WSIndex API to display curated collections.
The app is built using modern Android standards with a focus on offline accessibility and ePUB rendering.

- **Source Code**: [https://github.com/cis-india/Wikisource-Reader](https://github.com/cis-india/Wikisource-Reader)
- **Available on**: [Google Play Store](https://play.google.com/store/apps/details?id=org.cis_india.wsreader)

#### Built With:
The following core technologies and frameworks were used to build the mobile application:
- [Kotlin ^2.0.21](https://kotlinlang.org/docs/home.html) - The primary programming language for modern Android development.
- [Jetpack Compose](https://developer.android.com/develop/ui/compose/documentation) - The modern toolkit for building UI.
- [Room Database](https://developer.android.com/training/data-storage/room) - For local storage of book metadata and reading progress.
- [Readium](https://readium.org/kotlin-toolkit/2.4.0/) - The heavy-duty engine used for ePUB parsing and rendering.

#### Getting Started Locally:
Follow these instructions to set up the development environment and run the application on an emulator or physical device.

**Prerequisites:**
- **Hardware Requirements:**
  1. RAM: 8GB or higher (16GB recommended for Android Studio)
  2. Storage: 10GB or more of free space for SDKs and Build tools
- **Software Environment:**
  1. Android Studio: Ladybug (2024.2.1) or above
  2. JDK: Java Development Kit (JDK) 17
  3. Android SDK: API Level 34 (Target SDK)

**Running project Locally:**
To build and run the app locally, follow these steps:

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/cis-india/Wikisource-Reader.git
   cd Wikisource-Reader
   ```

2. **Open in Android Studio:**
   Launch Android Studio, select **File > Open**, and navigate to the cloned project directory. Allow Gradle to sync and download necessary dependencies.

3. **Run on Device or Emulator:**
   Connect a physical device via USB (with Debugging enabled) or start a Virtual Device (AVD). Press **Shift + F10** or click the green **Run** button in the toolbar.
   You can also pair your device via WIFI instead of using USB. Once you enable developer mode on your android phone, you will find both pair via usb or WIFI.

4. **Pointing to Local API (Optional):**
   If you are running WSIndex locally and want the app to connect to it, update the `baseApiUrl` in the `api/BookApi` to your local IP:
   ```kotlin
   // If you are running on Emulator
   private val baseApiUrl = "http://10.0.2.2:8000/books"
   // Use IP address of your host machine(PC), when you want to run in physical device.
   private val baseApiUrl = "http://192.168.100.2:8000/books"
   ```

**Allowing HTTP Traffic for Local Development:**
By default, Android prevents apps from connecting to servers via unencrypted HTTP. To connect the app to your local instance of **WSIndex**, follow these steps to configure Network Security:

1. **Create the Network Security Directory:**
   In your project explorer, navigate to `app/src/main/res`. If an `xml` folder does not exist, right-click **res**, select **New > Directory**, and name it `xml`.

2. **Create the Config File:**
   Right-click on the `res/xml` folder and select **New > XML Resource File**. Name it `network_security_config.xml` and replace its contents with the following:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <domain-config cleartextTrafficPermitted="true">
           <!-- Host IP -->
           <domain includeSubdomains="true">192.168.100.2</domain>
           <domain includeSubdomains="true">10.0.2.2</domain>
           <domain includeSubdomains="true">127.0.0.1</domain>
       </domain-config>
   </network-security-config>
   ```

3. **Link the Configuration in the Manifest:**
   Android requires you to register this file in the application manifest to apply the security exceptions.
   Open `app/src/main/AndroidManifest.xml` and add the `android:networkSecurityConfig` attribute inside the `<application>` tag:
   ```xml
   <application
       android:name=".WikisourceApplication"
       android:networkSecurityConfig="@xml/network_security_config"
       ... >
       <!-- Activities and Services -->
   </application>
   ```

#### Technical Overview: Book Data Model & API

The application relies on a structured **Book Data Model** heavily driven by `Kotlinx Serialization` mapped precisely against JSON payloads received from the **Book API** hosted on Toolforge.

##### 1. Book Data Model
The central model handling books is mapped into the `org.cis_india.wsreader.api.models.Book` data class. Essential fields include:
- **`wikidataQid`**: The unique identifier from Wikidata (e.g., "Q12345"), acting as the primary key.
- **`title` & `titleNativeLanguage`**: The normalized and native book titles.
- **`authors`, `translators`, `editors`**: Collections tracking the contributors mapped to the book.
- **`languages`, `genre`, `subjects`**: Arrays handling linguistic attributes and thematic metadata for filtering.
- **`epubUrl`, `wsUrl`**: Direct links to download the EPUB file parsing locally and viewing the source work directly on Wikisource.
- **`thumbnailUrl`**: URL directing to the mapped cover image.
- **`dateOfPublication`, `publishers`, `place_of_publication`**: Historical and chronological data mappings.

##### 2. Book API (`BookAPI`)
Network operations are managed via the `BookAPI` client located at `org.cis_india.wsreader.api.BookAPI`. It utilizes **OkHttp** integrated seamlessly with **Kotlin Coroutines** and a robust custom built **Cache Interceptor** (`CacheInterceptor`) minimizing repeat payload requests over active sessions.

**Important Endpoints:**
- **`getAllBooks`**: Queries default paginated feeds of proofread texts. (`?page={page}&languages={lang}&sort={sort}`)
- **`getBookById`**: Targets individual volumes globally identified by their Wikidata QID payload. (`?ids=Q{id}`)
- **`searchBooks`**: Pings URL-encoded user texts aggressively across the remote backend. (`?search={query}`)
- **`getBooksByCategory`**: Targets texts assigned distinct genres or topical tags inside the directory. (`?genres={category}&page={page}`)
- **`postDownloadedBookDetails`**: Fires analytic POST requests internally upon physical local book downloads to record engagement metrics mapping `wikidata_qid` and `book_title`.


## External Links
- [Website](https://cis-india.github.io/wikisource-reader-app)
- [Meta Wiki page](https://meta.wikimedia.org/wiki/Wikisource_reader_app)
- [Phabricator issue tracker](https://phabricator.wikimedia.org/project/board/8233/query/all/)
---
