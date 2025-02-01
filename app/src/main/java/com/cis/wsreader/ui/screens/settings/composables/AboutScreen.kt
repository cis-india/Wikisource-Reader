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

package com.cis.wsreader.ui.screens.settings.composables

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cis.wsreader.BuildConfig
import com.cis.wsreader.R
import com.cis.wsreader.helpers.Constants
import com.cis.wsreader.helpers.Utils
import com.cis.wsreader.ui.common.CustomTopAppBar
import com.cis.wsreader.ui.theme.poppinsFont


@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
        topBar = {
            CustomTopAppBar(headerText = stringResource(id = R.string.about_header)) {
                navController.navigateUp()
            }
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                AppInfoCard()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.developed_by_ws),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, bottom = 12.dp),
                    fontSize = 16.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                DeveloperCard(context = context)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.useful_links),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, bottom = 12.dp, top = 12.dp),
                    fontSize = 16.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    SettingItem(icon = Icons.Filled.Web,
                        mainText = stringResource(id = R.string.link_website),
                        subText = stringResource(id = R.string.link_metapage_desc),
                        onClick = { Utils.openWebLink(context, Constants.META_URL) }
                    )
                    SettingItem(icon = ImageVector.vectorResource(id = R.drawable.ic_github_logo),
                    mainText = stringResource(id = R.string.link_sourcecode),
                    subText = stringResource(id = R.string.link_sourcecode_repo),
                    onClick = { Utils.openWebLink(context, Constants.GITHUB_REPO) }
                    )
                    SettingItem(icon = Icons.Filled.PrivacyTip,
                        mainText = stringResource(id = R.string.link_privacy_policy),
                        subText = stringResource(id = R.string.link_privacy_policy_desc),
                        onClick = { Utils.openWebLink(context, Constants.PRIVACY_POLICY) }
                    )

                }

            }
        }
    }


}

@Composable
private fun LinkButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(all = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size = 18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.SemiBold,
            fontFamily = poppinsFont,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                4.dp
            )
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(245.dp)
        ) {
            AsyncImage(
                model = R.drawable.about_screen_bg,
                contentDescription = null,
                alpha = 0.35f,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            ), startY = 8f
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Image(
                        modifier = Modifier.size(120.dp),
                        painter = painterResource(id = R.drawable.ic_splash_screen),
                        contentDescription = null
                    )
                }

                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = 26.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.about_desc),
                    modifier = Modifier.padding(horizontal = 22.dp),
                    fontSize = 14.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DeveloperCard(context: Context) {
    Card(
        modifier = Modifier
            .height(135.dp)
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                2.dp
            )
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.cis),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(85.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.org_name),
                    fontSize = 18.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(id = R.string.org_website),
                    fontSize = 16.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    /*
                    modifier = Modifier.clickable {
                        Utils.openEmail(
                            context = context,
                            email = Constants.DEV_EMAIL,
                            subject = "Myne Feedback (v${BuildConfig.VERSION_NAME})"
                        )
                    }

                     */
                )
                /*
                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.offset(x = (-4.dp))) {
                    LinkButton(
                        text = "Github",
                        icon = ImageVector.vectorResource(id = R.drawable.ic_github_logo)
                    ) {
                        Utils.openWebLink(context, Constants.DEV_GITHUB_URL)
                    }

                    LinkButton(
                        text = "Telegram",
                        icon = ImageVector.vectorResource(id = R.drawable.ic_telegram_logo)
                    ) {
                        Utils.openWebLink(context, Constants.DEV_TELEGRAM_URL)
                    }
                }
                */
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
@Preview(showBackground = true)
fun AboutScreenPreview() {
    AboutScreen(rememberNavController())
}