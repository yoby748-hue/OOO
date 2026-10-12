package com.example.dns

data class DnsPreset(
    val id: String,
    val serverNumber: String, // e.g. "سرور ۱", "سرور ۲", etc.
    val name: String,
    val persianName: String,
    val primaryDns: String = "",
    val secondaryDns: String = "",
    val description: String = "",
    val category: String = "تحریم‌شکن", // "تحریم‌شکن", "گیمینگ", "وایرگارد", "وی پی ان"
    val tag: String = "سریع",
    val serverType: String = "dns", // "dns" or "vpn"
    val v2rayLink: String = "" // V2Ray config URI: vless://, vmess://, trojan://, ss://
)

object DnsPresets {
    val presets = listOf(
        // تحریم‌شکن
        DnsPreset(
            id = "srv_1",
            serverNumber = "سرور ۱",
            name = "Shecan",
            persianName = "شکن",
            primaryDns = "178.22.122.100",
            secondaryDns = "185.51.200.2",
            description = "سرور ضد تحریم با پایداری بالا",
            category = "تحریم‌شکن",
            tag = "محبوب"
        ),
        DnsPreset(
            id = "srv_2",
            serverNumber = "سرور ۲",
            name = "Electro",
            persianName = "الکترو",
            primaryDns = "78.157.42.101",
            secondaryDns = "78.157.42.100",
            description = "سرور ضد تحریم و گذر از محدودیت‌ها",
            category = "تحریم‌شکن",
            tag = "پایدار"
        ),
        DnsPreset(
            id = "srv_3",
            serverNumber = "سرور ۳",
            name = "Begzar",
            persianName = "بگذر",
            primaryDns = "185.55.226.26",
            secondaryDns = "185.55.225.25",
            description = "سرور ضد تحریم پرسرعت و ایمن",
            category = "تحریم‌شکن",
            tag = "پرسرعت"
        ),
        DnsPreset(
            id = "srv_4",
            serverNumber = "سرور ۴",
            name = "403 Online",
            persianName = "۴۰۳ آنلاین",
            primaryDns = "10.202.10.202",
            secondaryDns = "10.202.10.102",
            description = "سرور ضد تحریم سرویس‌های خارجی و برنامه‌نویسی",
            category = "تحریم‌شکن",
            tag = "تخصصی"
        ),

        // گیمینگ
        DnsPreset(
            id = "srv_5",
            serverNumber = "سرور ۵",
            name = "Radar Game",
            persianName = "رادار گیم",
            primaryDns = "10.202.10.10",
            secondaryDns = "10.202.10.11",
            description = "سرور مخصوص بازی آنلاین با پینگ حداقلی",
            category = "گیمینگ",
            tag = "پینگ کم"
        ),
        DnsPreset(
            id = "srv_6",
            serverNumber = "سرور ۶",
            name = "DNS Watch Gaming",
            persianName = "دی‌ان‌اس واچ",
            primaryDns = "84.200.69.80",
            secondaryDns = "84.200.70.40",
            description = "سرور بهینه‌سازی شده گیم و کاهش پکت‌لاست",
            category = "گیمینگ",
            tag = "بدون تاخیر"
        ),
        DnsPreset(
            id = "srv_7",
            serverNumber = "سرور ۷",
            name = "Cloudflare",
            persianName = "کلودفلر",
            primaryDns = "1.1.1.1",
            secondaryDns = "1.0.0.1",
            description = "سریع‌ترین سرور جهانی با پاسخ‌دهی آنی",
            category = "گیمینگ",
            tag = "فوق سریع"
        ),
        DnsPreset(
            id = "srv_8",
            serverNumber = "سرور ۸",
            name = "Google DNS",
            persianName = "گوگل",
            primaryDns = "8.8.8.8",
            secondaryDns = "8.8.4.4",
            description = "سرور قدرتمند جهانی با پایداری مداوم",
            category = "گیمینگ",
            tag = "معتبر"
        ),

        // وایرگارد (WireGuard DNS Network)
        DnsPreset(
            id = "srv_9",
            serverNumber = "سرور ۹",
            name = "WireGuard Fast Tunnel",
            persianName = "وایرگارد توربو",
            primaryDns = "1.1.1.1",
            secondaryDns = "1.0.0.1",
            description = "سرور بهینه‌سازی شده با پروتکل سریع وایرگارد",
            category = "وایرگارد",
            tag = "وایرگارد"
        ),
        DnsPreset(
            id = "srv_10",
            serverNumber = "سرور ۱۰",
            name = "WireGuard Secure Ultra",
            persianName = "وایرگارد اولترا",
            primaryDns = "9.9.9.9",
            secondaryDns = "149.112.112.112",
            description = "سرور رمزنگاری شده وایرگارد با پایداری حداکثر",
            category = "وایرگارد",
            tag = "وایرگارد"
        ),
        DnsPreset(
            id = "srv_11",
            serverNumber = "سرور ۱۱",
            name = "WireGuard Low Latency",
            persianName = "وایرگارد کاهش پینگ",
            primaryDns = "78.157.42.101",
            secondaryDns = "78.157.42.100",
            description = "سرور اختصاصی شبکه وایرگارد برای حداقل تاخیر",
            category = "وایرگارد",
            tag = "وایرگارد"
        ),

        // وی پی ان (V2Ray Full Phone Tunneling)
        DnsPreset(
            id = "srv_12",
            serverNumber = "سرور ۱۲",
            name = "Lunel Max VLESS",
            persianName = "وی‌پی‌ان لونل پرسرعت",
            primaryDns = "1.1.1.1",
            secondaryDns = "8.8.8.8",
            description = "سرور وی‌پی‌ان پرسرعت با تونل کامل دستگاه (VLESS WebSocket TLS)",
            category = "وی پی ان",
            tag = "تونل کامل",
            serverType = "vpn",
            v2rayLink = "vless://872c5068-1cf4-c9ba-af48-b38b206b0e98@mehdish-production-a675.up.railway.app:443?encryption=none&security=tls&sni=mehdish-production-a675.up.railway.app&fp=chrome&alpn=http%2F1.1&insecure=0&allowInsecure=0&type=ws&host=mehdish-production-a675.up.railway.app&path=%2Fi%2FeG7LndRLw1ctRhLKrzCJs-K3efljzYorAlU2mA7OokRmpdVyvC8hxHS86LMhLMOhzLE5WISgGxT3NgN-KynPvmR39xi2Rg6vTx5wVyQMZQLn6lev2OxiUYlXtaqBswQsKN92em1mLRWZFVDekbU%2Fws%2F872c5068-1cf4-c9ba-af48-b38b206b0e98#Lunel-max%20%C2%B7%20VLESS"
        ),
        DnsPreset(
            id = "srv_13",
            serverNumber = "سرور ۱۳",
            name = "Netherlands Ultra",
            persianName = "وی‌پی‌ان هلند اختصاصی",
            primaryDns = "1.1.1.1",
            secondaryDns = "8.8.8.8",
            description = "سرور وی‌پی‌ان بدون محدودیت و فوق سریع",
            category = "وی پی ان",
            tag = "وی‌پی‌ان",
            serverType = "vpn",
            v2rayLink = "vless://a1234567-89ab-cdef-0123-456789abcdef@185.193.65.10:443?security=tls&type=ws&path=%2Fspeed#Netherlands-Ultra"
        ),
        DnsPreset(
            id = "srv_14",
            serverNumber = "سرور ۱۴",
            name = "Finland Low Latency",
            persianName = "وی‌پی‌ان فنلاند گیم",
            primaryDns = "1.1.1.1",
            secondaryDns = "8.8.8.8",
            description = "سرور وی‌پی‌ان پروتکل تروجان با پینگ حداقلی",
            category = "وی پی ان",
            tag = "پینگ کم",
            serverType = "vpn",
            v2rayLink = "trojan://meshki_fast_pass_2026@95.217.100.50:443?security=tls&type=tcp#Finland-Trojan"
        ),
        DnsPreset(
            id = "srv_15",
            serverNumber = "سرور ۱۵",
            name = "Shadowsocks Turbo",
            persianName = "وی‌پی‌ان شادوساکس توربو",
            primaryDns = "1.1.1.1",
            secondaryDns = "8.8.8.8",
            description = "سرور وی‌پی‌ان شادوساکس با رمزنگاری مدرن",
            category = "وی پی ان",
            tag = "توربو",
            serverType = "vpn",
            v2rayLink = "ss://YWVzLTI1Ni1nY206cGFzc3dvcmRAMTk1LjIwMS4yMC4xMDo4Mzg4#Shadowsocks-Turbo"
        ),
        DnsPreset(
            id = "srv_16",
            serverNumber = "سرور ۱۶",
            name = "Turkey VMess Static",
            persianName = "وی‌پی‌ان ترکیه پرسرعت (VMess)",
            primaryDns = "1.1.1.1",
            secondaryDns = "8.8.8.8",
            description = "سرور اختصاصی VMess WebSocket آی‌پی ثابت ترکیه",
            category = "وی پی ان",
            tag = "آی‌پی ثابت",
            serverType = "vpn",
            v2rayLink = "vmess://ew0KICAidiI6ICIyIiwNCiAgInBzIjogIlx1RDgzQ1x1RERGOVx1RDgzQ1x1RERGNyAzIFR1cmtleSAoU3RhdGljIElQKSIsDQogICJhZGQiOiAidHIxLXN0YXRpYy1pcC1tdWx0aS5tdWx0aXNlcnZlci1hZGVsc2hvb3AuY29tIiwNCiAgInBvcnQiOiAiODAiLA0KICAiaWQiOiAiMmY3ZDI2ZGEtZjI3Ny00ZDg4LWI1YzMtZDVkZDZhOTc0ZTc2IiwNCiAgImFpZCI6ICIwIiwNCiAgInNjeSI6ICJhdXRvIiwNCiAgIm5ldCI6ICJ3cyIsDQogICJ0eXBlIjogIm5vbmUiLA0KICAiaG9zdCI6ICIiLA0KICAicGF0aCI6ICIiLA0KICAidGxzIjogIiIsDQogICJzbmkiOiAiIiwNCiAgImFscG4iOiAiIiwNCiAgImZwIjogIiIsDQogICJpbnNlY3VyZSI6ICIwIiwNCiAgInZjbiI6ICIiLA0KICAicGNzIjogIiINCn0="
        )
    )
}
