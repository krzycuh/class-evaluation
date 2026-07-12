# Ocena Przedszkolaka — projekt ekranów (UX)

Użytkownik główny: **nauczycielka przedszkola**, często z tabletem/telefonem
w ręku, wypełnia oceny „między zajęciami". Stąd nadrzędne zasady:

- **Minimalna liczba kliknięć do oceny** — z listy uczniów do pierwszego
  „Potrafi / Jeszcze nie" maksymalnie 2 tapnięcia.
- **Autosave** — każda zmiana zapisuje się natychmiast, widoczny dyskretny
  wskaźnik „Zapisano ✓". Brak ryzyka utraty pracy.
- **Duże cele dotykowe** — przyciski oceny wielkości kciuka, działa na tablecie.
- **Widoczny postęp** — nauczycielka zawsze wie, ile jej zostało.
- Interaktywne makiety: `docs/makiety.html` (otwórz w przeglądarce).

## Mapa nawigacji

```
Logowanie
   └── Uczniowie (ekran główny)          ← selektor semestru w nagłówku
         ├── Ocena ucznia                ← serce aplikacji
         │     └── (podgląd raportu ucznia)
         ├── Raporty (cała grupa)
         │     └── Podgląd / druk raportu
         └── Ustawienia
               ├── Umiejętności (konfiguracja)
               ├── Uczniowie i grupa (CRUD)
               └── Semestry
```

Stała dolna belka nawigacji (mobile) / boczna (desktop):
**Uczniowie · Raporty · Umiejętności · Ustawienia**.

---

## E1. Logowanie

Prosty formularz e-mail + hasło. Bez rejestracji publicznej — konta zakłada
admin (aplikacja prywatna).

```
┌──────────────────────────────┐
│         🧸 Ocena             │
│       Przedszkolaka          │
│                              │
│  E-mail    [____________]    │
│  Hasło     [____________]    │
│                              │
│       [  Zaloguj się  ]      │
└──────────────────────────────┘
```

---

## E2. Uczniowie — ekran główny

Lista dzieci w grupie z paskiem postępu wypełnienia ocen w bieżącym
semestrze. Klik w ucznia → E3 (ocenianie).

```
┌────────────────────────────────────────────────┐
│ Biedronki 2026/27         [Semestr I ▾]   (KM) │
├────────────────────────────────────────────────┤
│ 🔎 Szukaj ucznia…          [Wszyscy|Do uzup.]  │
├────────────────────────────────────────────────┤
│ ● Ania Kowalska      4-latki   ▓▓▓▓▓▓░░ 24/30 │
│ ● Bartek Nowak       4-latki   ▓▓▓▓▓▓▓▓ 30/30✓│
│ ● Celina Wiśniewska  3-latki   ▓▓░░░░░░  6/22 │
│ ● Dawid Mazur        5-latki   ░░░░░░░░  0/34 │
│   …                                            │
├────────────────────────────────────────────────┤
│  👧 Uczniowie   📄 Raporty   ⭐ Umiej.   ⚙️     │
└────────────────────────────────────────────────┘
```

Elementy:
- selektor semestru globalnie w nagłówku (kontekst całej aplikacji),
- badge grupy wiekowej przy każdym dziecku,
- filtr „Do uzupełnienia" — pokazuje tylko dzieci z brakami,
- postęp `ocenione / wszystkie umiejętności grupy wiekowej dziecka`.

---

## E3. Ocena ucznia — serce aplikacji

Umiejętności pogrupowane w akordeony obszarów rozwoju. Każdy wiersz:
tytuł + dwa duże przyciski. Ocena jednym tapnięciem, autosave.

```
┌────────────────────────────────────────────────┐
│ ← Ania Kowalska (4-latki)     24/30  Zapisano✓ │
├────────────────────────────────────────────────┤
│ ▾ SAMODZIELNOŚĆ                          5/6   │
│ ┌────────────────────────────────────────────┐ │
│ │ Samodzielnie ubiera kurtkę            ⓘ   │ │
│ │   [ ✓ Potrafi ]   [ ○ Jeszcze nie ]        │ │
│ │   ✎ notatka: „myli strony butów"           │ │
│ ├────────────────────────────────────────────┤ │
│ │ Korzysta samodzielnie z toalety       ⓘ   │ │
│ │   [ ✓ Potrafi ]   [ ○ Jeszcze nie ]        │ │
│ └────────────────────────────────────────────┘ │
│ ▸ MOTORYKA MAŁA                          6/8   │
│ ▸ MOWA I KOMUNIKACJA                     4/7   │
│ ▾ NOTATKA OGÓLNA O DZIECKU                     │
│ ┌────────────────────────────────────────────┐ │
│ │ Ania chętnie pomaga innym dzieciom…        │ │
│ └────────────────────────────────────────────┘ │
│            [ Podgląd raportu ]                 │
└────────────────────────────────────────────────┘
```

Detale interakcji:
- wybrany przycisk podświetlony (zielony „Potrafi" / bursztynowy „Jeszcze
  nie"); ponowne tapnięcie odznacza (wraca do „nieocenione"),
- `ⓘ` rozwija opis umiejętności (jak sprawdzić) + zalecenie dla rodziców,
- `✎` rozwija pole notatki do konkretnej umiejętności (freetext),
- licznik postępu na akordeonie i w nagłówku aktualizuje się na żywo,
- nawigacja poprzedni/następny uczeń (strzałki w nagłówku) — szybkie
  przejście przez całą grupę przy jednej umiejętności,
- lista umiejętności zależy od grupy wiekowej dziecka.

---

## E4. Umiejętności — konfiguracja

Zarządzanie obszarami i umiejętnościami (rola ADMIN; nauczycielka —
podgląd). Umiejętności dezaktywuje się, nie usuwa.

```
┌────────────────────────────────────────────────┐
│ Umiejętności                    [+ Obszar]     │
├────────────────────────────────────────────────┤
│ ▾ SAMODZIELNOŚĆ                 [+ Umiejętność]│
│ │ ⠿ Samodzielnie ubiera kurtkę   3l 4l    ✏️  │
│ │ ⠿ Korzysta z toalety           3l 4l 5l  ✏️  │
│ ▸ MOTORYKA MAŁA (8)                            │
└────────────────────────────────────────────────┘

Edycja umiejętności (modal / panel):
┌────────────────────────────────────────────────┐
│ Tytuł        [Samodzielnie ubiera kurtkę    ]  │
│ Opis         [Jak sprawdzić: poproś dziecko…]  │
│ Zalecenie    [Ćwiczcie wspólnie zapinanie   ]  │
│ dla rodziców [suwaka przy ubieraniu…        ]  │
│ Grupy wiekowe  [✓3l] [✓4l] [ 5l] [ 6l]        │
│ Obszar       [Samodzielność ▾]   Aktywna [✓]  │
│              [ Zapisz ]  [ Anuluj ]            │
└────────────────────────────────────────────────┘
```

- `⠿` uchwyt drag&drop do zmiany kolejności (`sort_order`),
- chipy grup wiekowych od razu widoczne na liście.

---

## E5. Raporty

Widok całej grupy: status gotowości raportu każdego dziecka + akcje.

```
┌────────────────────────────────────────────────┐
│ Raporty — Semestr I 2026/27                    │
├────────────────────────────────────────────────┤
│ Ania Kowalska    ▓▓▓▓▓▓▓▓ 100%  [Generuj]     │
│ Bartek Nowak     wygenerowany   [Podgląd] [🖨] │
│ Celina W.        ▓▓░░ 27% ⚠ braki  [Uzupełnij]│
├────────────────────────────────────────────────┤
│        [ Generuj wszystkie gotowe ]            │
└────────────────────────────────────────────────┘
```

### Podgląd raportu (print-friendly)

```
┌────────────────────────────────────────────────┐
│        RAPORT ROZWOJU DZIECKA                  │
│  Ania Kowalska · 4-latki · Semestr I 2026/27   │
│                                                │
│  CO ANIA JUŻ POTRAFI ✓                         │
│  Samodzielność: ubiera kurtkę, korzysta z…     │
│  Motoryka mała: trzyma poprawnie kredkę…       │
│                                                │
│  NAD CZYM PRACUJEMY                            │
│  • Zapinanie guzików                           │
│    Zalecenie: ćwiczcie wspólnie zapinanie…     │
│  • Wycinanie po linii                          │
│    Zalecenie: bezpieczne nożyczki i wspólne…   │
│                                                │
│  OBSERWACJE NAUCZYCIELA                        │
│  Ania chętnie pomaga innym dzieciom…           │
│                                                │
│  Data: 15.01.2027    Nauczyciel: K. Mazurek    │
└────────────────────────────────────────────────┘
```

- generowanie tworzy **snapshot** (zmiany konfiguracji po fakcie nie
  zmieniają wydanego raportu),
- druk / PDF przez `@media print` + `window.print()`,
- ostrzeżenie przy generowaniu z brakami („3 umiejętności nieocenione —
  pominąć w raporcie?").

---

## E6. Ustawienia

- **Uczniowie i grupa**: CRUD dzieci (imię, nazwisko, data urodzenia →
  podpowiedź grupy wiekowej), archiwizacja ucznia, nazwa grupy i rok szkolny.
- **Semestry**: lista okresów, otwieranie/zamykanie (zamknięty semestr =
  oceny tylko do odczytu).
- **Konto**: zmiana hasła, wylogowanie.

---

## Kierunek wizualny

- Ciepła, przyjazna kolorystyka (pastelowa zieleń/morski + bursztyn jako
  akcent), duże zaokrąglenia, czytelna typografia (min. 16 px na mobile).
- Kolory ocen: zielony = „Potrafi", bursztyn = „Jeszcze nie", szary =
  nieocenione. Nigdy czerwony — raport trafia do rodziców, ton ma być
  wspierający, nie alarmujący.
- Pełna responsywność: mobile-first (telefon nauczycielki), wygodny układ
  dwukolumnowy na desktopie.
- Dostępność: kontrasty WCAG AA, obsługa klawiatury, etykiety ARIA.
