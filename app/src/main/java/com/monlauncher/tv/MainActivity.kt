package com.monlauncher.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherApp() }
    }
}

const val ALL_APPS = "__all__"

class AppEntry(
    val label: String,
    val pkg: String,
    val icon: ImageBitmap?,
    val intent: Intent?
)

class Hero(
    val kicker: String,
    val title: String,
    val synopsis: String,
    val badge: String,
    val c1: Color,
    val c2: Color,
    val recent: List<String>
)

val FAVORITES = listOf(
    "com.netflix.ninja",
    "com.amazon.amazonvideo.livingroom",
    "com.disney.disneyplus",
    "com.google.android.youtube.tv",
    "com.spotify.tv.android",
    "fr.orange.tv"
)

val HEROES: Map<String, Hero> = mapOf(
    "com.netflix.ninja" to Hero(
        "Netflix · Top 10 · N°1 des séries en France", "BELOW",
        "Une mystérieuse créature marine sème la terreur dans un village de Terre-Neuve.",
        "6 épisodes", Color(0xFF1B3B2F), Color(0xFF0A0F0D),
        listOf("À l'est d'Éden", "Le Choc des loques", "Reprise 3", "Reprise 4")
    ),
    "com.amazon.amazonvideo.livingroom" to Hero(
        "Prime Video · Populaire en ce moment", "MARSHALS",
        "Kayce Dutton quitte le ranch Yellowstone pour rejoindre les U.S. Marshals.",
        "19 épisodes", Color(0xFF6B5A3A), Color(0xFF14110B),
        listOf("True Detective", "Échappées belles", "Euphoria", "Tanked")
    ),
    "com.disney.disneyplus" to Hero(
        "Disney+ · Populaire en ce moment", "AVATAR",
        "De feu et de cendres : Jake et Neytiri face au peuple des Cendres.",
        "Film", Color(0xFF7A2B1A), Color(0xFF0E0605),
        listOf("Reprise 1", "Reprise 2", "Reprise 3")
    ),
    "com.google.android.youtube.tv" to Hero(
        "YouTube · Pour vous", "Reprenez vos vidéos",
        "Vos abonnements et recommandations, directement sur l'écran d'accueil.",
        "Ouvrir", Color(0xFF3A1414), Color(0xFF0B0505),
        listOf("Abonnements", "Tendances", "Musique")
    ),
    "com.spotify.tv.android" to Hero(
        "Spotify · Écoutés récemment", "Votre musique",
        "Reprenez vos playlists et vos albums favoris.",
        "Ouvrir", Color(0xFF123B22), Color(0xFF050E09),
        listOf("Daily Mix 1", "Découvertes", "Titres likés")
    ),
    "fr.orange.tv" to Hero(
        "Ce soir · En direct", "Club Bruges / Lens",
        "21:00 – 22:56 · Ligue des champions · 3e journée.",
        "3 écrans en direct", Color(0xFF1E4D2B), Color(0xFF060D08),
        listOf("Le fil info", "Échappées belles", "Documentaires")
    ),
    ALL_APPS to Hero(
        "Applications", "Toutes les applis",
        "Parcourez toutes les applications installées sur la télé.",
        "Grille", Color(0xFF1B2433), Color(0xFF080A10),
        emptyList()
    )
)

val DEFAULT_HERO = Hero(
    "Accueil", "Bienvenue", "Sélectionnez une application pour la lancer.",
    "Ouvrir", Color(0xFF1B1F2A), Color(0xFF07080B), emptyList()
)

fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    val normal = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val infos = pm.queryIntentActivities(leanback, 0) + pm.queryIntentActivities(normal, 0)

    return infos
        .map { it.activityInfo.packageName to it }
        .distinctBy { it.first }
        .filter { it.first != ctx.packageName }
        .map { (pkg, ri) ->
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
                ?: pm.getLaunchIntentForPackage(pkg)
            val icon = runCatching { ri.loadIcon(pm).toBitmap(160, 160).asImageBitmap() }.getOrNull()
            AppEntry(ri.loadLabel(pm).toString(), pkg, icon, intent)
        }
        .sortedBy { it.label.lowercase() }
}

fun buildDock(apps: List<AppEntry>): List<AppEntry> {
    val favs = FAVORITES.mapNotNull { f -> apps.firstOrNull { it.pkg == f } }
    val base = if (favs.isNotEmpty()) favs else apps.take(6)
    return base + AppEntry("Toutes les apps", ALL_APPS, null, null)
}

@Composable
fun LauncherApp() {
    val ctx = LocalContext.current
    val apps = remember { loadApps(ctx) }
    val dock = remember(apps) { buildDock(apps) }
    var selected by remember { mutableIntStateOf(0) }
    var showAll by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }

    val current = dock.getOrNull(selected)
    val hero = HEROES[current?.pkg] ?: DEFAULT_HERO

    val c1 by animateColorAsState(hero.c1, tween(500), label = "c1")
    val c2 by animateColorAsState(hero.c2, tween(500), label = "c2")

    val now by produceState(Date()) {
        while (true) { value = Date(); delay(10_000) }
    }

    LaunchedEffect(showAll) {
        if (!showAll) runCatching { firstFocus.requestFocus() }
    }

    BackHandler(enabled = showAll) { showAll = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1, c2))))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)), startY = 200f)
            )
        )

        Column(
            Modifier.align(Alignment.TopEnd).padding(horizontal = 56.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold
            )
            Text(
                SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)
                    .replaceFirstChar { it.uppercase() },
                color = Color(0xB3FFFFFF), fontSize = 13.sp
            )
        }

        Crossfade(
            targetState = hero,
            animationSpec = tween(450),
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 190.dp),
            label = "hero"
        ) { h -> HeroPanel(h, current?.label ?: "") }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.Top
        ) {
            dock.forEachIndexed { i, e ->
                DockItem(
                    entry = e,
                    modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                    onFocused = { selected = i },
                    onClick = {
                        if (e.pkg == ALL_APPS) showAll = true
                        else e.intent?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                )
            }
        }

        if (showAll) AllAppsOverlay(apps) { e ->
            e.intent?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

@Composable
fun HeroPanel(h: Hero, appLabel: String) {
    Column(Modifier.padding(start = 64.dp).fillMaxWidth(0.6f)) {
        Text(h.kicker, color = Color(0xCCFFFFFF), fontSize = 14.sp)
        Text(
            h.title, color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            h.synopsis, color = Color(0xE6FFFFFF), fontSize = 15.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
        )
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Ouvrir $appLabel", true)
            Pill(h.badge, false)
        }
        if (h.recent.isNotEmpty()) {
            Text("Reprendre", color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                h.recent.take(5).forEach { r ->
                    Box(
                        Modifier.size(width = 150.dp, height = 84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x40FFFFFF)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(r, color = Color.White, fontSize = 11.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, primary: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (primary) Color.White else Color(0x40FFFFFF))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (primary) Color.Black else Color.White, maxLines = 1)
    }
}

@Composable
fun DockItem(
    entry: AppEntry,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.3f else 1f, tween(180), label = "scale")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier
                .size(76.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF22262E))
                .border(if (focused) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (entry.icon != null) {
                Image(entry.icon, entry.label, Modifier.size(76.dp))
            } else {
                GridGlyph()
            }
        }
        Text(
            entry.label, color = Color.White.copy(alpha = if (focused) 1f else 0f),
            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp).size(width = 90.dp, height = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GridGlyph() {
    Canvas(Modifier.size(34.dp)) {
        val cell = size.width / 3f
        for (r in 0..2) for (c in 0..2) {
            drawCircle(
                Color(0xFFDDDDDD), radius = cell * 0.28f,
                center = Offset(cell * c + cell / 2, cell * r + cell / 2)
            )
        }
    }
}

@Composable
fun AllAppsOverlay(apps: List<AppEntry>, onOpen: (AppEntry) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color(0xEE000000))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(56.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(apps) { i, e ->
                var focused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.12f else 1f, tween(150), label = "s")
                Column(
                    Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (focused) Color(0x33FFFFFF) else Color.Transparent)
                        .onFocusChanged { focused = it.isFocused }
                        .clickable { onOpen(e) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (e.icon != null) {
                        Image(e.icon, e.label, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
                    }
                    Text(e.label, color = Color.White, fontSize = 12.sp, maxLines = 1,
