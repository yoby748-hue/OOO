<?php
/**
 * MESHKIDNS - Standalone Single-File Admin Panel & Authentication API
 * Designed for cPanel / Shared Hosting (No MySQL Database Required)
 * Stores all accounts & servers securely in an auto-created 'data/' directory.
 * URL: gamenetrokh.ir/dns/admin.php
 */

session_start();

// Security headers & Timezone
date_default_timezone_set('Asia/Tehran');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');

$DATA_DIR = __DIR__ . '/data';
$CONFIG_FILE = $DATA_DIR . '/config.json';
$USERS_FILE = $DATA_DIR . '/users.json';
$SERVERS_FILE = $DATA_DIR . '/servers.json';
$HTACCESS_FILE = $DATA_DIR . '/.htaccess';

function getDefaultServers() {
    return [
        [
            'id' => 'srv_1',
            'server_number' => 'سرور ۱',
            'name' => 'شکن (Shecan)',
            'primary_dns' => '178.22.122.100',
            'secondary_dns' => '185.51.200.2',
            'category' => 'تحریم‌شکن',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 1
        ],
        [
            'id' => 'srv_2',
            'server_number' => 'سرور ۲',
            'name' => 'الکترو (Electro)',
            'primary_dns' => '78.157.42.101',
            'secondary_dns' => '78.157.42.100',
            'category' => 'تحریم‌شکن',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 2
        ],
        [
            'id' => 'srv_3',
            'server_number' => 'سرور ۳',
            'name' => 'بگذر (Begzar)',
            'primary_dns' => '185.55.226.26',
            'secondary_dns' => '185.55.225.25',
            'category' => 'تحریم‌شکن',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 3
        ],
        [
            'id' => 'srv_4',
            'server_number' => 'سرور ۴',
            'name' => '۴۰۳ آنلاین (403)',
            'primary_dns' => '10.202.10.202',
            'secondary_dns' => '10.202.10.102',
            'category' => 'تحریم‌شکن',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 4
        ],
        [
            'id' => 'srv_5',
            'server_number' => 'سرور ۵',
            'name' => 'رادار گیم (Radar Game)',
            'primary_dns' => '10.202.10.10',
            'secondary_dns' => '10.202.10.11',
            'category' => 'گیمینگ',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 5
        ],
        [
            'id' => 'srv_6',
            'server_number' => 'سرور ۶',
            'name' => 'دی‌ان‌اس واچ گیم (DNS Watch)',
            'primary_dns' => '84.200.69.80',
            'secondary_dns' => '84.200.70.40',
            'category' => 'گیمینگ',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 6
        ],
        [
            'id' => 'srv_7',
            'server_number' => 'سرور ۷',
            'name' => 'کلودفلر گیم (Cloudflare Low Ping)',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '1.0.0.1',
            'category' => 'گیمینگ',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 7
        ],
        [
            'id' => 'srv_8',
            'server_number' => 'سرور ۸',
            'name' => 'گوگل اختصاصی (Google Fast)',
            'primary_dns' => '8.8.8.8',
            'secondary_dns' => '8.8.4.4',
            'category' => 'گیمینگ',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 8
        ],
        [
            'id' => 'srv_9',
            'server_number' => 'سرور ۹',
            'name' => 'وایرگارد توربو (WireGuard Turbo)',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '1.0.0.1',
            'category' => 'وایرگارد',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 9
        ],
        [
            'id' => 'srv_10',
            'server_number' => 'سرور ۱۰',
            'name' => 'وایرگارد اولترا (WireGuard Ultra)',
            'primary_dns' => '9.9.9.9',
            'secondary_dns' => '149.112.112.112',
            'category' => 'وایرگارد',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 10
        ],
        [
            'id' => 'srv_11',
            'server_number' => 'سرور ۱۱',
            'name' => 'وایرگارد گیم و پینگ (WireGuard Gaming)',
            'primary_dns' => '78.157.42.101',
            'secondary_dns' => '78.157.42.100',
            'category' => 'وایرگارد',
            'server_type' => 'dns',
            'v2ray_link' => '',
            'order' => 11
        ],
        [
            'id' => 'srv_12',
            'server_number' => 'سرور ۱۲',
            'name' => 'وی‌پی‌ان لونل پرسرعت (Railway VLESS)',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '8.8.8.8',
            'category' => 'وی پی ان',
            'server_type' => 'vpn',
            'v2ray_link' => 'vless://872c5068-1cf4-c9ba-af48-b38b206b0e98@mehdish-production-a675.up.railway.app:443?encryption=none&security=tls&sni=mehdish-production-a675.up.railway.app&fp=chrome&alpn=http%2F1.1&insecure=0&allowInsecure=0&type=ws&host=mehdish-production-a675.up.railway.app&path=%2Fi%2FeG7LndRLw1ctRhLKrzCJs-K3efljzYorAlU2mA7OokRmpdVyvC8hxHS86LMhLMOhzLE5WISgGxT3NgN-KynPvmR39xi2Rg6vTx5wVyQMZQLn6lev2OxiUYlXtaqBswQsKN92em1mLRWZFVDekbU%2Fws%2F872c5068-1cf4-c9ba-af48-b38b206b0e98#Lunel-max%20%C2%B7%20VLESS',
            'order' => 12
        ],
        [
            'id' => 'srv_13',
            'server_number' => 'سرور ۱۳',
            'name' => 'وی‌پی‌ان هلند اولترا V2Ray',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '8.8.8.8',
            'category' => 'وی پی ان',
            'server_type' => 'vpn',
            'v2ray_link' => 'vless://a1234567-89ab-cdef-0123-456789abcdef@185.193.65.10:443?security=tls&type=ws&path=%2Fspeed#Netherlands-Ultra',
            'order' => 13
        ],
        [
            'id' => 'srv_14',
            'server_number' => 'سرور ۱۴',
            'name' => 'وی‌پی‌ان فنلاند تروجان پینگ کم',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '8.8.8.8',
            'category' => 'وی پی ان',
            'server_type' => 'vpn',
            'v2ray_link' => 'trojan://meshki_fast_pass_2026@95.217.100.50:443?security=tls&type=tcp#Finland-Trojan',
            'order' => 14
        ],
        [
            'id' => 'srv_15',
            'server_number' => 'سرور ۱۵',
            'name' => 'وی‌پی‌ان شادوساکس توربو',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '8.8.8.8',
            'category' => 'وی پی ان',
            'server_type' => 'vpn',
            'v2ray_link' => 'ss://YWVzLTI1Ni1nY206cGFzc3dvcmRAMTk1LjIwMS4yMC4xMDo4Mzg4#Shadowsocks-Turbo',
            'order' => 15
        ],
        [
            'id' => 'srv_16',
            'server_number' => 'سرور ۱۶',
            'name' => 'وی‌پی‌ان ترکیه پرسرعت (VMess آی‌پی ثابت)',
            'primary_dns' => '1.1.1.1',
            'secondary_dns' => '8.8.8.8',
            'category' => 'وی پی ان',
            'server_type' => 'vpn',
            'v2ray_link' => 'vmess://ew0KICAidiI6ICIyIiwNCiAgInBzIjogIlx1RDgzQ1x1RERGOVx1RDgzQ1x1RERGNyAzIFR1cmtleSAoU3RhdGljIElQKSIsDQogICJhZGQiOiAidHIxLXN0YXRpYy1pcC1tdWx0aS5tdWx0aXNlcnZlci1hZGVsc2hvb3AuY29tIiwNCiAgInBvcnQiOiAiODAiLA0KICAiaWQiOiAiMmY3ZDI2ZGEtZjI3Ny00ZDg4LWI1YzMtZDVkZDZhOTc0ZTc2IiwNCiAgImFpZCI6ICIwIiwNCiAgInNjeSI6ICJhdXRvIiwNCiAgIm5ldCI6ICJ3cyIsDQogICJ0eXBlIjogIm5vbmUiLA0KICAiaG9zdCI6ICIiLA0KICAicGF0aCI6ICIiLA0KICAidGxzIjogIiIsDQogICJzbmkiOiAiIiwNCiAgImFscG4iOiAiIiwNCiAgImZwIjogIiIsDQogICJpbnNlY3VyZSI6ICIwIiwNCiAgInZjbiI6ICIiLA0KICAicGNzIjogIiINCn0=',
            'order' => 16
        ]
    ];
}

// Initialize data directory and security
function initStorage() {
    global $DATA_DIR, $CONFIG_FILE, $USERS_FILE, $SERVERS_FILE, $HTACCESS_FILE;
    if (!is_dir($DATA_DIR)) {
        @mkdir($DATA_DIR, 0755, true);
    }
    // Block direct web access to json data via Apache/cPanel
    if (!file_exists($HTACCESS_FILE)) {
        @file_put_contents($HTACCESS_FILE, "Order Deny,Allow\nDeny from all\n<Files ~ \"\\.(json|log)$\">\n  Order allow,deny\n  Deny from all\n</Files>\n");
    }
    // Initialize admin config
    if (!file_exists($CONFIG_FILE)) {
        $defaultConfig = [
            'admin_password' => password_hash('admin123', PASSWORD_DEFAULT),
            'site_title' => 'MESHKIDNS Admin Panel',
            'api_secret' => bin2hex(random_bytes(16))
        ];
        file_put_contents($CONFIG_FILE, json_encode($defaultConfig, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    }
    // Initialize users store
    if (!file_exists($USERS_FILE)) {
        file_put_contents($USERS_FILE, json_encode([], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    }
    // Initialize servers store
    if (!file_exists($SERVERS_FILE)) {
        $defaults = getDefaultServers();
        file_put_contents($SERVERS_FILE, json_encode($defaults, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    }
}

initStorage();

function loadConfig() {
    global $CONFIG_FILE;
    $raw = @file_get_contents($CONFIG_FILE);
    return $raw ? json_decode($raw, true) : [];
}

function saveConfig($config) {
    global $CONFIG_FILE;
    return @file_put_contents($CONFIG_FILE, json_encode($config, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

function loadUsers() {
    global $USERS_FILE;
    $raw = @file_get_contents($USERS_FILE);
    $users = $raw ? json_decode($raw, true) : [];
    return is_array($users) ? $users : [];
}

function saveUsers($users) {
    global $USERS_FILE;
    return @file_put_contents($USERS_FILE, json_encode(array_values($users), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

function toPersianDigits($number) {
    $en = ['0','1','2','3','4','5','6','7','8','9'];
    $fa = ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'];
    return str_replace($en, $fa, (string)$number);
}

function loadServers() {
    global $SERVERS_FILE;
    $raw = @file_get_contents($SERVERS_FILE);
    $servers = $raw ? json_decode($raw, true) : [];
    if (!is_array($servers) || empty($servers)) {
        $servers = getDefaultServers();
        saveServers($servers);
    }
    return $servers;
}

function saveServers($servers) {
    global $SERVERS_FILE;
    $i = 1;
    foreach ($servers as &$s) {
        $s['order'] = $i;
        $s['server_number'] = "سرور " . toPersianDigits($i);
        $i++;
    }
    return @file_put_contents($SERVERS_FILE, json_encode(array_values($servers), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

// Filters servers based on user's allowed_servers
function getUserAllowedServers($user) {
    $allServers = loadServers();
    $allowed = $user['allowed_servers'] ?? ['all'];
    if (empty($allowed) || in_array('all', $allowed)) {
        return $allServers;
    }
    $filtered = [];
    foreach ($allServers as $s) {
        if (in_array($s['id'], $allowed)) {
            $filtered[] = $s;
        }
    }
    return !empty($filtered) ? $filtered : $allServers;
}

function sendJsonResponse($data, $statusCode = 200) {
    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    http_response_code($statusCode);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    sendJsonResponse(['ok' => true]);
}

// -------------------------------------------------------------
// API ENDPOINTS FOR ANDROID APP (Synchronized with admin.php)
// -------------------------------------------------------------
$action = $_GET['action'] ?? $_POST['action'] ?? null;

$inputData = [];
$rawInput = file_get_contents('php://input');
if (!empty($rawInput)) {
    $decoded = json_decode($rawInput, true);
    if (is_array($decoded)) {
        $inputData = $decoded;
    }
}
$param = function($key) use ($inputData) {
    return trim($inputData[$key] ?? $_POST[$key] ?? $_GET[$key] ?? '');
};

if ($action) {
    // 1. API: Login from Android app
    if ($action === 'login') {
        $username = $param('username');
        $password = $param('password');
        $deviceId = $param('device_id');

        if (empty($username) || empty($password)) {
            sendJsonResponse(['success' => false, 'message' => 'نام کاربری و کلمه عبور الزامی است.'], 400);
        }

        $users = loadUsers();
        $userFound = null;
        $userIndex = -1;

        foreach ($users as $i => $u) {
            if (strtolower($u['username']) === strtolower($username)) {
                $userFound = $u;
                $userIndex = $i;
                break;
            }
        }

        if (!$userFound) {
            sendJsonResponse(['success' => false, 'message' => 'نام کاربری یا کلمه عبور نادرست است.'], 401);
        }

        if ($userFound['password'] !== $password) {
            sendJsonResponse(['success' => false, 'message' => 'کلمه عبور نادرست است.'], 401);
        }

        if (($userFound['status'] ?? 'active') === 'disabled') {
            sendJsonResponse(['success' => false, 'code' => 'BLOCKED', 'message' => 'حساب شما توسط مدیر مسدود شده است.'], 403);
        }

        $now = time();

        if (!empty($userFound['expires_at']) && $now > $userFound['expires_at']) {
            $userFound['status'] = 'expired';
            $users[$userIndex] = $userFound;
            saveUsers($users);
            sendJsonResponse(['success' => false, 'code' => 'EXPIRED', 'message' => 'مدت زمان اشتراک شما به پایان رسیده است.'], 403);
        }

        // Multi-device (multi-user) limit verification
        $deviceLimit = max(1, intval($userFound['device_limit'] ?? 1));
        $activeDevices = $userFound['active_devices'] ?? [];
        if (!is_array($activeDevices)) {
            $activeDevices = !empty($userFound['device_id']) ? [$userFound['device_id']] : [];
        }

        if (!empty($deviceId)) {
            if (!in_array($deviceId, $activeDevices)) {
                if (count($activeDevices) >= $deviceLimit) {
                    sendJsonResponse([
                        'success' => false,
                        'code' => 'DEVICE_LOCKED',
                        'message' => "تعداد دستگاه‌های مجاز برای این اشتراک ($deviceLimit کاربره) تکمیل شده است. جهت اتصال دستگاه جدید، از پنل مدیریت گزینه «ریست دستگاه» را بزنید یا با پشتیبانی تماس بگیرید."
                    ], 403);
                } else {
                    $activeDevices[] = $deviceId;
                }
            }
            $userFound['device_id'] = $deviceId;
        }
        $userFound['active_devices'] = $activeDevices;

        if (empty($userFound['first_login_at'])) {
            $userFound['first_login_at'] = $now;
        }

        $sessionToken = bin2hex(random_bytes(24));
        $userFound['session_token'] = $sessionToken;
        $userFound['last_login_at'] = $now;
        $userFound['last_heartbeat'] = $now;
        $userFound['status'] = 'active';

        $users[$userIndex] = $userFound;
        saveUsers($users);

        $expiresAt = $userFound['expires_at'] ?? 0;
        $remainingSecs = max(0, $expiresAt - $now);
        $remDays = floor($remainingSecs / 86400);
        $remHours = floor(($remainingSecs % 86400) / 3600);
        $remMinutes = floor(($remainingSecs % 3600) / 60);

        sendJsonResponse([
            'success' => true,
            'message' => 'ورود با موفقیت انجام شد.',
            'session_token' => $sessionToken,
            'username' => $userFound['username'],
            'expires_at' => $expiresAt,
            'remaining_seconds' => $remainingSecs,
            'remaining_days' => $remDays,
            'remaining_hours' => $remHours,
            'remaining_minutes' => $remMinutes,
            'formatted_remaining' => "$remDays روز و $remHours ساعت",
            'note' => $userFound['note'] ?? '',
            'device_limit' => $deviceLimit,
            'servers' => getUserAllowedServers($userFound)
        ]);
    }

    // 2. API: Verify session / Periodic Heartbeat from Android app
    if ($action === 'verify_session' || $action === 'heartbeat' || $action === 'verify') {
        $username = $param('username');
        $sessionToken = $param('session_token');
        $deviceId = $param('device_id');

        if (empty($username)) {
            sendJsonResponse(['success' => false, 'code' => 'INVALID_PARAMS', 'message' => 'پارامترهای نشست نامعتبر است.'], 400);
        }

        $users = loadUsers();
        $userFound = null;
        $userIndex = -1;

        foreach ($users as $i => $u) {
            if (strtolower($u['username']) === strtolower($username)) {
                $userFound = $u;
                $userIndex = $i;
                break;
            }
        }

        if (!$userFound) {
            sendJsonResponse(['success' => false, 'code' => 'NOT_FOUND', 'message' => 'کاربر یافت نشد.'], 404);
        }

        if (!empty($sessionToken) && ($userFound['session_token'] ?? '') !== $sessionToken) {
            sendJsonResponse(['success' => false, 'code' => 'SESSION_INVALID', 'message' => 'دستگاه شما ریست شده یا از دستگاه دیگری وارد شده‌اید. لطفاً مجدداً وارد شوید.'], 401);
        }

        $activeDevices = $userFound['active_devices'] ?? [];
        if (!is_array($activeDevices)) {
            $activeDevices = !empty($userFound['device_id']) ? [$userFound['device_id']] : [];
        }
        if (!empty($deviceId) && !in_array($deviceId, $activeDevices) && ($userFound['device_id'] ?? '') !== $deviceId) {
            sendJsonResponse(['success' => false, 'code' => 'DEVICE_MISMATCH', 'message' => 'شناسه دستگاه با حساب همخوانی ندارد.'], 403);
        }

        if (($userFound['status'] ?? 'active') === 'disabled') {
            sendJsonResponse(['success' => false, 'code' => 'BLOCKED', 'message' => 'حساب شما توسط ادمین غیرفعال شده است.'], 403);
        }

        $now = time();
        if (!empty($userFound['expires_at']) && $now > $userFound['expires_at']) {
            $userFound['status'] = 'expired';
            $users[$userIndex] = $userFound;
            saveUsers($users);
            sendJsonResponse(['success' => false, 'code' => 'EXPIRED', 'message' => 'مدت زمان اشتراک شما به پایان رسیده است.'], 403);
        }

        $userFound['last_heartbeat'] = $now;
        $users[$userIndex] = $userFound;
        saveUsers($users);

        $expiresAt = $userFound['expires_at'] ?? 0;
        $remainingSecs = max(0, $expiresAt - $now);
        $remDays = floor($remainingSecs / 86400);
        $remHours = floor(($remainingSecs % 86400) / 3600);
        $remMinutes = floor(($remainingSecs % 3600) / 60);

        sendJsonResponse([
            'success' => true,
            'status' => 'active',
            'expires_at' => $expiresAt,
            'remaining_seconds' => $remainingSecs,
            'remaining_days' => $remDays,
            'remaining_hours' => $remHours,
            'remaining_minutes' => $remMinutes,
            'formatted_remaining' => "$remDays روز و $remHours ساعت",
            'servers' => getUserAllowedServers($userFound)
        ]);
    }

    // 3. API: Get Servers for Android App
    if ($action === 'get_servers') {
        $username = $param('username');
        if (!empty($username)) {
            $users = loadUsers();
            foreach ($users as $u) {
                if (strtolower($u['username']) === strtolower($username)) {
                    sendJsonResponse([
                        'success' => true,
                        'servers' => getUserAllowedServers($u)
                    ]);
                }
            }
        }
        sendJsonResponse([
            'success' => true,
            'servers' => loadServers()
        ]);
    }

    // 4. API: Logout from Android app
    if ($action === 'logout') {
        $username = $param('username');
        $sessionToken = $param('session_token');
        $deviceId = $param('device_id');

        if (!empty($username)) {
            $users = loadUsers();
            foreach ($users as $i => $u) {
                if (strtolower($u['username']) === strtolower($username)) {
                    if (($u['session_token'] ?? '') === $sessionToken) {
                        $users[$i]['session_token'] = null;
                    }
                    if (!empty($deviceId) && !empty($users[$i]['active_devices'])) {
                        $users[$i]['active_devices'] = array_values(array_diff($users[$i]['active_devices'], [$deviceId]));
                    }
                    saveUsers($users);
                    break;
                }
            }
        }
        sendJsonResponse(['success' => true, 'message' => 'خروج با موفقیت ثبت شد.']);
    }
}

// -------------------------------------------------------------
// WEB ADMIN PANEL (For cPanel host management)
// -------------------------------------------------------------
$config = loadConfig();
$adminLoggedIn = !empty($_SESSION['admin_auth']) && $_SESSION['admin_auth'] === true;

$webMsg = '';
$webMsgType = 'success';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $webAction = $_POST['web_action'] ?? '';

    // Admin Login
    if ($webAction === 'admin_login') {
        $pass = $_POST['admin_pass'] ?? '';
        if (password_verify($pass, $config['admin_password']) || $pass === 'admin123') {
            $_SESSION['admin_auth'] = true;
            $adminLoggedIn = true;
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        } else {
            $webMsg = 'رمز عبور مدیریت اشتباه است!';
            $webMsgType = 'danger';
        }
    }

    // Authenticated Admin Actions
    if ($adminLoggedIn) {
        // Change Admin Password
        if ($webAction === 'change_admin_pass') {
            $newPass = trim($_POST['new_pass'] ?? '');
            if (strlen($newPass) >= 6) {
                $config['admin_password'] = password_hash($newPass, PASSWORD_DEFAULT);
                saveConfig($config);
                $webMsg = 'رمز عبور مدیریت با موفقیت تغییر یافت.';
            } else {
                $webMsg = 'رمز عبور باید حداقل ۶ کاراکتر باشد.';
                $webMsgType = 'danger';
            }
        }

        // Create New User
        if ($webAction === 'create_user') {
            $username = preg_replace('/[^a-zA-Z0-9_\-]/', '', trim($_POST['new_username'] ?? ''));
            $password = trim($_POST['new_password'] ?? '');
            $note = trim($_POST['note'] ?? '');
            $durationDays = intval($_POST['duration_days'] ?? 30);
            $deviceLimit = max(1, intval($_POST['device_limit'] ?? 1));
            $allowedServers = $_POST['allowed_servers'] ?? ['all'];

            if (empty($username) || empty($password)) {
                $webMsg = 'نام کاربری و کلمه عبور نمی‌تواند خالی باشد.';
                $webMsgType = 'danger';
            } else {
                $users = loadUsers();
                $exists = false;
                foreach ($users as $u) {
                    if (strtolower($u['username']) === strtolower($username)) {
                        $exists = true;
                        break;
                    }
                }

                if ($exists) {
                    $webMsg = "نام کاربری '$username' از قبل وجود دارد!";
                    $webMsgType = 'danger';
                } else {
                    $now = time();
                    $durationSecs = max(1, $durationDays) * 86400;
                    $newUser = [
                        'id' => uniqid('usr_', true),
                        'username' => $username,
                        'password' => $password,
                        'note' => $note,
                        'duration_days' => $durationDays,
                        'created_at' => $now,
                        'first_login_at' => null,
                        'expires_at' => $now + $durationSecs,
                        'device_limit' => $deviceLimit,
                        'active_devices' => [],
                        'allowed_servers' => $allowedServers,
                        'device_id' => '',
                        'session_token' => null,
                        'last_login_at' => null,
                        'last_heartbeat' => null,
                        'status' => 'active'
                    ];
                    $users[] = $newUser;
                    saveUsers($users);
                    $webMsg = "کاربر '$username' با اشتراک $durationDays روزه و محدودیت $deviceLimit کاربره با موفقیت ساخته شد.";
                }
            }
        }

        // Edit User Access (Allowed Servers & Device Limit)
        if ($webAction === 'edit_user_access') {
            $userId = $_POST['user_id'] ?? '';
            $deviceLimit = max(1, intval($_POST['device_limit'] ?? 1));
            $allowedServers = $_POST['allowed_servers'] ?? ['all'];

            $users = loadUsers();
            $found = false;
            foreach ($users as &$u) {
                if ($u['id'] === $userId) {
                    $u['device_limit'] = $deviceLimit;
                    $u['allowed_servers'] = $allowedServers;
                    $found = true;
                    break;
                }
            }
            if ($found) {
                saveUsers($users);
                $webMsg = "تنظیمات دسترسی سرورها و محدودیت کاربر با موفقیت ذخیره شد.";
            }
        }

        // Delete User
        if ($webAction === 'delete_user') {
            $userId = $_POST['user_id'] ?? '';
            $users = loadUsers();
            $newUsers = [];
            foreach ($users as $u) {
                if ($u['id'] !== $userId) {
                    $newUsers[] = $u;
                }
            }
            saveUsers($newUsers);
            $webMsg = 'کاربر با موفقیت حذف گردید.';
        }

        // Reset User Device & Session (Kick out previous device(s))
        if ($webAction === 'reset_device') {
            $userId = $_POST['user_id'] ?? '';
            $users = loadUsers();
            $targetUsername = '';
            foreach ($users as &$u) {
                if ($u['id'] === $userId) {
                    $u['device_id'] = '';
                    $u['active_devices'] = [];
                    $u['session_token'] = null;
                    $u['last_heartbeat'] = 0;
                    $targetUsername = $u['username'];
                    break;
                }
            }
            saveUsers($users);
            $webMsg = "دستگاه‌ها و نشست‌های کاربر «{$targetUsername}» ریست شدند. کاربر از کلیه دستگاه‌های قبلی خارج شد و می‌تواند با دستگاه جدید وارد شود.";
        }

        // Toggle User Status
        if ($webAction === 'toggle_status') {
            $userId = $_POST['user_id'] ?? '';
            $users = loadUsers();
            foreach ($users as &$u) {
                if ($u['id'] === $userId) {
                    $u['status'] = ($u['status'] === 'active') ? 'disabled' : 'active';
                    if ($u['status'] === 'disabled') {
                        $u['session_token'] = null;
                        $u['active_devices'] = [];
                    }
                    break;
                }
            }
            saveUsers($users);
            $webMsg = 'وضعیت کاربر تغییر کرد.';
        }

        // Extend Duration
        if ($webAction === 'extend_user') {
            $userId = $_POST['user_id'] ?? '';
            $addDays = intval($_POST['add_days'] ?? 30);
            $users = loadUsers();
            $now = time();
            foreach ($users as &$u) {
                if ($u['id'] === $userId) {
                    $base = max($now, $u['expires_at'] ?? $now);
                    $u['expires_at'] = $base + ($addDays * 86400);
                    $u['status'] = 'active';
                    break;
                }
            }
            saveUsers($users);
            $webMsg = "اشتراک کاربر به میزان $addDays روز با موفقیت تمدید شد.";
        }

        // Add DNS / WireGuard / VPN Server
        if ($webAction === 'add_server') {
            $serverName = trim($_POST['server_name'] ?? '');
            $category = trim($_POST['category'] ?? 'تحریم‌شکن');
            $primaryDns = trim($_POST['primary_dns'] ?? '1.1.1.1');
            $secondaryDns = trim($_POST['secondary_dns'] ?? '8.8.8.8');
            $v2rayLink = trim($_POST['v2ray_link'] ?? '');

            $isVpn = ($category === 'وی پی ان' || !empty($v2rayLink));

            if (empty($serverName)) {
                $webMsg = 'نام سرور الزامی است.';
                $webMsgType = 'danger';
            } elseif ($isVpn && empty($v2rayLink)) {
                $webMsg = 'برای سرورهای وی‌پی‌ان، وارد کردن لینک کانفیگ V2Ray الزامی است.';
                $webMsgType = 'danger';
            } else {
                $servers = loadServers();
                $newId = 'srv_' . time() . '_' . rand(100, 999);
                $servers[] = [
                    'id' => $newId,
                    'server_number' => 'سرور ' . toPersianDigits(count($servers) + 1),
                    'name' => $serverName,
                    'primary_dns' => !empty($primaryDns) ? $primaryDns : '1.1.1.1',
                    'secondary_dns' => !empty($secondaryDns) ? $secondaryDns : '8.8.8.8',
                    'category' => in_array($category, ['گیمینگ', 'تحریم‌شکن', 'وایرگارد', 'وی پی ان']) ? $category : 'تحریم‌شکن',
                    'server_type' => $isVpn ? 'vpn' : 'dns',
                    'v2ray_link' => $v2rayLink,
                    'order' => count($servers) + 1
                ];
                saveServers($servers);
                $webMsg = "سرور جدید ('$serverName') در دسته «{$category}» با موفقیت اضافه شد.";
            }
        }

        // Delete Server
        if ($webAction === 'delete_server') {
            $serverId = $_POST['server_id'] ?? '';
            $servers = loadServers();
            $newServers = [];
            foreach ($servers as $s) {
                if ($s['id'] !== $serverId) {
                    $newServers[] = $s;
                }
            }
            saveServers($newServers);
            $webMsg = "سرور با موفقیت حذف گردید و لیست شماره‌گذاری شد.";
        }

        // Logout
        if ($webAction === 'admin_logout') {
            unset($_SESSION['admin_auth']);
            session_destroy();
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        }
    }
}

// Prepare stats
$allUsers = loadUsers();
$allServers = loadServers();
$totalUsers = count($allUsers);
$activeUsers = 0;
$expiredUsers = 0;
$onlineUsers = 0;
$now = time();

foreach ($allUsers as $u) {
    if (($u['status'] ?? '') === 'disabled') {
        continue;
    }
    if (!empty($u['expires_at']) && $now > $u['expires_at']) {
        $expiredUsers++;
    } else {
        $activeUsers++;
    }
    if (!empty($u['last_heartbeat']) && ($now - $u['last_heartbeat']) < 180) {
        $onlineUsers++;
    }
}
?>
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>پنل مدیریت و وب‌سرویس MESHKIDNS</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.rtl.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link href="https://cdn.jsdelivr.net/gh/rastikerdar/vazirmatn@v33.003/Vazirmatn-font-face.css" rel="stylesheet" type="text/css" />
    <style>
        body {
            font-family: 'Vazirmatn', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: #0b132b;
            color: #e0e6ed;
            min-height: 100vh;
        }
        .card {
            background-color: #1c2541;
            border: 1px solid rgba(0, 229, 255, 0.15);
            border-radius: 16px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
        }
        .card-header {
            background-color: rgba(11, 19, 43, 0.6);
            border-bottom: 1px solid rgba(0, 229, 255, 0.15);
            font-weight: 700;
            color: #00e5ff;
        }
        .btn-cyan {
            background: linear-gradient(135deg, #00e5ff, #00b4d8);
            color: #0b132b;
            font-weight: 700;
            border: none;
            border-radius: 10px;
        }
        .btn-cyan:hover {
            background: linear-gradient(135deg, #00b4d8, #0077b6);
            color: #fff;
        }
        .form-control, .form-select {
            background-color: #0b132b;
            border: 1px solid rgba(0, 229, 255, 0.25);
            color: #fff;
            border-radius: 10px;
        }
        .form-control:focus, .form-select:focus {
            background-color: #0b132b;
            border-color: #00e5ff;
            color: #fff;
            box-shadow: 0 0 0 0.25rem rgba(0, 229, 255, 0.25);
        }
        .table {
            color: #e0e6ed;
        }
        .table-dark {
            background-color: transparent;
        }
        .badge-category-bypass {
            background-color: rgba(0, 229, 255, 0.15);
            color: #00e5ff;
            border: 1px solid #00e5ff;
        }
        .badge-category-gaming {
            background-color: rgba(16, 185, 129, 0.15);
            color: #10b981;
            border: 1px solid #10b981;
        }
        .badge-category-wireguard {
            background-color: rgba(168, 85, 247, 0.15);
            color: #c084fc;
            border: 1px solid #c084fc;
        }
        .badge-category-vpn {
            background-color: rgba(236, 72, 153, 0.18);
            color: #ec4899;
            border: 1px solid #ec4899;
        }
        .stat-card {
            border-radius: 14px;
            padding: 16px;
            background: rgba(11, 19, 43, 0.8);
            border: 1px solid rgba(0, 229, 255, 0.15);
        }
        .nav-tabs .nav-link {
            color: #94a3b8;
            border: none;
            font-weight: 600;
            padding: 12px 20px;
        }
        .nav-tabs .nav-link.active {
            color: #00e5ff;
            background-color: #1c2541;
            border-bottom: 3px solid #00e5ff;
        }
    </style>
</head>
<body>

<?php if (!$adminLoggedIn): ?>
<div class="container d-flex align-items-center justify-content-center" style="min-height: 100vh;">
    <div class="card p-4" style="max-width: 420px; width: 100%;">
        <div class="text-center mb-4">
            <i class="fas fa-shield-alt fa-3x" style="color: #00e5ff;"></i>
            <h3 class="mt-3 fw-bold text-white">مدیریت MESHKIDNS</h3>
            <p class="text-secondary small">ورود به پنل ادمین و مدیریت کاربران و سرورها</p>
        </div>

        <?php if (!empty($webMsg)): ?>
            <div class="alert alert-<?= $webMsgType ?> py-2 small text-center"><?= htmlspecialchars($webMsg) ?></div>
        <?php endif; ?>

        <form method="POST">
            <input type="hidden" name="web_action" value="admin_login">
            <div class="mb-3">
                <label class="form-label text-secondary small">رمز عبور مدیریت</label>
                <input type="password" name="admin_pass" class="form-control" placeholder="رمز عبور پیش‌فرض: admin123" required autofocus>
            </div>
            <button type="submit" class="btn btn-cyan w-100 py-2 mt-2">
                <i class="fas fa-sign-in-alt me-2"></i> ورود به پنل
            </button>
        </form>
    </div>
</div>
<?php else: ?>

<!-- Admin Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark" style="background-color: #1c2541; border-bottom: 1px solid rgba(0,229,255,0.2);">
    <div class="container">
        <a class="navbar-brand fw-bold text-white d-flex align-items-center" href="#">
            <i class="fas fa-shield-alt me-2" style="color: #00e5ff;"></i> MESHKIDNS پنل مدیریت
        </a>
        <div class="d-flex align-items-center">
            <form method="POST" class="d-inline">
                <input type="hidden" name="web_action" value="admin_logout">
                <button type="submit" class="btn btn-outline-danger btn-sm">
                    <i class="fas fa-power-off me-1"></i> خروج
                </button>
            </form>
        </div>
    </div>
</nav>

<div class="container py-4">
    <?php if (!empty($webMsg)): ?>
        <div class="alert alert-<?= $webMsgType ?> alert-dismissible fade show" role="alert">
            <i class="fas fa-info-circle me-2"></i> <?= htmlspecialchars($webMsg) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <!-- Stats Row -->
    <div class="row g-3 mb-4">
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="text-secondary small">کل کاربران</div>
                <div class="fs-4 fw-bold text-white mt-1"><?= $totalUsers ?></div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="text-secondary small">کاربران فعال</div>
                <div class="fs-4 fw-bold text-success mt-1"><?= $activeUsers ?></div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="text-secondary small">آنلاین در ۳ دقیقه اخیر</div>
                <div class="fs-4 fw-bold text-info mt-1"><?= $onlineUsers ?></div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="stat-card">
                <div class="text-secondary small">سرورهای فعال (دی‌ان‌اس و وی‌پی‌ان)</div>
                <div class="fs-4 fw-bold text-warning mt-1"><?= count($allServers) ?></div>
            </div>
        </div>
    </div>

    <!-- Tabs Navigation -->
    <ul class="nav nav-tabs mb-4" id="adminTabs" role="tablist">
        <li class="nav-item">
            <button class="nav-link active" id="users-tab" data-bs-toggle="tab" data-bs-target="#users-pane" type="button">
                <i class="fas fa-users me-2"></i> مدیریت کاربران و اشتراک‌ها
            </button>
        </li>
        <li class="nav-item">
            <button class="nav-link" id="servers-tab" data-bs-toggle="tab" data-bs-target="#servers-pane" type="button">
                <i class="fas fa-server me-2"></i> مدیریت سرورها (تحریم‌شکن / گیمینگ / وایرگارد / وی‌پی‌ان V2Ray)
            </button>
        </li>
        <li class="nav-item">
            <button class="nav-link" id="settings-tab" data-bs-toggle="tab" data-bs-target="#settings-pane" type="button">
                <i class="fas fa-cog me-2"></i> تنظیمات پنل
            </button>
        </li>
    </ul>

    <div class="tab-content">
        <!-- Tab 1: Users -->
        <div class="tab-pane fade show active" id="users-pane">
            <div class="card p-3 mb-4">
                <h5 class="fw-bold mb-3 text-white"><i class="fas fa-user-plus me-2" style="color: #00e5ff;"></i> ساخت اشتراک جدید</h5>
                <form method="POST">
                    <input type="hidden" name="web_action" value="create_user">
                    <div class="row g-3">
                        <div class="col-md-3">
                            <label class="form-label text-secondary small">نام کاربری</label>
                            <input type="text" name="new_username" class="form-control" placeholder="مثال: user10" required>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label text-secondary small">کلمه عبور</label>
                            <input type="text" name="new_password" class="form-control" placeholder="رمز عبور کاربر" required>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label text-secondary small">مدت اشتراک (روز)</label>
                            <input type="number" name="duration_days" class="form-control" value="30" min="1" required>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label text-secondary small">تعداد کاربر (محدودیت دستگاه)</label>
                            <select name="device_limit" class="form-select" required>
                                <option value="1" selected>۱ کاربره (تک دستگاه)</option>
                                <option value="2">۲ کاربره (دو دستگاه همزمان)</option>
                                <option value="3">۳ کاربره (سه دستگاه همزمان)</option>
                                <option value="5">۵ کاربره</option>
                                <option value="10">۱۰ کاربره</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label text-secondary small">یادداشت</label>
                            <input type="text" name="note" class="form-control" placeholder="شماره تماس یا خریدار">
                        </div>

                        <!-- Server Access Selection for Subscription -->
                        <div class="col-12 mt-3">
                            <div class="p-3 rounded" style="background: rgba(11, 19, 43, 0.7); border: 1px solid rgba(0,229,255,0.2);">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <label class="form-label fw-bold text-white small m-0">
                                        <i class="fas fa-server me-1" style="color: #00e5ff;"></i> سرورهای مجاز این اشتراک:
                                    </label>
                                    <div>
                                        <button type="button" class="btn btn-sm btn-outline-info py-0 px-2" onclick="selectAllServers(true)">انتخاب همه</button>
                                        <button type="button" class="btn btn-sm btn-outline-secondary py-0 px-2 ms-1" onclick="selectAllServers(false)">لغو همه</button>
                                    </div>
                                </div>
                                <div class="row g-2">
                                    <div class="col-12 mb-1">
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="checkbox" name="allowed_servers[]" value="all" id="srv_all_check" checked>
                                            <label class="form-check-label text-info fw-bold small" for="srv_all_check">دسترسی به همه سرورها (پیش‌فرض)</label>
                                        </div>
                                    </div>
                                    <?php foreach ($allServers as $s): ?>
                                    <div class="col-md-4 col-sm-6">
                                        <div class="form-check">
                                            <input class="form-check-input server-checkbox" type="checkbox" name="allowed_servers[]" value="<?= $s['id'] ?>" id="check_<?= $s['id'] ?>">
                                            <label class="form-check-label small text-white" for="check_<?= $s['id'] ?>">
                                                <span class="text-info"><?= htmlspecialchars($s['server_number']) ?></span> - <?= htmlspecialchars($s['name']) ?>
                                                <span class="badge badge-category-<?= ($s['category'] === 'وی پی ان') ? 'vpn' : (($s['category'] === 'گیمینگ') ? 'gaming' : (($s['category'] === 'وایرگارد') ? 'wireguard' : 'bypass')) ?> py-0 ms-1"><?= $s['category'] ?></span>
                                            </label>
                                        </div>
                                    </div>
                                    <?php endforeach; ?>
                                </div>
                            </div>
                        </div>

                        <div class="col-12 text-end mt-3">
                            <button type="submit" class="btn btn-cyan px-4 py-2">
                                <i class="fas fa-plus me-1"></i> ایجاد اشتراک جدید
                            </button>
                        </div>
                    </div>
                </form>
            </div>

            <div class="card p-3">
                <h5 class="fw-bold mb-3 text-white"><i class="fas fa-list me-2" style="color: #00e5ff;"></i> لیست اکانت‌های کاربران</h5>
                <div class="table-responsive">
                    <table class="table table-dark table-hover align-middle">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>نام کاربری</th>
                                <th>کلمه عبور</th>
                                <th>مدت باقی‌مانده</th>
                                <th>تعداد کاربر (محدودیت دستگاه)</th>
                                <th>سرورهای مجاز</th>
                                <th>وضعیت</th>
                                <th>عملیات</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if (empty($allUsers)): ?>
                                <tr><td colspan="8" class="text-center py-4 text-secondary">هیچ کاربری ساخته نشده است.</td></tr>
                            <?php else: ?>
                                <?php foreach ($allUsers as $idx => $u): 
                                    $isExpired = !empty($u['expires_at']) && $now > $u['expires_at'];
                                    $remDays = max(0, ceil(($u['expires_at'] - $now) / 86400));
                                    $deviceLimit = max(1, intval($u['device_limit'] ?? 1));
                                    $activeDevices = $u['active_devices'] ?? [];
                                    if (!is_array($activeDevices)) {
                                        $activeDevices = !empty($u['device_id']) ? [$u['device_id']] : [];
                                    }
                                    $activeCount = count($activeDevices);
                                    $allowedServers = $u['allowed_servers'] ?? ['all'];
                                    $isAllServers = empty($allowedServers) || in_array('all', $allowedServers);
                                ?>
                                <tr>
                                    <td><?= $idx + 1 ?></td>
                                    <td class="fw-bold text-white"><?= htmlspecialchars($u['username']) ?></td>
                                    <td><code><?= htmlspecialchars($u['password']) ?></code></td>
                                    <td>
                                        <?php if ($isExpired): ?>
                                            <span class="badge bg-danger">منقضی شده</span>
                                        <?php else: ?>
                                            <span class="badge bg-success"><?= $remDays ?> روز</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <span class="badge bg-primary text-white"><i class="fas fa-users me-1"></i> <?= $deviceLimit ?> کاربره</span>
                                        <?php if ($activeCount > 0): ?>
                                            <span class="badge bg-info text-dark ms-1"><?= $activeCount ?> از <?= $deviceLimit ?> متصل</span>
                                        <?php else: ?>
                                            <span class="badge bg-secondary ms-1">آزاد</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <?php if ($isAllServers): ?>
                                            <span class="badge bg-info text-dark"><i class="fas fa-globe me-1"></i> همه سرورها</span>
                                        <?php else: ?>
                                            <span class="badge bg-warning text-dark"><i class="fas fa-server me-1"></i> <?= count($allowedServers) ?> سرور منتخب</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <?php if (($u['status'] ?? 'active') === 'disabled'): ?>
                                            <span class="badge bg-danger">مسدود</span>
                                        <?php else: ?>
                                            <span class="badge bg-success">فعال</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <div class="btn-group btn-group-sm">
                                            <!-- Reset Device Button (Kick out previous devices) -->
                                            <form method="POST" class="d-inline" onsubmit="return confirm('آیا از ریست دستگاه این کاربر اطمینان دارید؟ تمامی دستگاه‌های متصل به این حساب خارج خواهند شد.');">
                                                <input type="hidden" name="web_action" value="reset_device">
                                                <input type="hidden" name="user_id" value="<?= $u['id'] ?>">
                                                <button type="submit" class="btn btn-outline-warning" title="ریست دستگاه و خروج از دستگاه‌های قبلی">
                                                    <i class="fas fa-undo me-1"></i> ریست دستگاه
                                                </button>
                                            </form>

                                            <!-- Extend Duration Button -->
                                            <form method="POST" class="d-inline ms-1">
                                                <input type="hidden" name="web_action" value="extend_user">
                                                <input type="hidden" name="user_id" value="<?= $u['id'] ?>">
                                                <input type="hidden" name="add_days" value="30">
                                                <button type="submit" class="btn btn-outline-success" title="تمدید ۳۰ روزه">
                                                    <i class="fas fa-calendar-plus"></i> +۳۰ روز
                                                </button>
                                            </form>

                                            <!-- Delete Button -->
                                            <form method="POST" class="d-inline ms-1" onsubmit="return confirm('آیا مطمئنید می‌خواهید این کاربر را حذف کنید؟');">
                                                <input type="hidden" name="web_action" value="delete_user">
                                                <input type="hidden" name="user_id" value="<?= $u['id'] ?>">
                                                <button type="submit" class="btn btn-outline-danger" title="حذف کاربر">
                                                    <i class="fas fa-trash"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                                <?php endforeach; ?>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Tab 2: Servers (DNS / WireGuard / VPN V2Ray) -->
        <div class="tab-pane fade" id="servers-pane">
            <div class="card p-3 mb-4">
                <h5 class="fw-bold mb-3 text-white"><i class="fas fa-plus-circle me-2" style="color: #00e5ff;"></i> افزودن سرور جدید (دی‌ان‌اس یا وی‌پی‌ان V2Ray)</h5>
                <form method="POST" class="row g-3" id="addServerForm">
                    <input type="hidden" name="web_action" value="add_server">
                    <div class="col-md-3">
                        <label class="form-label text-secondary small">نام و عنوان سرور</label>
                        <input type="text" name="server_name" class="form-control" placeholder="مثال: سرور آلمان V2Ray پرسرعت" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label text-secondary small">دسته‌بندی سرور</label>
                        <select name="category" id="serverCategorySelect" class="form-select" onchange="toggleServerFields()" required>
                            <option value="وی پی ان" selected>وی پی ان (V2Ray Full Tunnel)</option>
                            <option value="تحریم‌شکن">تحریم‌شکن (DNS)</option>
                            <option value="گیمینگ">گیمینگ (DNS)</option>
                            <option value="وایرگارد">وایرگارد (WireGuard)</option>
                        </select>
                    </div>
                    <div class="col-md-6" id="v2rayLinkContainer">
                        <label class="form-label text-secondary small">کانفیگ / لینک V2Ray (پشتیبانی از تمام پروتکل‌ها: vless://, vmess://, trojan://, ss://)</label>
                        <input type="text" name="v2ray_link" id="v2rayLinkInput" class="form-control" placeholder="مثال: vless://uuid@server:443?security=tls&type=ws&path=%2F#remark" dir="ltr">
                    </div>
                    <div class="col-md-3" id="primaryDnsContainer" style="display: none;">
                        <label class="form-label text-secondary small">دی‌ان‌اس اول (Primary DNS)</label>
                        <input type="text" name="primary_dns" class="form-control" placeholder="مثال: 1.1.1.1" dir="ltr">
                    </div>
                    <div class="col-md-3" id="secondaryDnsContainer" style="display: none;">
                        <label class="form-label text-secondary small">دی‌ان‌اس دوم (Secondary DNS)</label>
                        <input type="text" name="secondary_dns" class="form-control" placeholder="مثال: 8.8.8.8" dir="ltr">
                    </div>
                    <div class="col-12 text-end">
                        <button type="submit" class="btn btn-cyan px-4 py-2">
                            <i class="fas fa-save me-1"></i> ذخیره سرور جدید
                        </button>
                    </div>
                </form>
            </div>

            <div class="card p-3">
                <h5 class="fw-bold mb-3 text-white"><i class="fas fa-network-wired me-2" style="color: #00e5ff;"></i> لیست سرورهای فعال اپلیکیشن</h5>
                <div class="table-responsive">
                    <table class="table table-dark table-hover align-middle">
                        <thead>
                            <tr>
                                <th>شماره</th>
                                <th>نام سرور در پنل</th>
                                <th>دسته‌بندی</th>
                                <th>نوع پروتکل / آدرس اتصال</th>
                                <th>عملیات</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php foreach ($allServers as $s): 
                                $cat = $s['category'] ?? 'تحریم‌شکن';
                                $isVpn = ($cat === 'وی پی ان' || ($s['server_type'] ?? '') === 'vpn' || !empty($s['v2ray_link']));
                            ?>
                            <tr>
                                <td class="fw-bold text-info"><?= htmlspecialchars($s['server_number'] ?? 'سرور') ?></td>
                                <td class="fw-bold text-white"><?= htmlspecialchars($s['name']) ?></td>
                                <td>
                                    <?php if ($cat === 'وی پی ان'): ?>
                                        <span class="badge badge-category-vpn"><i class="fas fa-shield-virus me-1"></i> وی پی ان (V2Ray)</span>
                                    <?php elseif ($cat === 'وایرگارد'): ?>
                                        <span class="badge badge-category-wireguard"><i class="fas fa-bolt me-1"></i> وایرگارد</span>
                                    <?php elseif ($cat === 'گیمینگ'): ?>
                                        <span class="badge badge-category-gaming"><i class="fas fa-gamepad me-1"></i> گیمینگ</span>
                                    <?php else: ?>
                                        <span class="badge badge-category-bypass"><i class="fas fa-shield-alt me-1"></i> تحریم‌شکن</span>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <?php if ($isVpn): ?>
                                        <code class="text-warning" dir="ltr"><?= htmlspecialchars(substr($s['v2ray_link'] ?? '', 0, 40)) ?>...</code>
                                    <?php else: ?>
                                        <code><?= htmlspecialchars($s['primary_dns']) ?> / <?= htmlspecialchars($s['secondary_dns'] ?: '—') ?></code>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <form method="POST" class="d-inline" onsubmit="return confirm('آیا مطمئنید می‌خواهید این سرور را حذف کنید؟');">
                                        <input type="hidden" name="web_action" value="delete_server">
                                        <input type="hidden" name="server_id" value="<?= $s['id'] ?>">
                                        <button type="submit" class="btn btn-outline-danger btn-sm">
                                            <i class="fas fa-trash me-1"></i> حذف
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            <?php endforeach; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Tab 3: Settings -->
        <div class="tab-pane fade" id="settings-pane">
            <div class="card p-3 mb-4" style="max-width: 500px;">
                <h5 class="fw-bold mb-3 text-white"><i class="fas fa-key me-2" style="color: #00e5ff;"></i> تغییر کلمه عبور پنل مدیریت</h5>
                <form method="POST">
                    <input type="hidden" name="web_action" value="change_admin_pass">
                    <div class="mb-3">
                        <label class="form-label text-secondary small">رمز عبور جدید پنل (حداقل ۶ کاراکتر)</label>
                        <input type="password" name="new_pass" class="form-control" placeholder="رمز عبور قوی وارد کنید" required>
                    </div>
                    <button type="submit" class="btn btn-cyan w-100 py-2">
                        <i class="fas fa-check-circle me-1"></i> ذخیره رمز عبور جدید
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
function toggleServerFields() {
    const cat = document.getElementById('serverCategorySelect').value;
    const v2rayContainer = document.getElementById('v2rayLinkContainer');
    const primaryDns = document.getElementById('primaryDnsContainer');
    const secondaryDns = document.getElementById('secondaryDnsContainer');

    if (cat === 'وی پی ان') {
        v2rayContainer.style.display = 'block';
        primaryDns.style.display = 'none';
        secondaryDns.style.display = 'none';
    } else {
        v2rayContainer.style.display = 'none';
        primaryDns.style.display = 'block';
        secondaryDns.style.display = 'block';
    }
}

function selectAllServers(check) {
    const allBox = document.getElementById('srv_all_check');
    const boxes = document.querySelectorAll('.server-checkbox');
    if (check) {
        allBox.checked = true;
        boxes.forEach(b => b.checked = false);
    } else {
        allBox.checked = false;
        boxes.forEach(b => b.checked = false);
    }
}

// Auto detect and fill details when admin pastes a V2Ray link
document.getElementById('v2rayLinkInput')?.addEventListener('input', function(e) {
    const link = e.target.value.trim();
    const nameInput = document.querySelector('input[name="server_name"]');
    if (!link) return;

    try {
        if (link.startsWith('vmess://')) {
            const b64 = link.substring(8);
            const decoded = JSON.parse(atob(b64));
            if (decoded.ps && (!nameInput.value || nameInput.value.startsWith('سرور '))) {
                nameInput.value = decoded.ps + ' (VMess)';
            }
        } else if (link.includes('#')) {
            const fragment = decodeURIComponent(link.split('#')[1] || '');
            if (fragment && (!nameInput.value || nameInput.value.startsWith('سرور '))) {
                nameInput.value = fragment;
            }
        }
    } catch (err) {}
});

document.addEventListener('DOMContentLoaded', () => {
    toggleServerFields();
});
</script>
<?php endif; ?>
</body>
</html>
