# TODO
- [ ] Niektóre wpisy wpadają do historii podwójnie - poprawić
- [ ] Pełna walidacja składni poleceń
- [ ] Wyciągnąć powtarzające się stringi do stałych
- [ ] Dodać kolejkowanie w kanałach specjalnych
- [ ] Usuwanie plików udostępnionych przez użytkownika jeśli ten się rozłączył?
- [ ] Usuwanie plików "kanałowych" przy likwidacji kanału
- [ ] Wyciągnąć główne komponenty w interfejsy
- [ ] Dostarczanie metod funkcyjnie z zewnątrz zamiast "na siłę" implementować wewnętrznie z przekazywaniem referencji do obiektów

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
**Czy pliki mają być na serwerze czy bezpośrednio przesyłane między użytkownikami?**
Implementuję opcję ze składowaniem na serwerze.

## Bezpieczeństwo
**Co to znaczy bezpieczny w użytkowaniu?**
- Czy użytkownicy mają mieć jakieś konta na serwerze?
- Co jeśli użytkownik X się rozłączy a potem inny użytkownik się podłączy pod tą samą nazwą? Czy ma mieć dostęp do jego historii czy też historia ma być czyszczona?

## Historia
- Co to znaczy plik płaski? Czy to oznacza, że to ma być standardowy log tekstowy, czy też może być w tym struktura na przykład JSON lub SQLite?
- Czy historia ma być wieczna czy tylko na potrzeby pojedynczego uruchomienia serwera?
- Czy serwer ma podawać historię tylko z momentu gdy użytkownik był na kanale i widział wiadomości, czy też całą historię (nawet wiadomości podczas nieobecności użytkownika, czyli na przykład wylogował się, ale po zalogowaniu może "nadrobić" zaległe wiadomości?).
- Czy historia ma być per serwer czy per user (w sensie czy serwer ma pamiętać co przyszło do serwera, czy też pamiętać to, co wysłał do userów)?

Implementuję:
- Zapis w postaci zwyczajnego pliku tekstowego.
- Historia "wieczna"
- Serwer zapamiętuje w historii to, co wysłał do użytkownika
- Serwer w historii nie poda tego, co się działo na kanale podczas jego nieobecności.
