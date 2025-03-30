# SMFishingPlanet

Plugin do Minecraft na łowienie ryb z własnym systemem poziomów, ekonomią i statystykami.

## Funkcje

- Własny system łowienia ryb z minigierką
- System poziomów łowienia
- Różne rodzaje wędek z bonusami i wytrzymałością
- System rzadkości ryb
- Regionalne łowiska (integracja z WorldGuard)
- System ekonomii (integracja z Vault)
- Rozbudowane statystyki łowienia
- Możliwość sprzedaży ryb
- System losowego łowienia śmieci (konfigurowalna szansa i wartość)
- System żyłek wędkarskich z różnymi grubościami (4mm, 5mm, 6mm)
- Rozbudowany system craftingu wędek z wykorzystaniem żyłek
- Rozbudowany system konfiguracji

## Komendy

- `/wedka <typ>` - Otrzymaj wędkę określonego typu (basic, advanced, professional, master)
- `/poziom` - Sprawdź swój aktualny poziom łowienia
- `/statystyki` - Wyświetl swoje statystyki łowienia
- `/sprzedajryby` - Otwórz menu sprzedaży ryb i śmieci
- `/zylka <4mm/5mm/6mm>` - Otrzymaj żyłkę o określonej grubości
- `/setlevel <gracz> <poziom>` - Ustaw poziom łowienia dla gracza (wymaga uprawnień)
- `/smfishingplanet reload` - Przeładuj konfigurację pluginu (wymaga uprawnień)
- `/ryby` - Wyświetl listę dostępnych ryb

## Łowienie ryb

1. Zdobądź wędkę za pomocą komendy `/wedka` lub poprzez crafting z odpowiednią żyłką
2. Znajdź odpowiednie łowisko (regiony WorldGuard)
3. Zarzuć wędkę do wody
4. Gdy zobaczysz powiadomienie "KLIKNIJ TERAZ!", kliknij prawym przyciskiem myszy
5. Jeśli złowisz rybę, otrzymasz XP i poziomy łowienia
6. Istnieje szansa na złowienie śmieci zamiast ryby (konfigurowalna)
7. Z czasem wędka traci wytrzymałość i może się złamać

## Żyłki wędkarskie

Podczas łowienia masz szansę na znalezienie żyłek wędkarskich, które są potrzebne do craftingu wędek:
- Żyłka 4mm - używana do craftingu Wędki Podstawowej
- Żyłka 5mm - używana do craftingu Wędki Zaawansowanej
- Żyłka 6mm - używana do craftingu Wędki Profesjonalnej

Każdy typ wędki ma szansę na znalezienie odpowiedniej żyłki podczas łowienia (konfigurowalne w config.yml).

## Śmieci

Podczas łowienia istnieje 20% szansa (konfigurowalna) na złowienie śmieci zamiast ryby. Śmieci można sprzedać za małą wartość pieniężną. Dostępne śmieci:
- Stare buty
- Stary zestaw rybacki
- Pusta butelka
- Zgniły patyk
- Glony
- Zardzewiały hak
- Kawałek sieci
- Plastikowa torba
- Kawałek metalu
- Mokry papier

## Statystyki

Plugin zapisuje następujące statystyki łowienia:
- Liczba złowionych ryb
- Liczba nieudanych prób
- Wskaźnik sukcesu
- Złowione ryby według nazwy i rzadkości
- Statystyki wagi ryb (małe, średnie, duże, ogromne)
- Rekord najcięższej ryby
- Zarobki ze sprzedaży ryb
- Data i czas złowienia ryby

## Konfiguracja

### config.yml
Główny plik konfiguracyjny pluginu. Zawiera ustawienia ogólne, ekonomii, wędek, żyłek, śmieci i systemu poziomów.

### fish.yml
Konfiguracja ryb, rzadkości i kategorii. Tutaj możesz dodać własne ryby i określić ich właściwości.

### messages.yml
Konfiguracja wiadomości wyświetlanych przez plugin. Możesz dostosować wszystkie komunikaty.

### Sekcja śmieci (trash) w config.yml
Możliwość konfiguracji szansy na złowienie śmieci, ich domyślnej wartości sprzedaży oraz własnych typów śmieci.

### Sekcja żyłek (fishing_lines) w config.yml
Możliwość konfiguracji różnych typów żyłek, ich szans na wypadnięcie oraz opisu.

## Uprawnienia

- `smfishing.wedka` - Pozwala na użycie komendy /wedka
- `smfishing.sprzedajryby` - Pozwala na użycie komendy /sprzedajryby
- `smfishing.poziom` - Pozwala na użycie komendy /poziom
- `smfishing.zylka` - Pozwala na użycie komendy /zylka
- `smfishing.setlevel` - Pozwala na ustawianie poziomu rybactwa graczy
- `smfishing.statystyki` - Pozwala na wyświetlanie statystyk łowienia
- `smfishing.ryby` - Pozwala na wyświetlanie listy dostępnych ryb
- `smfishing.reload` - Pozwala na przeładowanie konfiguracji pluginu
- `smfishing.*` - Dostęp do wszystkich komend pluginu

## Wymagane pluginy

- Vault - Do obsługi ekonomii
- WorldGuard - Do obsługi regionów łowisk
- WorldEdit - Wymagany przez WorldGuard

## Instalacja

1. Umieść plik `SMFishingPlanet.jar` w katalogu `/plugins/` na swoim serwerze
2. Zrestartuj serwer lub użyj komendy `/reload`
3. Skonfiguruj plugin według swoich potrzeb, edytując pliki konfiguracyjne
4. Zrestartuj serwer lub użyj komendy `/smfishingplanet reload`

## Rozwiązywanie problemów

- Jeśli masz problemy z łowieniem ryb, sprawdź czy jesteś w odpowiednim regionie
- Jeśli debug jest włączony w `config.yml`, możesz zobaczyć dodatkowe informacje w konsoli serwera
- Upewnij się, że wędka, której używasz, jest obsługiwana przez plugin
- Sprawdź, czy wszystkie wiadomości w messages.yml są poprawnie zdefiniowane
- Jeśli śmieci lub ryby mają niewłaściwą wartość sprzedaży, sprawdź konfigurację w config.yml

## Autorzy

Plugin stworzony przez Stylowa dla SMFishingPlanet. 