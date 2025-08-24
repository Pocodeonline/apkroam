# 🚀 Roam Tool - WiFi Bubble App

Ứng dụng Android hiển thị dưới dạng bong bóng chat floating overlay, đọc file WiFi và tự động mở WiFi settings.

## 🎯 Tính năng

- **Floating Bubble**: Hiển thị như bong bóng chat Zalo
- **File Reader**: Đọc file TXT từ thư mục Downloads
- **WiFi Parser**: Parse format "TênWiFi|MatKhau"  
- **Auto Settings**: Tự động mở cài đặt WiFi
- **Error Checking**: Kiểm tra lỗi từng dòng file
- **Anti-Ban Toggle**: Nút bật/tắt (giao diện)
- **Smooth Animation**: Hiệu ứng thu nhỏ/phóng to mượt

## 🔧 Build APK Tự Động

Repository này đã được cấu hình GitHub Actions để tự động build APK khi push code.

### Cách lấy APK:

1. **Vào tab Actions** của repository
2. **Chọn build mới nhất** (có dấu ✅ xanh)
3. **Download APK** từ phần Artifacts
4. **Cài APK** lên điện thoại

### Hoặc từ Releases:

1. **Vào tab Releases**
2. **Download file APK** từ release mới nhất

## 📱 Cách sử dụng

1. Cài APK và cấp quyền overlay
2. App hiện thành bong bóng xanh
3. Nhấn bong bóng → hiện tab điều khiển
4. Chọn file TXT từ Downloads
5. Nhấn "Bắt đầu" để mở WiFi settings

## 📄 Format file WiFi

Tạo file TXT trong Downloads với format:
```
TenWiFi1|matkhau123
TenWiFi2|password456
HomeWiFi|secretkey
```

## 🛠️ Development

- **Language**: Java
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 32
- **Build System**: Gradle

---

**© 2025 Roam Tool - Made with ❤️**
