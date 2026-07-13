# Propozycja: konta nauczycielek i dostęp do grup

Plan dodania obsługi wielu nauczycielek (przedszkolanek) do aplikacji.
Dokument odpowiada też na dwa pytania projektowe: *czy nauczycielki widzą
nawzajem swoje grupy* i *jak zorganizować dostęp*.

## 1. Stan obecny

Fundament wielu użytkowników już istnieje i działa poprawnie:

- tabela `users` z rolami `TEACHER` / `ADMIN` (przy pierwszym starcie
  powstaje tylko konto admina — `BootstrapRunner`),
- `class_groups.owner_user_id` — każda grupa ma właścicielkę,
- **wszystkie** serwisy (uczniowie, oceny, raporty, kalendarz, projekty)
  przechodzą przez `StudentService.requireClassGroupAccess`, które
  przepuszcza tylko właścicielkę grupy albo admina,
- nauczycielka z kilkoma grupami ma już przełącznik grupy w UI
  (`AppContext.setClassGroupId`).

Czego brakuje:

- endpointu i ekranu do zakładania kont nauczycielek (dziś konto można
  dodać tylko ręcznie w bazie),
- zarządzania grupami (tworzenie, przypisywanie nauczycielek) — jedyną
  grupę tworzy bootstrap i należy ona do admina,
- zmiany hasła (własnego i resetu przez admina),
- dezaktywacji konta (nauczycielka odchodzi z pracy).

## 2. Czy nauczycielki widzą nawzajem swoje grupy?

**Nie — i tak zostaje.** Izolacja per grupa jest już wymuszona w backendzie
i jest właściwym domyślnym modelem:

- dane dzieci (oceny, notatki, raporty) to dane wrażliwe — zasada
  minimalnego dostępu (RODO, sekcja „Bezpieczeństwo" w architektura.md:
  „nauczycielka widzi wyłącznie swoje grupy"),
- nauczycielka nie ma zawodowej potrzeby wglądu w cudzą grupę,
- admin (dyrekcja / prowadząca) widzi wszystko i rozstrzyga wyjątki.

Prywatne per grupa pozostają: uczniowie, oceny, notatki, raporty,
wydarzenia o zasięgu `CLASS_GROUP`/`STUDENT`, projekty grupowe.

Wspólne dla wszystkich pozostają (bez zmian): słownik umiejętności
i obszarów, grupy wiekowe, semestry, wydarzenia `NATIONAL`/`PRESCHOOL`,
projekty ogólnoprzedszkolne.

## 3. Jak zorganizować dostęp? Przypisania zamiast właścicielki

Jedna zmiana modelu: **relacja wiele-do-wielu** zamiast pojedynczego
`owner_user_id`. Powód praktyczny: w przedszkolu grupę zwykle prowadzą
**dwie** nauczycielki (zmiana rano/popołudnie), bywają też zastępstwa.

```
class_group_teachers (
    class_group_id UUID REFERENCES class_groups(id) ON DELETE CASCADE,
    user_id        UUID REFERENCES users(id),
    PRIMARY KEY (class_group_id, user_id)
)
```

Zasady:

- nauczycielka widzi grupę ⇔ jest do niej przypisana; admin widzi wszystko,
- przypisania edytuje wyłącznie admin,
- dwie nauczycielki w jednej grupie pracują na tych samych danych;
  `assessments.updated_by` już dziś zapisuje, kto ocenił — audyt zostaje,
- `requireClassGroupAccess` zmienia warunek z `owner_user_id = user`
  na `EXISTS (... class_group_teachers ...)` — reszta serwisów nie
  wymaga zmian, bo wszystkie idą przez ten jeden punkt,
- `owner_user_id` znika po backfillu (właścicielka → pierwsze przypisanie).

## 4. Plan wdrożenia

### Etap 1 — konta nauczycielek

Migracja `V4`: `users.active BOOLEAN NOT NULL DEFAULT TRUE`.

Backend:

- `GET/POST /api/users`, `PATCH /api/users/{id}` — tylko `ADMIN`
  (`SecurityConfig` + nowy `UserAdminController`),
- tworzenie konta: e-mail + imię i nazwisko; hasło startowe generowane
  (jak w `BootstrapRunner`) i pokazane adminowi **jednorazowo** w odpowiedzi,
- `PATCH`: zmiana nazwy, `active` (dezaktywacja zamiast usuwania —
  `updated_by`/`generated_by` wskazują na usera), reset hasła,
- `AppUserDetailsService`: konto nieaktywne → odmowa logowania,
- `POST /api/auth/password` — zmiana własnego hasła (stare + nowe),
  dostępna dla każdej zalogowanej osoby.

Frontend:

- Ustawienia → sekcja **„Nauczycielki"** (tylko admin): lista, dodawanie
  (z pokazaniem wygenerowanego hasła), dezaktywacja, reset hasła,
- menu awatara → „Zmień hasło".

### Etap 2 — grupy i przypisania

Migracja `V5`: tabela `class_group_teachers` + backfill
z `owner_user_id`, potem usunięcie kolumny.

Backend:

- `requireClassGroupAccess` / `listClassGroups` na bazie przypisań,
- `POST /api/class-groups` (nazwa, rok szkolny), `PATCH /api/class-groups/{id}`,
  `PUT /api/class-groups/{id}/teachers` (pełna lista przypisań) — tylko `ADMIN`,
- `BootstrapRunner`: grupa startowa dostaje przypisanie admina.

Frontend:

- Ustawienia → sekcja **„Grupy"** (tylko admin): tworzenie grupy,
  checkboxy przypisanych nauczycielek.

### Etap 3 — później / opcjonalnie

- wymuszenie zmiany hasła przy pierwszym logowaniu (`must_change_password`),
- „nowy rok szkolny": kopiowanie/archiwizacja grup i przekazywanie dzieci,
- widok admina „kto ma dostęp do czego" (proste zestawienie przypisań).

## 5. Przypadki brzegowe

| Sytuacja | Zachowanie |
|----------|------------|
| Nauczycielka odchodzi | Admin dezaktywuje konto; grupa i dane zostają, admin przypisuje następczynię |
| Zastępstwo | Admin doraźnie dopisuje nauczycielkę do grupy, po zastępstwie wypisuje |
| Dwie nauczycielki oceniają to samo dziecko | Ostatni zapis wygrywa (jak dziś), `updated_by` mówi kto |
| Grupa bez przypisań | Widzi ją tylko admin — stan przejściowy, nie błąd |
| Nieaktywna nauczycielka próbuje się zalogować | Odmowa jak przy złym haśle (bez zdradzania powodu) |

## 6. Testy

Rozszerzenie `ApplicationFlowTest` (Testcontainers):

- admin zakłada nauczycielkę → nauczycielka loguje się hasłem startowym,
- nauczycielka A nie widzi grupy nauczycielki B (`GET /api/class-groups`
  nie zwraca, bezpośrednie `GET` → 403),
- dwie nauczycielki przypisane do jednej grupy widzą te same dzieci,
- konto nieaktywne nie może się zalogować,
- endpointy `/api/users` i zarządzanie grupami odrzucają rolę `TEACHER`.
