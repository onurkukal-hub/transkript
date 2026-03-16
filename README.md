# Transkript APK - Android Studio Projesi

Bu proje, telefondan ses dosyası seçip OpenAI API ile transkript alman için hazırlanmış basit bir Android uygulamasıdır.

## Özellikler
- Ses/video dosyası seçme
- OpenAI API key girme
- `gpt-4o-mini-transcribe` veya `gpt-4o-transcribe` ile transkript alma
- Sonucu uygulama içinde gösterme
- Panoya kopyalama
- TXT olarak kaydetme

## Notlar
- OpenAI resmi dökümantasyonuna göre transkripsiyon endpoint'i `audio/transcriptions` üzerinden çalışır.
- Desteklenen giriş formatları arasında `mp3`, `mp4`, `mpeg`, `mpga`, `m4a`, `wav`, `webm`, `ogg`, `flac` yer alır.
- Yükleme boyutu sınırı 25 MB olabilir; büyük dosyalarda parçalama gerekebilir.

## Kullanım
1. Android Studio ile klasörü aç.
2. Gradle senkronizasyonunu çalıştır.
3. Telefona veya emülatöre yükle.
4. Uygulamayı aç.
5. OpenAI API key gir.
6. Ses dosyasını seç.
7. `Transkript Başlat` butonuna bas.

## Geliştirme Önerileri
- Uzun kayıtlar için chunk/parçalama
- SRT/VTT çıktısı
- Konuşmacı ayrımı (`gpt-4o-transcribe-diarize`)
- Dosya paylaşma/export

## Referanslar
OpenAI resmi dökümantasyona göre transkripsiyon API'si ses dosyasını multipart/form-data ile kabul eder ve `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `whisper-1` gibi modeller desteklenir. Ayrıca yaygın ses formatları desteklenir ve dosya boyutu sınırı belirtilmiştir. citeturn905853search0turn905853search5
