# Crew Portal 2.2.9 — historical report

> This file is the preserved 2.2.9 baseline. The active report for this archive is `IMPLEMENTATION_REPORT_2_2_10.md`.

## Что реализовано

- Сохранена TAS-логика: Thursday outbound → Friday/Saturday STAY → Sunday operating return; Sunday arrival → same-Sunday deadhead return.
- Уже опубликованный current month не регенерируется при обновлении приложения или изменении generator rules.
- Route Map переведена на полностью автономный Compose Canvas без tile server/API key; LED теперь имеет встроенные координаты.
- BKK-LED использует 650/625 минут, BKK-LHR — 760/705 минут; fixed fallback 150/150 заменён distance-based оценкой.
- STAY heading получает город из AirportDatabase, а hotel остаётся отдельным полем.
- Добавлен заполняемый Electronic Pilot Logbook в Flight Details.
- Добавлен Aircraft Delivery / Ferry с ручной регистрацией `HS-…`, новым типом A330neo и автоматическим добавлением борта в persistent Fleet после прибытия.
- Выполнен согласованный рефакторинг persistence, roster state, routes, payroll и generator scheduling/time storage.
- Версия во всех активных metadata/workflows/UI обновлена до `2.2.9`, `versionCode 2209`.

## Основные добавленные файлы

- `data/local/DatabaseMigrations.kt`
- `data/local/DutyType.kt`
- `data/local/LogbookEntryEntity.kt`, `LogbookEntryDao.kt`
- `data/local/FleetAircraftEntity.kt`, `FleetAircraftDao.kt`
- `data/local/LeavePeriodEntity.kt`, `LeavePeriodDao.kt`
- `data/repository/LogbookRepository.kt`
- `data/repository/FleetRepository.kt`
- `data/repository/LeaveRepository.kt`
- `data/fleet/AircraftTypeCatalog.kt`
- `data/route/RouteCatalog.kt`
- `data/airport/AirportGeoDirectory.kt`
- `data/payroll/PayrollCalculator.kt`
- `data/roster/NextRosterWorker.kt`
- `ui/schedule/ElectronicLogbookCard.kt`
- unit tests `RosterGeneratorDeterminismTest.kt`, `AircraftTypeCatalogTest.kt`

## Основные изменённые файлы

- `MainActivity.kt`, `MainNavigation.kt`
- `FlightEntity.kt`, `FlightDao.kt`, `AppDatabase.kt`
- `FlightRepository.kt`, `LeaveDatabase.kt`
- `RosterGenerator.kt`, `AirportTime.kt`
- `ScheduleScreen.kt`, `FlightDetailsScreen.kt`, `FleetScreen.kt`, `LeaveManagementScreen.kt`, `PayrollScreen.kt`, `CompanyRoutesScreen.kt`
- `app/build.gradle.kts`
- `.github/workflows/android-build.yml`, `.github/workflows/release-apk.yml`
- `README.md`, `CHANGELOG.md`, `CREW_PORTAL_DEVELOPER_GUIDE.md`, `update/app_update.json`

## Persistence и migrations

Room schema повышена с 3 до 4. Явная `MIGRATION_3_4`:

- сохраняет существующую таблицу `flights` и все опубликованные roster rows;
- добавляет UTC instant columns и Aircraft Delivery flags;
- создаёт таблицы `electronic_logbook_entries`, `fleet_aircraft`, `leave_periods`;
- заменяет destructive fallback на контролируемую migration.

`FlightDao.replaceAll` выполняет `clearAll + insertAll` в одной Room transaction. Automatic roster change, draft replacement/deletion и manual roster change больше не оставляют временно пустое состояние.

## Route/timezone logic

Legacy ISO strings остаются airport-local для совместимости UI и прежней БД. Новые `departureEpochMillis`/`arrivalEpochMillis` хранят UTC instant; старые rows backfill-ятся через airport UTC offsets. Arrival calculation по-прежнему переводит departure local → instant → arrival local, поэтому BKK/TAS не складываются как одинаковые часовые зоны.

`RouteCatalog` является общим источником airport/block/hotel данных для Company Routes и manual duties. Explicit company time всегда приоритетен: LED 10h50/10h25, LHR 12h40/11h45. Unknown-but-known airport больше не получает 2h30: fallback рассчитывается по great-circle distance из `AirportGeoDirectory`. Generator-specific flight numbers/times остаются в генераторе.

Route Map не делает сетевых запросов. `OfflineRouteMap` рисует встроенный world outline, coordinate grid, route arc и endpoints через Compose Canvas; osmdroid dependency удалена. LED добавлен в coordinate directory и generated layover routes.

## Monthly flight time и payroll

Monthly Progress считает operating `FLIGHT` и отдельно предусмотренные simulator minutes через `RosterMetrics`; DEADHEAD/STAY/OFF/RESERVE не дают operating block credit. Cumulative profile time добавляется один раз после arrival по `isFlightTimeAdded`.

Payroll formula вынесена из Compose в `PayrollCalculator`. Расчёт использует completed operating flights, а если их ещё нет — planned projection; включает duty/ground, reserve, deadhead, night, holiday, augmented crew, layover, leave и deductions. Неподтверждённая компенсация прошлогоднего отпуска больше не выдумывается.

## Operational Roster Change

Обычный режим поддерживает turnaround/layover, route, aircraft, optional registration, return fields, explicit replacement permission и instructor/observer. Изменение строится в памяти и записывается одним `replaceAll`, затем alarms пересоздаются.

Режим Aircraft Delivery создаёт один outbound sector. UI фиксирует префикс `HS-`; suffix вводится вручную и повторно валидируется repository. После фактического arrival борт upsert-ится в `fleet_aircraft`, а `deliveryProcessed` не допускает повторной обработки.

## Electronic Logbook

Форма находится непосредственно во вкладке/карточке рейса. Prefill берётся из FlightEntity, пользователь может менять EASA-style flight fields, сохранить draft и выполнить `Certify & lock`. Certification требует signature, реальную registration и total time > 0, сохраняет timestamp и блокирует случайные изменения.

Основа полей: EASA FCL.050/AMC1. Для production/legal use всё ещё потребуются server identity, immutable audit/amendment history, controlled backup/retention и одобрение применимого авиационного регулятора.

## Следующий месяц и generator

Generator стал детерминированным: один month/seed/ruleset даёт одинаковый roster. `NextRosterScheduler` подключает immediate + daily WorkManager; worker вызывает idempotent date-gated generation за шесть дней до конца месяца. Current published month этот процесс не меняет.

## Signing и GitHub

Оба workflow запускают `testDebugUnitTest`, `lintDebug`, `assembleDebug`. `android-build.yml` загружает APK как artifact, а `release-apk.yml` публикует installable debug APK как `CrewPortal-2.2.9.apk`. Debug build подписывается legacy `crewportal-debug.keystore`, GitHub signing secrets не нужны. Для согласованного clean-test пользователь удаляет 2.2.8 перед установкой: Android удалит старую локальную БД, а 2.2.9 создаст новую БД и roster текущего Bangkok month на первом запуске.

## Проверка

- Gradle wrapper: 8.7; AGP 8.5.2; Kotlin 1.9.24; JDK 17.
- `gradlew help --warning-mode all`: `BUILD SUCCESSFUL`.
- Active metadata согласованы с `2.2.9/2209`.
- Добавлен `RouteCatalogTest`: LED/LHR exact block time, LED coordinate и отсутствие fixed 150-minute fallback.
- Runtime spelling `Rooster`: не найдено.
- `fallbackToDestructiveMigration`: удалён.
- Полный `assembleDebug` локально не запускался по прямой просьбе пользователя; Android SDK в локальной среде отсутствует. Архив подготовлен для проверки GitHub Actions.

## Известные ограничения

- Результат Android compile/test/lint станет окончательно известен после первого GitHub Actions run.
- Удаление 2.2.8 и установка APK выполняются пользователем на Android-устройстве; к телефону из рабочей среды доступа нет.
- Electronic Logbook — local operational record, не сертифицированная регулятором система хранения.
- Leave compatibility cache всё ещё существует для старых synchronous consumers, хотя source records теперь persist в Room.
- Generator-specific schedule templates пока остаются в `RosterGenerator`; `RouteCatalog` владеет общими route facts, но не номерами/временем рейсов.
- Публикуемый GitHub APK является debug/testing build, а не Play Store production release.
