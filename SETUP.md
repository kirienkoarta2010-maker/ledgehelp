# Ledge Help (NeoForge 1.21.1)

Требуется JDK 21.

## Как собрать
1. Скачайте официальный шаблон: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle (Code -> Download ZIP) и распакуйте.
2. В шаблоне удалите папку src/main/java/com/example/examplemod и папку src/main/templates.
3. Скопируйте из этого архива папку src в шаблон (с заменой/слиянием).
4. В gradle.properties поставьте: mod_id=ledgehelp
5. Запуск для теста: gradlew runClient (второй клиент: Run Configurations -> второй клиент, либо игра по локальной сети).
6. Сборка jar: gradlew build, готовый файл в build/libs.

Если Gradle ругается на org.spongepowered.asm, добавьте в build.gradle в dependencies:
    compileOnly 'org.spongepowered:mixin:0.8.5'

## Управление
Клавиша G (меняется в настройках управления, категория Ledge Help).
- В воздухе рядом с краем блока: зажать G, чтобы повиснуть. Shift или отпустить G, чтобы упасть.
- Стоя на краю возле висящего игрока: зажать G, чтобы протянуть руку и вытянуть его.
