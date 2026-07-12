-- Słowniki startowe: grupy wiekowe, obszary rozwoju, przykładowy zestaw
-- umiejętności dla 3–6 latków. Umiejętności są w pełni edytowalne w aplikacji.

INSERT INTO age_groups (id, name, min_age_years, sort_order) VALUES
    ('a0000000-0000-0000-0000-000000000003', '3-latki', 3, 1),
    ('a0000000-0000-0000-0000-000000000004', '4-latki', 4, 2),
    ('a0000000-0000-0000-0000-000000000005', '5-latki', 5, 3),
    ('a0000000-0000-0000-0000-000000000006', '6-latki', 6, 4);

INSERT INTO development_areas (id, name, description, sort_order) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'Samodzielność',
     'Czynności samoobsługowe i samodzielne funkcjonowanie w grupie', 1),
    ('d0000000-0000-0000-0000-000000000002', 'Motoryka mała',
     'Sprawność dłoni i palców, przygotowanie do pisania', 2),
    ('d0000000-0000-0000-0000-000000000003', 'Motoryka duża',
     'Sprawność ruchowa całego ciała', 3),
    ('d0000000-0000-0000-0000-000000000004', 'Mowa i komunikacja',
     'Rozwój mowy, budowanie wypowiedzi, komunikacja z otoczeniem', 4),
    ('d0000000-0000-0000-0000-000000000005', 'Rozwój społeczny',
     'Relacje z rówieśnikami i dorosłymi, emocje, współpraca', 5),
    ('d0000000-0000-0000-0000-000000000006', 'Rozwój poznawczy',
     'Spostrzeganie, pamięć, myślenie, wiedza o świecie', 6);

INSERT INTO skills (id, area_id, title, description, parent_recommendation, sort_order) VALUES
    -- Samodzielność
    ('50000000-0000-0000-0000-000000000101', 'd0000000-0000-0000-0000-000000000001',
     'Samodzielnie ubiera kurtkę',
     'Poproś dziecko o samodzielne ubranie się przed wyjściem do ogrodu.',
     'Ćwiczcie wspólnie zapinanie suwaka przy codziennym ubieraniu — bez pośpiechu, z pochwałą za próby.', 1),
    ('50000000-0000-0000-0000-000000000102', 'd0000000-0000-0000-0000-000000000001',
     'Korzysta samodzielnie z toalety',
     'Obserwacja w ciągu dnia.',
     'Utrwalajcie rutynę korzystania z toalety i mycia rąk.', 2),
    ('50000000-0000-0000-0000-000000000103', 'd0000000-0000-0000-0000-000000000001',
     'Zapina guziki',
     'Zabawa w przebieranie lalki lub własny sweterek.',
     'Wybierajcie ubrania z dużymi guzikami i ćwiczcie zapinanie wspólnie, bez pośpiechu.', 3),
    ('50000000-0000-0000-0000-000000000104', 'd0000000-0000-0000-0000-000000000001',
     'Samodzielnie je posiłki',
     'Obserwacja podczas posiłków.',
     'Pozwalajcie dziecku jeść samodzielnie, nawet jeśli trwa to dłużej.', 4),
    ('50000000-0000-0000-0000-000000000105', 'd0000000-0000-0000-0000-000000000001',
     'Sprząta po sobie zabawki',
     'Obserwacja po zakończonej zabawie.',
     'Wprowadźcie w domu stały rytuał sprzątania zabawek przed snem.', 5),

    -- Motoryka mała
    ('50000000-0000-0000-0000-000000000201', 'd0000000-0000-0000-0000-000000000002',
     'Trzyma poprawnie kredkę',
     'Obserwacja podczas rysowania.',
     'Krótkie kredki i pastele naturalnie wymuszają poprawny chwyt trzema palcami.', 1),
    ('50000000-0000-0000-0000-000000000202', 'd0000000-0000-0000-0000-000000000002',
     'Wycina nożyczkami po linii',
     'Zadanie plastyczne: wycinanie prostej, potem falowanej linii.',
     'Bezpieczne nożyczki dziecięce i wspólne wycinanki w domu.', 2),
    ('50000000-0000-0000-0000-000000000203', 'd0000000-0000-0000-0000-000000000002',
     'Lepi proste kształty z plasteliny',
     'Zadanie: kulka, wałek, placek.',
     'Zabawy plasteliną, ciastoliną lub masą solną wzmacniają dłonie.', 3),
    ('50000000-0000-0000-0000-000000000204', 'd0000000-0000-0000-0000-000000000002',
     'Rysuje po śladzie',
     'Karta pracy ze szlaczkami.',
     'Proste szlaczki i labirynty — kilka minut dziennie wystarczy.', 4),

    -- Motoryka duża
    ('50000000-0000-0000-0000-000000000301', 'd0000000-0000-0000-0000-000000000003',
     'Skacze obunóż',
     'Zabawa ruchowa: przeskakiwanie przez linię.',
     'Zabawy w podskoki: gra w klasy, skakanie przez skakankę trzymaną nisko.', 1),
    ('50000000-0000-0000-0000-000000000302', 'd0000000-0000-0000-0000-000000000003',
     'Utrzymuje równowagę na jednej nodze',
     'Zabawa „bocian" — stanie na jednej nodze 5 sekund.',
     'Zabawy równoważne: chodzenie po krawężniku, „bocian", jazda na hulajnodze.', 2),
    ('50000000-0000-0000-0000-000000000303', 'd0000000-0000-0000-0000-000000000003',
     'Rzuca i łapie piłkę',
     'Zabawa w parach z piłką średniej wielkości.',
     'Codzienne krótkie zabawy piłką — rzuty do celu, turlanie, łapanie.', 3),

    -- Mowa i komunikacja
    ('50000000-0000-0000-0000-000000000401', 'd0000000-0000-0000-0000-000000000004',
     'Buduje zdania złożone',
     'Rozmowa o obrazku, opowiadanie historyjki.',
     'Codzienne czytanie i rozmowa o przeczytanej bajce — pytania otwarte.', 1),
    ('50000000-0000-0000-0000-000000000402', 'd0000000-0000-0000-0000-000000000004',
     'Wypowiada się na temat',
     'Rozmowa kierowana w kręgu.',
     'Zachęcajcie dziecko do opowiadania o swoim dniu, słuchajcie bez przerywania.', 2),
    ('50000000-0000-0000-0000-000000000403', 'd0000000-0000-0000-0000-000000000004',
     'Recytuje krótkie wierszyki',
     'Nauka wierszyka na pamięć w grupie.',
     'Wspólna nauka krótkich rymowanek, śpiewanie piosenek.', 3),
    ('50000000-0000-0000-0000-000000000404', 'd0000000-0000-0000-0000-000000000004',
     'Dzieli wyrazy na sylaby',
     'Zabawa w klaskanie sylab.',
     'Zabawy słowne: wyklaskiwanie sylab imion domowników.', 4),

    -- Rozwój społeczny
    ('50000000-0000-0000-0000-000000000501', 'd0000000-0000-0000-0000-000000000005',
     'Bawi się zgodnie z innymi dziećmi',
     'Obserwacja zabawy swobodnej.',
     'Organizujcie spotkania z rówieśnikami, ćwiczcie czekanie na swoją kolej w grach.', 1),
    ('50000000-0000-0000-0000-000000000502', 'd0000000-0000-0000-0000-000000000005',
     'Stosuje formy grzecznościowe',
     'Obserwacja w sytuacjach codziennych.',
     'Modelujcie w domu „proszę", „dziękuję", „przepraszam" — dzieci uczą się przez naśladowanie.', 2),
    ('50000000-0000-0000-0000-000000000503', 'd0000000-0000-0000-0000-000000000005',
     'Radzi sobie z porażką w grze',
     'Obserwacja podczas gier z regułami.',
     'Grajcie w proste gry planszowe; nazywajcie emocje i pokazujcie, że przegrana jest w porządku.', 3),

    -- Rozwój poznawczy
    ('50000000-0000-0000-0000-000000000601', 'd0000000-0000-0000-0000-000000000006',
     'Rozpoznaje i nazywa kolory',
     'Zabawa w sortowanie klocków według koloru.',
     'Nazywajcie kolory przy codziennych czynnościach: ubieraniu, zakupach, spacerze.', 1),
    ('50000000-0000-0000-0000-000000000602', 'd0000000-0000-0000-0000-000000000006',
     'Liczy w zakresie 10',
     'Przeliczanie klocków, schodów, dzieci w kole.',
     'Liczcie wspólnie przedmioty codziennego użytku — schody, sztućce, zabawki.', 2),
    ('50000000-0000-0000-0000-000000000603', 'd0000000-0000-0000-0000-000000000006',
     'Układa historyjkę obrazkową',
     'Zadanie: ułożenie 3–4 obrazków w kolejności zdarzeń.',
     'Wspólne oglądanie książeczek i pytania „co było najpierw, co potem?".', 3),
    ('50000000-0000-0000-0000-000000000604', 'd0000000-0000-0000-0000-000000000006',
     'Rozpoznaje swoje imię w druku',
     'Karteczki z imionami w szatni.',
     'Podpisujcie prace dziecka drukowanymi literami, pokazujcie imię w książkach.', 4);

-- Przypisanie umiejętności do grup wiekowych
INSERT INTO skill_age_groups (skill_id, age_group_id)
SELECT m.skill_id::uuid, a.age_group_id::uuid FROM (VALUES
    -- Samodzielność
    ('50000000-0000-0000-0000-000000000101', ARRAY['3','4']),
    ('50000000-0000-0000-0000-000000000102', ARRAY['3','4','5']),
    ('50000000-0000-0000-0000-000000000103', ARRAY['4','5','6']),
    ('50000000-0000-0000-0000-000000000104', ARRAY['3','4']),
    ('50000000-0000-0000-0000-000000000105', ARRAY['3','4','5','6']),
    -- Motoryka mała
    ('50000000-0000-0000-0000-000000000201', ARRAY['3','4','5']),
    ('50000000-0000-0000-0000-000000000202', ARRAY['4','5','6']),
    ('50000000-0000-0000-0000-000000000203', ARRAY['3','4']),
    ('50000000-0000-0000-0000-000000000204', ARRAY['4','5','6']),
    -- Motoryka duża
    ('50000000-0000-0000-0000-000000000301', ARRAY['3','4']),
    ('50000000-0000-0000-0000-000000000302', ARRAY['4','5','6']),
    ('50000000-0000-0000-0000-000000000303', ARRAY['3','4','5','6']),
    -- Mowa i komunikacja
    ('50000000-0000-0000-0000-000000000401', ARRAY['4','5','6']),
    ('50000000-0000-0000-0000-000000000402', ARRAY['3','4','5','6']),
    ('50000000-0000-0000-0000-000000000403', ARRAY['3','4','5']),
    ('50000000-0000-0000-0000-000000000404', ARRAY['5','6']),
    -- Rozwój społeczny
    ('50000000-0000-0000-0000-000000000501', ARRAY['3','4','5','6']),
    ('50000000-0000-0000-0000-000000000502', ARRAY['3','4','5','6']),
    ('50000000-0000-0000-0000-000000000503', ARRAY['5','6']),
    -- Rozwój poznawczy
    ('50000000-0000-0000-0000-000000000601', ARRAY['3','4']),
    ('50000000-0000-0000-0000-000000000602', ARRAY['5','6']),
    ('50000000-0000-0000-0000-000000000603', ARRAY['4','5','6']),
    ('50000000-0000-0000-0000-000000000604', ARRAY['4','5','6'])
) AS m(skill_id, ages)
CROSS JOIN LATERAL unnest(m.ages) AS age(n)
JOIN (VALUES
    ('3', 'a0000000-0000-0000-0000-000000000003'),
    ('4', 'a0000000-0000-0000-0000-000000000004'),
    ('5', 'a0000000-0000-0000-0000-000000000005'),
    ('6', 'a0000000-0000-0000-0000-000000000006')
) AS a(n, age_group_id) ON a.n = age.n;
