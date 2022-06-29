# TODO
- [ ] Komunikaty o prawidłowym wykonaniu operacji lub błędach (częściowo zrobione)
- [ ] Obsługa wyjątków tam, gdzie to potrzebne (częściowo zrobione)
- [ ] Wyciągnąć główne komponenty w interfejsy
- [ ] Dostarczanie metod funkcyjnie z zewnątrz zamiast "na siłę" implementować wewnętrznie z przekazywaniem referencji do obiektów
- [ ] Komunikaty związane z nieistniejącymi kanałami i użytkownikami (obecnie ramki są dropowane bez informacji zwrotnej dla użytkownika)
- [x] Opakowanie warstwy transportowej do przesyłania plików w JSON->Base64
- [x] Wyciągnąć powtarzające się stringi do stałych
- [x] Niektóre wpisy wpadają do historii podwójnie - poprawić
- [x] Walidacja nazwy pobieranego pliku
- [x] Zmiana wysyłania pliku (aby nie był widoczny dla innych dopóki nie jest w pełni wysłany)
- [x] Pełna walidacja składni poleceń
- [x] Rozłączenie się użytkownika daje komunikat i wylogowuje z kanałów
- [x] Usuwanie plików "kanałowych" przy likwidacji kanału
- [x] Aktualizacja pliku pomocy

# Projekt 1a
## Założenia ogólne
1. Projekt realizowany samodzielnie przez wszystkich uczestników kursu
2. Nieprzekraczalny termin oddania projektu to 11.06.2022
3. Stworzony kod powinien być opublikowany na repozytorium git np. GitHub
4. Realizując projekt używamy wyłącznie standardowego SDK Java 11 tzn. nie korzystamy z frameworków, ani zewnętrznych bibliotek
5. Ze względu na kolejność realizowanych zajęć testy jednostkowe nie są wymagane, ale mile widziane

# Projekt 1b
## Założenia ogólne
1. Projekt realizowany samodzielnie przez wszystkich uczestników kursu
2. Nieprzekraczalny termin oddania projektu to 17.07.2022
3. Stworzony kod powinien być opublikowany na repozytorium git np. GitHub
4. Realizując projekt używamy standardowego SDK Java 11 oraz technologii poznanych w dalszej części kursu tj. CDI, JPA/Hibernate, elementy Jakarta EE (JAX-RS, JMS)
5. Ze względu na kolejność realizowanych zajęć testy jednostkowe nie są wymagane, ale mile widziane

# Opis aplikacji
Stwórz czat tekstowy/aplikację klient-server wykorzystując Java Sockets. Aplikacja powinna umożliwiać:
- rozmowę wielu osób na kanale grupowym
- rozmowę 2 lub więcej osób na kanale prywatnym
- przesyłanie plików między osobami na danym kanale
- zapamiętywanie historii rozmów po stronie serwera w bazie opartej o plik płaski
- możliwość przeglądania historii rozmów z poziomu klienta (jeśli uczestniczył on w rozmowie/był na kanale)
- obsługa aplikacji powinna odbywać się z terminala/linii komend (interfejs tekstowy)

Uwaga! Należy zwrócić szczególną uwagę na aspekty związane z wielowątkowością - zapewnić zarówno bezpieczeństwo jak wydajność całego rozwiązania.

# Decyzje i wątpliwości implementacyjne

## Przesyłanie plików
### Czy pliki mają być na serwerze czy bezpośrednio przesyłane między użytkownikami?
Implementuję opcję ze składowaniem na serwerze.

## Bezpieczeństwo
### Co to znaczy bezpieczny w użytkowaniu?
#### Czy użytkownicy mają mieć jakieś konta na serwerze?
Na razie nie implementuję rejestracji kont (dodanie stosunkowo proste, baza w pliku płaskim o strukturze login:hashHasła)

#### Co jeśli użytkownik X się rozłączy a potem inny użytkownik się podłączy pod tą samą nazwą?
Czy ma mieć dostęp do jego historii i plików czy też historia i pliki mają być czyszczone po rozłączeniu się użytkownika (czyli bez opcji wejdź, zostaw innym plik i wyjdź)?


## Historia
#### Co to znaczy plik płaski?
Czy to oznacza, że to ma być standardowy log tekstowy, czy też może być w tym struktura na przykład JSON lub SQLite?
Na razie implementuję proste username:tekstHistorii

#### Czy historia ma być wieczna czy tylko na potrzeby pojedynczego uruchomienia serwera?
Na razie robię "wieczną", nie ma problemu z czyszczeniem pliku przy starcie.

#### Czy serwer ma podawać historię tylko z okresu gdy użytkownik był na kanale?
Na razie historia tylko z tego, co użytkownik widział podczas swojej obecności (nie ma możliwości zalogowania się później i "nadrobienia" tego, co się działo na kanale w trakcie jego nieobecności).

#### Czy historia ma być per serwer czy per user?
Czy serwer ma pamiętać co przyszło do serwera, czy tylko to, co faktycznie odebrał user (czyli nie da się wysłać wiadomości do usera podczas gdy jest wylogowany, żeby po zalogowaniu mógł to odczytać).
