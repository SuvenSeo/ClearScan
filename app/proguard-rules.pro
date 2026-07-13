# Room: keep entities, DAOs, and database class used via reflection.
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * extends androidx.room.RoomDatabase
-keep class com.ardeno.clearscan.data.db.** { *; }

# PDFBox Android (PDF import/render/export).
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# Tesseract OCR (tessdata + native bindings).
-keep class com.googlecode.tesseract.android.** { *; }

# ML Kit document scanner (GMS result/options types).
-keep class com.google.mlkit.vision.documentscanner.** { *; }
-dontwarn com.google.mlkit.**
