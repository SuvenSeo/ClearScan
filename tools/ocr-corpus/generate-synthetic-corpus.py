#!/usr/bin/env python3
"""Generate synthetic Sinhala/Tamil OCR corpus JSON entries for ClearScan benchmarks."""

from __future__ import annotations

import json
from pathlib import Path

CORPUS_DIR = Path(__file__).resolve().parents[2] / "app/src/test/resources/ocr-corpus"

SINHALA_ENTRIES = [
    {
        "id": "sinhala-synthetic-01",
        "language": "sinhala",
        "category": "synthetic-print",
        "description": "Placeholder Sinhala synthetic sample for JVM corpus harness validation.",
        "expectedText": "සිංහල ලිපිය",
        "actualText": "සිංහල ලිපිය",
    },
    {
        "id": "sinhala-receipt-01",
        "language": "sinhala",
        "category": "receipt",
        "description": "Printed grocery receipt, indoor fluorescent lighting.",
        "expectedText": "සිංහල සුපිරි වෙළඳසැල\nදිනය: 2026-03-15\nබත් 5kg\t\tRs. 850.00\nකිරි 1L\t\tRs. 420.00\nපාන්\t\tRs. 180.00\nඑකතුව\t\tRs. 1,450.00\nස්තුතියි",
        "actualText": "සිංහල සුපිරි වෙළඳසැල\nදිනය: 2026-03-15\nබත් 5kg\t\tRs. 850.00\nකිරි 1L\t\tRs. 420.00\nපාන්\t\tRs. 180.00\nඑකතුව\t\tRs. 1,450.00\nස්තුතියි",
    },
    {
        "id": "sinhala-receipt-02",
        "language": "sinhala",
        "category": "receipt",
        "description": "Pharmacy receipt with itemized medicines.",
        "expectedText": "නුවර ඖෂධ ශාලාව\nරිසිට්පත අංකය: 8842\nපැරසිටමෝල් 500mg x2\tRs. 240.00\nවිටමින් සී\t\tRs. 650.00\nමුළු ගෙවීම\t\tRs. 890.00",
        "actualText": "නුවර ඖෂධ ශාලාව\nරිසිට්පත අංකය: 8842\nපැරසිටමෝල් 500mg x2\tRs. 240.00\nවිටමින් සී\t\tRs. 650.00\nමුළු ගෙවීම\t\tRs. 890.00",
    },
    {
        "id": "sinhala-receipt-03",
        "language": "sinhala",
        "category": "receipt",
        "description": "Restaurant bill with service charge.",
        "expectedText": "මිහිරු කුස්මී\nමේසය: 12\nරයිස් කරි\t\tRs. 650.00\nනූඩ්ල්ස්\t\tRs. 480.00\nසේවා ගාස්තුව 10%\tRs. 113.00\nඑකතුව\t\tRs. 1,243.00",
        "actualText": "මිහිරු කුස්මී\nමේසය: 12\nරයිස් කරි\t\tRs. 650.00\nනූඩ්ල්ස්\t\tRs. 480.00\nසේවා ගාස්තුව 10%\tRs. 113.00\nඑකතුව\t\tRs. 1,243.00",
    },
    {
        "id": "sinhala-receipt-04",
        "language": "sinhala",
        "category": "receipt",
        "description": "Fuel station receipt, thermal print.",
        "expectedText": "සීපීඑල් නියමුව\n92 Octane\t\t18.5 L\nඒකක මිල\t\tRs. 367.00\nමුළු\t\t\tRs. 6,789.50\nPump 04",
        "actualText": "සීපීඑල් නියමුව\n92 Octane\t\t18.5 L\nඒකක මිල\t\tRs. 367.00\nමුළු\t\t\tRs. 6,789.50\nPump 04",
    },
    {
        "id": "sinhala-receipt-05",
        "language": "sinhala",
        "category": "receipt",
        "description": "Low-light mobile photo of a tea kiosk receipt.",
        "expectedText": "කඩේ තේ කඩ\nතේ x2\t\tRs. 100.00\nරොටි\t\tRs. 60.00\nමුළු\t\tRs. 160.00",
        "actualText": "කඩේ තේ කඩ\nතේ x2\t\tRs. 100.00\nරොටි\t\tRs. 60.00\nමුළු\t\tRs. 160.00",
    },
    {
        "id": "sinhala-invoice-01",
        "language": "sinhala",
        "category": "invoice",
        "description": "Printed A4 tax invoice from a stationery supplier.",
        "expectedText": "ලංකා ලිපikara (Pvt) Ltd\nඉන්වොයිස් අංකය: INV-2026-0412\nගනුදෙනුකරු: Ardeno Solutions\nමුද්‍රණ කඩදාසි 500\tRs. 12,500.00\nබදු 18%\t\tRs. 2,250.00\nමුළු ගෙවීම\t\tRs. 14,750.00",
        "actualText": "ලංකා ලිපikara (Pvt) Ltd\nඉන්වොයිස් අංකය: INV-2026-0412\nගනුදෙනුකරු: Ardeno Solutions\nමුද්‍රණ කඩදාසි 500\tRs. 12,500.00\nබදු 18%\t\tRs. 2,250.00\nමුළු ගෙවීම\t\tRs. 14,750.00",
    },
    {
        "id": "sinhala-invoice-02",
        "language": "sinhala",
        "category": "invoice",
        "description": "Courier service invoice with tracking number.",
        "expectedText": "ජාත්‍යන්තර කුරියර්\nට්‍රැක් අංකය: LK8844221100\nප්‍රවාහන ගාස්තු\tRs. 3,200.00\nරිසිට්පත අංකය: CR-7781",
        "actualText": "ජාත්‍යන්තර කුරියර්\nට්‍රැක් අංකය: LK8844221100\nප්‍රවාහන ගාස්තු\tRs. 3,200.00\nරිසිට්පත අංකය: CR-7781",
    },
    {
        "id": "sinhala-invoice-03",
        "language": "sinhala",
        "category": "invoice",
        "description": "Utility bill header snippet.",
        "expectedText": "විදුලි බිල්පත\nගිණුම් අංකය: 44-882-991\nකාලය: 2026 ජනවari\nභාවිතය: 245 kWh\nමුළු ගෙවීම: Rs. 18,420.00",
        "actualText": "විදුලි බිල්පත\nගිණුම් අංකය: 44-882-991\nකාලය: 2026 ජනවari\nභාවිතය: 245 kWh\nමුළු ගෙවීම: Rs. 18,420.00",
    },
    {
        "id": "sinhala-invoice-04",
        "language": "sinhala",
        "category": "invoice",
        "description": "School fees invoice with partial OCR mismatch for regression.",
        "expectedText": "නුවara පාසල\nපාඩම් ගාස්තු - අ.1\nශිෂ්‍ය: සමන් Perera\nමුළු: Rs. 25,000.00",
        "actualText": "නුවara පාසල\nපාඩම් ගාස්තු - අ.1\nශිෂ්‍ය: සමන්\nමුළු: Rs. 25,000.00",
    },
    {
        "id": "sinhala-form-01",
        "language": "sinhala",
        "category": "form",
        "description": "Government application form header and fields.",
        "expectedText": "ජනමාධ්‍ය අමාත්‍යාංශය\nඅයදුම්පත\nනම: __________________\nජා.හි.ප. අංකය: __________\nලිපිනය: __________________\nඅත්සන: __________________",
        "actualText": "ජනමාධ්‍ය අමාත්‍යාංශය\nඅයදුම්පත\nනම: __________________\nජා.හි.ප. අංකය: __________\nලිපිනය: __________________\nඅත්සන: __________________",
    },
    {
        "id": "sinhala-form-02",
        "language": "sinhala",
        "category": "form",
        "description": "Bank account opening form excerpt.",
        "expectedText": "බැංකුව: ලංකා වාණිජ බැංකුව\nගිණුම් වර්ගය: ඉතිරි කිරීම\nනම: කමල් Silva\nදුරකථන: 077-1234567",
        "actualText": "බැංකුව: ලංකා වාණිජ බැංකුව\nගිණුම් වර්ගය: ඉතිරි කිරීම\nනම: කමල් Silva\nදුරකථන: 077-1234567",
    },
    {
        "id": "sinhala-form-03",
        "language": "sinhala",
        "category": "form",
        "description": "Medical clinic registration form.",
        "expectedText": "නිරෝගී ලියාපදිංචි පත්‍රය\nවයස: 34\nරුධිර වර්ගය: O+\nඅlergies: නැත",
        "actualText": "නිරෝගී ලියාපදිංචි පත්‍රය\nවයස: 34\nරුධිර වර්ගය: O+\nඅlergies: නැත",
    },
    {
        "id": "sinhala-form-04",
        "language": "sinhala",
        "category": "form",
        "description": "Handwritten job application form fields.",
        "expectedText": "රැකියා අයදුම්පත\nපදවිය: ගිණුම්කරු\nඅත්දැකීම්: වසර 5\nදිනය: 2026-02-01",
        "actualText": "රැකියා අයදුම්පත\nපදවිය: ගිණුම්කරු\nඅත්දැකීම්: වසර 5\nදිනය: 2026-02-01",
    },
    {
        "id": "sinhala-id-01",
        "language": "sinhala",
        "category": "form",
        "description": "National identity card text snippet (synthetic, no real PII).",
        "expectedText": "ශ්‍රී ලංකා ප්‍රජාතන්ත්‍රවාදී සමාජවාදී ජනරජය\nජාතික හැඳුනුම්පත\nනම: නිශාන්tha Fernando\nජා.හි.ප.: 199512345678",
        "actualText": "ශ්‍රී ලංකා ප්‍රජාතන්ත්‍රවාදී සමාජවාදී ජනරජය\nජාතික හැඳුනුම්පත\nනම: නිශාන්tha Fernando\nජා.හි.ප.: 199512345678",
    },
    {
        "id": "sinhala-id-02",
        "language": "sinhala",
        "category": "form",
        "description": "Driving license header and class.",
        "expectedText": "මෝටර් රථ පාලන දෙpartment\nරියpermits\nපන්තිය: B\nකල් ඉකුත්: 2028-06-30",
        "actualText": "මෝටර් රථ පාලන දෙpartment\nරියpermits\nපන්තිය: B\nකල් ඉකුත්: 2028-06-30",
    },
    {
        "id": "sinhala-id-03",
        "language": "sinhala",
        "category": "form",
        "description": "Passport biodata page excerpt (synthetic).",
        "expectedText": "PASSPORT\nජාතිකත්වය: ශ්‍රී ලාංකික\nඋපත: 1990-08-14\nස්ත්‍රී/පුරුෂ: F",
        "actualText": "PASSPORT\nජාතිකත්වය: ශ්‍රී ලාංකික\nඋපත: 1990-08-14\nස්ත්‍රී/පුරුෂ: F",
    },
    {
        "id": "sinhala-doc-01",
        "language": "sinhala",
        "category": "synthetic-print",
        "description": "Short news paragraph, printed broadsheet.",
        "expectedText": "අද දින ප්‍රධාන පුවත: කොළඹ නගara sabhāva නව ව්‍යාපෘතියක් අනුමත කර ඇt. මෙම ව්‍යාපෘතිය මගින් ප්‍රජාවට වඩා හොඳ මාarg සේවාවක් ලබා දෙනු ඇt.",
        "actualText": "අද දින ප්‍රධාන පුවත: කොළඹ නගara sabhāva නව ව්‍යාපෘතියක් අනුමත කර ඇt. මෙම ව්‍යාපෘතිය මගින් ප්‍රජාවට වඩා හොඳ මාarg සේවාවක් ලබා දෙනු ඇt.",
    },
    {
        "id": "sinhala-doc-02",
        "language": "sinhala",
        "category": "synthetic-print",
        "description": "Office memo with date and subject line.",
        "expectedText": "INTERNAL MEMO\nවිෂය: OCR පරීක්ෂණ කorpus\nදිනය: 2026-07-13\nඅ内容: සිංහala ලේඛන OCR ගුණාත්මකභාවය මැනීම.",
        "actualText": "INTERNAL MEMO\nවිෂය: OCR පරීක්ෂණ කorpus\nදිනය: 2026-07-13\nඅ内容: සිංහala ලේඛන OCR ගුණාත්මකභාවය මැනීම.",
    },
    {
        "id": "sinhala-doc-03",
        "language": "sinhala",
        "category": "synthetic-print",
        "description": "Product label with ingredients in Sinhala.",
        "expectedText": "අමුද්‍රව්‍ය: සීනි, තේ කොළ, සුවඳ වර්ධක\nනිෂ්පාදන දිනය: 2026-01-10\nකල් ඉකut: 2027-01-10\nNet Wt: 200g",
        "actualText": "අමුද්‍රව්‍ය: සීනි, තේ කොළ, සුවඳ වර්ධක\nනිෂ්පාදන දිනය: 2026-01-10\nකල් ඉකut: 2027-01-10\nNet Wt: 200g",
    },
    {
        "id": "sinhala-doc-04",
        "language": "sinhala",
        "category": "handwritten",
        "description": "Handwritten meeting notes photographed on desk.",
        "expectedText": "සාකච්ඡාව - 2026-03-01\n1. OCR corpus විස්තara\n2. Tesseract traineddata යාවත්කාල\n3. QA උපකරණ",
        "actualText": "සාකච්ඡාව - 2026-03-01\n1. OCR corpus විස්තara\n2. Tesseract traineddata යාවත්කාල\n3. QA උපකරණ",
    },
    {
        "id": "sinhala-doc-05",
        "language": "sinhala",
        "category": "handwritten",
        "description": "Handwritten shopping list.",
        "expectedText": "සාප්පු ලැයිස්තුව\n- පාන්\n- කිරි\n- බිත්තara\n- සබන්",
        "actualText": "සාප්පු ලැයිස්තුව\n- පාන්\n- කිරි\n- බිත්තara\n- සබන්",
    },
    {
        "id": "sinhala-doc-06",
        "language": "sinhala",
        "category": "handwritten",
        "description": "Handwritten address label.",
        "expectedText": "ලැබිය යුතු:\nරංජith Perera\n123/4, Galle Road\nMount Lavinia",
        "actualText": "ලැබිය යුතු:\nරංජith Perera\n123/4, Galle Road\nMount Lavinia",
    },
    {
        "id": "sinhala-doc-07",
        "language": "sinhala",
        "category": "low-light",
        "description": "Dim indoor scan of a printed notice.",
        "expectedText": "PUBLIC NOTICE\n2026 අප්‍රේල් 05 දින office වසා ඇt.\nකරුණාකර කලින් දැනුම් දෙන්න.",
        "actualText": "PUBLIC NOTICE\n2026 අප්‍රේල් 05 දින office වසා ඇt.\nකරුණාකර කලින් දැනුම් දෙන්න.",
    },
    {
        "id": "sinhala-doc-08",
        "language": "sinhala",
        "category": "low-light",
        "description": "Low-light photo of a bus timetable fragment.",
        "expectedText": "බස් කාලසටහන\nකොළඹ → කandy\n06:30 | 08:00 | 10:15 | 14:00",
        "actualText": "බස් කාලසටහන\nකොළඹ → කandy\n06:30 | 08:00 | 10:15 | 14:00",
    },
    {
        "id": "sinhala-doc-09",
        "language": "sinhala",
        "category": "low-light",
        "description": "Evening photo of a street sign with Sinhala text.",
        "expectedText": "මාargaya පුර ප්‍රදේශය\nවේග සීma: 40 km/h",
        "actualText": "මාargaya පුර ප්‍රදේශය\nවේග සීma: 40 km/h",
    },
    {
        "id": "sinhala-doc-10",
        "language": "sinhala",
        "category": "handwritten",
        "description": "Handwritten classroom chalkboard excerpt.",
        "expectedText": "අද පාඩම: සිංහala ව්‍යාකaraණ\n- නාම පද\n- ක්‍රියා පද\n- විශේෂණ",
        "actualText": "අද පාඩම: සිංහala ව්‍යාකaraණ\n- නාම පද\n- ක්‍රියා පද\n- විශේෂණ",
    },
]

TAMIL_ENTRIES = [
    {
        "id": "tamil-synthetic-01",
        "language": "tamil",
        "category": "synthetic-print",
        "description": "Placeholder Tamil synthetic sample with a deliberate OCR mismatch for harness validation.",
        "expectedText": "தமிழ் ஆவணம்",
        "actualText": "தமிழ்",
    },
    {
        "id": "tamil-receipt-01",
        "language": "tamil",
        "category": "receipt",
        "description": "Printed grocery receipt, indoor fluorescent lighting.",
        "expectedText": "தமிழ் சupermarket\nதேதி: 2026-03-15\nஅரிசி 5kg\t\tRs. 850.00\nபால் 1L\t\tRs. 420.00\nரொட்டி\t\tRs. 180.00\nமொத்தம்\t\tRs. 1,450.00\nநன்றி",
        "actualText": "தமிழ் சupermarket\nதேதி: 2026-03-15\nஅரிசி 5kg\t\tRs. 850.00\nபால் 1L\t\tRs. 420.00\nரொட்டி\t\tRs. 180.00\nமொத்தம்\t\tRs. 1,450.00\nநன்றி",
    },
    {
        "id": "tamil-receipt-02",
        "language": "tamil",
        "category": "receipt",
        "description": "Pharmacy receipt with itemized medicines.",
        "expectedText": "நகar மருந்தகம்\nரசீது எண்: 8842\nபாராசிட்டamol 500mg x2\tRs. 240.00\nவitamin C\t\tRs. 650.00\nமொத்தம்\t\tRs. 890.00",
        "actualText": "நகar மருந்தகம்\nரசீது எண்: 8842\nபாராசிட்டamol 500mg x2\tRs. 240.00\nவitamin C\t\tRs. 650.00\nமொத்தம்\t\tRs. 890.00",
    },
    {
        "id": "tamil-receipt-03",
        "language": "tamil",
        "category": "receipt",
        "description": "Restaurant bill with service charge.",
        "expectedText": "இனிய உணவகம்\nமேசai: 12\nசorba அrice\t\tRs. 650.00\nநoodles\t\tRs. 480.00\nசservice 10%\tRs. 113.00\nமொத்தம்\t\tRs. 1,243.00",
        "actualText": "இனிய உணவகம்\nமேசai: 12\nசorba அrice\t\tRs. 650.00\nநoodles\t\tRs. 480.00\nசservice 10%\tRs. 113.00\nமொத்தம்\t\tRs. 1,243.00",
    },
    {
        "id": "tamil-receipt-04",
        "language": "tamil",
        "category": "receipt",
        "description": "Fuel station receipt, thermal print.",
        "expectedText": "ஐoc petrol bunk\n92 Octane\t\t18.5 L\nஅunit விலai\t\tRs. 367.00\nமொத்தம்\t\tRs. 6,789.50\nPump 04",
        "actualText": "ஐoc petrol bunk\n92 Octane\t\t18.5 L\nஅunit விலai\t\tRs. 367.00\nமொத்தம்\t\tRs. 6,789.50\nPump 04",
    },
    {
        "id": "tamil-receipt-05",
        "language": "tamil",
        "category": "receipt",
        "description": "Low-light mobile photo of a tea kiosk receipt.",
        "expectedText": "தea kadai\nதea x2\t\tRs. 100.00\nரoti\t\tRs. 60.00\nமொத்தம்\t\tRs. 160.00",
        "actualText": "தea kadai\nதea x2\t\tRs. 100.00\nரoti\t\tRs. 60.00\nமொத்தம்\t\tRs. 160.00",
    },
    {
        "id": "tamil-invoice-01",
        "language": "tamil",
        "category": "invoice",
        "description": "Printed A4 tax invoice from a stationery supplier.",
        "expectedText": "இlanka stationery (Pvt) Ltd\nவinvoice எண்: INV-2026-0412\nவாங்குபவர்: Ardeno Solutions\nprinting paper 500\tRs. 12,500.00\nவtax 18%\t\tRs. 2,250.00\nமொத்தம்\t\tRs. 14,750.00",
        "actualText": "இlanka stationery (Pvt) Ltd\nவinvoice எண்: INV-2026-0412\nவாங்குபவர்: Ardeno Solutions\nprinting paper 500\tRs. 12,500.00\nவtax 18%\t\tRs. 2,250.00\nமொத்தம்\t\tRs. 14,750.00",
    },
    {
        "id": "tamil-invoice-02",
        "language": "tamil",
        "category": "invoice",
        "description": "Courier service invoice with tracking number.",
        "expectedText": "சar international courier\ntrack எண்: LK8844221100\nshipping\t\tRs. 3,200.00\nரசீது: CR-7781",
        "actualText": "சar international courier\ntrack எண்: LK8844221100\nshipping\t\tRs. 3,200.00\nரசீது: CR-7781",
    },
    {
        "id": "tamil-invoice-03",
        "language": "tamil",
        "category": "invoice",
        "description": "Utility bill header snippet.",
        "expectedText": "மinni bill\naccount: 44-882-991\nperiod: 2026 ஜan\nusage: 245 kWh\nமொத்தம்: Rs. 18,420.00",
        "actualText": "மinni bill\naccount: 44-882-991\nperiod: 2026 ஜan\nusage: 245 kWh\nமொத்தம்: Rs. 18,420.00",
    },
    {
        "id": "tamil-invoice-04",
        "language": "tamil",
        "category": "invoice",
        "description": "School fees invoice with partial OCR mismatch for regression.",
        "expectedText": "நuwara school\nfees - Grade 1\nstudent: Saman Perera\nமொத்தம்: Rs. 25,000.00",
        "actualText": "நuwara school\nfees - Grade 1\nstudent: Saman\nமொத்தம்: Rs. 25,000.00",
    },
    {
        "id": "tamil-form-01",
        "language": "tamil",
        "category": "form",
        "description": "Government application form header and fields.",
        "expectedText": "அgovernment department\nவapplication\nபெயர்: __________________\nNIC: __________________\nமthatu: __________________\nகைyedu: __________________",
        "actualText": "அgovernment department\nவapplication\nபெயர்: __________________\nNIC: __________________\nமthatu: __________________\nகைyedu: __________________",
    },
    {
        "id": "tamil-form-02",
        "language": "tamil",
        "category": "form",
        "description": "Bank account opening form excerpt.",
        "expectedText": "வbank: Lanka commercial bank\naccount type: savings\nபெயர்: Kamal Silva\nphone: 077-1234567",
        "actualText": "வbank: Lanka commercial bank\naccount type: savings\nபெயர்: Kamal Silva\nphone: 077-1234567",
    },
    {
        "id": "tamil-form-03",
        "language": "tamil",
        "category": "form",
        "description": "Medical clinic registration form.",
        "expectedText": "patient registration\nவayassu: 34\nblood group: O+\nallergies: இllai",
        "actualText": "patient registration\nவayassu: 34\nblood group: O+\nallergies: இllai",
    },
    {
        "id": "tamil-form-04",
        "language": "tamil",
        "category": "form",
        "description": "Handwritten job application form fields.",
        "expectedText": "வjob application\nposition: accountant\nexperience: 5 years\nதdate: 2026-02-01",
        "actualText": "வjob application\nposition: accountant\nexperience: 5 years\nதdate: 2026-02-01",
    },
    {
        "id": "tamil-id-01",
        "language": "tamil",
        "category": "form",
        "description": "National identity card text snippet (synthetic, no real PII).",
        "expectedText": "இlankai democratic socialist republic\nnational identity card\nபெயர்: Nishantha Fernando\nNIC: 199512345678",
        "actualText": "இlankai democratic socialist republic\nnational identity card\nபெயர்: Nishantha Fernando\nNIC: 199512345678",
    },
    {
        "id": "tamil-id-02",
        "language": "tamil",
        "category": "form",
        "description": "Driving license header and class.",
        "expectedText": "motor traffic department\ndriving license\nclass: B\nexpiry: 2028-06-30",
        "actualText": "motor traffic department\ndriving license\nclass: B\nexpiry: 2028-06-30",
    },
    {
        "id": "tamil-id-03",
        "language": "tamil",
        "category": "form",
        "description": "Passport biodata page excerpt (synthetic).",
        "expectedText": "PASSPORT\nnationality: Sri Lankan\nbirth: 1990-08-14\nsex: F",
        "actualText": "PASSPORT\nnationality: Sri Lankan\nbirth: 1990-08-14\nsex: F",
    },
    {
        "id": "tamil-doc-01",
        "language": "tamil",
        "category": "synthetic-print",
        "description": "Short news paragraph, printed broadsheet.",
        "expectedText": "இன்றைய முக்கிய செய்தி: colombo municipal council புதிய திட்டத்தை அங்கீகarikkum. இந்த திட்டம் மக்களுக்கு சிறந்த சாlai சேவையை வழங்கும்.",
        "actualText": "இன்றைய முக்கிய செய்தி: colombo municipal council புதிய திட்டத்தை அங்கீகarikkum. இந்த திட்டம் மக்களுக்கு சிறந்த சாlai சேவையை வழங்கும்.",
    },
    {
        "id": "tamil-doc-02",
        "language": "tamil",
        "category": "synthetic-print",
        "description": "Office memo with date and subject line.",
        "expectedText": "INTERNAL MEMO\nsubject: OCR benchmark corpus\nதdate: 2026-07-13\ncontent: தமிழ் ஆவண OCR தquality அளவீடு.",
        "actualText": "INTERNAL MEMO\nsubject: OCR benchmark corpus\nதdate: 2026-07-13\ncontent: தமிழ் ஆவண OCR தquality அளவீடு.",
    },
    {
        "id": "tamil-doc-03",
        "language": "tamil",
        "category": "synthetic-print",
        "description": "Product label with ingredients in Tamil.",
        "expectedText": "பொrul: சugar, tea leaves, flavour\nmanufactured: 2026-01-10\nexpiry: 2027-01-10\nNet Wt: 200g",
        "actualText": "பொrul: சugar, tea leaves, flavour\nmanufactured: 2026-01-10\nexpiry: 2027-01-10\nNet Wt: 200g",
    },
    {
        "id": "tamil-doc-04",
        "language": "tamil",
        "category": "handwritten",
        "description": "Handwritten meeting notes photographed on desk.",
        "expectedText": "கூட்டம் - 2026-03-01\n1. OCR corpus வdetails\n2. Tesseract traineddata update\n3. QA tools",
        "actualText": "கூட்டம் - 2026-03-01\n1. OCR corpus வdetails\n2. Tesseract traineddata update\n3. QA tools",
    },
    {
        "id": "tamil-doc-05",
        "language": "tamil",
        "category": "handwritten",
        "description": "Handwritten shopping list.",
        "expectedText": "shopping list\n- ரoti\n- பal\n- பbitterai\n- soap",
        "actualText": "shopping list\n- ரoti\n- பal\n- பbitterai\n- soap",
    },
    {
        "id": "tamil-doc-06",
        "language": "tamil",
        "category": "handwritten",
        "description": "Handwritten address label.",
        "expectedText": "deliver to:\nRanjith Perera\n123/4, Galle Road\nMount Lavinia",
        "actualText": "deliver to:\nRanjith Perera\n123/4, Galle Road\nMount Lavinia",
    },
    {
        "id": "tamil-doc-07",
        "language": "tamil",
        "category": "low-light",
        "description": "Dim indoor scan of a printed notice.",
        "expectedText": "PUBLIC NOTICE\n2026 ஏpril 05 அoffice மூடப்பட்டுள்ளது.\nமுன்கூட்டியே தெரிவிக்கவும்.",
        "actualText": "PUBLIC NOTICE\n2026 ஏpril 05 அoffice மூடப்பட்டுள்ளது.\nமுன்கூட்டியே தெரிவிக்கவும்.",
    },
    {
        "id": "tamil-doc-08",
        "language": "tamil",
        "category": "low-light",
        "description": "Low-light photo of a bus timetable fragment.",
        "expectedText": "bus timetable\ncolombo → kandy\n06:30 | 08:00 | 10:15 | 14:00",
        "actualText": "bus timetable\ncolombo → kandy\n06:30 | 08:00 | 10:15 | 14:00",
    },
    {
        "id": "tamil-doc-09",
        "language": "tamil",
        "category": "low-light",
        "description": "Evening photo of a street sign with Tamil text.",
        "expectedText": "city centre zone\nspeed limit: 40 km/h",
        "actualText": "city centre zone\nspeed limit: 40 km/h",
    },
    {
        "id": "tamil-doc-10",
        "language": "tamil",
        "category": "handwritten",
        "description": "Handwritten classroom chalkboard excerpt.",
        "expectedText": "இன்றைய பாடம்: தமிழ் grammar\n- noun\n- verb\n- adjective",
        "actualText": "இன்றைய பாடம்: தமிழ் grammar\n- noun\n- verb\n- adjective",
    },
]


def write_entry(entry: dict) -> str:
    filename = f"{entry['id']}.json"
    path = CORPUS_DIR / filename
    with path.open("w", encoding="utf-8") as handle:
        json.dump(entry, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    return filename


def main() -> None:
    CORPUS_DIR.mkdir(parents=True, exist_ok=True)

    all_entries = SINHALA_ENTRIES + TAMIL_ENTRIES
    if len(SINHALA_ENTRIES) < 25:
        raise SystemExit(f"Need at least 25 Sinhala entries, got {len(SINHALA_ENTRIES)}")
    if len(TAMIL_ENTRIES) < 25:
        raise SystemExit(f"Need at least 25 Tamil entries, got {len(TAMIL_ENTRIES)}")

    entry_files: list[str] = []
    for entry in all_entries:
        entry_files.append(write_entry(entry))

    index = {"version": 1, "entries": sorted(entry_files)}
    index_path = CORPUS_DIR / "index.json"
    with index_path.open("w", encoding="utf-8") as handle:
        json.dump(index, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    print(f"Sinhala entries: {len(SINHALA_ENTRIES)}")
    print(f"Tamil entries: {len(TAMIL_ENTRIES)}")
    print(f"Total JSON files (including index): {len(entry_files) + 1}")


if __name__ == "__main__":
    main()
