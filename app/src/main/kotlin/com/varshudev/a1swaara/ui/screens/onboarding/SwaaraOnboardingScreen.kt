package com.varshudev.a1swaara.ui.screens.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.LocalAppContainer
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraObsidian
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.utils.thumbnail
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class SwaaraLangItem(
    val id: String,
    val name: String,
    val nativeScript: String,
    val singerName: String,
    val singerMonogram: String
)

@Composable
fun SwaaraOnboardingScreen(
    onOnboardingFinished: () -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current

    val languages = remember {
        listOf(
            SwaaraLangItem("kannada", "Kannada", "ಕನ್ನಡ", "Vijay Prakash", "VP"),
            SwaaraLangItem("hindi", "Hindi", "हिंदी", "Arijit Singh", "AS"),
            SwaaraLangItem("english", "English", "English", "Taylor Swift", "TS"),
            SwaaraLangItem("tamil", "Tamil", "தமிழ்", "Anirudh Ravichander", "AR"),
            SwaaraLangItem("telugu", "Telugu", "తెలుగు", "Sid Sriram", "SS"),
            SwaaraLangItem("punjabi", "Punjabi", "ਪੰਜਾਬੀ", "Diljit Dosanjh", "DD"),
            SwaaraLangItem("malayalam", "Malayalam", "മലയാളം", "K. J. Yesudas", "KY"),
            SwaaraLangItem("marathi", "Marathi", "मराठी", "Ajay-Atul", "AA"),
            SwaaraLangItem("bengali", "Bengali", "বাংলা", "Shreya Ghoshal", "SG"),
            SwaaraLangItem("gujarati", "Gujarati", "ગુજરાતી", "Alka Yagnik", "AY"),
            SwaaraLangItem("bhojpuri", "Bhojpuri", "भोजपुरी", "Pawan Singh", "PS"),
            SwaaraLangItem("haryanvi", "Haryanvi", "हरियाणवी", "Sonu Nigam", "SN")
        )
    }

    val singerAvatars = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) {
        coroutineScope {
            languages.map { lang ->
                async(Dispatchers.IO) {
                    val fetchedUrl = runCatching {
                        container.searchResultRepository
                            .searchArtists(lang.singerName, null)
                            ?.getOrNull()
                            ?.items
                            ?.firstOrNull()
                            ?.thumbnail
                            ?.url
                            ?.thumbnail(400)
                    }.getOrNull()
                    if (!fetchedUrl.isNullOrBlank()) {
                        singerAvatars[lang.id] = fetchedUrl
                    }
                }
            }.awaitAll()
        }
    }

    val selectedLanguages = remember { mutableStateListOf("kannada") }
    var userNameInput by remember { mutableStateOf("") }

    val completeWelcome: () -> Unit = {
        val finalName = if (userNameInput.isNotBlank()) userNameInput.trim() else "Music Lover"
        val prefs = context.getSharedPreferences("swaara_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", finalName)
            .putStringSet("selected_languages", selectedLanguages.toSet())
            .putString("active_language", selectedLanguages.firstOrNull() ?: "kannada")
            .putBoolean("onboarding_completed", true)
            .commit()
        onOnboardingFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwaaraObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Hero Brand Showcase
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.8f),
                        spotColor = SwaaraMagenta.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(SwaaraElevatedSurface)
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(SwaaraMagenta, SwaaraViolet, SwaaraCyan)),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.a1_swaara_logo),
                    contentDescription = "A1 Swaara",
                    modifier = Modifier.size(42.dp)
                )
            }

            Column {
                Text(
                    text = "Welcome to A1 Swaara",
                    color = SwaaraPrimaryText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "High-Fidelity Sovereign Audio Experience",
                    color = SwaaraCyan.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Frosted Glass Name Input Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SwaaraDeepSurface.copy(alpha = 0.92f))
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.person),
                        contentDescription = null,
                        tint = SwaaraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    textStyle = TextStyle(
                        color = SwaaraPrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(SwaaraCyan),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (userNameInput.isEmpty()) {
                            Text(
                                text = "Enter your display name (e.g. Varshan)",
                                color = SwaaraSecondaryText.copy(alpha = 0.6f),
                                fontSize = 13.5.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choose Your Sonic Languages",
                color = SwaaraPrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${selectedLanguages.size} selected",
                color = SwaaraMagenta,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Languages Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(languages, key = { it.id }) { lang ->
                val isSelected = selectedLanguages.contains(lang.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) SwaaraElevatedSurface
                            else SwaaraDeepSurface
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) Brush.horizontalGradient(listOf(SwaaraMagenta, SwaaraViolet))
                            else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f))),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            if (isSelected) {
                                if (selectedLanguages.size > 1) {
                                    selectedLanguages.remove(lang.id)
                                }
                            } else {
                                selectedLanguages.add(lang.id)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Artist Portrait with instant stylized gradient & monogram fallback
                        val resolvedAvatar = singerAvatars[lang.id]
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            SwaaraViolet.copy(alpha = 0.55f),
                                            SwaaraMagenta.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    if (isSelected) SwaaraCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.14f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.singerMonogram,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            if (!resolvedAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = resolvedAvatar,
                                    contentDescription = "${lang.singerName} (${lang.name})",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lang.name,
                                color = SwaaraPrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${lang.nativeScript} • ${lang.singerName}",
                                color = if (isSelected) SwaaraCyan else SwaaraSecondaryText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Selection Indicator
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(SwaaraMagenta, SwaaraViolet))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.sparkles),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Get Started Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(27.dp),
                    ambientColor = Color.Black.copy(alpha = 0.7f),
                    spotColor = SwaaraMagenta.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SwaaraMagenta,
                            SwaaraViolet,
                            SwaaraCyan.copy(alpha = 0.8f)
                        )
                    )
                )
                .clickable { completeWelcome() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Start Listening to A1 Swaara",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp
                )
                Icon(
                    painter = painterResource(id = R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}
