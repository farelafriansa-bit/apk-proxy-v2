// ========================================
// TEST KONEKSI KE JAVA
// ========================================
function testConnection() {
    try {
        if (typeof Android !== 'undefined' && Android.testConnection) {
            Android.testConnection();
            showToast('✅ Koneksi ke Java BERHASIL!');
        } else {
            showToast('❌ Android tidak ditemukan!');
        }
    } catch(e) {
        showToast('❌ Error: ' + e.message);
    }
}

// ========================================
// TOGGLE MENU
// ========================================
const menuState = {
    body: false, lock: false, speed: false,
    jump: false, bypass: false, refresh: false
};

function getMenuName(menu) {
    const names = {
        body: 'aim body', lock: 'aim lock', speed: 'speed up',
        jump: 'back jump', bypass: 'bypass', refresh: 'auto refres'
    };
    return names[menu] || menu;
}

function toggleMenu(menu, isOn) {
    menuState[menu] = isOn;
    var statusId = 'status' + menu.charAt(0).toUpperCase() + menu.slice(1);
    var statusEl = document.getElementById(statusId);
    if (isOn) {
        statusEl.textContent = 'ON';
        statusEl.className = 'menu-status on';
        showToast('✅ ' + getMenuName(menu) + ' AKTIF');
        Android.executeCommand(menu, 'start');
    } else {
        statusEl.textContent = 'OFF';
        statusEl.className = 'menu-status off';
        showToast('⛔ ' + getMenuName(menu) + ' NONAKTIF');
        Android.executeCommand(menu, 'stop');
    }
}

// ========================================
// TOGGLE BOOSTER
// ========================================
function toggleBooster(type, isOn) {
    var statusId = 'status' + type.charAt(0).toUpperCase() + type.slice(1);
    var statusEl = document.getElementById(statusId);
    if (isOn) {
        statusEl.textContent = 'ON';
        statusEl.className = 'menu-status on';
        Android.executeBooster(type, 'start');
        showToast('⚡ ' + getBoosterName(type) + ' AKTIF');
    } else {
        statusEl.textContent = 'OFF';
        statusEl.className = 'menu-status off';
        Android.executeBooster(type, 'stop');
        showToast('⛔ ' + getBoosterName(type) + ' NONAKTIF');
    }
}

function getBoosterName(type) {
    var names = {
        fps: 'FPS Booster', res: 'Lag Fix', mode: 'Gaming Mode',
        network: 'Network Booster', ram: 'RAM Booster'
    };
    return names[type] || type;
}

// ========================================
// SHIZUKU FUNCTIONS
// ========================================
function openShizuku() {
    showToast('🔓 Membuka Shizuku...');
    try {
        if (typeof Android !== 'undefined') {
            Android.openShizuku();
            setTimeout(function() { updateStatus(); }, 3000);
        } else {
            showToast('❌ Android tidak tersedia!');
        }
    } catch(e) {
        showToast('❌ Error: ' + e.message);
    }
}

// ========================================
// UPDATE FLOATING STATUS
// ========================================
function updateStatus() {
    try {
        var status = JSON.parse(Android.checkDebugStatus());
        var shizukuEl = document.getElementById('floatShizuku');
        var shizukuMenu = document.getElementById('statusShizuku');

        if (shizukuEl) {
            shizukuEl.textContent = status.shizuku ? 'SHIZUKU: ON' : 'SHIZUKU: OFF';
            shizukuEl.className = 'status-pill ' + (status.shizuku ? 'on' : 'off');
        }
        if (shizukuMenu) {
            shizukuMenu.textContent = status.shizuku ? 'ON' : 'OFF';
            shizukuMenu.style.background = status.shizuku ? '#2ecc71' : '#9b59b6';
            shizukuMenu.style.color = '#fff';
        }
    } catch(e) {
        console.log('Gagal update status:', e);
    }
}

// ========================================
// GAME FUNCTIONS
// ========================================
function openAddGameModal() {
    var modal = document.getElementById('addGameModal');
    modal.classList.add('active');
    document.getElementById('gameScanResult').innerHTML = '<p style="text-align:center;color:#999;font-size:13px;">🔍 Scanning game...</p>';
    try {
        var gamesJson = Android.scanAllGames();
        var games = JSON.parse(gamesJson);
        if (games.length === 0) {
            document.getElementById('gameScanResult').innerHTML = '<p style="text-align:center;color:#999;font-size:13px;">📭 Tidak ada game terdeteksi</p>';
            return;
        }
        var html = '';
        for (var i = 0; i < games.length; i++) {
            var game = games[i];
            var isAdded = game.isAdded;
            var btnClass = isAdded ? 'btn-add-game added' : 'btn-add-game';
            var btnText = isAdded ? '✅ Sudah' : '➕ Tambah';
            var btnAction = isAdded ? '' : 'onclick="addGame(\'' + game.packageName + '\')"';
            html += '<div class="game-item"><div class="game-info"><img class="game-icon" src="data:image/png;base64,' + game.iconBase64 + '" alt="icon"><div><div class="game-name">' + game.appName + '</div><div class="game-pkg">' + game.packageName + '</div></div></div><button class="' + btnClass + '" ' + btnAction + '>' + btnText + '</button></div>';
        }
        document.getElementById('gameScanResult').innerHTML = html;
    } catch(e) {
        document.getElementById('gameScanResult').innerHTML = '<p style="text-align:center;color:#e74c3c;font-size:13px;">❌ Gagal scan: ' + e.message + '</p>';
    }
}

function closeAddGameModal() {
    document.getElementById('addGameModal').classList.remove('active');
    refreshUserGames();
}

function addGame(packageName) {
    Android.addGame(packageName);
    showToast('✅ Game berhasil ditambahkan!');
    refreshUserGames();
    openAddGameModal();
}

function removeGameAndRefresh(packageName) {
    Android.removeGame(packageName);
    showToast('🗑️ Game dihapus dari daftar');
    refreshUserGames();
}

function playGame(packageName) {
    Android.openGame(packageName);
    showToast('🎮 Membuka game...');
}

function refreshUserGames() {
    var container = document.getElementById('userGameList');
    var count = document.getElementById('gameCount');
    if (!container) return;
    try {
        var gamesJson = Android.getUserGames();
        var games = JSON.parse(gamesJson);
        if (games.length === 0) {
            container.innerHTML = '<div class="game-empty-state"><span class="empty-icon">🎯</span><p class="empty-title">Belum Ada Game</p><p class="empty-desc">Klik <strong>"Tambah Game"</strong> untuk menambahkan</p></div>';
            if (count) count.textContent = '0';
            return;
        }
        var html = '';
        for (var i = 0; i < games.length; i++) {
            var game = games[i];
            var shortName = game.appName.length > 14 ? game.appName.substring(0,12) + '..' : game.appName;
            html += '<div class="game-card" onclick="playGame(\'' + game.packageName + '\')"><div class="game-icon-wrapper"><img src="data:image/png;base64,' + game.iconBase64 + '" alt="' + game.appName + '"></div><div class="game-name">' + shortName + '</div><div class="game-actions"><button class="btn-play" onclick="event.stopPropagation(); playGame(\'' + game.packageName + '\')">▶ Play</button><button class="btn-remove" onclick="event.stopPropagation(); removeGameAndRefresh(\'' + game.packageName + '\')">✕</button></div><span class="game-badge">⚡</span></div>';
        }
        container.innerHTML = html;
        if (count) count.textContent = games.length;
    } catch(e) {
        container.innerHTML = '<div class="game-empty-state"><span class="empty-icon">⚠️</span><p class="empty-title">Gagal Memuat</p><p class="empty-desc">Terjadi kesalahan saat memuat game</p></div>';
        if (count) count.textContent = '0';
    }
}

// ========================================
// START GAME
// ========================================
function startGame() {
    var activeMenus = [];
    for (var key in menuState) {
        if (menuState[key]) activeMenus.push(getMenuName(key));
    }
    try {
        var gamesJson = Android.getUserGames();
        var games = JSON.parse(gamesJson);
        if (games.length > 0) {
            var firstGame = games[0];
            var msg = '🎮 Membuka ' + firstGame.appName;
            if (activeMenus.length > 0) msg += ' dengan: ' + activeMenus.join(', ');
            showToast(msg);
            Android.openGame(firstGame.packageName);
        } else {
            if (activeMenus.length > 0) {
                showToast('🎮 Membuka Free Fire dengan: ' + activeMenus.join(', '));
            } else {
                showToast('🎮 Membuka Free Fire...');
            }
            Android.openFreeFire();
        }
    } catch(e) {
        showToast('❌ Gagal membuka game!');
    }
}

// ========================================
// TOAST
// ========================================
function showToast(message) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add('show');
    clearTimeout(toast.timeout);
    toast.timeout = setTimeout(function() {
        toast.classList.remove('show');
    }, 3000);
}

// ========================================
// UPDATE STATUS PERIODIK
// ========================================
setInterval(updateStatus, 5000);

// ========================================
// LOAD SAAT PERTAMA
// ========================================
document.addEventListener('DOMContentLoaded', function() {
    updateStatus();
    refreshUserGames();
    testConnection();
});
