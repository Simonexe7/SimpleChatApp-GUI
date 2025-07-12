
# ☕ Aplikasi Chat Sederhana dengan Java

Aplikasi chat multi-room sederhana menggunakan Java Sockets.  
Mendukung banyak klien, ruang obrolan, pesan pribadi, dan perintah-perintah dasar — semua dijalankan di terminal.

---

## 🚀 Fitur

✅ Mendukung banyak klien secara bersamaan  
✅ Ruang obrolan (buat/masuk/keluar room)  
✅ Melihat semua room yang tersedia dengan jumlah anggota (`/rooms`)  
✅ Melihat anggota di room saat ini (`/list`)  
✅ Broadcast & pesan pribadi (`@username <pesan>`)  
✅ Pesan broadcast dari server  
✅ Tampilan berwarna di terminal  
✅ Aktivitas server dicatat ke file log

---

## 🏗 Struktur Direktori

```
.
├── lib
├── src
    └── chat
        └── client
            └── ChatClient.java
        └── server
            └── ChatServer.java
├── chat-server.log
└── README.md
```

---

## 📋 Perintah

| Perintah | Deskripsi |
|----------|-----------|
| `/rooms` | Melihat semua room yang tersedia dan jumlah anggotanya |
| `/join <nama_room>` | Masuk atau membuat room baru |
| `/leave` | Keluar dari room saat ini dan kembali ke Lobby |
| `/list` | Melihat anggota yang ada di room saat ini |
| `@username <pesan>` | Mengirim pesan pribadi ke pengguna tertentu |
| teks biasa | Mengirim pesan broadcast ke semua anggota di room |

---

## 🖥 Cara Menjalankan

### 📝 Syarat
- Java JDK 8+ sudah terpasang

### 🧪 Langkah-langkah

1️⃣ Kompilasi server & klien:
```bash
javac -d build src/chat/client/ChatClient.java src/chat/server/ChatServer.java
```

2️⃣ Jalankan server:
```bash
java -cp build ChatServer
```

3️⃣ Jalankan klien (di jendela terminal terpisah, bisa lebih dari satu):
```bash
java -cp build chat.client.ChatClient
```

4️⃣ Ikuti petunjuk & mulai chatting!

---

## 🎨 Catatan

- Output di terminal sudah mendukung warna ANSI supaya lebih mudah dibaca.
- Semua aktivitas chat dicatat di file `chat-server.log`.
- Port default: `12345`
- Room default: `Lobby`

---

## 📜 Lisensi

Proyek ini bebas digunakan, dimodifikasi, dan didistribusikan.  
Selamat belajar & semoga bermanfaat! ✨
