# Hızlı kullanım

1. Bu klasördeki tüm dosyaları GitHub repo köküne yükle.
2. `.github/workflows/android.yml` dosyası zaten hazır.
3. GitHub > Actions > Build Android APK > Run workflow.
4. Bitince `android-apk` artifact'ını indir.

## Not
- GitHub workflow, wrapper'a değil doğrudan kurulan Gradle 8.7'ye dayanır.
- `gradle/wrapper/gradle-wrapper.jar` bu pakette yoktur. GitHub Actions build'i için gerekli değildir.
- Android Studio'da wrapper ile build almak istersen wrapper jar ayrıca eklenmelidir.
