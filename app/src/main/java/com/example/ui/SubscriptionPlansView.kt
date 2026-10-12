package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class SubscriptionPlan(
    val id: String,
    val durationDays: Int,
    val title: String,
    val subtitle: String,
    val price: String,
    val priceNumber: Long,
    val imageUrl: String,
    val buyUrl: String = "https://t.me/MESHKIYT",
    val badge: String? = null
)

object SubscriptionPlansCatalog {
    val plans = listOf(
        SubscriptionPlan(
            id = "plan_7",
            durationDays = 7,
            title = "اشتراک ۷ روزه",
            subtitle = "ویژه تست، پینگ حداقلی و مصرف کوتاه‌مدت",
            price = "۱۵۹,۰۰۰ تومان",
            priceNumber = 159_000L,
            imageUrl = "https://i.postimg.cc/qR3Mm7b0/grok-1790475480816.jpg",
            buyUrl = "https://t.me/MESHKIYT",
            badge = "تست و اولیه"
        ),
        SubscriptionPlan(
            id = "plan_30",
            durationDays = 30,
            title = "اشتراک ۳۰ روزه (یک ماهه)",
            subtitle = "یک ماه دسترسی پرسرعت و پایدار بدون قطعی",
            price = "۲۸۹,۰۰۰ تومان",
            priceNumber = 289_000L,
            imageUrl = "https://i.postimg.cc/XvhYc5gx/grok-1790475474578.jpg",
            buyUrl = "https://t.me/MESHKIYT",
            badge = "پرفروش‌ترین"
        ),
        SubscriptionPlan(
            id = "plan_90",
            durationDays = 90,
            title = "اشتراک ۹۰ روزه (سه ماهه)",
            subtitle = "سه ماه دسترسی مداوم با اولویت ترافیکی بالا",
            price = "۶۸۹,۰۰۰ تومان",
            priceNumber = 689_000L,
            imageUrl = "https://i.postimg.cc/nh3zrg9f/grok-1790475484219.jpg",
            buyUrl = "https://t.me/MESHKIYT",
            badge = "پیشنهادی"
        ),
        SubscriptionPlan(
            id = "plan_180",
            durationDays = 180,
            title = "اشتراک ۱۸۰ روزه (شش ماهه)",
            subtitle = "شش ماه اتصال بدون محدودیت ویژه گیمرها و تریدرها",
            price = "۱,۱۸۹,۰۰۰ تومان",
            priceNumber = 1_189_000L,
            imageUrl = "https://i.postimg.cc/5Nqjd6xs/grok-1790475471488.jpg",
            buyUrl = "https://t.me/MESHKIYT",
            badge = "تخفیف ویژه"
        ),
        SubscriptionPlan(
            id = "plan_365",
            durationDays = 365,
            title = "اشتراک ۳۶۵ روزه (یک ساله)",
            subtitle = "یک سال کامل با پشتیبانی VIP و حداکثر پایداری",
            price = "۱,۸۹۸,۰۰۰ تومان",
            priceNumber = 1_898_000L,
            imageUrl = "https://i.postimg.cc/J0j0hhjt/grok-1790475468255.jpg",
            buyUrl = "https://t.me/MESHKIYT",
            badge = "اقتصادی‌ترین"
        )
    )
}

fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
fun SubscriptionPlansDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.2.dp, CyanPrimary.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .testTag("subscription_plans_dialog"),
                color = CyberNavyBg
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSurface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "پلن‌های اشتراک MESHKIDNS",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تحویل آنی مشخصات ورود پس از پرداخت",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted
                            )
                        }
                    }

                    // Plans List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(SubscriptionPlansCatalog.plans, key = { it.id }) { plan ->
                            SubscriptionPlanCard(
                                plan = plan,
                                onBuyClick = {
                                    openExternalUrl(context, plan.buyUrl)
                                }
                            )
                        }
                    }

                    // Bottom info bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSurface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "پشتیبانی ۲۴ ساعته در تلگرام:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { openExternalUrl(context, "https://t.me/MESHKIYT") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary.copy(alpha = 0.15f),
                                contentColor = CyanPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "@MESHKIYT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    onBuyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (plan.durationDays == 30) CyanPrimary else CyberBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Plan Image Header with AsyncImage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                SubcomposeAsyncImage(
                    model = plan.imageUrl,
                    contentDescription = plan.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CyanPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = plan.title,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CyberSurface.copy(alpha = 0.85f))
                            )
                        )
                )

                // Badge top corner
                plan.badge?.let { b ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (plan.durationDays == 30) CyanPrimary else EmeraldSuccess)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = b,
                            color = CyberNavyBg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Plan Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.title,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = plan.price,
                        color = EmeraldSuccess,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = plan.subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Buy Direct Telegram Button
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("buy_plan_btn_${plan.durationDays}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = CyberNavyBg
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Buy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "خرید اشتراک (${plan.price})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
