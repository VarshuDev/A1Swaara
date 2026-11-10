package com.varshudev.a1swaara.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varshudev.a1swaara.models.Album
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.secondary
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape
import app.a1swaara.providers.youtubemusic.innertube.YoutubeMusicInnertube

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistInfo(
    description: String?,
    year: String?,
    otherInfo: String?,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (!otherInfo.isNullOrBlank() || !year.isNullOrBlank()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                otherInfo?.takeIf { it.isNotBlank() }?.let { info ->
                    Box(
                        modifier = Modifier
                            .clip(12.dp.roundedShape)
                            .background(
                                if (colorPalette.isDefault && colorPalette.isDark) SwaaraElevatedSurface
                                else colorPalette.background1
                            )
                            .border(1.dp, colorPalette.glassBorder, 12.dp.roundedShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        BasicText(
                            text = info,
                            style = typography.xxs.medium.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }

                year?.takeIf { it.isNotBlank() }?.let { yearText ->
                    Box(
                        modifier = Modifier
                            .clip(12.dp.roundedShape)
                            .background(colorPalette.secondaryAccent.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                color = colorPalette.secondaryAccent.copy(alpha = 0.35f),
                                shape = 12.dp.roundedShape
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        BasicText(
                            text = yearText,
                            style = typography.xxs.medium.copy(color = colorPalette.secondaryAccent),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }
        }

        description?.takeIf { it.isNotBlank() }?.let { desc ->
            Attribution(
                text = desc,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PlaylistInfo(
    playlist: YoutubeMusicInnertube.PlaylistOrAlbumPage?,
    modifier: Modifier = Modifier
) = PlaylistInfo(
    description = playlist?.description,
    year = playlist?.year,
    otherInfo = playlist?.otherInfo,
    modifier = modifier
)

@Composable
fun PlaylistInfo(
    playlist: Album?,
    modifier: Modifier = Modifier
) = PlaylistInfo(
    description = playlist?.description,
    year = playlist?.year,
    otherInfo = playlist?.otherInfo,
    modifier = modifier
)
